package com.avos.avoscloud;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.LogUtil.log;

/**
 * <p>
 * The AVAnalytics class provides an interface to AVOS Cloud logging and analytics backend. Methods
 * will return immediately and cache requests (+ timestamps) to be handled "eventually." That is,
 * the request will be sent immediately if possible or the next time a network connection is
 * available otherwise.
 * </p>
 */
public class AVAnalytics {

  private static final String NEW_CHANNEL_ID = "leancloud";

  private static final String OLD_CHANNEL_ID = "Channel ID";

  public static final String TAG = AVAnalytics.class.getSimpleName();

  static private String endPoint = "statistics";
  static private String appOpen = "_appOpen";
  static private String appOpenWithPush = "_appOpenWithPush";
  static AnalyticsImpl impl = AnalyticsImpl.getInstance();

  /**
   * AVAnalytics是统计的核心类，本身不需要实例化，所有方法以类方法的形式提供. 目前发送策略有REALTIME, BATCH, SENDDAILY, SENDWIFIONLY,
   * SEND_INTERVAL, SEND_ON_EXIT. 其中REALTIME, SENDWIFIONLY 只在模拟器和DEBUG模式下生效，真机release模式会自动改成BATCH.
   * SEND_INTERVAL 为按最小间隔发送,默认为10秒,取值范围为10 到 86400(一天)， 如果不在这个区间的话，会按10设置. SEND_ON_EXIT
   * 为退出或进入后台时发送,这种发送策略在App运行过程中不发送，对开发者和用户的影响最小.
   * 
   * @since 1.4.1
   */
  public AVAnalytics() {
    super();
  }

  /**
   * Tracks this application being launched (and if this happened as the result of the user opening
   * a push notification, this method sends along information to correlate this open with that
   * push).
   * 
   * @param intent The Intent that started an Activity, if any. Can be null.
   */

  static public void trackAppOpened(Intent intent) {
    Map<String, String> map = statisticsDictionary(appOpen);
    onEvent(AVOSCloud.applicationContext, "!AV!AppOpen", map);
    // It's opened by push notification.
    if (intent != null && intent.getIntExtra(AVConstants.PUSH_INTENT_KEY, -1) == 1) {
      trackPushOpened(intent);
    }
  }

  /**
   * 请使用在线配置指定的报告发送策略
   */
  @Deprecated
  public void setDefaultReportPolicy(Context ctx, ReportPolicy policy) {
    impl.setReportPolicy(policy);
  }

  private static void trackPushOpened(Intent intent) {
    Map<String, String> map = statisticsDictionary(appOpenWithPush);
    onEvent(AVOSCloud.applicationContext, "!AV!PushOpen", map);
  }

  /**
   * Set app store channel.
   * 
   * @param channel The Channel name.
   */
  static public void setAppChannel(final String channel) {
    if (AVUtils.isBlankString(channel)) {
      throw new IllegalArgumentException("Blank channel string.");
    }
    impl.setAppChannel(channel);
  }

  static String getAppChannel() {
    return impl.getAppChannel();
  }

  /**
   * 设置扩展用户信息，您可以使用此方法加入自定义的信息，比如，您可以通过此函数收集额外的设备相关信息或者其他信息。 请使用setCustomInfo来替代本方法
   * 
   * @param customInfo 扩展信息map.
   * @return .
   * @since 2.3.1
   */
  @Deprecated
  static public void SetCustomInfo(final Map<String, String> customInfo) {
    impl.setCustomInfo(customInfo);
  }

  /**
   * 设置扩展用户信息，您可以使用此方法加入自定义的信息，比如，您可以通过此函数收集额外的设备相关信息或者其他信息。
   * 
   * @param customInfo 扩展信息map.
   * @return .
   * @since 2.3.1
   */
  static public void setCustomInfo(final Map<String, String> customInfo) {
    impl.setCustomInfo(customInfo);
  }

  /**
   * 获取扩展的用户信息.
   * 
   * @return 扩展设备信息map.
   * @since 2.3.1
   */
  static public Map<String, String> getCustomInfo() {
    return impl.getCustomInfo();
  }

  static private Map<String, String> statisticsDictionary(String event) {
    if (AVUtils.isBlankString(event)) {
      throw new IllegalArgumentException("Blank event string.");
    }
    Map<String, String> map = new HashMap<String, String>();
    map.put("event_id", event);
    map.put("channel", impl.getAppChannel());
    return map;
  }

