package com.avos.avoscloud;

import android.Manifest;
import android.content.Context;
import android.os.Build;

/**
 * Created by wli on 16/6/27.
 */
public class AVMixPushManager {

  static final String MIXPUSH_PRIFILE = "deviceProfile";

  /**
   * 小米推送的 deviceProfile
   */
  static String miDeviceProfile = "";

  /**
   * 华为推送的 deviceProfile
   */
  static String hwDeviceProfile = "";

  /**
   * 魅族推送的 deviceProfile
   */
  static String flymeDevicePrifile = "";
  static int flymeMStatusBarIcon = 0;

  /**
   * 注册小米推送
   * 只有 appId、appKey 有效 && MIUI && manifest 正确填写 才能注册
   *
   * @param context
   * @param miAppId
   * @param miAppKey
   */
  public static void registerXiaomiPush(Context context, String miAppId, String miAppKey) {
    registerXiaomiPush(context, miAppId, miAppKey, null);
  }

  /**
   * 注册小米推送
   * 只有 appId、appKey 有效 && MIUI && manifest 正确填写 才能注册
   *
   * @param context
   * @param miAppId
   * @param miAppKey
   * @param profile  小米推送配置
   */
  public static void registerXiaomiPush(Context context, String miAppId, String miAppKey, String profile) {
    if (null == context) {
      throw new IllegalArgumentException("context cannot be null.");
    }

    if (AVUtils.isBlankString(miAppId)) {
      throw new IllegalArgumentException("miAppId cannot be null.");
    }

    if (AVUtils.isBlankString(miAppKey)) {
      throw new IllegalArgumentException("miAppKey cannot be null.");
    }

    if (!isXiaomiPhone()) {
      printErrorLog("register error, is not xiaomi phone!");
      return;
    }

    if (!checkXiaomiManifest(context)) {
      printErrorLog("register error, mainifest is incomplete!");
      return;
    }

    miDeviceProfile = profile;

    com.xiaomi.mipush.sdk.MiPushClient.registerPush(context, miAppId, miAppKey);

    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("start register mi push");
    }
  }

  /**
   * 注册华为推送
   * 只有 EMUI && manifest 正确填写 才能注册
   *
   * @param context
   */
  public static void registerHuaweiPush(Context context) {
    registerHuaweiPush(context, null);
  }

  /**
   * 注册华为推送
   * 只有是 EMUI && manifest 正确填写 才能注册
   *
   * @param context
   * @param profile 华为推送配置
   */
  public static void registerHuaweiPush(Context context, String profile) {
    if (null == context) {
      throw new IllegalArgumentException("context cannot be null.");
    }

    if (!isHuaweiPhone()) {
      printErrorLog("register error, is not huawei phone!");
      return;
    }

    if (!checkHuaweiManifest(context)) {
      printErrorLog("register error, mainifest is incomplete!");
      return;
    }

    hwDeviceProfile = profile;
    com.huawei.android.pushagent.PushManager.requestToken(context);

    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("start register hawei push");
    }
  }

  /**
   * 注册魅族推送
   * @param context
   * @param flymeId
   * @param flymeKey
   * @param profile 魅族推送配置
   */
  public static boolean registerFlymePush(Context context, String flymeId, String flymeKey, String profile) {
    if (null == context) {
      printErrorLog("register error, context is null!");
      return false;
    }
    boolean result = false;
    if (!com.meizu.cloud.pushsdk.util.MzSystemUtils.isBrandMeizu(context)) {
      printErrorLog("register error, is not flyme phone!");
    } else {
      if (!checkFlymeManifest(context)) {
        printErrorLog("register error, mainifest is incomplete!");
      } else {
        flymeDevicePrifile = profile;
        com.meizu.cloud.pushsdk.PushManager.register(context, flymeId, flymeKey);
        result = true;
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.d("start register flyme push");
        }
      }
    }
    return result;
  }

  /**
   * 注册魅族推送
   * @param context
   * @param flymeId
   * @param flymeKey
   */
  public static boolean registerFlymePush(Context context, String flymeId, String flymeKey) {
    return registerFlymePush(context, flymeId, flymeKey, null);
  }

  public static void setFlymeMStatusbarIcon(int icon) {
    flymeMStatusBarIcon = icon;
  }
  /**
   * 取消混合推送的注册
   * 取消成功后，消息会通过 LeanCloud websocket 发送
   */
  public static void unRegisterMixPush() {
    AVInstallation installation = AVInstallation.getCurrentInstallation();
    String vendor = installation.getString(AVInstallation.VENDOR);
    if (!AVUtils.isBlankContent(vendor)) {
      installation.put(AVInstallation.VENDOR, "lc");
      installation.saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (null != e) {
            printErrorLog("unRegisterMixPush error!");
          } else {
            LogUtil.avlog.d("Registration canceled successfully!");
          }
        }
      });
    }
  }

  private static boolean isHuaweiPhone() {
    final String phoneBrand = Build.BRAND;
    try {
      return (phoneBrand.equalsIgnoreCase("huawei") || phoneBrand.equalsIgnoreCase("honor"));
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean isXiaomiPhone() {
    final String phoneManufacturer = Build.MANUFACTURER;
    return !AVUtils.isBlankString(phoneManufacturer)
      && phoneManufacturer.toLowerCase().contains("xiaomi");
  }

  private static boolean checkXiaomiManifest(Context context) {
    try {
      return AVManifestUtils.checkReceiver(context, AVMiPushMessageReceiver.class);
    } catch (Exception e) {
      LogUtil.avlog.d(e.getMessage());
    }
    return false;
  }

  private static boolean checkHuaweiManifest(Context context) {
    boolean result = false;
    try {
      result = AVManifestUtils.checkPermission(context, android.Manifest.permission.INTERNET)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.READ_PHONE_STATE)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.WAKE_LOCK)
        && AVManifestUtils.checkService(context, com.huawei.android.pushagent.PushService.class)
        && AVManifestUtils.checkReceiver(context, AVHwPushMessageReceiver.class)
        && AVManifestUtils.checkReceiver(context, com.huawei.android.pushagent.PushEventReceiver.class);
    } catch (Exception e) {
    }
    return result;
  }

  private static boolean checkFlymeManifest(Context context) {
    boolean result = false;
    try {
      result = AVManifestUtils.checkPermission(context, android.Manifest.permission.INTERNET)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.READ_PHONE_STATE)
        && AVManifestUtils.checkPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE)
        && AVManifestUtils.checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        && AVManifestUtils.checkReceiver(context, AVFlymePushMessageReceiver.class);
    } catch (Exception e) {
    }
    return result;
  }

  private static void printErrorLog(String error) {
    if (AVOSCloud.isDebugLogEnabled() && !AVUtils.isBlankString(error)) {
      LogUtil.avlog.e(error);
    }
  }
}
