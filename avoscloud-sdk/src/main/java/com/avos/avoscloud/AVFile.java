package com.avos.avoscloud;

import android.webkit.MimeTypeMap;

import com.alibaba.fastjson.annotation.JSONField;
import com.avos.avoscloud.upload.FileUploader;
import com.avos.avoscloud.upload.Uploader;
import com.avos.avoscloud.upload.UrlDirectlyUploader;

import org.json.JSONObject;

import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * AVFile is a local representation of a file that is saved to the AVOSCloud cloud.
 * </p>
 * <p>
 * The workflow is to construct a AVFile with data and optionally a filename. Then save it and set
 * it as a field on a AVObject.
 * </p>
 * Example:
 * <p/>
 * <pre>
 * AVFile file = new AVFile(&quot;hello&quot;.getBytes());
 * file.save();
 * AVObject object = new AVObject(&quot;TestObject&quot;);
 * object.put(&quot;file&quot;, file);
 * object.save();
 * </pre>
 */
public final class AVFile {

  /**
   * set http header while uploading.
   * @param key - header name
   * @param value - header value
   */
  public static void setUploadHeader(String key, String value) {
    FileUploader.setUploadHeader(key, value);
  }

  /**
   * 需要上传但是未上传的 dirty 会标记为 true
   */
  private boolean dirty;

  private String name;

  /**
   * AVFile 中的具体文件的来源有三种，一种是用户传过来的本地文件，一种是用户传过来的 byte[]，一种是通过 Url 下载来的
   * 此变量存储对应的“通过 Url 下载来的”
   */
  private String url;

  /**
   * 此变量存储对应的“用户传过来的本地文件”
   */
  private String localPath;

  /**
   * 此变量存储对应的“用户传过来的 byte[]”
   * 接收到 byte[] 后存储到本地文件中，localTmpFilePath 为本地缓存文件的地址
   */
  private String localTmpFilePath;
  private String objectId;
  private AVObject fileObject;
  private String bucket;
  private AVACL acl;

  transient private Uploader uploader;
  transient private AVFileDownloader downloader;
  // metadata for file,added by dennis<xzhuang@avos.com>,2013-09-06
  private final HashMap<String, Object> metaData = new HashMap<String, Object>();
  private static long MAX_FILE_BUF_SIZE = 1024 * 2014 * 4;
  public static String DEFAULTMIMETYPE = "application/octet-stream";
  private static final String FILE_SUM_KEY = "_checksum";
  static final String FILE_NAME_KEY = "_name";
  private static final String ELDERMETADATAKEYFORIOSFIX = "metadata";
  private static final String THUMBNAIL_FMT = "?imageView/%d/w/%d/h/%d/q/%d/format/%s";
  public static final String AVFILE_ENDPOINT = "files";

  public AVFile() {
    super();
    if (PaasClient.storageInstance().getDefaultACL() != null) {
      acl = new AVACL(PaasClient.storageInstance().getDefaultACL());
    }
  }

  AVObject getFileObject() {
    if (fileObject == null && !AVUtils.isBlankString(objectId)) {
      fileObject = AVObject.createWithoutData("_File", objectId);
    }
    return fileObject;
  }

  /**
   * Creates a new file from a byte array.
   *
   * @param data The file's data. 请注明文件名
   */
  @Deprecated
  public AVFile(byte[] data) {
    this(null, data);
  }


  /**
   * 创建一个基于网络文件的AVFile对象
   *
   * @param name     文件名
   * @param url      网络文件的url
   * @param metaData 网络文件的元信息，可以为空
   */
  public AVFile(String name, String url, Map<String, Object> metaData) {
    this(name, url, metaData, true);
  }

  protected AVFile(String name, String url, Map<String, Object> metaData, boolean external) {
    this();
    this.name = name;
    this.url = url;
    this.dirty = false;
    if (metaData != null) {
      this.metaData.putAll(metaData);
    }
    if (external) {
      this.metaData.put("__source", "external");
    }
  }


