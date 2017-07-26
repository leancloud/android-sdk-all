package com.avos.avoscloud;

import android.content.Context;
import android.content.Intent;
import com.meizu.cloud.pushsdk.MzPushMessageReceiver;
import com.meizu.cloud.pushsdk.notification.PushNotificationBuilder;
import com.meizu.cloud.pushsdk.platform.message.PushSwitchStatus;
import com.meizu.cloud.pushsdk.platform.message.RegisterStatus;
import com.meizu.cloud.pushsdk.platform.message.SubAliasStatus;
import com.meizu.cloud.pushsdk.platform.message.SubTagsStatus;
import com.meizu.cloud.pushsdk.platform.message.UnRegisterStatus;

/**
 * Created by wli on 2017/2/14.
 */

public class AVFlymePushMessageReceiver extends MzPushMessageReceiver {

  private final String FLYME_VERDOR = "mz";

  private void updateAVInstallation(String flymePushId) {
    if (!AVUtils.isBlankString(flymePushId)) {
      AVInstallation installation = AVInstallation.getCurrentInstallation();

      if (!FLYME_VERDOR.equals(installation.getString(AVInstallation.VENDOR))) {
        installation.put(AVInstallation.VENDOR, FLYME_VERDOR);
      }
      if (!flymePushId.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
        installation.put(AVInstallation.REGISTRATION_ID, flymePushId);
      }

      String localProfile = installation.getString(AVMixpushManager.MIXPUSH_PRIFILE);
      localProfile = (null != localProfile ? localProfile : "");
      if (!localProfile.equals(AVMixpushManager.flymeDevicePrifile)) {
        installation.put(AVMixpushManager.MIXPUSH_PRIFILE, AVMixpushManager.flymeDevicePrifile);
      }

      installation.saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (null != e) {
            LogUtil.avlog.d("update installation error!");
          } else {
            LogUtil.avlog.d("flyme push registration successful!");
          }
        }
      });
    }
  }

  @Override
  public void onRegister(Context context, String s) {
  }

  @Override
  public void onMessage(Context context, String s) {
    AVNotificationManager.getInstance().processMixPushMessage(s);
  }

  @Override
  public void onMessage(Context context, Intent intent) {
  }

  @Override
  @Deprecated
  public void onUnRegister(Context context, boolean b) {
  }

  @Override
  public void onPushStatus(Context context, PushSwitchStatus pushSwitchStatus) {
  }

  @Override
  public void onRegisterStatus(Context context, RegisterStatus registerStatus) {
    String pushId = registerStatus.getPushId();
    if (!AVUtils.isBlankContent(pushId)) {
      updateAVInstallation(pushId);
    }
  }

  @Override
  public void onUnRegisterStatus(Context context, UnRegisterStatus unRegisterStatus) {
  }

  @Override
  public void onSubTagsStatus(Context context, SubTagsStatus subTagsStatus) {
  }

  @Override
  public void onSubAliasStatus(Context context, SubAliasStatus subAliasStatus) {
  }

  @Override
  public void onUpdateNotificationBuilder(PushNotificationBuilder pushNotificationBuilder) {
  }

  @Override
  public void onNotificationArrived(Context context, String title, String content, String selfDefineContentString) {
  }

  @Override
  public void onNotificationClicked(Context context, String title, String content, String selfDefineContentString) {
    AVNotificationManager.getInstance().processMixNotification(selfDefineContentString, AVConstants.AV_MIXPUSH_FLYME_NOTIFICATION_ACTION);
  }

  @Override
  public void onNotificationDeleted(Context context, String title, String content, String selfDefineContentString) {
  }

  @Override
  public void onNotifyMessageArrived(Context context, String message) {
  }
}
