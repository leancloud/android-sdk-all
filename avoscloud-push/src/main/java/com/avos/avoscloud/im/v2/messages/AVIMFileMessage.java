package com.avos.avoscloud.im.v2.messages;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.ProgressCallback;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@AVIMMessageType(type = AVIMMessageType.FILE_MESSAGE_TYPE)
public class AVIMFileMessage extends AVIMTypedMessage {
  static final String OBJECT_ID = "objId";
  static final String FILE_URL = "url";
  static final String FILE_META = "metaData";
  static final String FILE_SIZE = "size";
  static final String FORMAT = "format";
  static final String DURATION = "duration";
  private static final String LOCAL_PATH = "local_path";

  File localFile;
  AVFile actualFile;

  @AVIMMessageField(name = "_lcfile")
  protected Map<String, Object> file;
  @AVIMMessageField(name = "_lctext")
  String text;
  @AVIMMessageField(name = "_lcattrs")
  Map<String, Object> attrs;

  ProgressCallback progressCallback;

  public AVIMFileMessage() {
  }

  public AVIMFileMessage(String localPath) throws IOException {
    this(new File(localPath));
  }

  public AVIMFileMessage(File localFile) throws IOException {
    this.localFile = localFile;
    actualFile = AVFile.withFile(localFile.getName(), localFile);
    this.file = new HashMap<String, Object>();
    file.put(LOCAL_PATH, localFile.getPath());
  }

  public AVIMFileMessage(AVFile file) {
    this.actualFile = file;
  }

  public Map<String, Object> getFile() {
    return file;
  }

  /**
   * 获取本地文件地址，如果用户并未指定任何本地文件，则返回 null
   *
   * @return
   */
  public String getLocalFilePath() {
    return (null != localFile && localFile.exists() ? localFile.getPath() : null);
  }

  /**
   * 获取文件消息中得AVFile对象
   *
   * @return
   */
  public AVFile getAVFile() {
    if (actualFile != null) {
      return actualFile;
    } else if (file.containsKey(FILE_URL)) {
      Map<String, Object> avfileMeta = null;
      if (file.containsKey(FILE_META)) {
        avfileMeta = (Map) file.get(FILE_META);
      }
      AVFile avfile = new AVFile(null, (String) file.get(FILE_URL), avfileMeta);
      if (file.containsKey(OBJECT_ID)) {
        avfile.setObjectId((String) file.get(OBJECT_ID));
      }
      return avfile;
    }
    return null;
  }

  protected void setFile(Map<String, Object> file) {
    this.file = file;
    Map<String, Object> metaData = (Map<String, Object>) file.get(FILE_META);
    actualFile = new AVFile(null, (String) file.get(FILE_URL), metaData);
    actualFile.setObjectId((String) file.get(OBJECT_ID));
    if (file.containsKey(LOCAL_PATH)) {
      this.localFile = new File((String) file.get(LOCAL_PATH));
    }
  }


  /**
   * 获取文件地址
   *
   * @return
   */
  public String getFileUrl() {
    if (file != null) {
      return (String) file.get(FILE_URL);
    } else {
      return null;
    }
  }

  /**
   * 获取文件的metaData
   *
   * @return
   */
  public Map<String, Object> getFileMetaData() {
    if (file == null) {
      file = new HashMap<String, Object>();
    }
    Map<String, Object> meta;
    if (!file.containsKey(FILE_META)) {
      meta = new HashMap<String, Object>();
      meta.put(FILE_SIZE, actualFile.getSize());
    } else {
      meta = (Map<String, Object>) file.get(FILE_META);
    }
    return meta;
  }

  /**
   * 获取文件大小
   *
   * @return
   */
  public long getSize() {
    Map<String, Object> meta = getFileMetaData();
    if (meta != null && meta.containsKey(FILE_SIZE)) {
      return Long.valueOf(meta.get(FILE_SIZE).toString());
    }
    return 0;
  }

  protected void upload(final SaveCallback callback) {
    if (actualFile != null) {
      actualFile.saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (e != null) {
            callback.internalDone(e);
          } else {
            fulFillFileInfo(callback);
          }
        }
      }, this.progressCallback);
    } else {
      callback.internalDone(new AVException(new RuntimeException("cannot find the file!")));
    }
  }

  /**
   * 设置文件上传进度回调
   *
   * @param callback
   */
  public void setProgressCallback(ProgressCallback callback) {
    this.progressCallback = callback;
  }

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Map<String, Object> getAttrs() {
    return this.attrs;
  }

  public void setAttrs(Map<String, Object> attr) {
    this.attrs = attr;
  }

  protected void fulFillFileInfo(final SaveCallback callback) {
    // fulfill the file info map with AVFile
    if (actualFile != null) {
      file = getFile() == null ? new HashMap<String, Object>() : getFile();
      file.put(OBJECT_ID, actualFile.getObjectId());
      file.put(FILE_URL, actualFile.getUrl());
      file.remove(LOCAL_PATH);
      final Map<String, Object> metaData =
        getFileMetaData() == null ? new HashMap<String, Object>() : getFileMetaData();
      if (!metaData.containsKey(FILE_SIZE)) {
        metaData.put(FILE_SIZE, actualFile.getSize());
      }

      getAdditionalMetaData(metaData, new SaveCallback() {
        @Override
        public void done(AVException e) {
          file.put(FILE_META, metaData);
          if (callback != null) {
            callback.internalDone(e);
          }
        }
      });
    } else {
      callback.internalDone(new AVException(new RuntimeException("cannot find the file!")));
    }
  }

  /**
   * 判断是不是通过外部设置 url 来的 AVFile
   *
   * @param avFile
   * @return
   */
  static boolean isExternalAVFile(AVFile avFile) {
    return null != avFile
      && null != avFile.getMetaData()
      && avFile.getMetaData().containsKey("__source")
      && avFile.getMetaData().get("__source").equals("external");
  }

  protected void getAdditionalMetaData(Map<String, Object> meta, SaveCallback callback) {
    callback.internalDone(null);
  }

  public static final AVIMMessageCreator<AVIMFileMessage> CREATOR = new AVIMMessageCreator<AVIMFileMessage>(AVIMFileMessage.class);
}
