package com.avos.avoscloud.upload;

import android.util.SparseArray;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVErrorUtils;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.ProgressCallback;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.utils.AVFileUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * User: summer Date: 13-4-16 Time: AM10:43
 */
public class FileUploader extends HttpClientUploader {
  static final int PROGRESS_GET_TOKEN = 10;
  static final int PROGRESS_UPLOAD_FILE = 90;
  static final int PROGRESS_COMPLETE = 100;

  private String token;
//  private String url;
//  private String objectId;
  private String bucket;
  private String uploadUrl;
  private String provider;
  private Uploader.UploadCallback callback = null;

  protected static final int defaultFileKeyLength = 40;

  public FileUploader(AVFile avFile, SaveCallback saveCallback,
                         ProgressCallback progressCallback, Uploader.UploadCallback uploadCallback) {
    super(avFile, saveCallback, progressCallback);
    this.callback = uploadCallback;
  }

  public AVException doWork() {
    // fileKey 是随机值，在 fileTokens 请求与真正的 upload 请求时都会用到，这里要保证是同一个值
    String fileKey = AVUtils.parseFileKey(avFile.getName());
    if (AVUtils.isBlankString(uploadUrl)) {
      final AVException getBucketException = fetchUploadBucket("fileTokens", fileKey, true, new AVCallback<String>() {
        @Override
        protected void internalDone0(String s, AVException avException) {
          if (null == avException) {
            AVException ex = handleGetBucketResponse(s);
            if (null != ex) {
              LogUtil.log.e("failed to parse response of fileTokens.", ex);
            }
          } else {
            LogUtil.log.e("failed to invoke fileTokens.", avException);
          }
        }

        @Override
        protected boolean mustRunOnUIThread() {
          // 必须要同步执行，不然会导致没有 handleGetBucketResponse 没有执行，而造成以后的逻辑错误
          return false;
        }
      });
      if (getBucketException != null) {
        return getBucketException;
      }
    }
    publishProgress(PROGRESS_GET_TOKEN);
    Uploader uploader = getUploaderImplementation(fileKey);
    if (null == uploader) {
      return new AVException(new Throwable("Uploader can not be instantiated."));
    }

    AVException uploadException = uploader.doWork();
    if (uploadException == null) {
      if (null != callback) {
        callback.finishedWithResults(finalObjectId, finalUrl);
      }
//      avFile.handleUploadedResponse(objectId, objectId, url);
      publishProgress(PROGRESS_COMPLETE);
      completeFileUpload(true);
      return null;
    } else {
      completeFileUpload(false);
      return uploadException;
    }
  }

  private Uploader getUploaderImplementation(String fileKey) {
    if (!AVUtils.isBlankString(provider)) {
      switch (provider) {
        case "qcloud":
          return new QCloudUploader(avFile, fileKey, token, uploadUrl, saveCallback, progressCallback);
        case "s3":
          return new S3Uploader(avFile, uploadUrl, saveCallback, progressCallback);
        default:
          return new QiniuSlicingUploader(avFile, token, fileKey, saveCallback, progressCallback);
      }
    } else {
      return null;
    }
  }

  private AVException fetchUploadBucket(String path, String fileKey, boolean sync, final AVCallback<String> callback) {
    final AVException[] exceptionWhenGetBucket = new AVException[1];
    PaasClient.storageInstance().postObject(path, getGetBucketParameters(fileKey), sync,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            callback.internalDone(content, e);
            exceptionWhenGetBucket[0] = e;
          }

          @Override
          public void onFailure(Throwable error, String content) {
            callback.internalDone(null, AVErrorUtils.createException(error, content));
            exceptionWhenGetBucket[0] = AVErrorUtils.createException(error, content);
          }
        });
    if (null != exceptionWhenGetBucket[0]) {
      return exceptionWhenGetBucket[0];
    }
    return null;
  }

  private AVException handleGetBucketResponse(String responseStr) {
    if (!AVUtils.isBlankContent(responseStr)) {
      try {
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(responseStr);
        this.bucket = jsonObject.getString("bucket");
        this.finalObjectId = jsonObject.getString("objectId");
        this.uploadUrl = jsonObject.getString("upload_url");
        this.provider = jsonObject.getString("provider");
        this.token = jsonObject.getString("token");
        this.finalUrl = jsonObject.getString("url");
      } catch (JSONException e) {
        return new AVException(e);
      }
    }
    return null;
  }

  private String getGetBucketParameters(String fileKey) {
    // decide file mimetype.
    String fileName = avFile.getName();
    String mimeType = AVFileUtil.getFileMimeType(avFile);

    Map<String, Object> parameters = new HashMap<String, Object>(3);
    parameters.put("key",  fileKey);
    parameters.put("name", fileName);
    parameters.put("mime_type", mimeType);
    parameters.put("metaData", avFile.getMetaData());
    parameters.put("__type", AVFile.className());
    if (avFile.getACL() != null) {
      parameters.putAll(AVUtils.getParsedMap(avFile.getACL().getACLMap()));
    }
    return AVUtils.restfulServerData(parameters);
  }

  private void completeFileUpload(boolean success){
    if (!AVUtils.isBlankString(token)) {
      try {
        JSONObject completeResult = new JSONObject();
        completeResult.put("result",success);
        completeResult.put("token",this.token);
        PaasClient.storageInstance().postObject("fileCallback", completeResult.toJSONString(), false, new GenericObjectCallback() {});
      } catch (Exception e) {
        // ignore
      }
    }
  }

  protected static class ProgressCalculator {
    SparseArray<Integer> blockProgress = new SparseArray<Integer>();
    FileUploadProgressCallback callback;
    int fileBlockCount = 0;

    public ProgressCalculator(int blockCount, FileUploadProgressCallback callback) {
      this.callback = callback;
      this.fileBlockCount = blockCount;
    }

    public synchronized void publishProgress(int offset, int progress) {
      blockProgress.put(offset, progress);
      if (callback != null) {
        int progressSum = 0;
        for (int index = 0; index < blockProgress.size(); index++) {
          progressSum += blockProgress.valueAt(index);
        }
        callback.onProgress(PROGRESS_GET_TOKEN + (PROGRESS_UPLOAD_FILE - PROGRESS_GET_TOKEN)
            * progressSum / (100 * fileBlockCount));
      }
    }
  }

  public static void setUploadHeader(String key, String value) {
    UPLOAD_HEADERS.put(key, value);
  }

  public static interface FileUploadProgressCallback {
    void onProgress(int progress);
  }
  static HashMap<String, String> UPLOAD_HEADERS = new HashMap<>();
}
