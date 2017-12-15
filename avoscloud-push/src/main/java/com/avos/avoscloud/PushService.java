package com.avos.avoscloud;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageOption;
import com.avos.avoscloud.im.v2.Conversation;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avospush.push.*;
import com.avos.avospush.session.CommandPacket;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A service to listen for push notifications. This operates in the same process as the parent
 * application. To use this class, the PushService must be registered. Add this XML right before the
 * </application> tag in your AndroidManifest.xml:
 * </p>
 * <p/>
 * <pre>
 *    <service android:name="com.avos.avoscloud.PushService" />
 *    <receiver android:name="com.avos.avoscloud.AVBroadcastReceiver">
 *        <intent-filter>
 *            <action android:name="android.intent.action.BOOT_COMPLETED" />
 *            <action android:name="android.intent.action.USER_PRESENT" />
 *        </intent-filter>
 *    </receiver>
 * </pre>
 * <p>
 * Next, you must ensure your app has the permissions needed to show a notification. Make sure that
 * these permissions are present in your AndroidManifest.xml, typically immediately before the
 * </manifest> tag:
 * </p>
 * <p/>
 * <pre>
 *    <uses-permission android:name="android.permission.INTERNET" />
 *    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 *    <uses-permission android:name="android.permission.VIBRATE" />
 * </pre>
 * <p>
 * Once push notifications are configured in the manifest, you can subscribe to a push channel by
 * calling
 * </p>
 * <p/>
 * <pre>
 * PushService.subscribe(context, &quot;the_channel_name&quot;, YourActivity.class);
 * </pre>
 * <p>
 * When the client receives a push message, a notification will appear in the system tray. When the
 * user taps the notification, they will enter the application through a new instance of
 * YourActivity.
 * </p>
 */
public class PushService extends Service {
  private static final String LOGTAG = PushService.class.getName();
  private static AVPushConnectionManager sPushConnectionManager;
  private static Object connecting = new Object();
  private volatile static boolean isStarted = false;

  // 是否需要唤醒其他同样使用 LeanCloud 服务的 app，此变量用于缓存结果，避免无意义调用
  private static boolean isNeedNotifyApplication = true;

  /**
   * 是否自动唤醒 PushService
   */
  private static boolean isAutoWakeUp = true;
  static String DefaultChannelId = "";
  
  AVConnectivityReceiver connectivityReceiver;
  AVShutdownReceiver shutdownReceiver;

  /**
   * Client code should not call onCreate directly.
   */
  @Override
  public void onCreate() {
    LogUtil.log.d(LOGTAG, "On Create");
    super.onCreate();
    sPushConnectionManager = AVPushConnectionManager.getInstance(this);

    connectivityReceiver = new AVConnectivityReceiver(new AVConnectivityListener() {
      @Override
      public void onMobile(Context context) {
        sPushConnectionManager.initConnection();
      }

      @Override
      public void onWifi(Context context) {
        sPushConnectionManager.initConnection();
      }

      @Override
      public void onNotConnected(Context context) {
        //sPushConnectionManager.cleanupSocketConnection();
      }
    });
    registerReceiver(connectivityReceiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    shutdownReceiver = new AVShutdownReceiver(new AVShutdownListener() {
      @Override
      public void onShutdown(Context context) {
        sPushConnectionManager.cleanupSocketConnection();
      }
    });
    registerReceiver(shutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
    isStarted = true;
  }

  @TargetApi(Build.VERSION_CODES.ECLAIR)
  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    notifyOtherApplication(null != intent ? intent.getAction() : null);
    // Here to get intent from SessionManager
    if (AVUtils.isConnected(this) && isPushConnectionBroken()) {
      synchronized (connecting) {
        try {
          AVInstallation installation = AVInstallation.getCurrentInstallation();
          final String installationId = installation.getInstallationId();
          if (installation.isDirty()) {
            installation.saveInBackground();
          }
          LogUtil.log.d(LOGTAG, "Start to connect to push server with installationId "
              + installationId);
          sPushConnectionManager
              .initConnection(new AVCallback() {
                @Override
                protected void internalDone0(Object o, AVException exception) {
                  if (exception == null) {
                    processIMRequests(intent);
                  } else {
                    reportRouterConnectionException(intent, exception);
                  }
                }
              });

        } catch (Exception e) {
          LogUtil.log
              .e(LOGTAG,
                  "Exception when init connection, looks like you have not called AVOSCloud.initialized yet",
                  e);
          e.printStackTrace();
        }
      }
    } else {
      processIMRequests(intent);
    }
    // http://developer.android.com/reference/android/app/Service.html#START_STICKY
    return START_STICKY;
  }

  private void reportRouterConnectionException(Intent intent, AVException e) {
    if (intent != null
        && Conversation.AV_CONVERSATION_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
      int operationCode = intent.getExtras().getInt(Conversation.INTENT_KEY_OPERATION);
      String clientId = intent.getExtras().getString(Conversation.INTENT_KEY_CLIENT);
      String conversationId = intent.getExtras().getString(Conversation.INTENT_KEY_CONVERSATION);
      int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);

      BroadcastUtil.sendIMLocalBroadcast(clientId, conversationId, requestId, e,
          AVIMOperation.getAVIMOperation(operationCode));
    }
  }

