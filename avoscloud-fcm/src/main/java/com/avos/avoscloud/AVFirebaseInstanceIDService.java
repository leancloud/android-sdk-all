package com.avos.avoscloud;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import com.avos.avoscloud.utils.StringUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by fengjunwen on 2018/8/28.
 */

public class AVFirebaseInstanceIDService extends FirebaseInstanceIdService {
  private final String VENDOR = "fcm";

  @Override
  public void onTokenRefresh() {
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    sendRegistrationToServer(refreshedToken);
    LogUtil.log.d("refreshed token: " + refreshedToken);
  }

  private void sendRegistrationToServer(String refreshedToken) {
    if (StringUtils.isBlankString(refreshedToken)) {
      return;
    }
    AVInstallation installation = AVInstallation.getCurrentInstallation();
    if (!VENDOR.equals(installation.getString(AVInstallation.VENDOR))) {
      installation.put(AVInstallation.VENDOR, VENDOR);
    }
    if (!refreshedToken.equals(installation.getString(AVInstallation.REGISTRATION_ID))) {
      installation.put(AVInstallation.REGISTRATION_ID, refreshedToken);
    }
    installation.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        if (null != e) {
          LogUtil.avlog.e("failed to update installation.", e);
        } else {
          LogUtil.avlog.d("succeed to update installation.");
        }
      }
    });

    LogUtil.log.d("FCM registration success! registrationId=" + refreshedToken);
  }
}