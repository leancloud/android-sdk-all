package com.avos.avospush.push;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVHttpClient;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.AppRouterManager;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.GetHttpResponseHandler;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.im.v2.AVIMOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Request;

/**
 * Created by yangchaozhong on 3/14/14.
 */
public class AVPushRouter {
  public static final String SERVER = "server";
  private static final String EXPIRE_AT = "expireAt";
  private static final String SECONDARY = "secondary";

  private static String ROUTER_QUERY_SRTING = "/v1/route?appId=%s&installationId=%s&secure=1";
  private static final String PUSH_SERVER_CACHE_KEY_FMT = "com.avos.push.router.server.cache%s";

  private final Context context;
  private final String installationId;
  private int ttlInSecs = -1;

  // router 请求的默认超时时间为 5 秒，否则 15 秒时间太长了
  private static final int ROUTER_REQUEST_TIME_OUT = 5 * 1000;

  private AVHttpClient routerHttpClient;

  private RouterResponseListener listener;

  // 是否正在发送请求，没有必要重复发送请求
  private volatile boolean isRequesting = false;

  // socket 连续连接失败的次数
  private AtomicInteger socketLostNum = new AtomicInteger(0);

  // 是否读取 permary 缓存
  private volatile boolean isPrimarySever = true;

  /**
   * AVPushRouter is used to get push server refer to appId and installationId.
   *
   * @param context application context of PushService
   */
  public AVPushRouter(Context context, RouterResponseListener listener) {
    this.context = context;
    this.listener = listener;
    this.installationId = AVInstallation.getCurrentInstallation().getInstallationId();
  }

  private String getRouterUrl() {
    String routerUrl = AppRouterManager.getInstance().getRtmRouterServer() + ROUTER_QUERY_SRTING;
    return String.format(routerUrl, AVOSCloud.applicationId, installationId);
  }

  private synchronized AVHttpClient getRouterHttpClient() {
    if (null == routerHttpClient) {
      routerHttpClient = AVHttpClient.newClientInstance(ROUTER_REQUEST_TIME_OUT);
    }
    return routerHttpClient;
  }

  /**
   * 避免对 router 进行重复请求，对 socket 结果进行处理
   * 如果 socket 连接成功，则继续使用缓存数据
   * 如果 连续两次失败，则第三次使用 secondary server
   * 如果连续四次失败，则第五次会去网络请求 router 数据
   * @param e
   */
  public void processSocketConnectionResult(AVException e) {
    if (null == e) {
      socketLostNum.set(0);
    } else {
      final String errorMessage = e.getMessage();
      if (AVUtils.isConnected(this.context)
        && (AVUtils.isBlankContent(errorMessage) || !errorMessage.contains("Permission"))) {
        // 只有有网络时才需要累加，并且如果是因为权限引起的错误，不需要累加（同时也是为了处理小米的神隐模式）
        socketLostNum.incrementAndGet();
        if (socketLostNum.get() > 1) {
          // 当连续两次失败后，换用 secondary server
          isPrimarySever = false;
        }
      }
    }
  }