  /**
   * Creates a new file from a byte array and a name. Giving a name with a proper file extension
   * (e.g. ".png") is ideal because it allows AVOSCloud to deduce the content type of the file and
   * set appropriate HTTP headers when it is fetched.
   *
   * @param name The file's name, ideally with extension.
   * @param data The file's data
   */
  public AVFile(String name, byte[] data) {
    this();

    this.dirty = true;
    this.name = name;
    if (null != data) {
      String md5 = AVUtils.computeMD5(data);
      localTmpFilePath = AVFileDownloader.getAVFileCachePath() + md5;
      AVPersistenceUtils.saveContentToFile(data, new File(localTmpFilePath));
      this.metaData.put(FILE_SUM_KEY, md5);
      this.metaData.put("size", data.length);
    } else {
      this.metaData.put("size", 0);
    }

    AVUser currentUser = AVUser.getCurrentUser();
    this.metaData.put("owner", currentUser != null ? currentUser.getObjectId() : "");
    this.metaData.put(FILE_NAME_KEY, name);
  }

  /**
   * @hide For internal use only.
   */
  protected AVFile(String name, String url) {
    this(name, url, null);
  }

  /**
   * Returns the file object Id.
   *
   * @return The file object id.
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * Set the file objectId.
   *
   * @param objectId file object id.
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * Retrieve a AVFile object by object id from AVOSCloud in background.If the file is not found,it
   * will call the callback with java.io.FileNotFoundException.
   *
   * @param objectId The file object id.
   * @param cb       The GetFileCallback instance.
   * @since 1.3.4
   * @deprecated Please use #{@link #withObjectIdInBackground(String, GetFileCallback)}
   */
  @Deprecated
  static public void parseFileWithObjectIdInBackground(final String objectId,
                                                       final GetFileCallback<AVFile> cb) {
    withObjectIdInBackground(objectId, cb);
  }

  /**
   * Retrieve a AVFile object by object id from AVOSCloud in background.If the file is not found,it
   * will call the callback with java.io.FileNotFoundException.
   *
   * @param objectId The file object id.
   * @param cb       The GetFileCallback instance.
   * @since 2.0.2
   */
  public static void withObjectIdInBackground(final String objectId,
                                              final GetFileCallback<AVFile> cb) {
    AVQuery<AVObject> query = new AVQuery<AVObject>("_File");
    query.getInBackground(objectId, new GetCallback<AVObject>() {

      @Override
      public void done(AVObject object, AVException e) {
        if (e != null) {
          if (null != cb) {
            cb.internalDone(null, e);
          }
          return;
        }
        if (object != null && !AVUtils.isBlankString(object.getObjectId())) {
          AVFile file = createFileFromAVObject(object);
          if (cb != null) {
            cb.internalDone(file, null);
          }
        } else if (null != cb) {
          cb.internalDone(null, new AVException(AVException.OBJECT_NOT_FOUND,
            "Could not find file object by id:" + objectId));
        }
      }
    });
  }

  /**
   * Retrieve a AVFile object by object id from AVOSCloud.If the file is not found,it will throw
   * java.io.FileNotFoundException.
   *
   * @param objectId
   * @return
   * @throws AVException ,FileNotFoundException
   * @since 1.3.4
   * @deprecated Please use #{@link #withObjectId(String)}
   */
  @Deprecated
  static public AVFile parseFileWithObjectId(String objectId) throws AVException,
    FileNotFoundException {
    return withObjectId(objectId);
  }

  /**
   * Retrieve a AVFile object by object id from AVOSCloud.If the file is not found,it will throw
   * java.io.FileNotFoundException.
   *
   * @param objectId
   * @return
   * @throws AVException ,FileNotFoundException
   * @since 2.0.2
   */
  public static AVFile withObjectId(String objectId) throws AVException, FileNotFoundException {
    AVQuery<AVObject> query = new AVQuery<AVObject>("_File");
    AVObject object = query.get(objectId);
    if (object != null && !AVUtils.isBlankString(object.getObjectId())) {
      AVFile file = createFileFromAVObject(object);
      return file;
    } else {
      throw new FileNotFoundException("Could not find file object by id:" + objectId);
    }
  }

