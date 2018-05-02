package com.avos.avoscloud;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.utils.StringUtils;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by fengjunwen on 2018/4/24.
 */

public class AVHMSPushMessageReceiver extends com.huawei.hms.support.api.push.PushReceiver{
  static final String MIXPUSH_PRIFILE = "deviceProfile";
  static final String VENDOR = "HMS";

  private void updateAVInstallation(String hwToken) {
    if (StringUtils.isBlankString(hwToken)) {
      return;
    }
    AVInstallation installation = AVInstallation.getCurrentInstallation();
    if (!VENDOR.equals(installation.getString(AVInstallation.VENDOR))) {
      installation.put(AVInstallation.VENDOR, VENDOR);
    }
    if (!hwToken.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
      installation.put(AVInstallation.REGISTRATION_ID, hwToken);
    }
    String localProfile = installation.getString(MIXPUSH_PRIFILE);
    if (null == localProfile) {
      localProfile = "";
    }
    if (!localProfile.equals(AVMixPushManager.hwDeviceProfile)) {
      installation.put(AVMixPushManager.MIXPUSH_PRIFILE, AVMixPushManager.hwDeviceProfile);
    }

    installation.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        if (null != e) {
          LogUtil.avlog.d("update installation error!");
        } else {
          LogUtil.avlog.d("Huawei push registration successful!");
        }
      }
    });
  }

  @Override
  public void onToken(Context context, String token, Bundle bundle) {
    updateAVInstallation(token);
  }

  @Override
  public void onPushMsg(Context var1, byte[] var2, String var3) {
    try {
      String message = new String(var2, "UTF-8");
      notifyPushMessage(var1, message);
    } catch (Exception ex) {
      LogUtil.avlog.e("failed to process PushMessage.", ex);
    }
  }

  @Override
  public void onEvent(Context context, Event event, Bundle extras) {
    if (Event.NOTIFICATION_CLICK_BTN.equals(event) || Event.NOTIFICATION_OPENED.equals(event)) {
      int notifyId = extras.getInt(BOUND_KEY.pushNotifyId, 0);
      LogUtil.avlog.d("received Push Event. notifyId:" + notifyId);
      if (0 != notifyId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notifyId);
      }
      String message = extras.getString(BOUND_KEY.pushMsgKey);
      notifyPushMessage(context, message);
    }
  }

  @Override
  public void onPushState(Context context, boolean pushState) {
    LogUtil.avlog.d("pushState changed, current=" + pushState);
  }

  private void notifyPushMessage(Context context, String message) {
    Map<String, String> msgObject = JSON.parseObject(message, Map.class);
    if (msgObject.containsKey("action")) {
      String action = msgObject.get("action");

      LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
      Intent intent = new Intent(action);
      Bundle bundle = new Bundle();
      for (Map.Entry<String, String> kv: msgObject.entrySet()) {
        bundle.putString(kv.getKey(), kv.getValue());
      }
      intent.putExtras(bundle);
      localBroadcastManager.sendBroadcast(intent);
    } else {
      LogUtil.avlog.e("invalid pushMessage: " + message);
    }

  }
}
