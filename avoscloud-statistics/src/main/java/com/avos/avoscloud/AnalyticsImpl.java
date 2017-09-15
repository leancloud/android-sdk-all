package com.avos.avoscloud;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/6/13 Time: 4:48 PM To change this template use
 * File | Settings | File Templates.
 */
class AnalyticsImpl implements AnalyticsRequestController.AnalyticsRequestDispatcher {
  private static AnalyticsImpl instance;
  private static final Map<String/* sid */, AnalyticsSession> sessions =
      new ConcurrentHashMap<String, AnalyticsSession>();
  private static long SESSIONTHRESHOLD = 30 * 1000;
  private static final String TAG = AnalyticsImpl.class.getSimpleName();
  private static final String FIRSTBOOTTAG = "firstBoot";
  private static final List<String> WHITELIST = new LinkedList<String>();
  private static boolean REPORTENABLEFLAG = true;

  private String appChannel = "AVOS Cloud";
  private boolean autoLocation;
  private String currentSessionId;

  private AVUncaughtExceptionHandler handler = null;
  private AnalyticsOnlineConfig onlineConfig = null;
  private AVOnlineConfigureListener listener = null;
  private Map<String, String> customInfo;

  private AnalyticsRequestController requestController;
  private RealTimeRequestController realTimeController;

  private AnalyticsImpl() {
    super();
    onlineConfig = new AnalyticsOnlineConfig(this);
    requestController =
        new BatchRequestController(currentSessionId, this, AnalyticsUtils.getRequestInterval());
    realTimeController = new RealTimeRequestController(this);
  }


  static AnalyticsImpl getInstance() {
    if (instance == null) {
      instance = new AnalyticsImpl();
    }
    return instance;
  }

  void setAutoLocation(boolean b) {
    autoLocation = b;
  }

  public boolean isAutoLocation() {
    return autoLocation;
  }

  private boolean isEnableStats() {
    return onlineConfig.isEnableStats();
  }

  void setAppChannel(final String channel) {
    appChannel = channel;
  }

  String getAppChannel() {
    return appChannel;
  }

  void enableCrashReport(Context context, boolean enable) {
    if (enable && handler == null) {
      handler = new AVUncaughtExceptionHandler(context);
    }
    if (handler != null) {
      handler.enableCrashHanlder(enable);
    }
  }

  private ReportPolicy getReportPolicy(Context context) {
    ReportPolicy value = onlineConfig.getReportPolicy();
    // add white list for realtime requirement business clients
    if (value == ReportPolicy.REALTIME && WHITELIST.contains(AVOSCloud.applicationId)) {
      return ReportPolicy.REALTIME;
    }
    if (value == ReportPolicy.REALTIME && (!AnalyticsUtils.inDebug(context))) {
      return ReportPolicy.BATCH;
    }
    if (value == ReportPolicy.SENDWIFIONLY && (!AnalyticsUtils.inDebug(context))) {
      return ReportPolicy.BATCH;
    }
    return value;
  }

  void setReportPolicy(ReportPolicy p) {
    if (onlineConfig.setReportPolicy(p)) {
      if (requestController != null) {
        requestController.quit();
      }

      requestController =
          AnalyticsRequestControllerFactory.getAnalyticsRequestController(currentSessionId,
              getReportPolicy(AVOSCloud.applicationContext), this);

      AnalyticsSession session = getCurrentSession(false);
      if (session != null && requestController instanceof BatchRequestController) {
        ((BatchRequestController) requestController).resetMessageCount(session
            .getMessageCount());
      }
    };
  }

  void notifyOnlineConfigListener(JSONObject data) {
    if (listener != null) {
      try {
        listener.onDataReceived(data);
      } catch (Exception e) {
        Log.e(TAG, "Notify online data received failed.", e);
      }
    }
  }


