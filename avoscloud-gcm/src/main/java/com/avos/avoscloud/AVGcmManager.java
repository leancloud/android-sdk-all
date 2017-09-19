package com.avos.avoscloud;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.android.gms.gcm.GcmReceiver;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by wli on 15/8/17
 * 负责上传 gcm registrationId
 * 只有在 (美国节点 && 没有上传 registrationId && 声明 AVGCMService && 声明 GcmReceiver) == true 时
 * 才允许使用 GCM
 */
public class AVGcmManager {
  private static final String GCM_SENDER_ID = "617426794530";

  /**
   * 获取 GCM token 并上传至 AVInstallation
   * @param context
   */
  public static void getGcmTokenInBackground(final Context context) {
    if (hasRegistrationId()) {
      return;
    }
    if (!AVManifestUtils.checkService(context, AVGCMService.class)
      || !AVManifestUtils.checkReceiver(context, GcmReceiver.class)) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.log.e("GCM registration failed！");
      }
      return;
    }

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        try {
          String token = InstanceID.getInstance(context).getToken(GCM_SENDER_ID,
            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
          LogUtil.log.d("retrieved token:" + token + " from GCM");

          AVInstallation installation = AVInstallation.getCurrentInstallation();
          if (!installation.containsKey(AVInstallation.VENDOR)
            || !installation.containsKey(AVInstallation.REGISTRATION_ID)
            || !installation.getString(AVInstallation.REGISTRATION_ID).equals(token)) {

            installation.put(AVInstallation.VENDOR, "gcm");
            installation.put(AVInstallation.REGISTRATION_ID, token);
            installation.save();

            LogUtil.log.d("GCM registration success! registrationId=" + token);
          }
        } catch (IOException e) {
          LogUtil.log.e("Exception happended during get gcm token", e);
        } catch (AVException e) {
          LogUtil.log.e("AVInstallation save exception", e);
        }
        return null;
      }
    }.execute();
  }

  /**
   * 是否已经上传 registrationId
   */
  private static boolean hasRegistrationId() {
    AVInstallation installation = AVInstallation.getCurrentInstallation();
    return installation.containsKey(AVInstallation.REGISTRATION_ID) &&
      !TextUtils.isEmpty(installation.getString(AVInstallation.REGISTRATION_ID));
  }
}
