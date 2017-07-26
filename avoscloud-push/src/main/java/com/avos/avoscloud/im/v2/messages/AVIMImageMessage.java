package com.avos.avoscloud.im.v2.messages;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVHttpClient;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.GetHttpResponseHandler;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;

@AVIMMessageType(type = AVIMMessageType.IMAGE_MESSAGE_TYPE)
public class AVIMImageMessage extends AVIMFileMessage {
  static final String IMAGE_HEIGHT = "height";
  static final String IMAGE_WIDTH = "width";

  public AVIMImageMessage() {
  }

  public AVIMImageMessage(String localPath) throws IOException {
    super(localPath);
  }

  public AVIMImageMessage(File localFile) throws IOException {
    super(localFile);
  }

  public AVIMImageMessage(AVFile file) {
    super(file);
  }

  /**
   * 获取文件的metaData
   *
   * @return
   */
  @Override
  public Map<String, Object> getFileMetaData() {
    if (file == null) {
      file = new HashMap<String, Object>();
    }
    if (file.containsKey(FILE_META)) {
      return (Map<String, Object>) file.get(FILE_META);
    }
    if (localFile != null) {
      Map<String, Object> meta = AVIMFileMessageAccessor.getImageMeta(localFile);
      meta.put(FILE_SIZE, actualFile.getSize());
      file.put(FILE_META, meta);
      return meta;
    } else if (actualFile != null) {
      Map<String, Object> meta = actualFile.getMetaData();
      file.put(FILE_META, meta);
      return meta;
    }
    return null;
  }

  /**
   * 获取图片的高
   *
   * @return
   */
  public int getHeight() {
    Map<String, Object> metaData = getFileMetaData();
    if (metaData != null && metaData.containsKey(IMAGE_HEIGHT)) {
      return (Integer) metaData.get(IMAGE_HEIGHT);
    }
    return 0;
  }

  /**
   * 获取图片的宽度
   *
   * @return
   */
  public int getWidth() {
    Map<String, Object> metaData = getFileMetaData();
    if (metaData != null && metaData.containsKey(IMAGE_WIDTH)) {
      return (Integer) metaData.get(IMAGE_WIDTH);
    }
    return 0;
  }



  @Override
  protected void getAdditionalMetaData(final Map<String, Object> meta, final SaveCallback callback) {
    if (!AVUtils.isBlankString(actualFile.getUrl()) && localFile == null
      && !isExternalAVFile(actualFile)) {
      AVHttpClient client = AVHttpClient.clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(actualFile.getUrl() + "?imageInfo").get();
      client.execute(builder.build(), false, new GetHttpResponseHandler(
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              try {
                com.alibaba.fastjson.JSONObject response = JSON.parseObject(content);
                meta.put(FORMAT, response.getString(FORMAT));
                meta.put(IMAGE_HEIGHT, response.getInteger(IMAGE_HEIGHT));
                meta.put(IMAGE_WIDTH, response.getInteger(IMAGE_WIDTH));
              } catch (Exception e1) {
                callback.internalDone(new AVException(e1));
              }
              callback.internalDone(null);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              callback.internalDone(new AVException(error));
            }

            @Override
            public boolean isRequestStatisticNeed() {
              return false;
            }
          }));
    } else {
      callback.internalDone(null);
    }
  }

  public static final Creator<AVIMImageMessage> CREATOR = new AVIMMessageCreator<AVIMImageMessage>(AVIMImageMessage.class);
}