  /**
   * Construct a AVFile from AVObject.
   *
   * @param obj The AVObject.
   * @return The file object.
   * @since 1.3.4
   * @deprecated Please use #{@link #withAVObject(AVObject)}
   */
  @Deprecated
  static public AVFile parseFileWithAVObject(AVObject obj) {
    return withAVObject(obj);
  }

  /**
   * Construct a AVFile from AVObject.
   *
   * @param obj The object.
   * @return The file object.
   * @since 2.0.2
   */
  public static AVFile withAVObject(AVObject obj) {
    if (obj != null && !AVUtils.isBlankString(obj.getObjectId())) {
      AVFile file = createFileFromAVObject(obj);
      return file;
    } else {
      throw new IllegalArgumentException("Invalid AVObject.");
    }
  }

  private static AVFile createFileFromAVObject(AVObject object) {
    AVFile file = new AVFile(object.getObjectId(), object.getString("url"), null, false);
    if (object.getMap(ELDERMETADATAKEYFORIOSFIX) != null
      && !object.getMap(ELDERMETADATAKEYFORIOSFIX).isEmpty()) {
      file.metaData.putAll(object.getMap(ELDERMETADATAKEYFORIOSFIX));
    }
    if (object.getMap("metaData") != null) {
      file.metaData.putAll(object.getMap("metaData"));
    }
    file.setObjectId(object.getObjectId());
    file.fileObject = object;
    file.setBucket((String) object.get("bucket"));
    if (!file.metaData.containsKey(FILE_NAME_KEY)) {
      file.metaData.put(FILE_NAME_KEY, object.getString("name"));
    }
    return file;
  }

  /**
   * Creates a new file from local file path. Giving a name with a proper file extension (e.g.
   * ".png") is ideal because it allows AVOSCloud to deduce the content type of the file and set
   * appropriate HTTP headers when it is fetched.
   *
   * @param name                  The file's name, ideally with extension.
   * @param absoluteLocalFilePath The file's absolute path.
   * @deprecated Please use #{@link #withAbsoluteLocalPath(String, String)}
   */
  @Deprecated
  static public AVFile parseFileWithAbsoluteLocalPath(String name, String absoluteLocalFilePath)
    throws Exception {
    return withAbsoluteLocalPath(name, absoluteLocalFilePath);
  }

  /**
   * Creates a new file from local file path. Giving a name with a proper file extension (e.g.
   * ".png") is ideal because it allows AVOSCloud to deduce the content type of the file and set
   * appropriate HTTP headers when it is fetched.
   *
   * @param name                  The file's name, ideally with extension.
   * @param absoluteLocalFilePath The file's absolute path.
   * @since 2.0.2
   */
  public static AVFile withAbsoluteLocalPath(String name, String absoluteLocalFilePath)
    throws FileNotFoundException {
    return withFile(name, new File(absoluteLocalFilePath));
  }

  /**
   * Creates a new file from java.io.File object.
   *
   * @param name The file's name, ideally with extension.
   * @param file The file object.
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @since 1.3.4
   * @deprecated Please use #{@link #withFile(String, File)}
   */
  @Deprecated
  public static AVFile parseFileWithFile(String name, File file) throws FileNotFoundException {
    return withFile(name, file);
  }

