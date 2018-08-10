package com.avos.avoscloud;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.utils.StringUtils;
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
          LogUtil.avlog.e("update installation error!", e);
        } else {
          LogUtil.avlog.d("Huawei push registration successful!");
        }
      }
    });
  }

  /**
   * 响应 token 通知。
   *
   * @param context
   * @param token
   * @param bundle
   */
  @Override
  public void onToken(Context context, String token, Bundle bundle) {
    updateAVInstallation(token);
  }

  /**
   * 收到透传消息
   *
   * 消息格式类似于：
   *      {"alert":"", "title":"", "action":"", "silent":true}
   * SDK 内部会转换成 {"content":\\"{"alert":"", "title":"", "action":"", "silent":true}\\"}
   * 再发送给本地的 Receiver。
   *
   * 所以，开发者如果想自己处理透传消息，则需要从 Receiver#onReceive(Context context, Intent intent) 的 intent 中通过
   * getStringExtra("content") 获取到实际的数据。
   *
   * @param var1
   * @param var2
   * @param var3
   */
  @Override
  public void onPushMsg(Context var1, byte[] var2, String var3) {
    try {
      String message = new String(var2, "UTF-8");
      AVNotificationManager.getInstance().processMixPushMessage(message);
    } catch (Exception ex) {
      LogUtil.avlog.e("failed to process PushMessage.", ex);
    }
  }

  /**
   * 响应通知栏点击事件
   *
   * @param context
   * @param event
   * @param extras
   */
  @Override
  public void onEvent(Context context, Event event, Bundle extras) {
    LogUtil.avlog.d("received Notify Event. Event=" + event);
    if (Event.NOTIFICATION_CLICK_BTN.equals(event) || Event.NOTIFICATION_OPENED.equals(event)) {
      int notifyId = extras.getInt(BOUND_KEY.pushNotifyId, 0);
      LogUtil.avlog.d("received Push Event. notifyId:" + notifyId);
      if (0 != notifyId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notifyId);
      }
    } else {
      LogUtil.avlog.d("unknow event.");
    }
    super.onEvent(context, event, extras);
  }

  /**
   * 响应推送状态变化通知。
   *
   * @param context
   * @param pushState
   */
  @Override
  public void onPushState(Context context, boolean pushState) {
    LogUtil.avlog.d("pushState changed, current=" + pushState);
  }

}