  /**
   * fetch push server:<br/>
   * <ul>
   * <li>fetch push server from router.</li>
   * <li>use ttl to judge if it's no need to fetch push server from router and get push server from
   * cache instead.</li>
   * <ul/>
   *
   * @return null or {"groupId":"xxx", "server":"ws://..."}
   */
  public void fetchPushServer() {
    if (AVUtils.isBlankString(AVOSCloud.applicationId)) {
      LogUtil.avlog.e("Please initialize Application first");
      return;
    }

    String specifiedServer = AVIMOptions.getGlobalOptions().getRTMServer();
    if (!AVUtils.isBlankString(specifiedServer)) {
      listener.onServerAddress(specifiedServer);
    } else if (!AVUtils.isConnected(this.context)) {
      // 无网络时只能返回缓存数据
      Map<String, Object> pushServerCache = getPushServerFromCache();
      if (pushServerCache != null) {
        listener.onServerAddress((String) pushServerCache.get(SERVER));
      } else {
        listener.onServerAddress(null);
      }
    } else {
      Map<String, Object> pushServerCache = getPushServerFromCache();
      if ((pushServerCache != null && (Long) pushServerCache.get(EXPIRE_AT) > System.currentTimeMillis())
        && socketLostNum.get() <= 3) {
        // 如果缓存有效并且连续失败小于四次，则从缓存中获取数据
        String serverAddress = (String) pushServerCache.get(SERVER);
        if (!isPrimarySever) {
          String secondaryAddress = (String)pushServerCache.get(SECONDARY);
          if (!AVUtils.isBlankContent(secondaryAddress)) {
            serverAddress = secondaryAddress;
          }
         }
        listener.onServerAddress(serverAddress);
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.d("get push server from cache:" + serverAddress);
        }
      } else {
        // 如果缓存无效，则发送请求
        fetchPushServerFromServer();
      }
    }
  }

  private void fetchPushServerFromServer() {
    if (isRequesting) {
      // 避免重复请求
      return;
    }
    isRequesting = true;
    final String routerUrlStr = getRouterUrl();
    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("try to fetch push server from :" + routerUrlStr);
    }

    final GenericObjectCallback callback = new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        if (e == null) {
          try {
            socketLostNum.set(0);
            HashMap<String, Object> response = JSON.parseObject(content, HashMap.class);
            ttlInSecs = (Integer) response.get("ttl");

            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put(SERVER, response.get(SERVER));
            result.put(EXPIRE_AT, ttlInSecs * 1000l + System.currentTimeMillis());
            result.put(SECONDARY, response.get(SECONDARY));
            if (response.containsKey("groupUrl")) {
              AppRouterManager.getInstance().updateRtmRouterServer(
                (String) response.get("groupUrl"), true);
            }
            cachePushServer(result);
            listener.onServerAddress((String) result.get(SERVER));
            isPrimarySever = true;
          } catch (Exception e1) {
            this.onFailure(e1, content);
          }
        }
        isRequesting = false;
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (AVOSCloud.showInternalDebugLog()) {
          LogUtil.avlog.d("failed to fetch push server:" + error);
        }
        listener.onServerAddress(null);
        isRequesting = false;
      }

      @Override
      public boolean isRequestStatisticNeed() {
        return false;
      }
    };
    Request.Builder builder = new Request.Builder();
    builder.url(routerUrlStr).get();
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("get router url: " + routerUrlStr);
    }
    getRouterHttpClient().execute(builder.build(), false, new GetHttpResponseHandler(callback));
  }

  private HashMap<String, Object> getPushServerFromCache() {
    HashMap<String, Object> pushServerMap = new HashMap<String, Object>();
    SharedPreferences pushServerData =
        context
            .getSharedPreferences(
                String.format(PUSH_SERVER_CACHE_KEY_FMT, AVOSCloud.applicationId),
                Context.MODE_PRIVATE);
    pushServerMap.put(SERVER, pushServerData.getString(SERVER, null));
    pushServerMap.put(EXPIRE_AT, pushServerData.getLong(EXPIRE_AT, 0));
    pushServerMap.put(SECONDARY, pushServerData.getString(SECONDARY, null));
    return pushServerMap;
  }

  private void cachePushServer(HashMap<String, Object> pushServerMap) {
    SharedPreferences pushServerData =
        context
            .getSharedPreferences(
                String.format(PUSH_SERVER_CACHE_KEY_FMT, AVOSCloud.applicationId),
                Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = pushServerData.edit();
    editor.putString(SERVER, (String) pushServerMap.get(SERVER));
    editor.putLong(EXPIRE_AT, (Long) pushServerMap.get(EXPIRE_AT));
    editor.putString(SECONDARY, (String) pushServerMap.get(SECONDARY));
    editor.commit();
  }

  public interface RouterResponseListener {
    void onServerAddress(String address);
  }
}