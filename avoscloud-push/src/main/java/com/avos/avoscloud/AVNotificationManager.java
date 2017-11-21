package com.avos.avoscloud;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.avos.avospush.session.StaleMessageDepot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wli on 2017/6/16.
 * 用来处理所有和 notification 相关的逻辑
 */

class AVNotificationManager {
  private static final String PUSH_INTENT_KEY = "com.avoscloud.push";
  private static final String PUSH_MESSAGE_DEPOT = "com.avos.push.message";
  private static final String LOGTAG = "AVNotificationManager";
  private static final String AV_PUSH_SERVICE_APP_DATA = "AV_PUSH_SERVICE_APP_DATA";
  private static final String ICON_KEY = "_notification_icon";

  private static final Random random = new Random();
  private final ConcurrentHashMap<String, String> defaultPushCallback =
    new ConcurrentHashMap<String, String>();
  private int notificationIcon;
  private final StaleMessageDepot depot;
  private Context context;

  private static AVNotificationManager notificationManager;

  public synchronized static AVNotificationManager getInstance() {
    if (null == notificationManager) {
      notificationManager = new AVNotificationManager(AVOSCloud.applicationContext);
    }
    return notificationManager;
  }

  private AVNotificationManager(Context context) {
    this.context = context;
    // Use application small icon by default.
    this.notificationIcon = context.getApplicationInfo().icon;

    depot = new StaleMessageDepot(PUSH_MESSAGE_DEPOT);
    readDataFromCache();
    if (AVOSCloud.isDebugLogEnabled()) {
      Log.d(LOGTAG, "Init AppManager Done, read data from cache: " + defaultPushCallback.size());
    }
  }

  @SuppressWarnings("unchecked")
  private void sendNotification(String from, String msg) throws IllegalArgumentException {
    Intent resultIntent = buildUpdateIntent(from, msg, null);
    sendNotification(from, msg, resultIntent);
  }

  private Intent buildUpdateIntent(String channel, String msg, String action) {
    Intent updateIntent = new Intent();
    if (action != null) {
      updateIntent.setAction(action);
    }
    updateIntent.putExtra(PUSH_INTENT_KEY, 1);
    updateIntent.putExtra("com.avos.avoscloud.Channel", channel);
    updateIntent.putExtra("com.avoscloud.Channel", channel);
    updateIntent.putExtra("com.parse.Channel", channel);
    updateIntent.putExtra("com.avos.avoscloud.Data", msg);
    updateIntent.putExtra("com.avoscloud.Data", msg);
    updateIntent.putExtra("com.parse.Data", msg);
    updateIntent.setPackage(context.getPackageName());
    return updateIntent;
  }

