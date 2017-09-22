package com.avos.avoscloud;

import android.content.Context;
import android.os.Handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.avos.avoscloud.callback.AVServerDateCallback;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

/**
 * The AVOSCloud class contains static functions that handle global configuration for the AVOSCloud
 * library.
 */
public class AVOSCloud {
  public static Context applicationContext = null;
  public static String applicationId = null;
  public static String clientKey = null;
  protected static Handler handler = null;

  public static final int LOG_LEVEL_VERBOSE = 1 << 1;
  public static final int LOG_LEVEL_DEBUG = 1 << 2;
  public static final int LOG_LEVEL_INFO = 1 << 3;
  public static final int LOG_LEVEL_WARNING = 1 << 4;
  public static final int LOG_LEVEL_ERROR = 1 << 5;
  public static final int LOG_LEVEL_NONE = ~0;
  static final String AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY =
      "AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY";
  static final String AV_CLOUD_CACHE_EXPIRE_DATE_KEY = "AV_CLOUD_CACHE_EXPIRE_DATE_KEY";
  static final Integer AV_CLOUD_CACHE_DEFAULT_EXPIRE_DATE = 30;
  static final String AV_CLOUD_CACHE_EXPIRE_KEY_ZONE = "AV_CLOUD_CACHE_EXPIRE_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY_ZONE = "AV_CLOUD_API_VERSION_KEY_ZONE";
  static final String AV_CLOUD_API_VERSION_KEY = "AV_CLOUD_API_VERSION";

  private static int logLevel = LOG_LEVEL_NONE;

  private static boolean internalDebugLog = false;
  private static boolean userInternalDebugLog = false;

  private static boolean isGcmOpen = false;

  public static final int DEFAULT_NETWORK_TIMEOUT = 15000;

  static final int DEFAULT_THREAD_POOL_SIZE = 10;

  private static int networkTimeoutInMills = DEFAULT_NETWORK_TIMEOUT;

  private static int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

  private static boolean isCN = true;

  /**
   * 服务区分，注意 name 值不能随意修改修改，要根据这个值来拼 host
   * RTM is indicating router server.
   */
  public enum SERVER_TYPE {
    API("api"), PUSH("push"), RTM("rtm"), STATS("stats"), ENGINE("engine");
    public final String name;