  private void processLiveQueryEventFromClient(Intent intent) {
    String subscribeId = intent.getExtras().getString("id");
    int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);
    sPushConnectionManager.sendLiveQueryLoginCmd(subscribeId, requestId);
  }

  private void processIMRequests(Intent intent) {
    if (null != intent) {
      if (Conversation.AV_CONVERSATION_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
        processConversationEventsFromClient(intent);
      }

      if (AVLiveQuery.ACTION_LIVE_QUERY_LOGIN.equalsIgnoreCase(intent.getAction())) {
        processLiveQueryEventFromClient(intent);
      }

      if (Conversation.AV_CONVERSATION_PARCEL_ACTION.equalsIgnoreCase(intent.getAction())) {
        processConversationParcelEventFromClient(intent);
      }
    }
  }

  /**
   * Client code should not call onDestroy directly.
   */
  @Override
  public void onDestroy() {
    LogUtil.log.d(LOGTAG, "On Destroy");
    if (sPushConnectionManager != null) {
      sPushConnectionManager.stop();
    }
    unregisterReceiver(connectivityReceiver);
    unregisterReceiver(shutdownReceiver);
    isStarted = false;

    if (isAutoWakeUp && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      // Let's try to wake PushService again
      try {
        Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(i);
      } catch (Exception ex) {
        // i have tried my best.
        LogUtil.log.e("failed to start PushService. cause: " + ex.getMessage());
      }
    }
    super.onDestroy();
  }

  /**
   * onBind should not be called directly.
   */
  @Override
  public IBinder onBind(Intent intent) {
    LogUtil.log.d(LOGTAG, "On bind");
    return null;
  }

  private static void startServiceIfRequired(Context context,
                                             final java.lang.Class<? extends android.app.Activity> cls) {
    if (isStarted) {
      return;
    }

    if (context == null) {
      LogUtil.log.d(LOGTAG, "context is null");
      return;
    }

    if (!AVUtils.checkPermission(context, "android.permission.INTERNET")) {
      LogUtil.log
          .e(LOGTAG,
              "Please add <uses-permission android:name=\"android.permission.INTERNET\"/> in your AndroidManifest file");
      return;
    }

    if (!AVUtils.isConnected(context)) {
      LogUtil.log.d(LOGTAG, "No network available now");
      return;
    }

    if (!AVUtils.isPushServiceAvailable(context, PushService.class)) {
      LogUtil.log
          .e(LOGTAG,
              "Please add <service android:name=\"com.avos.avoscloud.PushService\"/> in your AndroidManifest file");
      return;
    }

    startService(context, cls);
  }

  private static synchronized void startService(Context context, final java.lang.Class cls) {
    final Context finalContext = context;
    new Thread(new Runnable() {
      @Override
      public void run() {
        LogUtil.log.d(LOGTAG, "Start service");
        try {
          Intent intent = new Intent(finalContext, PushService.class);
          intent.putExtra(AVConstants.AV_PUSH_SERVICE_APPLICATION_ID, AVOSCloud.applicationId);
          if (cls != null) {
            intent.putExtra(AVConstants.AV_PUSH_SERVICE_DEFAULT_CALLBACK, cls.getName());
          }
          finalContext.startService(intent);
        } catch (Exception ex) {
          // i have tried my best.
          LogUtil.log.e("failed to start PushService. cause: " + ex.getMessage());
        }
      }
    }).start();
  }

  /**
   * Helper function to subscribe to push notifications with the default application icon.
   *
   * @param context This is used to access local storage to cache the subscription, so it must
   *                currently be a viable context.
   * @param channel A string identifier that determines which messages will cause a push
   *                notification to be sent to this client. The channel name must start with a letter and
   *                contain only letters, numbers, dashes, and underscores.
   * @param cls     This should be a subclass of Activity. An instance of this Activity is started when
   *                the user responds to this push notification. If you are not sure what to use here,
   *                just
   *                use your application's main Activity subclass.
   */
  public static synchronized void subscribe(android.content.Context context,
                                            java.lang.String channel, java.lang.Class<? extends android.app.Activity> cls) {
    startServiceIfRequired(context, cls);
    final String finalChannel = channel;
    AVInstallation.getCurrentInstallation().addUnique("channels", finalChannel);
    _installationSaveHandler.sendMessage(Message.obtain());

    if (cls != null) {
      AVNotificationManager manager = AVNotificationManager.getInstance();
      manager.addDefaultPushCallback(channel, cls.getName());

      // set default push callback if it's not exist yet
      if (manager.getDefaultPushCallback(AVOSCloud.applicationId) == null) {
        manager.addDefaultPushCallback(AVOSCloud.applicationId, cls.getName());
      }
    }

  }

  /**
   * 设置推送消息的Icon图标，如果没有设置，默认使用您配置里的应用图标。
   *
   * @param icon A resource ID in the application's package of the drawble to use.
   * @since 1.4.4
   */
  public static void setNotificationIcon(int icon) {
    AVNotificationManager.getInstance().setNotificationIcon(icon);
  }

  /**
   * Provides a default Activity class to handle pushes. Setting a default allows your program to
   * handle pushes that aren't registered with a subscribe call. This can happen when your
   * application changes its subscriptions directly through the AVInstallation or via push-to-query.
   *
   * @param context This is used to access local storage to cache the subscription, so it must
   *                currently be a viable context.
   * @param cls     This should be a subclass of Activity. An instance of this Activity is started when
   *                the user responds to this push notification. If you are not sure what to use here,
   *                just
   *                use your application's main Activity subclass.
   */
  public static void setDefaultPushCallback(android.content.Context context,
                                            java.lang.Class<? extends android.app.Activity> cls) {
    startServiceIfRequired(context, cls);
    AVNotificationManager.getInstance().addDefaultPushCallback(AVOSCloud.applicationId, cls.getName());
  }

  /**
   * Cancels a previous call to subscribe. If the user is not subscribed to this channel, this is a
   * no-op. This call does not require internet access. It returns without blocking
   *
   * @param context A currently viable Context.
   * @param channel The string defining the channel to unsubscribe from.
   */
  public static synchronized void unsubscribe(android.content.Context context,
                                              java.lang.String channel) {
    if (channel == null) {
      return;
    }
    AVNotificationManager.getInstance().removeDefaultPushCallback(channel);
    final java.lang.String finalChannel = channel;
    if (AVUtils.isBlankString(AVInstallation.getCurrentInstallation().getObjectId())) {
      AVInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (e == null) {
            AVInstallation.getCurrentInstallation().removeAll("channels",
                Arrays.asList(finalChannel));
            _installationSaveHandler.sendMessage(Message.obtain());
          } else {
            if (AVOSCloud.showInternalDebugLog()) {
              e.printStackTrace();
            }
          }
        }
      });
    } else {
      AVInstallation.getCurrentInstallation().removeAll("channels", Arrays.asList(finalChannel));
      _installationSaveHandler.sendMessage(Message.obtain());
    }
  }

  static synchronized void sendData(CommandPacket packet) {
    if (sPushConnectionManager != null) {
      sPushConnectionManager.sendData(packet);
    }
  }
  
  /*
   * https://groups.google.com/forum/#!topic/android-developers/H-DSQ4-tiac
   * @see android.app.Service#onTaskRemoved(android.content.Intent)
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTaskRemoved(Intent rootIntent) {
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.log.d("try to restart service on task Removed");
    }

    if (isAutoWakeUp) {
      Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
      restartServiceIntent.setPackage(getPackageName());

      PendingIntent restartServicePendingIntent =
        PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent,
          PendingIntent.FLAG_UPDATE_CURRENT);
      AlarmManager alarmService =
        (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
      alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500,
        restartServicePendingIntent);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      super.onTaskRemoved(rootIntent);
    }
  }

  // 这个方法只是获取当前是否连接的状态
  protected static boolean isPushConnectionBroken() {
    return (sPushConnectionManager == null || (sPushConnectionManager != null && !sPushConnectionManager
        .isConnectedToPushServer()));
  }

  private void processConversationParcelEventFromClient(Intent intent) {
    int operationCode = intent.getExtras().getInt(Conversation.INTENT_KEY_OPERATION);
    String clientId = intent.getExtras().getString(Conversation.INTENT_KEY_CLIENT);
    String conversationId = intent.getExtras().getString(Conversation.INTENT_KEY_CONVERSATION);
    int convType = intent.getExtras().getInt(Conversation.INTENT_KEY_CONV_TYPE, Conversation.CONV_TYPE_NORMAL);
    int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);
    AVIMOperation operation = AVIMOperation.getAVIMOperation(operationCode);
    AVSession session = sPushConnectionManager.getOrCreateSession(clientId);

    PushServiceParcel parcel = intent.getExtras().getParcelable(Conversation.INTENT_KEY_DATA);

    switch (operation) {
      case CONVERSATION_RECALL_MESSAGE:
      case CONVERSATION_UPDATE_MESSAGE:
        if (!AVUtils.isBlankString(conversationId)) {
          AVInternalConversation conversation = session.getConversation(conversationId, convType);
          if (null != conversation) {
            conversation.patchMessage(parcel, operation, requestId);
          } else {
            LogUtil.log.d("can't find out conversation with id:" + conversationId);
          }
        } else {
          LogUtil.log.d("conversationId is mandatory for MessageRecall or MessageUpdate.");
        }
        break;
    }
  }

  private void processConversationEventsFromClient(Intent intent) {
    int operationCode = intent.getExtras().getInt(Conversation.INTENT_KEY_OPERATION);
    String clientId = intent.getExtras().getString(Conversation.INTENT_KEY_CLIENT);
    String conversationId = intent.getExtras().getString(Conversation.INTENT_KEY_CONVERSATION);
    int convType = intent.getExtras().getInt(Conversation.INTENT_KEY_CONV_TYPE, Conversation.CONV_TYPE_NORMAL);
    int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);
    AVSession session = sPushConnectionManager.getOrCreateSession(clientId);

    Map<String, Object> params = null;
    AVIMOperation operation = AVIMOperation.getAVIMOperation(operationCode);
    if (operation != AVIMOperation.CONVERSATION_SEND_MESSAGE) {
      String intentData = intent.getExtras().getString(Conversation.INTENT_KEY_DATA);
      if (!AVUtils.isBlankString(intentData)) {
        params = JSON.parseObject(intentData, Map.class);
      }
    }

    // 先检查一下session的状态，但是消息记录查询和conversation查询由于支持缓存则跳过
    if (operation != AVIMOperation.CLIENT_OPEN
        && operation != AVIMOperation.CONVERSATION_MESSAGE_QUERY
        && operation != AVIMOperation.CONVERSATION_QUERY) {
      AVException connectionException = session.checkSessionStatus();
      if (connectionException != null) {
        BroadcastUtil.sendIMLocalBroadcast(clientId, conversationId, requestId,
            connectionException, operation);
        return;
      }
    }

    switch (operation) {
      case CLIENT_OPEN:
        AVIMClientParcel parcel = intent.getExtras().getParcelable(Conversation.INTENT_KEY_CLIENT_PARCEL);
        session.open(parcel, requestId);
        break;
      case CLIENT_DISCONNECT:
        session.close(requestId);
        break;
      case CLIENT_ONLINE_QUERY:
        List<String> idList = (List<String>) params.get(Conversation.PARAM_ONLINE_CLIENTS);
        session.queryOnlinePeers(idList, requestId);
        break;
      case CONVERSATION_CREATION:
        List<String> memberList = (List<String>) params.get(Conversation.PARAM_CONVERSATION_MEMBER);
        Map<String, Object> attribute = null;
        if (params.containsKey(Conversation.PARAM_CONVERSATION_ATTRIBUTE)) {
          attribute = (Map<String, Object>) params.get(Conversation.PARAM_CONVERSATION_ATTRIBUTE);
        }
        boolean isTransient = false;
        if (params.containsKey(Conversation.PARAM_CONVERSATION_ISTRANSIENT)) {
          isTransient = (Boolean) params.get(Conversation.PARAM_CONVERSATION_ISTRANSIENT);
        }

        boolean isUnique = (Boolean) params.get(Conversation.PARAM_CONVERSATION_ISUNIQUE);

        boolean isSystem = false;
        if (params.containsKey(Conversation.PARAM_CONVERSATION_ISSYSTEM)) {
          isSystem = (Boolean) params.get(Conversation.PARAM_CONVERSATION_ISSYSTEM);
        }
        boolean isTemp = false;
        if (params.containsKey(Conversation.PARAM_CONVERSATION_ISTEMPORARY)) {
          isTemp = (Boolean) params.get(Conversation.PARAM_CONVERSATION_ISTEMPORARY);
        }
        int tempTTL = 86400*3;
        if (params.containsKey(Conversation.PARAM_CONVERSATION_TEMPORARY_TTL)) {
          tempTTL = (Integer) params.get(Conversation.PARAM_CONVERSATION_TEMPORARY_TTL);
        }

        session.createConversation(memberList, attribute, isTransient, isUnique, isTemp, tempTTL, isSystem, requestId);
        break;
      case CONVERSATION_QUERY:
        session.conversationQuery(params, requestId);
        break;
      case CONVERSATION_SEND_MESSAGE:
        if (!AVUtils.isBlankString(conversationId)) {
          AVInternalConversation conversation = session.getConversation(conversationId, convType);
          if (null != conversation) {
            AVIMMessage message = intent.getExtras().getParcelable(Conversation.INTENT_KEY_DATA);
            AVIMMessageOption messageOption = null;
            if (intent.getExtras().containsKey(Conversation.INTENT_KEY_MESSAGE_OPTION)) {
              messageOption = intent.getExtras().getParcelable(Conversation.INTENT_KEY_MESSAGE_OPTION);
            } else {
              messageOption = new AVIMMessageOption();
            }
            message.setFrom(clientId);
            conversation.sendMessage(message, requestId, messageOption);
          }
        }
        break;
      case CLIENT_STATUS:
        processSessionConnectionStatus(session, requestId);
        break;
      case CONVERSATION_PROMOTE_MEMBER:
        if (AVUtils.isBlankString(conversationId)) {
          LogUtil.log.e("conversation id is null during promoting MemberInfo");
        } else {
          AVInternalConversation internalConversation = session.getConversation(conversationId, convType);
          if (null == internalConversation) {
            LogUtil.log.w("not found target conversation with id=" + conversationId);
          } else {
            internalConversation.processConversationCommandFromClient(operationCode, params, requestId);
          }
        }
        break;
      default:
        if (!AVUtils.isBlankString(conversationId)) {
          AVInternalConversation conversation = session.getConversation(conversationId, convType);
          if (null != conversation) {
            conversation.processConversationCommandFromClient(operationCode, params, requestId);
          }
        }
    }
  }

  private void processSessionConnectionStatus(AVSession session, int requestId) {
    AVIMClient.AVIMClientStatus status = AVIMClient.AVIMClientStatus.AVIMClientStatusNone;
    if (session.sessionOpened.get() && session.sessionPaused.get()) {
      status = AVIMClient.AVIMClientStatus.AVIMClientStatusPaused;
    } else if (session.sessionOpened.get()) {
      status = AVIMClient.AVIMClientStatus.AVIMClientStatusOpened;
    }
    Bundle bundle = new Bundle();
    bundle.putInt(Conversation.callbackClientStatus, status.getCode());
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId, bundle,
        AVIMOperation.CLIENT_STATUS);
  }

  /**
   * 唤醒其他同样使用 LeanCloud 服务的 app
   * 必须要在 AndroidManifest PushService 注册时主动添加 exported = true 才会开启此功能
   * 只需要在每次 app 启动时唤醒其他 app
   * @param action
   */
  private void notifyOtherApplication(final String action) {
    if (isNeedNotifyApplication && !NotifyUtil.SERVICE_RESTART_ACTION.equals(action)) {
      // 每次 app 启动只需要唤醒一次就行了
      isNeedNotifyApplication = false;

      try {
        ServiceInfo info = getApplicationContext().getPackageManager().getServiceInfo(
          new ComponentName(getApplicationContext(), PushService.class), 0);
        if(info.exported) {
          NotifyUtil.notifyHandler.sendEmptyMessage(NotifyUtil.SERVICE_RESTART);
        }
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
  }

  /**
   * Set whether to automatically wake up PushService
   * @param isAutoWakeUp the default value is true
   */
  public static void setAutoWakeUp(boolean isAutoWakeUp) {
    PushService.isAutoWakeUp = isAutoWakeUp;
  }

  /**
   * Set default channel for Android Oreo or newer version
   * Notice: it isn"t necessary to invoke this method for any Android version before Oreo.
   *
   * @param context   context
   * @param channelId default channel id.
   */
  @TargetApi(Build.VERSION_CODES.O)
  public static void setDefaultChannelId(Context context, String channelId) {
    DefaultChannelId = channelId;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      // do nothing for Android versions before Ore
      return;
    }

    try {
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      CharSequence name = context.getPackageName();
      String description = "PushNotification";
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, name, importance);
      channel.setDescription(description);
      notificationManager.createNotificationChannel(channel);
    } catch (Exception ex) {
      LogUtil.log.w("failed to create NotificationChannel, then perhaps PushNotification doesn't work well on Android O and newer version.");
    }
  }

  private static Handler _installationSaveHandler = new Handler(Looper.getMainLooper()) {

    public void handleMessage(Message m) {

      AVInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
        @Override
        public void done(AVException e) {
          if (e != null && "already has one request sending".equals(e.getMessage())) {
            _installationSaveHandler.removeMessages(0);
            Message m = Message.obtain();
            m.what = 0;
            _installationSaveHandler.sendMessageDelayed(m, 2000);
          }
        }
      });
    }
  };
}