  /**
   * Creates a new file from java.io.File object.
   *
   * @param name The file's name, ideally with extension.
   * @param file The file object.
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @since 2.0.2
   */
  public static AVFile withFile(String name, File file) throws FileNotFoundException {
    if (file == null) {
      throw new IllegalArgumentException("null file object.");
    }
    if (!file.exists() || !file.isFile()) {
      throw new FileNotFoundException();
    }

    AVFile avFile = new AVFile();
    avFile.setLocalPath(file.getAbsolutePath());
    avFile.setName(name);

    avFile.dirty = true;
    avFile.name = name;
    long fileSize = file.length();
    String fileMD5 = "";
    try {
      InputStream is = AVPersistenceUtils.getInputStreamFromFile(file);
      MessageDigest md = MessageDigest.getInstance("MD5");
      if (null != is) {
        byte buf[] = new byte[(int)MAX_FILE_BUF_SIZE];
        int len;
        while ((len = is.read(buf)) != -1) {
          md.update(buf, 0, len);
        }
        byte[] md5bytes = md.digest();
        fileMD5 = AVUtils.hexEncodeBytes(md5bytes);
        is.close();
      }
    } catch (Exception ex) {
      fileMD5 = "";
    }
    avFile.metaData.put("size", fileSize);
    avFile.metaData.put(FILE_SUM_KEY, fileMD5);

    AVUser currentUser = AVUser.getCurrentUser();
    avFile.metaData.put("owner", currentUser != null ? currentUser.getObjectId() : "");
    avFile.metaData.put(FILE_NAME_KEY, name);
    return avFile;
  }

  /**
   * Returns the file's metadata map.
   *
   * @return The file's metadata map.
   * @since 1.3.4
   */
  public HashMap<String, Object> getMetaData() {
    return metaData;
  }

  /**
   * Added meta data to file.
   *
   * @param key The meta data's key.
   * @param val The meta data's value.
   * @return The old metadata value.
   * @since 1.3.4
   */
  public Object addMetaData(String key, Object val) {
    return metaData.put(key, val);
  }

  /**
   * Returns the metadata value by key.
   *
   * @param key The metadata key
   * @return The value.
   * @since 1.3.4
   */
  public Object getMetaData(String key) {
    return this.metaData.get(key);
  }

  /**
   * Returns the file size in bytes.
   *
   * @return File size in bytes
   * @since 1.3.4
   */
  public int getSize() {
    Number size = (Number) getMetaData("size");
    if (size != null)
      return size.intValue();
    else
      return -1;
  }

  /**
   * Returns the file's owner
   *
   * @return File's owner
   * @since 1.3.4
   */
  public String getOwnerObjectId() {
    return (String) getMetaData("owner");
  }

  /**
   * Remove file meta data.
   *
   * @param key The meta data's key
   * @return The metadata value.
   * @since 1.3.4
   */
  public Object removeMetaData(String key) {
    return metaData.remove(key);
  }

  /**
   * Clear file metadata.
   */
  public void clearMetaData() {
    this.metaData.clear();
  }

  /**
   * The filename. Before save is called, this is just the filename given by the user (if any).
   * After save is called, that name gets prefixed with a unique identifier.
   *
   * @return The file's name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return
   */
  public String getOriginalName() {
    return (String) metaData.get(FILE_NAME_KEY);
  }

  /**
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @param url
   * @return
   * @deprecated replaced by {@link AVUtils#getMimeTypeFromLocalFile(String)} or
   * {@link AVUtils#getMimeTypeFromUrl(String)}
   */
  public static String getMimeType(String url) {
    String type = DEFAULTMIMETYPE;
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (extension != null) {
      MimeTypeMap mime = MimeTypeMap.getSingleton();
      type = mime.getMimeTypeFromExtension(extension);
    }
    if (type == null) {
      type = DEFAULTMIMETYPE;
    }
    return type;
  }

  /**
   * Whether the file still needs to be saved.
   *
   * @return Whether the file needs to be saved.
   */
  public boolean isDirty() {
    return this.dirty;
  }

  /**
   * Whether the file has available data.
   */
  @Deprecated
  public boolean isDataAvailable() {
    return AVUtils.isBlankString(localPath) || (!AVUtils.isBlankString(localTmpFilePath) &&
      new File(localTmpFilePath).exists());
  }

  /**
   * This returns the url of the file. It's only available after you save or after you get the file
   * from a AVObject.
   *
   * @return The url of the file.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns a thumbnail image url using QiNiu endpoints.
   *
   * @param scaleToFit
   * @param width
   * @param height
   * @return
   * @see #getThumbnailUrl(boolean, int, int, int, String)
   */
  public String getThumbnailUrl(boolean scaleToFit, int width, int height) {
    return this.getThumbnailUrl(scaleToFit, width, height, 100, "png");
  }

