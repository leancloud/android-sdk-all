package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Request;

/**
 * Created by wli on 16/5/6.
 */
public class AppRouterManager {

  /**
   * app router 地址
   */
  private static final String ROUTER_ADDRESS = " https://app-router.leancloud.cn/2/route?appId=";

  /**
   * share preference 的 key 值
   */
  private static final String API_SERVER_KEY = "api_server";
  private static final String STATS_SERVRE_KEY = "stats_server";
  private static final String RTM_ROUTER_SERVRE_KEY = "rtm_router_server";
  private static final String PUSH_SERVRE_KEY = "push_server";
  private static final String ENGINE_SERVRE_KEY = "engine_server";
  private static final String TTL_KEY = "ttl";

  private static final String LATEST_UPDATE_TIME_KEY = "latest_update_time";

  private static final String DEFAULT_REGION_EAST_CHINA = "lncldapi.com";
  private static final String DEFAULT_REGION_NORTH_CHINA = "lncld.net";
  private static final String DEFAULT_REGION_NORTH_AMERICA = "lncldglobal.com";

  public static final int APP_REGION_CN_NORTH = 0;
  public static final int APP_REGION_CN_EAST = 1;
  public static final int APP_REGION_US_NORTH = 2;

  private Map<String, String> serverHostsMap = new ConcurrentHashMap<>();
  private static Map<String, String> customServerHostsMap = new ConcurrentHashMap<>();

  private static AppRouterManager appRouterManager;

  public synchronized static AppRouterManager getInstance() {
    if (null == appRouterManager) {
      appRouterManager = new AppRouterManager();
    }
    return appRouterManager;
  }

  private AppRouterManager() {
  }

  static void setServer(AVOSCloud.SERVER_TYPE server, String host) {
    customServerHostsMap.put(server.name, host);
    RequestStatisticsUtil.REPORT_INTERNAL_STATS = false;
  }

  String getStorageServer() {
    return getServerUrl(AVOSCloud.SERVER_TYPE.API);
  }

  /**
   * 处理  /push, /installations 所有请求
   * @return
   */
  String getPushServer() {
    return getServerUrl(AVOSCloud.SERVER_TYPE.PUSH);
  }

  String getStatsServer() {
    return getServerUrl(AVOSCloud.SERVER_TYPE.STATS);
  }

  String getEngineServer() {
    return getServerUrl(AVOSCloud.SERVER_TYPE.ENGINE);
  }

  public String getRtmRouterServer() {
    return getServerUrl(AVOSCloud.SERVER_TYPE.RTM);
  }

  private String getServerUrl(AVOSCloud.SERVER_TYPE type) {
    if (customServerHostsMap.containsKey(type.name)) {
      return customServerHostsMap.get(type.name);
    }

    if (serverHostsMap.containsKey(type.name) && !AVUtils.isBlankString(serverHostsMap.get(type.name))) {
      return serverHostsMap.get(type.name);
    } else {
      int appRegion = getAppRegion(AVOSCloud.applicationId);
      return getDefaultServer(type, appRegion);
    }
  }

  /**
   * 获取默认的 url
   * @param type
   * @param region
   * @return
   */
  private String getDefaultServer(AVOSCloud.SERVER_TYPE type, int region) {
    String prefix = AVOSCloud.applicationId.substring(0, 8);
    String suffix = DEFAULT_REGION_NORTH_CHINA;
    if (APP_REGION_US_NORTH == region) {
      suffix = DEFAULT_REGION_NORTH_AMERICA;
    } else if (APP_REGION_CN_EAST == region) {
      suffix = DEFAULT_REGION_EAST_CHINA;
    }
    return String.format("https://%s.%s.%s", prefix, type.name, suffix);
  }

  /**
   * 更新 router url
   * 有可能因为测试或者 301 等原因需要运行过程中修改 url
   *
   * @param router
   * @param persistence 是否需要持久化存储到本地
   *                    为 true 则存到本地，app 下次打开后仍有效果，否则仅当次声明周期内有效
   */
  public void updateRtmRouterServer(String router, boolean persistence) {
    serverHostsMap.put(AVOSCloud.SERVER_TYPE.RTM.name, addHttpsPrefix(router));
    if (persistence) {
      AVPersistenceUtils.sharedInstance().savePersistentSettingString(
        getAppRouterSPName(), RTM_ROUTER_SERVRE_KEY, serverHostsMap.get(AVOSCloud.SERVER_TYPE.RTM.name));
    }
  }

  /**
   * 拉取 router 地址
   *
   * @param force 是否强制拉取，如果为 true 则强制拉取，如果为 false 则需要间隔超过 ttl 才会拉取
   */
  void fetchRouter(boolean force) {
    fetchRouter(force, null);
  }

