package com.avos.avoscloud;

import android.content.Context;

import com.huawei.android.pushagent.api.PushEventReceiver;

/**
 * Created by wli on 16/6/22.
 */
public class AVHwPushMessageReceiver extends PushEventReceiver {

  private void updateAVInstallation(String hwToken) {
    if (!AVUtils.isBlankString(hwToken)) {
      AVInstallation installation = AVInstallation.getCurrentInstallation();

      if (!"hw".equals(installation.getString(AVInstallation.VENDOR))) {
        installation.put(AVInstallation.VENDOR, "hw");
      }
      if (!hwToken.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
        installation.put(AVInstallation.REGISTRATION_ID, hwToken);
      }
      String localProfile = installation.getString(AVMixPushManager.MIXPUSH_PRIFILE);
      localProfile = (null != localProfile ? localProfile : "");
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
  }

  @Override
  public void onPushMsg(Context context, byte[] bytes, String s) {
    String data = new String(bytes);
    AVNotificationManager.getInstance().processMixPushMessage(data);
  }

  @Override
  public void onToken(Context context, String s) {
    super.onToken(context, s);
    updateAVInstallation(s);
  }
}