    SERVER_TYPE(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  /**
   * Set network timeout in milliseconds.default is 10 seconds.
   *
   * @param timeoutInMills
   */
  public static void setNetworkTimeout(int timeoutInMills) {
    networkTimeoutInMills = timeoutInMills;
  }

  /**
   * Returns the network timeout in milliseconds.It's 15 seconds by default.
   *
   * @return
   */
  public static int getNetworkTimeout() {
    return networkTimeoutInMills;
  }

  public static int getThreadPoolSize() {
    return threadPoolSize;
  }

  public static void setThreadPoolSize(int size) {
    threadPoolSize = size;
  }

  static {
    JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    ParserConfig.getGlobalInstance().putDeserializer(AVObject.class, AVObjectDeserializer.instance);
    ParserConfig.getGlobalInstance().putDeserializer(AVUser.class, AVObjectDeserializer.instance);

    SerializeConfig.getGlobalInstance().put(AVObject.class, AVObjectSerializer.instance);
    SerializeConfig.getGlobalInstance().put(AVUser.class, AVObjectSerializer.instance);
    try {
      Class avInstallationClass = Class.forName("com.avos.avoscloud.AVInstallation");
      ParserConfig.getGlobalInstance().putDeserializer(avInstallationClass,
          AVObjectDeserializer.instance);
      SerializeConfig.getGlobalInstance().put(avInstallationClass, AVObjectSerializer.instance);
    } catch (Exception e) {
    }
  }

  private AVOSCloud() {
  }

  /**
   * <p>
   * Authenticates this client as belonging to your application. This must be called before your
   * application can use the AVOSCloud library. The recommended way is to put a call to
   * AVOSCloud.initialize in each of your onCreate methods. An example:
   * </p>
   * <p/>
   * <pre>
   *         import android.app.Application;
   *         import com.avos.avoscloud.AVOSCloud;
   *
   *         public class MyApplication extends Application {
   *             public void onCreate() {
   *                 AVOSCloud.initialize(this, "your application id", "your client key");
   *             }
   *         }
   * @param context The active Context for your application.
   * @param applicationId  The application id provided in the AVOSCloud dashboard.
   * @param clientKey The client key provided in the AVOSCloud dashboard.
   */
  public static void initialize(Context context, String applicationId, String clientKey) {
    if (handler == null && !AVUtils.isMainThread()) {
      throw new IllegalStateException("Please call AVOSCloud.initialize in main thread.");
    }
    if (null == context || AVUtils.isBlankString(applicationId) || AVUtils.isBlankString(clientKey)) {
      throw new IllegalArgumentException("Parameter(context or applicationId or clientKey) is illegal.");
    }
    if (null != AVOSCloud.applicationContext) {
      if (applicationId.equals(AVOSCloud.applicationId) && clientKey.equals(AVOSCloud.clientKey)) {
        // ignore duplicated init.
        return;
      } else {
        throw new IllegalStateException("Can't initialize more than once.");
      }
    }
    AVOSCloud.applicationId = applicationId;
    AVOSCloud.clientKey = clientKey;
    AVOSCloud.applicationContext = context;

    if (handler == null) {
      AVOSCloud.handler = new Handler();
    }
    ArchiveRequestTaskController.schedule();
    initialize();
  }

  /**
   * Authenticates this client as belonging to your application. This must be called before your
   * application can use the AVOSCloud library. The recommended way is to put a call to
   * AVOSCloud.initialize in each of your onCreate methods.
   *
   * @param context The active Context for your application.
   * @param applicationId The application id provided in the AVOSCloud dashboard.
   * @param clientKey The client key provided in the AVOSCloud dashboard.
   * @param callback callback.internalDone() is called when the initialize completes.
   */
  public static void initialize(Context context, String applicationId, String clientKey, final AVCallback callback) {
    if (handler == null && !AVUtils.isMainThread()) {
      throw new IllegalStateException("Please call AVOSCloud.initialize in main thread.");
    }
    if (null == context || AVUtils.isBlankString(applicationId) || AVUtils.isBlankString(clientKey)) {
      throw new IllegalArgumentException("Parameter(context or applicationId or clientKey) is illegal.");
    }
    if (null != AVOSCloud.applicationContext) {
      if (applicationId.equals(AVOSCloud.applicationId) && clientKey.equals(AVOSCloud.clientKey)) {
        // ignore duplicated init.
        if (null != callback) {
          callback.internalDone(null);
        }
        return;
      } else {
        throw new IllegalStateException("Can't initialize more than once.");
      }
    }

    AVOSCloud.applicationId = applicationId;
    AVOSCloud.clientKey = clientKey;
    AVOSCloud.applicationContext = context;

    if (handler == null) {
      AVOSCloud.handler = new Handler();
    }

    ArchiveRequestTaskController.schedule();

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        initialize();
        handler.post(new Runnable() {
          @Override
          public void run() {
            callback.internalDone(null);
          }
        });
      }
    });
    thread.start();
  }

  private static void initialize() {
    AppRouterManager.getInstance().fetchRouter(false);

    // 这里需要注意时序，因为 statistics 会依赖 appRouter 返回的 api server
    startAnalytics(applicationContext);

    if (AVPersistenceUtils.sharedInstance().getPersistentSettingBoolean(
      AV_CLOUD_CACHE_EXPIRE_KEY_ZONE, AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY, true)) {
      AVCacheManager.clearCacheMoreThanDays(AVPersistenceUtils.sharedInstance()
        .getPersistentSettingInteger(AV_CLOUD_CACHE_EXPIRE_KEY_ZONE,
          AV_CLOUD_CACHE_EXPIRE_DATE_KEY, AV_CLOUD_CACHE_DEFAULT_EXPIRE_DATE));
      // 文件存储就时间长一点好了
      AVCacheManager.clearFileCacheMoreThanDays(AVPersistenceUtils.sharedInstance()
        .getPersistentSettingInteger(AV_CLOUD_CACHE_EXPIRE_KEY_ZONE,
          AV_CLOUD_CACHE_EXPIRE_DATE_KEY, AV_CLOUD_CACHE_DEFAULT_EXPIRE_DATE) * 2);
    }

    // update
    AVOSCloud.onUpgrade(
      AVPersistenceUtils.sharedInstance().getPersistentSettingString(
        AV_CLOUD_API_VERSION_KEY_ZONE, AV_CLOUD_API_VERSION_KEY, "1"), PaasClient
        .storageInstance().getApiVersion());
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(AV_CLOUD_API_VERSION_KEY_ZONE,
      AV_CLOUD_API_VERSION_KEY, PaasClient.storageInstance().getApiVersion());
  }

  public static void useAVCloudUS() {
    isCN = false;
    PaasClient.useAVCloudUS();
  }

  public static void useAVCloudCN() {
    isCN = true;
    PaasClient.useAVCloudCN();
  }

  static boolean isCN() {
    return isCN;
  }

  /**
   * Gets the level of logging that will be displayed.
   *
   * @return
   */
  public static int getLogLevel() {
    return logLevel;
  }

  public static void setLogLevel(int logLevel) {
    AVOSCloud.logLevel = logLevel;
  }

  static void showInternalDebugLog(boolean show) {
    internalDebugLog = show;
  }

  public static boolean showInternalDebugLog() {
    return internalDebugLog;
  }

  public static void setDebugLogEnabled(boolean enable) {
    userInternalDebugLog = enable;
  }

  /**
   * set server for the specified service
   * this function must be called before {@link #initialize}
   * @param serverType
   * @param host
   */
  public static void setServer(SERVER_TYPE serverType, String host) {
    AppRouterManager.setServer(serverType, host);
  }

  public static boolean isDebugLogEnabled() {
    return userInternalDebugLog || internalDebugLog;
  }

  public static void setGcmOpen(boolean isOpen) {
    isGcmOpen = isOpen;
  }

  public static boolean isGcmOpen() {
    return isGcmOpen;
  }

  public static boolean isLastModifyEnabled() {
    return PaasClient.isLastModifyEnabled();
  }

  public static void setLastModifyEnabled(boolean e) {
    PaasClient.setLastModifyEnabled(e);
  }

  public static void clearLastModifyCache() {
    PaasClient.clearLastModifyCache();
  }

  public static void enableAutoCacheCleaner() {
    AVPersistenceUtils.sharedInstance().savePersistentSettingBoolean(
        AV_CLOUD_CACHE_EXPIRE_KEY_ZONE, AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY, true);
  }

  public static void disableAutoCacheCleaner() {
    AVPersistenceUtils.sharedInstance().savePersistentSettingBoolean(
        AV_CLOUD_CACHE_EXPIRE_KEY_ZONE, AV_CLOUD_CACHE_EXPIRE_AUTO_CLEAN_KEY, false);
  }

  public static void setCacheFileAutoExpireDate(int expireDays) {
    AVPersistenceUtils.sharedInstance().savePersistentSettingInteger(
        AV_CLOUD_CACHE_EXPIRE_KEY_ZONE, AV_CLOUD_CACHE_EXPIRE_DATE_KEY, expireDays);
  }

  protected static void onUpgrade(String oldVersion, String newVersion) {
    if (!oldVersion.equals(newVersion) && AVUtils.compareNumberString(newVersion, oldVersion)) {
      if ("1.1".equals(newVersion)) {
        if (AVOSCloud.showInternalDebugLog()) {
          LogUtil.avlog.d("try to do some upgrade work");
        }
        AVUser localUser = AVUser.getCurrentUser();
        // update AVUser date type data
        if (localUser != null && !AVUtils.isBlankString(localUser.getObjectId())) {
          localUser.fetchInBackground(new GetCallback<AVObject>() {

            @Override
            public void done(AVObject object, AVException e) {
              AVUser.changeCurrentUser((AVUser) object, true);
            }
          });
        }
        // update AVInstallation date type data
        try {
          Class<?> installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
          Method updateMethod = installationClass.getDeclaredMethod("updateCurrentInstallation");
          updateMethod.invoke(installationClass);
        } catch (Exception e) {
          LogUtil.avlog.i("failed to update local Installation");
        }
        AVCacheManager.clearAllCache();
      }
    }
  }

  private static void startAnalytics(Context context) {
    try {
      Class<?> statisticsClass = Class.forName("com.avos.avoscloud.AVAnalytics");
      Method startMethod = statisticsClass.getDeclaredMethod("start", Context.class);
      startMethod.setAccessible(true);
      startMethod.invoke(statisticsClass, context);
    } catch (Exception e) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.i("statistics library not started since not included");
      }
    }
  }

  /**
   * 获取服务器端当前时间
   *
   * @param callback
   */
  public static void getServerDateInBackground(AVServerDateCallback callback) {
    getServerDateInBackground(false, callback);
  }

  /**
   * 获取服务器端当前时间
   *
   * @return
   * @throws AVException
   */
  public static Date getServerDate() throws AVException {
    final Date[] results = {null};
    getServerDateInBackground(true, new AVServerDateCallback() {
      @Override
      public void done(Date serverDate, AVException e) {
        if (e == null) {
          results[0] = serverDate;
        } else {
          AVExceptionHolder.add(e);
        }
      }

      @Override
      public boolean mustRunOnUIThread() {
        return false;
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return results[0];
  }

  private static void getServerDateInBackground(boolean sync, final AVServerDateCallback callback) {
    PaasClient.storageInstance().getObject("date", null, sync, null, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        try {
          Date date = AVUtils.dateFromMap(JSON.parseObject(content, Map.class));
          if (callback != null) {
            callback.internalDone(date, null);
          }
        } catch (Exception ex) {
          if (callback != null) {
            callback.internalDone(null, AVErrorUtils.createException(ex, null));
          }
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (callback != null) {
          callback.internalDone(null, AVErrorUtils.createException(error, content));
        }
      }
    });
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCode(String, AVSMSOption)}
   */
  public static void requestSMSCode(String phone) throws AVException {
    AVSMS.requestSMSCode(phone, null);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCode(String, AVSMSOption)}
   */
  public static void requestSMSCode(String phone, String name, String op, int ttl) throws AVException {
    AVSMSOption option = new AVSMSOption();
    option.setApplicationName(name);
    option.setOperation(op);
    option.setTtl(ttl);
    AVSMS.requestSMSCode(phone, option);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCode(String, AVSMSOption)}
   */
  public static void requestSMSCode(String phone, String templateName, Map<String, Object> env)
    throws AVException {
    AVSMSOption option = new AVSMSOption();
    option.setTemplateName(templateName);
    option.setEnvMap(env);
    AVSMS.requestSMSCode(phone, option);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCode(String, AVSMSOption)}
   */
  public static void requestSMSCode(String phone, String templateName, String sign,
                                    Map<String, Object> env) throws AVException {
    AVSMSOption option = new AVSMSOption();
    option.setTemplateName(templateName);
    option.setSignatureName(sign);
    option.setEnvMap(env);
    AVSMS.requestSMSCode(phone, option);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCodeInBackground(String, AVSMSOption, RequestMobileCodeCallback)}
   */
  public static void requestSMSCodeInBackground(String phone, String name, String op, int ttl,
                                                RequestMobileCodeCallback callback) {
    AVSMSOption option = new AVSMSOption();
    option.setApplicationName(name);
    option.setOperation(op);
    option.setTtl(ttl);
    AVSMS.requestSMSCodeInBackground(phone, option, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCodeInBackground(String, AVSMSOption, RequestMobileCodeCallback)}
   */
  public static void requestSMSCodeInBackground(String phone, String templateName,
                                                Map<String, Object> env, RequestMobileCodeCallback callback) {
    AVSMSOption option = new AVSMSOption();
    option.setTemplateName(templateName);
    option.setEnvMap(env);
    AVSMS.requestSMSCodeInBackground(phone, option, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCodeInBackground(String, AVSMSOption, RequestMobileCodeCallback)}
   */
  public static void requestSMSCodeInBackground(String phone, String templateName, String sign,
                                                Map<String, Object> env, RequestMobileCodeCallback callback) {
    AVSMSOption option = new AVSMSOption();
    option.setTemplateName(templateName);
    option.setSignatureName(sign);
    option.setEnvMap(env);
    AVSMS.requestSMSCodeInBackground(phone, option, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCodeInBackground(String, AVSMSOption, RequestMobileCodeCallback)}
   */
  public static void requestSMSCodeInBackground(String phone, RequestMobileCodeCallback callback) {
    AVSMS.requestSMSCodeInBackground(phone, null, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCode(String, AVSMSOption)}
   */
  public static void requestVoiceCode(String phoneNumber) throws AVException {
    AVSMSOption smsOption = new AVSMSOption();
    smsOption.setSmsType(AVSMSOption.AVSMS_TYPE.VOICE_SMS);
    AVSMS.requestSMSCode(phoneNumber, smsOption);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#requestSMSCodeInBackground(String, AVSMSOption, RequestMobileCodeCallback)}
   */
  public static void requestVoiceCodeInBackground(String phoneNumber, RequestMobileCodeCallback callback) {
    AVSMSOption smsOption = new AVSMSOption();
    smsOption.setSmsType(AVSMSOption.AVSMS_TYPE.VOICE_SMS);
    AVSMS.requestSMSCodeInBackground(phoneNumber, smsOption, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#verifySMSCode(String, String)}
   */
  public static void verifySMSCode(String code, String mobilePhoneNumber) throws AVException {
    AVSMS.verifySMSCode(code, mobilePhoneNumber);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#verifySMSCode(String, String)}
   */
  public static void verifyCode(String code, String mobilePhoneNumber) throws AVException {
    AVSMS.verifySMSCode(code, mobilePhoneNumber);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#verifySMSCodeInBackground(String, String, AVMobilePhoneVerifyCallback)}
   */
  public static void verifySMSCodeInBackground(String code, String mobilePhoneNumber, AVMobilePhoneVerifyCallback callback) {
    AVSMS.verifySMSCodeInBackground(code, mobilePhoneNumber, callback);
  }

  /**
   * @deprecated Please use {@link com.avos.avoscloud.AVSMS#verifySMSCodeInBackground(String, String, AVMobilePhoneVerifyCallback)}
   */
  public static void verifyCodeInBackground(String code, String mobilePhoneNumber,
                                            AVMobilePhoneVerifyCallback callback) {
    AVSMS.verifySMSCodeInBackground(code, mobilePhoneNumber, callback);
  }
}