  /**
   * 返回缩略图URl 这个服务仅仅适用于保存在Qiniu的图片
   *
   * @param scaleToFit Whether to scale the image
   * @param width      The thumbnail image's width
   * @param height     The thumbnail image'height
   * @param quality    The thumbnail image quality in 1 - 100.
   * @param fmt        The thumbnail image format such as 'jpg','gif','png','tif' etc.
   * @return
   */
  public String getThumbnailUrl(boolean scaleToFit, int width, int height, int quality, String fmt) {
    if (!AVOSCloud.isCN() || AppRouterManager.isQCloudApp(AVOSCloud.applicationId)) {
      throw new UnsupportedOperationException("We only support this method for qiniu storage.");
    }
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Invalid width or height.");
    }
    if (quality < 1 || quality > 100) {
      throw new IllegalArgumentException("Invalid quality,valid range is 0-100.");
    }
    if (fmt == null || AVUtils.isBlankString(fmt.trim())) {
      fmt = "png";
    }
    int mode = scaleToFit ? 2 : 1;
    String resultUrl =
      this.getUrl() + String.format(THUMBNAIL_FMT, mode, width, height, quality, fmt);
    return resultUrl;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  void setLocalPath(String localPath) {
    this.localPath = localPath;
  }

  /**
   * Saves the file to the AVOSCloud cloud synchronously.
   *
   * @throws AVException
   */
  public void save() throws AVException {
    // 如果文件已经上传过，则不再进行上传
    if (AVUtils.isBlankString(objectId)) {
      cancelUploadIfNeed();
      final AVException[] avExceptions = new AVException[1];
      uploader = getUploader(null, null);

      if (null != avExceptions[0]) {
        throw avExceptions[0];
      }
      AVException exception = uploader.doWork();
      if (exception != null) {
        throw exception;
      }
    }
  }

  /**
   * Saves the file to the AVOSCloud cloud in a background thread. progressCallback is guaranteed to
   * be called with 100 before saveCallback is called.
   *
   * @param saveCallback     A SaveCallback that gets called when the save completes.
   * @param progressCallback A ProgressCallback that is called periodically with progress updates.
   */
  public synchronized void saveInBackground(final SaveCallback saveCallback,
                                            final ProgressCallback progressCallback) {
    if (AVUtils.isBlankString(objectId)) {
      cancelUploadIfNeed();
      uploader = getUploader(saveCallback, progressCallback);
      uploader.execute();
    } else {
      if (null != saveCallback) {
        saveCallback.internalDone(null);
      }
      if (null != progressCallback) {
        progressCallback.internalDone(100, null);
      }
    }
  }

  /**
   * Saves the file to the AVOSCloud cloud in a background thread.
   *
   * @param callback A SaveCallback that gets called when the save completes.
   */
  public void saveInBackground(SaveCallback callback) {
    saveInBackground(callback, null);
  }

  /**
   * Saves the file to the AVOSCloud cloud in a background thread.
   */
  public void saveInBackground() {
    saveInBackground(null);
  }

  /**
   * Synchronously gets the data for this object. You probably want to use
   * {@link #getDataInBackground(GetDataCallback)} instead
   * unless you're already in a background thread.
   *
   * Notice: for large file(above 20MB), it is dangerous to read whole data once time,
   * instead of, you should use {@link #getDataStream()} to read the content.
   *
   * @throws AVException
   */
  @Deprecated
  @JSONField(serialize = false)
  public byte[] getData() throws AVException {
    if (!AVUtils.isBlankString(localPath)) {
      return getLocalFileData();
    } else if (!AVUtils.isBlankString(localTmpFilePath)) {
      return getTmpFileData();
    } else if (!AVUtils.isBlankString(url)) {
      byte[] data = getCacheData();
      if (null != data) {
        return data;
      }
      if (!AVUtils.isConnected(AVOSCloud.applicationContext)) {
        throw new AVException(AVException.CONNECTION_FAILED, "Connection lost");
      } else {
        cancelDownloadIfNeed();
        downloader = new AVFileDownloader();
        AVException exception = downloader.doWork(getUrl());
        if (exception != null) {
          throw exception;
        }
        return getCacheData();
      }
    }
    return null;
  }