  private AnalyticsSession getCurrentSession(boolean create) {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session != null) {
      return session;
    }
    if (!create) {
      return null;
    }
    session = createSession();
    currentSessionId = session.getSessionId();
    return session;
  }

  void setSessionContinueMillis(long ms) {
    SESSIONTHRESHOLD = ms;
  }

  public void setSessionDuration(long ms) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.setSessionDuration(ms);
    }
  }


  private AnalyticsSession sessionByName(String sid) {
    if (sid == null) {
      return null;
    }
    return sessions.get(sid);
  }

  private AnalyticsSession createSession() {
    AnalyticsSession session = new AnalyticsSession();
    session.beginSession();
    if (session.getSessionId() != null) {
      sessions.put(session.getSessionId(), session);
    }
    return session;
  }

  // 此处强制发送上次session剩余没有发送的数据
  void flushLastSessions(Context context) {
    AnalyticsSession cachedSession =
        AnalyticsSessionCacheRepository.getInstance().getCachedSession();

    if (AVOSCloud.showInternalDebugLog() && cachedSession != null) {
      LogUtil.avlog.i("get cached sessions:" + cachedSession.getSessionId());
    }
    if (cachedSession != null) {
      sessions.put(cachedSession.getSessionId(), cachedSession);
    }
    sendInstantRecordingRequest();
  }

  void beginSession() {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      session = createSession();
    }
    currentSessionId = session.getSessionId();
    updateOnlineConfig();
  }

  void endSession() {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      return;
    }
    session.endSession();
    postRecording();
    currentSessionId = null;
  }

  void pauseSession() {
    AnalyticsSession session = sessionByName(currentSessionId);
    if (session == null) {
      return;
    }
    session.pauseSession();
  }

  public void addActivity(String name, long ms) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.addActivity(name, ms);
    }
  }

  void beginActivity(String name) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.beginActivity(name);
      postRecording();
    }
  }

  void beginFragment(String name) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.beginFragment(name);
      postRecording();
    }
  }

  public void beginEvent(Context context, String name) {
    beginEvent(context, name, "", "");
  }

  AnalyticsEvent beginEvent(Context context, String name, String label, String key) {
    AnalyticsSession session = getCurrentSession(true);
    AnalyticsEvent event = session.beginEvent(context, name, label, key);
    postRecording();
    return event;
  }

  void endEvent(Context context, String name, String label, String key) {
    getCurrentSession(true).endEvent(context, name, label, key);
    postRecording();
  }

  void setCustomInfo(final Map<String, String> extensionInfo) {
    customInfo = extensionInfo;
  }

  Map<String, String> getCustomInfo() {
    return customInfo;
  }

  private long getSessionTimeoutThreshold() {
    return SESSIONTHRESHOLD;
  }

  boolean shouldRegardAsNewSession() {
    AnalyticsSession session = getCurrentSession(false);
    if (session == null) {
      return true;
    }
    long current = AnalyticsUtils.getCurrentTimestamp();
    long start = session.getDuration().getPausedTimeStamp();
    long delta = current - start;
    if (delta > getSessionTimeoutThreshold() && start > 0) {
      return true;
    }
    return false;
  }

  void endActivity(String name) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.endActivity(name);
      postRecording();
    }
  }

  void endFragment(String name) {
    AnalyticsSession session = getCurrentSession(true);
    if (null != session) {
      session.endFragment(name);
      postRecording();
    }
  }

  private void dumpJsonMap(Context context) {
    for (AnalyticsSession session : sessions.values()) {
      Map<String, Object> map = session.jsonMap(context, customInfo, false);
      try {
        if (map != null) {
          String jsonString = JSONHelper.toJsonString(map);
          LogUtil.log.d(jsonString);
        }
      } catch (Exception exception) {
        LogUtil.log.e(TAG, "", exception);
      }
    }

  }

  private synchronized void report(Context context, boolean clear) {
    try {

      saveSessionsToServer(context);
      // once sent, remove stopped sessions.
      Iterator<Map.Entry<String, AnalyticsSession>> iter = sessions.entrySet().iterator();
      while (iter.hasNext()) {
        AnalyticsSession session = iter.next().getValue();
        if (session.isSessionFinished()) {
          iter.remove();
        }
      }

      AnalyticsSession currentSession = getCurrentSession(false);
      if (requestController != null) {
        requestController.appraisalSession(currentSession);
      }
      if (clear) {
        clearSessions();
      }
    } catch (Exception e) {
      Log.e(TAG, "Send statstics report failed", e);
    }
  }

  void debugDump(Context context) {
    if (!AVOSCloud.showInternalDebugLog()) {
      return;
    }

    for (AnalyticsSession session : sessions.values()) {
      Map<String, Object> map = session.jsonMap(context, customInfo, false);
      Log.i(TAG, "json data: " + map);
    }
  }

  private void postRecording() {
    if (AVOSCloud.showInternalDebugLog()) {
      Log.d(TAG, "report policy:" + onlineConfig.getReportPolicy());
    }
    // 未开启统计，忽略
    if (!isEnableStats()) {
      return;
    }
    if (requestController != null) {
      requestController.requestToSend(currentSessionId);
    }
    getCurrentSession(false);
    archiveCurrentSession();
  }

  void archiveCurrentSession() {
    AnalyticsSession currentSession = sessionByName(currentSessionId);
    if (currentSession != null) {
      AnalyticsSessionCacheRepository.getInstance().cacheSession(currentSession);
    }
  }

  private void saveSessionsToServer(final Context context) {
    try {
      sendArchivedRequests(true);
      for (final AnalyticsSession session : sessions.values()) {
        Map<String, Object> map = session.jsonMap(context, customInfo, true);
        if (map != null) {
          String jsonString = JSON.toJSONString(map);
          if (AVOSCloud.showInternalDebugLog()) {
            LogUtil.log.i(jsonString);
          }
          sendAnalysisRequest(jsonString, true, true, new GenericObjectCallback() {

            @Override
            public boolean isRequestStatisticNeed() {
              return false;
            }

            @Override
            public void onSuccess(String content, AVException e) {
              // once success, we clear the events.
              if (AVOSCloud.showInternalDebugLog()) {
                Log.i(TAG, "Save success: " + content);
              }
            }

            @Override
            public void onFailure(Throwable error, String content) {
              if (AVOSCloud.showInternalDebugLog()) {
                Log.i(TAG, "Save failed: " + content);
              }

            }
          });
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "saveSessionsToServer failed.", e);
    }
  }

  private void clearSessions() {
    sessions.clear();
    currentSessionId = null;
  }

  void setAVOnlineConfigureListener(AVOnlineConfigureListener listener) {
    this.listener = listener;
  }

  void updateOnlineConfig() {
    if (AVOSCloud.showInternalDebugLog()) {
      Log.d(TAG, "try to update statistics config from online data");
    }
    onlineConfig.update();
  }

  void updateOnlineConfig(Context context, AVCallback<Map<String, Object>> callback) {
    if (AVOSCloud.showInternalDebugLog()) {
      Log.d(TAG, "try to update statistics config from online data");
    }
    onlineConfig.update(context, callback);
  }

  void reportFirstBoot(Context context) {
    SharedPreferences sharedPref =
        context.getSharedPreferences("AVOSCloud-SDK", Context.MODE_PRIVATE);
    boolean firstBoot = sharedPref.getBoolean(FIRSTBOOTTAG, true);
    if (firstBoot) {
      sendInstantRecordingRequest();
      Map<String, Object> firstBootMap = getCurrentSession(false).firstBootMap(context, customInfo);
      if (firstBootMap != null) {
        if (AVOSCloud.showInternalDebugLog()) {
          LogUtil.avlog.d("report data on first boot");
        }
        String jsonString = JSON.toJSONString(firstBootMap);
        sendAnalysisRequest(jsonString, false, true, null);
      }
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putBoolean(FIRSTBOOTTAG, false);
      editor.commit();
    } else if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("no need to first boot report");
    }
  }

  void sendInstantRecordingRequest() {
    realTimeController.requestToSend(currentSessionId);
  }

  String getConfigParams(String key, String defaultValue) {
    String result = onlineConfig.getConfigParams(key);
    if (result == null) {
      return defaultValue;
    } else {
      return result;
    }
  }

  private static void sendAnalysisRequest(String jsonString, boolean sync, boolean eventually,
      GenericObjectCallback callback) {
    if (REPORTENABLEFLAG) {
      PaasClient.statistisInstance().postObject("stats/collect", jsonString, sync, eventually,
          callback, null, AVUtils.md5(jsonString));
    }
  }

  synchronized void setAnalyticsEnabled(boolean enable) {
    REPORTENABLEFLAG = enable;
  }

  private synchronized void sendArchivedRequests(boolean sync) {
    if (REPORTENABLEFLAG) {
      PaasClient.statistisInstance().handleAllArchivedRequest(sync);
    }
  }

  @Override
  public void sendRequest() {
    report(AVOSCloud.applicationContext, false);
  }
}