  static private void postAnalytics(Map<String, Object> map) {
    try {
      String postData = AVUtils.jsonStringFromMapWithNull(map);
      PaasClient.statistisInstance().postObject(endPoint, postData, false, true,
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              LogUtil.log.d(content);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              LogUtil.log.e(content);
            }
          }, null, AVUtils.md5(postData));
    } catch (Exception e) {
      log.e(TAG, "post analytics data failed.", e);
    }
  }

  /**
   * 开启统计,默认以BATCH方式发送log.
   * 该函数会在 AVOSCloud.initialize 时被反射调用，为了防止乱用，此处设为 private
   * @param context Application的context.
   * @return void
   * @since 1.4.1
   */
  private static void start(Context context) {
    try {
      ApplicationInfo info =
          context.getPackageManager().getApplicationInfo(context.getPackageName(),
              PackageManager.GET_META_DATA);
      Bundle bundle = info.metaData;
      if (bundle != null) {
        String channel =
            info.metaData.get(OLD_CHANNEL_ID) == null ? null : String.valueOf(info.metaData
                .get(OLD_CHANNEL_ID));
        String newChannel =
            info.metaData.get(NEW_CHANNEL_ID) == null ? null : String.valueOf(info.metaData
                .get(NEW_CHANNEL_ID));
        if (!AVUtils.isBlankString(channel)) {
          impl.setAppChannel(channel);
        } else if (!AVUtils.isBlankString(newChannel)) {
          impl.setAppChannel(newChannel);
        }
      }
      impl.enableCrashReport(context, true);
      impl.flushLastSessions(context);
      impl.updateOnlineConfig();
      impl.beginSession();
      impl.reportFirstBoot(context);
    } catch (Exception exception) {
      log.e(TAG, "Start context failed.", exception);
    }
  }

  /**
   * 记录Android Fragment的启动事件.
   * 
   * @param pageName Fragment的页面名称.
   * @return .
   * @since 1.4.2
   */
  public static void onFragmentStart(String pageName) {
    if (AVUtils.isBlankString(pageName)) {
      throw new IllegalArgumentException("Blank page name string.");
    }
    impl.beginFragment(pageName);
  }

  /**
   * 记录Android Fragment的结束事件.
   * 
   * @param pageName Fragment的页面名称.
   * @return .
   * @since 1.4.2
   */
  public static void onFragmentEnd(String pageName) {
    if (AVUtils.isBlankString(pageName)) {
      throw new IllegalArgumentException("Blank page name string.");
    }
    impl.endFragment(pageName);
  }

  public static void setAutoLocation(boolean b) {
    impl.setAutoLocation(b);
  }

  /**
   * 设置session的持续时间阈值，超过此时间间隔，认为是一个新的session.
   * 
   * @param ms session持续时间，毫秒为单位.
   * @return .
   * @since 1.4.1
   */
  public static void setSessionContinueMillis(long ms) {
    if (ms <= 0) {
      throw new IllegalArgumentException("Invalid session continute milliseconds.");
    }
    impl.setSessionContinueMillis(ms);
  }

  /**
   * 设置是否打印sdk的log信息,默认不开启
   * 
   * @param enable true, SDK 会输出log信息, 在发布的时候，请设置为false.
   * @return .
   * @since 1.4.1
   */
  public static void setDebugMode(boolean enable) {
    impl.setEnableDebugLog(enable);
  }

  /**
   * 开启CrashReport收集, 默认是开启状态.
   * 
   * @param enable 设置成false,就可以关闭CrashReport收集.
   * @return void.
   * @since 1.4.1
   */
  public static void enableCrashReport(Context context, boolean enable) {
    impl.enableCrashReport(context, enable);
  }

  /**
   * 页面时长统计, 当页面隐藏或者停止时，调用此函数.通常它在Activity子类的onPause中调用.
   * 
   * @param context 已经停止的记录时长的context，通常是Activity.
   * @return void.
   * @since 1.4.1
   */
  public static void onPause(Context context) {
    onPause(context,context.getClass().getSimpleName());
  }

  /**
   * 页面时长统计, 当页面隐藏或者停止时，调用此函数.通常它在Activity子类的onPause中调用.
   * @param context 已经停止的记录时长的context，通常是Activity.
   * @param name 自定义页面名
     */
  public static void onPause(Context context,String name){
    impl.endActivity(AVUtils.isBlankString(name)?context.getClass().getSimpleName():name);
    impl.pauseSession();
  }

  /**
   * 页面时长统计, 当页面resume时调用此函数.通常它在Activity子类的onResume中调用.
   * 
   * @param context 重新执行的context，通常是Activity.
   * @return void.
   * @since 1.4.1
   */
  public static void onResume(Context context) {
    onResume(context, context.getClass().getSimpleName());
  }

  /**
   * 页面时长统计, 当页面resume时调用此函数.通常它在Activity子类的onResume中调用.
   *
   * @param context 重新执行的context，通常是Activity.
   * @param name 自定义页面名
     */
  public static void onResume(android.content.Context context, java.lang.String name) {
    if (impl.shouldRegardAsNewSession()) {
      impl.endSession();
      impl.beginSession();
      LogUtil.avlog.d("new session start when resume");
    }
    // impl.endActivity(name); 这个地方为什么要先停再开呢？
    impl.beginActivity(AVUtils.isBlankString(name)?context.getClass().getSimpleName():name);
  }

  // kind of crash or exception.
  public static void onError(android.content.Context context) {

  }

  public static void onError(android.content.Context context, java.lang.String s) {

  }

  public static void reportError(android.content.Context context, java.lang.String s) {

  }

  public static void reportError(android.content.Context context, java.lang.Throwable throwable) {

  }

  static void reportError(Context context, Map<String, Object> crashData,
      final SaveCallback callback) {
    Map<String, Object> map = AnalyticsUtils.deviceInfo(context);
    map.putAll(crashData);
    String jsonString = JSON.toJSONString(map);
    PaasClient.statistisInstance().postObject("stats/crash", jsonString, false, true,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (impl.isEnableDebugLog()) {
              Log.i(TAG, "Save success: " + content);
            }
            if (callback != null) {
              callback.internalDone(null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (impl.isEnableDebugLog()) {
              Log.i(TAG, "Save failed: " + content);
            }
            if (callback != null) {
              callback.internalDone(AVErrorUtils.createException(error, content));
            }
          }
        }, null, AVUtils.md5(jsonString));

  }

  /**
   * 强制发送本地所有收集的统计.
   * 
   * @param context 通常是Activity.
   * @return void.
   * @since 1.4.1
   */
  public static void flush(android.content.Context context) {
    impl.sendInstantRecordingRequest();
  }

  protected static void debugDump(android.content.Context context) {
    impl.debugDump(context);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @return void.
   * @since 1.4.1
   */
  public static void onEvent(android.content.Context context, java.lang.String eventId) {
    onEvent(context, eventId, 1);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param acc 事件的累计发生次数，可以将相同的事件一起发送，节约流量.
   * @return void.
   * @since 1.4.1
   */
  public static void onEvent(android.content.Context context, java.lang.String eventId, int acc) {
    onEvent(context, eventId, "", acc);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件标签.
   * @return void.
   * @since 1.4.1
   */
  public static void onEvent(android.content.Context context, java.lang.String eventId,
      java.lang.String label) {
    onEvent(context, eventId, label, 1);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件标签.
   * @param acc 事件的累计发生次数，可以将相同的事件一起发送，节约流量.
   * @return void.
   * @since 1.4.1
   */
  public static void onEvent(android.content.Context context, java.lang.String eventId,
      java.lang.String label, int acc) {
    AnalyticsEvent event = impl.beginEvent(context, eventId, label, "");
    event.setDurationValue(0);
    event.setAccumulation(acc);
    impl.endEvent(context, eventId, label, "");
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param stringHashMap 事件的属性列表.
   * @return void.
   * @since 1.4.1
   */
  public static void onEvent(android.content.Context context, java.lang.String eventId,
      java.util.Map<java.lang.String, java.lang.String> stringHashMap) {
    AnalyticsEvent event = impl.beginEvent(context, eventId, "", "");
    event.addAttributes(stringHashMap);
    impl.endEvent(context, eventId, "", "");
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param msDuration 事件的持续时间，以毫秒为单位.
   * @return void.
   * @since 1.4.1
   */
  public static void onEventDuration(android.content.Context context, java.lang.String eventId,
      long msDuration) {
    onEventDuration(context, eventId, "", msDuration);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件的标签.
   * @param msDuration 事件的持续时间，以毫秒为单位.
   * @return void.
   * @since 1.4.1
   */
  public static void onEventDuration(android.content.Context context, java.lang.String eventId,
      java.lang.String label, long msDuration) {
    onEventDuration(context, eventId, label, null, msDuration);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param stringHashMap 事件的属性列表.
   * @param msDuration 事件的持续时间，以毫秒为单位.
   * @return void.
   * @since 1.4.1
   */
  public static void onEventDuration(android.content.Context context, java.lang.String eventId,
      java.util.Map<java.lang.String, java.lang.String> stringHashMap, long msDuration) {
    onEventDuration(context, eventId, "", stringHashMap, msDuration);
  }

  /**
   * 自定义事件,数量统计.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件的标签.
   * @param stringHashMap 事件的属性列表.
   * @param msDuration 事件的持续时间，以毫秒为单位.
   * @return void.
   * @since 1.4.1
   */
  private static void onEventDuration(android.content.Context context, java.lang.String eventId,
      String label, java.util.Map<java.lang.String, java.lang.String> stringHashMap, long msDuration) {
    AnalyticsEvent event = impl.beginEvent(context, eventId, label, "");
    event.addAttributes(stringHashMap);
    event.setDurationValue(msDuration);
    impl.endEvent(context, eventId, label, "");
  }

  /**
   * 自定义事件,数量统计. 记录事件开始数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @since 1.4.1
   */
  public static void onEventBegin(android.content.Context context, java.lang.String eventId) {
    onEventBegin(context, eventId, "");
  }

  /**
   * 自定义事件,数量统计. 记录事件结束数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @since 1.4.1
   */
  public static void onEventEnd(android.content.Context context, java.lang.String eventId) {
    impl.endEvent(context, eventId, "", "");
  }

  /**
   * 自定义事件,数量统计. 记录事件开始数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件标签.
   * @since 1.4.1
   */
  public static void onEventBegin(android.content.Context context, java.lang.String eventId,
      java.lang.String label) {
    impl.beginEvent(context, eventId, label, "");
  }

  /**
   * 自定义事件,数量统计. 记录事件结束数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param label 事件标签.
   * @since 1.4.1
   */
  public static void onEventEnd(android.content.Context context, java.lang.String eventId,
      java.lang.String label) {
    impl.endEvent(context, eventId, label, "");
  }

  /**
   * 自定义事件,数量统计. 记录关键事件开始数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param stringStringHashMap 事件属性列表.
   * @param primaryKey 关键事件标签。
   * @since 1.4.1
   */
  public static void onKVEventBegin(android.content.Context context, java.lang.String eventId,
      java.util.HashMap<java.lang.String, java.lang.String> stringStringHashMap,
      java.lang.String primaryKey) {
    AnalyticsEvent event = impl.beginEvent(context, eventId, "", primaryKey);
    event.setPrimaryKey(primaryKey);
  }

  /**
   * 自定义事件,数量统计. 记录关键事件结束数据.
   * 
   * @param context 通常是Activity.
   * @param eventId 事件Id.
   * @param primaryKey 关键事件标签。
   * @since 1.4.1
   */
  public static void onKVEventEnd(android.content.Context context, java.lang.String eventId,
      java.lang.String primaryKey) {
    impl.endEvent(context, eventId, "", primaryKey);
  }

  /**
   * 获取在线配置数据，如果没有返回空字符串""
   * 
   * @param context 通常是Activity.
   * @param key 需要获取的数据的键值.
   * @return key对应的配置字符串数据,如果数据不存在，返回空字符串""。
   * @since 1.4.1
   */
  public static java.lang.String getConfigParams(android.content.Context context,
      java.lang.String key) {
    return getConfigParams(context, key, "");
  }

  /**
   * 获取在线配置数据，如果没有返回传入的默认参数。
   * 
   * @param ctx 通常是Activity.
   * @param key 需要获取的数据的键值.
   * @param defaultValue 当key对应的在线配置数据不存在的时候，返回此值
   * @since 1.4.2
   * @return
   */
  public static String getConfigParams(Context ctx, String key, String defaultValue) {
    return impl.getConfigParams(key, defaultValue);
  }

  /**
   * 更新在线配置数据.
   * 
   * @param context 通常是Activity.
   * @return.
   * @since 1.4.1
   */
  public static void updateOnlineConfig(Context context) {
    impl.updateOnlineConfig();
  }

  /**
   * 更新在线配置数据
   * @param context 通常是Activity.
   * @param callback 回调函数,配置数据会通过此函数返回
   */
  public static void updateOnlineConfig(Context context, AVCallback<Map<String, Object>> callback) {
    impl.updateOnlineConfig(context, callback);
  }

  public void setGender(Context context, String gender) {}

  public void setAge(Context context, int i) {}

  public void setUserID(Context context, String s, String s1) {}

  public static void onKillProcess(Context context) {}

  /**
   * 请使用在线配置指定的报告发送策略
   */
  @Deprecated
  public static void setReportPolicy(Context context, ReportPolicy policy) {
    if (policy == null) {
      throw new IllegalArgumentException("Null report policy.");
    }
    impl.setReportPolicy(policy);
  }

  /**
   * 设置在线参数监听器，只在在线参数有变化的时候才会回调监听器
   * 
   * @since 1.4.2
   * @see AVOnlineConfigureListener
   * @param listener
   */
  public static void setOnlineConfigureListener(AVOnlineConfigureListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Null AVOnlineConfigureListener.");
    }
    impl.setAVOnlineConfigureListener(listener);
  }

  /**
   * 全局开关，用于关闭所有统计数据.
   * 
   * @param enable
   */

  public static void setAnalyticsEnabled(boolean enable) {
    impl.setAnalyticsEnabled(enable);
  }
}
