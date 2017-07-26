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

@AVIMMessageType(type = AVIMMessageType.VIDEO_MESSAGE_TYPE)
public class AVIMVideoMessage extends AVIMFileMessage {

  public AVIMVideoMessage() {
  }

  public AVIMVideoMessage(String localPath) throws IOException {
    super(localPath);
  }

  public AVIMVideoMessage(File localFile) throws IOException {
    super(localFile);
  }

  public AVIMVideoMessage(AVFile file) {
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
      Map<String, Object> meta = AVIMFileMessageAccessor.mediaInfo(localFile);
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
   * 获取音频的时长
   *
   * @return
   */
  public double getDuration() {
    Map<String, Object> meta = getFileMetaData();
    if (meta != null && meta.containsKey(DURATION)) {
      return ((Number) meta.get(DURATION)).doubleValue();
    }
    return 0;
  }

  @Override
  protected void getAdditionalMetaData(final Map<String, Object> meta, final SaveCallback callback) {
    if (!AVUtils.isBlankString(actualFile.getUrl()) && localFile == null
       && !isExternalAVFile(actualFile)) {
      AVHttpClient client = AVHttpClient.clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(actualFile.getUrl() + "?avinfo").get();
      client.execute(builder.build(), false, new GetHttpResponseHandler(
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              try {
                com.alibaba.fastjson.JSONObject response = JSON.parseObject(content);
                com.alibaba.fastjson.JSONObject formatInfo = response.getJSONObject(FORMAT);
                String fileFormat = formatInfo.getString("format_name");
                Double durationInDouble = formatInfo.getDouble("duration");
                long size = formatInfo.getLong(FILE_SIZE);
                meta.put(FILE_SIZE, size);
                meta.put(DURATION, durationInDouble);
                meta.put(FORMAT, fileFormat);
              } catch (Exception e1) {
                callback.internalDone(new AVException(e1));
              }
              callback.internalDone(null);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              callback.internalDone(new AVException(error));
            }
          }));
    } else {
      callback.internalDone(null);
    }
  }

  public static final Creator<AVIMVideoMessage> CREATOR = new AVIMMessageCreator<>(AVIMVideoMessage.class);
}