  /**
   * Synchronously gets the data input stream for this object. You probably want to use
   * {@link #getDataStreamInBackground(GetDataStreamCallback)} instead
   * unless you're already in a background thread.
   *
   * Notice: You need to close the input stream after reading content from it.
   *
   * @throws AVException
   */
  @JSONField(serialize = false)
  public InputStream getDataStream() throws AVException {
    String filePath = "";
    if(!AVUtils.isBlankString(localPath)) {
      filePath = localPath;
    } else if (!AVUtils.isBlankString(localTmpFilePath)) {
      filePath = localTmpFilePath;
    } else if (!AVUtils.isBlankString(url)) {
      File cacheFile = AVFileDownloader.getCacheFile(url);
      if (null == cacheFile || !cacheFile.exists()) {
        if (!AVUtils.isConnected(AVOSCloud.applicationContext)) {
          throw new AVException(AVException.CONNECTION_FAILED, "Connection lost");
        } else {
          cancelDownloadIfNeed();
          downloader = new AVFileDownloader();
          AVException exception = downloader.doWork(getUrl());
          if (exception != null) {
            throw exception;
          }
        }
      }
      filePath = cacheFile.getAbsolutePath();
    }
    if(!AVUtils.isBlankString(filePath)) {
      try {
        return AVPersistenceUtils.getInputStreamFromFile(new File(filePath));
      } catch (IOException e){
        throw new AVException(e);
      }
    }
    return null;
  }

  /**
   * Gets the data for this object in a background thread. progressCallback is guaranteed to be
   * called with 100 before dataCallback is called.
   *
   * Notice: for large file(above 20MB), it is dangerous to read whole data once time,
   * instead of, you should use {@link #getDataStreamInBackground(GetDataStreamCallback, ProgressCallback)} to read the content.
   *
   * @param dataCallback     A GetDataCallback that is called when the get completes.
   * @param progressCallback A ProgressCallback that is called periodically with progress updates.
   */
  public void getDataInBackground(final GetDataCallback dataCallback,
                                  final ProgressCallback progressCallback) {
    if (!AVUtils.isBlankString(localPath)) {
      if (null != progressCallback) {
        progressCallback.done(100);
      }
      if (dataCallback != null) {
        dataCallback.internalDone(getLocalFileData(), null);
      }
    } else if (!AVUtils.isBlankString(localTmpFilePath)) {
      if (null != progressCallback) {
        progressCallback.done(100);
      }
      if (dataCallback != null) {
        dataCallback.internalDone(getTmpFileData(), null);
      }
    } else if (!AVUtils.isBlankString(getUrl())) {
      cancelDownloadIfNeed();
      downloader = new AVFileDownloader(progressCallback, dataCallback);
      downloader.execute(getUrl());
    } else if (dataCallback != null) {
      dataCallback.internalDone(new AVException(AVException.INVALID_FILE_URL, ""));
    }
  }

  /**
   * Gets the data for this object in a background thread.
   *
   * Notice: for large file(above 20MB), it is dangerous to read whole data once time,
   * instead of, you should use {@link #getDataStreamInBackground(GetDataStreamCallback)} to read the content.
   *
   * @param dataCallback A GetDataCallback that is called when the get completes.
   */
  public void getDataInBackground(GetDataCallback dataCallback) {
    getDataInBackground(dataCallback, null);
  }

  /**
   * Gets the data input stream for this object in a background thread.
   * Notice: You need to close the input stream after reading content from it.
   *
   * @param callback A GetDataStreamCallback that is called when the get completes.
   */
  public void getDataStreamInBackground(GetDataStreamCallback callback) {
    getDataStreamInBackground(callback, null);
  }

