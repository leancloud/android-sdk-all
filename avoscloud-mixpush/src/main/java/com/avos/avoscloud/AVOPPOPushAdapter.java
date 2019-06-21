package com.avos.avoscloud;

import com.avos.avoscloud.utils.StringUtils;

/**
 * OPPO推送暂时只支持通知栏消息的推送。消息下发到OS系统模块并由系统通知模块展示，在用户点击通知前，不启动应用。
 * 参考：https://open.oppomobile.com/wiki/doc#id=10196
 */
public class AVOPPOPushAdapter extends com.coloros.mcssdk.callback.PushAdapter {
  private static final String VENDOR_OPPO = "oppo";

  private void updateAVInstallation(String registerId) {
    if (!AVUtils.isBlankString(registerId)) {
      AVInstallation installation = AVInstallation.getCurrentInstallation();

      if (!VENDOR_OPPO.equals(installation.getString(AVInstallation.VENDOR))) {
        installation.put(AVInstallation.VENDOR, VENDOR_OPPO);
      }
      if (!registerId.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
        installation.put(AVInstallation.REGISTRATION_ID, registerId);
      }
      String localProfile = installation.getString(AVMixPushManager.MIXPUSH_PROFILE);
      localProfile = (null != localProfile ? localProfile : "");
      if (!localProfile.equals(AVMixPushManager.oppoDeviceProfile)) {
        installation.put(AVMixPushManager.MIXPUSH_PROFILE, AVMixPushManager.oppoDeviceProfile);
      }
      installation.saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (null != e) {
            LogUtil.avlog.e("update installation error!", e);
          } else {
            LogUtil.avlog.d("Xiaomi push registration successful!");
          }
        }
      });
    }
  }

  @Override
  public void onRegister(int responseCode, String registerID) {
    if (responseCode != com.coloros.mcssdk.mode.ErrorCode.SUCCESS) {
      LogUtil.avlog.e("failed to register device. errorCode: " + responseCode);
      return;
    }
    if (StringUtils.isBlankString(registerID)) {
      LogUtil.avlog.e("oppo register id is empty.");
      return;
    }
    updateAVInstallation(registerID);
  }
}