  /**
   * 添加此函数仅仅是为了测试时使用
   * @param force
   * @param callback
   */
  void fetchRouter(boolean force, final AVCallback callback) {
    updateServers();

    Long lastTime = AVPersistenceUtils.sharedInstance().getPersistentSettingLong(
        getAppRouterSPName(), LATEST_UPDATE_TIME_KEY, 0L);

    int ttl = AVPersistenceUtils.sharedInstance().getPersistentSettingInteger(
        getAppRouterSPName(), TTL_KEY, 0);

    if (force || System.currentTimeMillis() - lastTime > ttl * 1000) {
      AVHttpClient client = AVHttpClient.clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(ROUTER_ADDRESS + AVOSCloud.applicationId).get();
      client.execute(builder.build(), false, new GetHttpResponseHandler(new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          if (null == e) {
            if (AVOSCloud.showInternalDebugLog()) {
              LogUtil.avlog.d(" fetchRouter :" + content);
            }

            saveRouterResult(content);
          } else {
            LogUtil.avlog.e("get router error ", e);
          }
          if (null != callback) {
            callback.internalDone(e);
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          LogUtil.avlog.e("get router error ", new AVException(error));
          if (null != callback) {
            callback.internalDone(new AVException(error));
          }
        }

        @Override
        public boolean isRequestStatisticNeed() {
          return false;
        }
      }));
    } else {
      if (null != callback) {
        callback.internalDone(null);
      }
    }
  }

  private void saveRouterResult(String result) {
    com.alibaba.fastjson.JSONObject response = null;
    try {
      response = JSON.parseObject(result);
    } catch (Exception exception) {
      LogUtil.avlog.e("get router error ", exception);
    }

    if (null != response) {
      updateMapAndSaveLocal(serverHostsMap, response, AVOSCloud.SERVER_TYPE.RTM.name, RTM_ROUTER_SERVRE_KEY);
      updateMapAndSaveLocal(serverHostsMap, response, AVOSCloud.SERVER_TYPE.PUSH.name, PUSH_SERVRE_KEY);
      updateMapAndSaveLocal(serverHostsMap, response, AVOSCloud.SERVER_TYPE.API.name, API_SERVER_KEY);
      updateMapAndSaveLocal(serverHostsMap, response, AVOSCloud.SERVER_TYPE.STATS.name, STATS_SERVRE_KEY);
      updateMapAndSaveLocal(serverHostsMap, response, AVOSCloud.SERVER_TYPE.ENGINE.name, ENGINE_SERVRE_KEY);

      if (response.containsKey(TTL_KEY)) {
        AVPersistenceUtils.sharedInstance().savePersistentSettingInteger(
          getAppRouterSPName(), TTL_KEY, response.getIntValue(TTL_KEY));
      }

      AVPersistenceUtils.sharedInstance().savePersistentSettingLong(
        getAppRouterSPName(), LATEST_UPDATE_TIME_KEY, System.currentTimeMillis());
    }
  }

  private void updateMapAndSaveLocal(Map<String, String> maps, JSONObject jsonObject, String mapKey, String jsonKey) {
    if (jsonObject.containsKey(jsonKey)) {
      String value = addHttpsPrefix(jsonObject.getString(jsonKey));
      AVPersistenceUtils.sharedInstance().savePersistentSettingString(
        getAppRouterSPName(), jsonKey, value);
      if (!AVUtils.isBlankString(value)) {
        maps.put(mapKey, value);
      }
    }
  }

  private void refreshMap(Map<String, String> maps, String mapKey, String spKey) {
    String value = AVPersistenceUtils.sharedInstance().getPersistentSettingString(
      getAppRouterSPName(), spKey, "");
    if (!AVUtils.isBlankString(value)) {
      maps.put(mapKey, value);
    }
  }

  private String getAppRouterSPName() {
    return "com.avos.avoscloud.approuter." + AVOSCloud.applicationId;
  }

  /**
   * 根据当前 appId 更新 shareprefenence 的 name
   * 这样如果运行过程中动态切换了 appId，app router 仍然可以正常 work
   */
  private void updateServers() {
    refreshMap(serverHostsMap, AVOSCloud.SERVER_TYPE.RTM.name, RTM_ROUTER_SERVRE_KEY);
    refreshMap(serverHostsMap, AVOSCloud.SERVER_TYPE.PUSH.name, PUSH_SERVRE_KEY);
    refreshMap(serverHostsMap, AVOSCloud.SERVER_TYPE.API.name, API_SERVER_KEY);
    refreshMap(serverHostsMap, AVOSCloud.SERVER_TYPE.STATS.name, STATS_SERVRE_KEY);
    refreshMap(serverHostsMap, AVOSCloud.SERVER_TYPE.ENGINE.name, ENGINE_SERVRE_KEY);
  }

  /**
   * 添加 https 前缀
   * 主要是因为 server 部分 url 返回数据不一致，有的有前缀，有的没有
   *
   * @param url
   * @return
   */
  private String addHttpsPrefix(String url) {
    if (!AVUtils.isBlankString(url) && !url.startsWith("http")) {
      return "https://" + url;
    }
    return url;
  }

  static int getAppRegion(String appId) {
    if (AVUtils.isBlankString(appId)) {
      return APP_REGION_CN_NORTH;
    }
    if (appId.endsWith("9Nh9j0Va")) {
      return APP_REGION_CN_EAST;
    }
    if (appId.endsWith("MdYXbMMI")) {
      return APP_REGION_US_NORTH;
    }
    return APP_REGION_CN_NORTH;
  }
}