  /**
   * Gets the data input stream for this object in a background thread. progressCallback is guaranteed to be
   * called with 100 before dataStreamCallback is called.
   * Caution: You need to close the input stream after reading content from it.
   *
   * @param callback     A GetDataStreamCallback that is called when the get completes.
   * @param progressCallback A ProgressCallback that is called periodically with progress updates.
   */
  public void getDataStreamInBackground(final GetDataStreamCallback callback, final ProgressCallback progressCallback) {
    String filePath = "";
    if (!AVUtils.isBlankString(localPath)) {
      filePath = localPath;
    } else if (!AVUtils.isBlankString(localTmpFilePath)) {
      filePath = localTmpFilePath;
    } else if (!AVUtils.isBlankString(getUrl())) {
      File cacheFile = AVFileDownloader.getCacheFile(url);
      if (null != cacheFile && cacheFile.exists()) {
        filePath = cacheFile.getAbsolutePath();
      }
    }
    if (!AVUtils.isBlankString(filePath)) {
      if (null != progressCallback) {
        progressCallback.done(100);
      }
      if (callback != null) {
        try {
          InputStream is = AVPersistenceUtils.getInputStreamFromFile(new File(filePath));
          callback.internalDone0(is, null);
        } catch (IOException e) {
          callback.internalDone(new AVException(e));
        }
      }
    } else {
      cancelDownloadIfNeed();
      downloader = new AVFileDownloader(progressCallback, callback);
      downloader.execute(getUrl());
    }
  }

  /**
   * Cancels the current network request and callbacks whether it's uploading or fetching data from
   * the server.
   */
  public void cancel() {
    cancelDownloadIfNeed();
    cancelUploadIfNeed();
  }

  // ================================================================================
  // Private Methods
  // ================================================================================

  private void cancelDownloadIfNeed() {
    if (downloader != null) downloader.cancel(true);
  }

  private void cancelUploadIfNeed() {
    if (uploader != null) uploader.cancel(true);
  }

  // ================================================================================
  // Private fields
  // ================================================================================

  /**
   * need call this if upload success
   *
   * @param uniqueName
   * @param url
   */
  void handleUploadedResponse(String objectId, String uniqueName, String url) {
    this.dirty = false;
    this.objectId = objectId;
    this.fileObject = AVObject.createWithoutData("_File", objectId);
    this.name = uniqueName;
    this.url = url;
  }

  /**
   * @throws AVException
   * @see AVObject#delete()
   * @since 1.3.4
   */
  public void delete() throws AVException {
    if (getFileObject() != null)
      getFileObject().delete();
    else
      throw AVErrorUtils.createException(AVException.FILE_DELETE_ERROR,
        "File object is not exists.");
  }

  /**
   * @see AVObject#deleteEventually()
   * @since 1.3.4
   */
  public void deleteEventually() {
    if (getFileObject() != null) getFileObject().deleteEventually();
  }

  /**
   * @param callback
   * @see AVObject#deleteEventually(DeleteCallback callback);
   * @since 1.3.4
   */
  public void deleteEventually(DeleteCallback callback) {
    if (getFileObject() != null) getFileObject().deleteEventually(callback);
  }

  /**
   * @see AVObject#deleteInBackground();
   * @since 1.3.4
   */
  public void deleteInBackground() {
    if (getFileObject() != null) getFileObject().deleteInBackground();
  }

  /**
   * @param callback
   * @see AVObject#deleteInBackground(DeleteCallback callback);
   * @since 1.3.4
   */
  public void deleteInBackground(DeleteCallback callback) {
    if (getFileObject() != null)
      getFileObject().deleteInBackground(callback);
    else
      callback
        .internalDone(null, AVErrorUtils.createException(AVException.FILE_DELETE_ERROR,
          "File object is not exists."));
  }