  private void sendBroadcast(String channel, String msg, String action) {
    Intent updateIntent = buildUpdateIntent(channel, msg, action);
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("action: " + updateIntent.getAction());
    }
    context.sendBroadcast(updateIntent);
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("sent broadcast");
    }
  }

  /**
   * 给订阅了小米 action 的 broadcastreciver 发 broadcast
   * @param channel
   * @param msg
   */
  private void sendNotificationBroadcast(String channel, String msg, String action) {
    Intent updateIntent = buildUpdateIntent(channel, msg, action);
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("action: " + updateIntent.getAction());
    }
    context.sendBroadcast(updateIntent);
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("sent broadcast");
    }
  }

  private String getChannel(String msg) {
    return AVUtils.getJSONValue(msg, "_channel");
  }

  private String getAction(String msg) {
    return AVUtils.getJSONValue(msg, "action");
  }

  /**
   * 是否为静默推送
   * 默认值为 false，及如果 server 并没有传 silent 字段，则默认为通知栏推送
   * @param message
   * @return
   */
  private boolean getSilent(String message) {
    if (!AVUtils.isBlankString(message)) {
      try {
        JSONObject object = new JSONObject(message);
        return (object.optBoolean("silent", false));
      } catch (JSONException e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.e("getSilent failed.", e);
        }
      }
    }
    return false;
  }

  private Date getExpiration(String msg) {
    String result = "";
    try {
      JSONObject object = new JSONObject(msg);
      result = object.getString("_expiration_time");
    } catch (JSONException e) {
      // LogUtil.avlog.i(e);
      // 不应该当做一个Error发出来，既然expire仅仅是一个option的数据
      // Log.e(LOGTAG, "Get expiration date error.", e);
    }
    if (AVUtils.isBlankString(result)) {
      return null;
    }
    Date date = AVUtils.dateFromString(result);
    return date;
  }

  /**
   * 处理 GCM 的透传消息
   * @param channel
   * @param action
   * @param message
   */
  void processGcmMessage(String channel, String action, String message) {
    if (channel == null || !containsDefaultPushCallback(channel)) {
      channel = AVOSCloud.applicationId;
      if (action != null) {
        sendBroadcast(channel, message, action);
      } else {
        sendNotification(channel, message);
      }
    }
  }

  /**
   * 处理华为、小米的透传消息（华为只有透传）
   * @param message
   */
  void processMixPushMessage(String message) {
    if (!AVUtils.isBlankString(message)) {
      String channel = getChannel(message);
      if (channel == null || !containsDefaultPushCallback(channel)) {
        channel = AVOSCloud.applicationId;
      }

      String action = getAction(message);
      boolean slient = getSilent(message);
      if (action != null) {
        sendBroadcast(channel, message, action);
      } else if (!slient) {
        sendNotification(channel, message);
      }
    }
  }

  /**
   * 处理混合推送通知栏消息点击后的事件（现在支持小米、魅族，华为不支持）
   * 处理逻辑：如果是自定义 action 的消息点击事件，则发送 broadcast，否则按照 sdk 自有逻辑打开相应的 activity
   * @param message
   */
  void processMixNotification(String message, String defaultAction) {
    if (!AVUtils.isBlankString(message)) {
      String channel = getChannel(message);
      if (channel == null || !containsDefaultPushCallback(channel)) {
        channel = AVOSCloud.applicationId;
      }

      String action = getAction(message);
      if (null != action) {
        sendNotificationBroadcast(channel, message, defaultAction);
      } else {
        String clsName = getDefaultPushCallback(channel);
        if (!AVUtils.isBlankString(clsName)) {
          Intent intent = buildUpdateIntent(channel, message, null);
          ComponentName cn = new ComponentName(context, clsName);
          intent.setComponent(cn);
          intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
          PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
          try {
            pendingIntent.send();
          } catch (PendingIntent.CanceledException e) {
            LogUtil.avlog.e("PendingIntent.CanceledException");
          }
        }
      }
    }
  }

  /**
   * 处理 LeanCloud 自有 socket 传过来的推送消息
   * @param message
   * @param messageId
   */
  void processPushMessage(String message, String messageId) {
    try {
      String channel = getChannel(message);
      if (channel == null || !containsDefaultPushCallback(channel)) {
        channel = AVOSCloud.applicationId;
      }

      Date expiration = getExpiration(message);
      if (expiration != null) {
        if (expiration.before(new Date())) {
          LogUtil.avlog.d("message expired:" + message);
          return;
        }
      }

      if (depot.putStableMessage(messageId)) {
        String action = getAction(message);
        if (action != null) {
          sendBroadcast(channel, message, action);
        } else {
          sendNotification(channel, message);
        }
      }
    } catch (Exception e) {
      LogUtil.avlog.e("Process notification failed.", e);
    }
  }

  /**
   * 处理混合推送到达事件（暂只支持小米）
   * @param message
   * @param action
   */
  void porcessMixNotificationArrived(String message, String action) {
    if (!AVUtils.isBlankString(message) && !AVUtils.isBlankString(action)) {
      String channel = getChannel(message);
      if (channel == null || !containsDefaultPushCallback(channel)) {
        channel = AVOSCloud.applicationId;
      }

      sendNotificationBroadcast(channel, message, action);
    }
  }

  private void readDataFromCache() {
    SharedPreferences appData = context.getSharedPreferences(AV_PUSH_SERVICE_APP_DATA, Context.MODE_PRIVATE);
    for (Map.Entry entry : appData.getAll().entrySet()) {
      String channel = (String) entry.getKey();
      if (channel.equals(ICON_KEY)) {
        try {
          notificationIcon = Integer.valueOf((String) entry.getValue());
        } catch (Exception e) {
          // ignore;
        }
      } else {
        String defaultCls = String.valueOf(entry.getValue());
        defaultPushCallback.put(channel, defaultCls);
      }
    }
  }

  private int getNotificationIcon() {
    return notificationIcon;
  }

  void setNotificationIcon(int icon) {
    notificationIcon = icon;
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(AV_PUSH_SERVICE_APP_DATA,
      ICON_KEY, String.valueOf(icon));
  }

  void addDefaultPushCallback(String channel, String clsName) {
    defaultPushCallback.put(channel, clsName);
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(AV_PUSH_SERVICE_APP_DATA,
      channel, String.valueOf(clsName));
  }

  void removeDefaultPushCallback(String channel) {
    defaultPushCallback.remove(channel);
    AVPersistenceUtils.sharedInstance().removePersistentSettingString(AV_PUSH_SERVICE_APP_DATA,
      channel);
  }

  private boolean containsDefaultPushCallback(String channel) {
    return defaultPushCallback.containsKey(channel);
  }

  String getDefaultPushCallback(String channel) {
    return AVUtils.isBlankString(channel) ? null : defaultPushCallback.get(channel);
  }

  public int size() {
    return defaultPushCallback.size();
  }

  @TargetApi(Build.VERSION_CODES.O)
  private void sendNotification(String from, String msg, Intent resultIntent) {
    String clsName = getDefaultPushCallback(from);
    if (AVUtils.isBlankString(clsName)) {
      throw new IllegalArgumentException(
        "No default callback found, did you forget to invoke setDefaultPushCallback?");
    }
    int lastIndex = clsName.lastIndexOf(".");
    if (lastIndex != -1) {
      // String packageName = clsName.substring(0, lastIndex);
      // Log.d(LOGTAG, "packageName: " + packageName);
      int notificationId = random.nextInt();
      ComponentName cn = new ComponentName(context, clsName);
      resultIntent.setComponent(cn);
      PendingIntent contentIntent =
        PendingIntent.getActivity(context, notificationId, resultIntent, 0);
      String sound = getSound(msg);
      Notification notification = null;
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(getTitle(msg)).setAutoCancel(true).setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentText(getText(msg));
        notification = mBuilder.build();
      } else {
        Notification.Builder builder = new Notification.Builder(context)
            .setSmallIcon(getNotificationIcon())
            .setContentTitle(getTitle(msg))
            .setAutoCancel(true).setContentIntent(contentIntent)
            .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
            .setContentText(getText(msg))
            .setChannelId(PushService.DefaultChannelId);

        notification = builder.build();
      }
      if (sound != null && sound.trim().length() > 0) {
        notification.sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + sound);
      }
      NotificationManager manager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      manager.notify(notificationId, notification);
    } else {
      Log.e(LOGTAG, "Class name is invalid, which must contain '.': " + clsName);
    }
  }

  private String getTitle(String msg) {
    return getValue(msg, "title");
  }

  private String getSound(String msg) {
    return getValue(msg, "sound");
  }

  private String getValue(String msg, String key) {
    String title = AVUtils.getJSONValue(msg, key);
    if (title != null && title.trim().length() > 0) {
      return title;
    } else {
      Map<String, Object> jsonMap = JSON.parseObject(msg, HashMap.class);
      if (jsonMap == null || jsonMap.isEmpty()) return getApplicationName();

      Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
      if (data == null || data.isEmpty()) {
        return getApplicationName();
      }
      Object val = data.get(key);
      if (val != null) {
        return val.toString();
      } else {
        return getApplicationName();
      }
    }
  }

  private String getApplicationName() {
    final PackageManager pm = context.getPackageManager();
    ApplicationInfo ai;
    try {
      ai = pm.getApplicationInfo(context.getPackageName(), 0);
    } catch (final PackageManager.NameNotFoundException e) {
      ai = null;
    }
    final String applicationName =
      (String) (ai != null ? pm.getApplicationLabel(ai) : "Notification");
    return applicationName;
  }

  @SuppressWarnings("unchecked")
  private String getText(String msg) {
    String text = AVUtils.getJSONValue(msg, "alert");
    if (text != null && text.trim().length() > 0) {
      return text;
    } else {
      Map<String, Object> jsonMap = JSON.parseObject(msg, HashMap.class);
      if (jsonMap == null || jsonMap.isEmpty()) return null;

      Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
      if (data == null || data.isEmpty()) {
        return null;
      }
      Object val = data.get("message");
      if (val != null) {
        return val.toString();
      } else {
        return null;
      }
    }
  }
}
