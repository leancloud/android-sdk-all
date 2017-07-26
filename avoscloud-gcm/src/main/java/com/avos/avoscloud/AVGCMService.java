package com.avos.avoscloud;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.alibaba.fastjson.JSONObject;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by wli on 15/8/19.
 * 接收并处理 GCM 推送过来的消息
 */
public class AVGCMService extends GcmListenerService {

  @Override
  public void onMessageSent(String msgId) {
    super.onMessageSent(msgId);
  }

  @Override
  public void onDeletedMessages() {
    super.onDeletedMessages();
  }

  @Override
  public void onMessageReceived(String from, Bundle data) {
    if (null != data) {
      String channel = data.getString("_channel");
      String action = data.getString("action");
      String content = data.getString("gcm.notification.body");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("alert", TextUtils.isEmpty(content) ? "" : content);

      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.d("got gcm message:" + jsonObject.toJSONString());
      }

      AVNotificationManager.getInstance().processGcmMessage(channel, action, jsonObject.toJSONString());
    }
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);
  }
}