  String mimeType() {
    String mimeType = "";
    if (!AVUtils.isBlankString(name)) {
      mimeType = AVUtils.getMimeTypeFromLocalFile(name);
    } else if (!AVUtils.isBlankString(url)) {
      mimeType = AVUtils.getMimeTypeFromUrl(url);
    }
    return AVUtils.isBlankContent(mimeType) ? DEFAULTMIMETYPE : mimeType;
  }

  public static String className() {
    return "File";
  }

  public Uploader getUploader(SaveCallback saveCallback, ProgressCallback progressCallback) {
    Uploader.UploadCallback callback = new Uploader.UploadCallback() {
      @Override
      public void finishedWithResults(String finalObjectId, String finalUrl) {
        handleUploadedResponse(finalObjectId, finalObjectId, finalUrl);
      }
    };
    if (AVUtils.isBlankString(url)) {
      return new FileUploader(this, saveCallback, progressCallback, callback);
    } else {
      return new UrlDirectlyUploader(this, saveCallback, progressCallback, callback);
    }
  }

  public String getBucket() {
    return this.bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  /**
   * 此函数对应的是获得“用户传过来的本地文件”的具体内容
   *
   * @return
   */
  @JSONField(serialize = false)
  private byte[] getLocalFileData() {
    if (!AVUtils.isBlankString(localPath)) {
      return AVPersistenceUtils.readContentBytesFromFile(new File(localPath));
    }
    return null;
  }

  /**
   * 此函数对应的是获得“用户传过来的 byte[]”的具体内容
   *
   * @return
   */
  @JSONField(serialize = false)
  private byte[] getTmpFileData() {
    if (!AVUtils.isBlankString(localTmpFilePath)) {
      return AVPersistenceUtils.readContentBytesFromFile(new File(localTmpFilePath));
    }
    return null;
  }

  /**
   * 此函数对应的是获得“通过 Url 下载来的”的具体内容
   *
   * @return
   */
  @JSONField(serialize = false)
  private byte[] getCacheData() {
    if (!AVUtils.isBlankString(url)) {
      File file = AVFileDownloader.getCacheFile(url);
      if (null != file && file.exists()) {
        return AVPersistenceUtils.readContentBytesFromFile(file);
      }
    }
    return null;
  }

  /**
   * 获取AVFile的ACL
   *
   * @since 2.6.9
   */
  public AVACL getACL() {
    return acl;
  }

  public void setACL(AVACL acl) {
    this.acl = acl;
  }

  protected org.json.JSONObject toJSONObject() {
    Map<String, Object> data = AVUtils.mapFromFile(this);
    if (!AVUtils.isBlankString(url)) {
      data.put("url", url);
    }
    return new JSONObject(data);
  }

  /**
   * clear cached file for this file.
   */
  public void clearCachedFile() {
    if (!AVUtils.isBlankString(localTmpFilePath)) {
      File cacheFile = new File(localTmpFilePath);
      if (null != cacheFile && cacheFile.exists() && cacheFile.isFile()) {
        cacheFile.delete();
      }
    }
    if (!AVUtils.isBlankString(url)) {
      File cacheFile = AVFileDownloader.getCacheFile(url);
      if (null != cacheFile && cacheFile.exists() && cacheFile.isFile()) {
        cacheFile.delete();
      }
    }
  }

  /**
   * clear all cached files
   */
  public static void clearAllCachedFiles() {
    clearCacheMoreThanDays(0);
  }

  /**
   * clear cached files which created before x days.
   * @param days - peroid from now.
   */
  public static void clearCacheMoreThanDays(int days) {
    long curTime = System.currentTimeMillis();
    if ( days > 0) {
      curTime -= days * 86400000; // 86400000 is one day.
    }
    String cacheDir = AVFileDownloader.getAVFileCachePath();
    clearDir(new File(cacheDir), curTime);
  }

  private static void clearDir(File dir, long lastModified) {
    File[] files = dir.listFiles();
    for (File f: files) {
      if (f.isFile()) {
        if (f.lastModified() < lastModified) {
          f.delete();
        }
      } else if (f.isDirectory()) {
        clearDir(f, lastModified);
      }
    }
  }
}
