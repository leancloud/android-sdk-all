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

  /**
   * api 默认的地址
   */
  private static final String DEFAULT_QCLOUD_API_SERVER = "https://e1-api.leancloud.cn";
  private static final String DEFALUT_US_API_SERVER = "https://us-api.leancloud.cn";

  /**
   * push router 默认地址
   */
  private static final String DEFAULT_QCLOUD_ROUTER_SERVER = "https://router-q0-push.leancloud.cn";
  private static final String DEFAULT_US_ROUTER_SERVER = "https://router-a0-push.leancloud.cn";


  private Map<String, String> apiMaps = new ConcurrentHashMap<>();
  private static Map<String, String> customApiMaps = new ConcurrentHashMap<>();

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
    customApiMaps.put(server.name, host);
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
    if (customApiMaps.containsKey(type.name)) {
      return customApiMaps.get(type.name);
    }

    final boolean isRtm = type.equals(AVOSCloud.SERVER_TYPE.RTM);

    // 美国节点
    if (isUsApp(AVOSCloud.applicationId)) {
      return isRtm ? DEFAULT_US_ROUTER_SERVER : DEFALUT_US_API_SERVER;
    }

    // QCloud 节点
    if (isQCloudApp(AVOSCloud.applicationId)) {
      return isRtm ? DEFAULT_QCLOUD_ROUTER_SERVER : DEFAULT_QCLOUD_API_SERVER;
    }

    // UCloud 节点
    if (apiMaps.containsKey(type.name) && !AVUtils.isBlankString(apiMaps.get(type.name))) {
      return apiMaps.get(type.name);
    } else {
      return getUcloudDefaultServer(type);
    }
  }

  /**
   * 获取默认的 UCloud 节点的 url
   * @param type
   * @return
   */
  private String getUcloudDefaultServer(AVOSCloud.SERVER_TYPE type) {
    if (!AVUtils.isBlankString(AVOSCloud.applicationId)) {
      return String.format("https://%s.%s.lncld.net", AVOSCloud.applicationId.substring(0, 8), type.name);
    } else {
      LogUtil.avlog.e("AppId is null, Please call AVOSCloud.initialize first");
      return "";
    }
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
    apiMaps.put(AVOSCloud.SERVER_TYPE.RTM.name, addHttpsPrefix(router));
    if (persistence) {
      AVPersistenceUtils.sharedInstance().savePersistentSettingString(
        getAppRouterSPName(), RTM_ROUTER_SERVRE_KEY, apiMaps.get(AVOSCloud.SERVER_TYPE.RTM.name));
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
    if (!isUsApp(AVOSCloud.applicationId)) {
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
      updateMapAndSaveLocal(apiMaps, response, AVOSCloud.SERVER_TYPE.RTM.name, RTM_ROUTER_SERVRE_KEY);
      updateMapAndSaveLocal(apiMaps, response, AVOSCloud.SERVER_TYPE.PUSH.name, PUSH_SERVRE_KEY);
      updateMapAndSaveLocal(apiMaps, response, AVOSCloud.SERVER_TYPE.API.name, API_SERVER_KEY);
      updateMapAndSaveLocal(apiMaps, response, AVOSCloud.SERVER_TYPE.STATS.name, STATS_SERVRE_KEY);
      updateMapAndSaveLocal(apiMaps, response, AVOSCloud.SERVER_TYPE.ENGINE.name, ENGINE_SERVRE_KEY);

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
    refreshMap(apiMaps, AVOSCloud.SERVER_TYPE.RTM.name, RTM_ROUTER_SERVRE_KEY);
    refreshMap(apiMaps, AVOSCloud.SERVER_TYPE.PUSH.name, PUSH_SERVRE_KEY);
    refreshMap(apiMaps, AVOSCloud.SERVER_TYPE.API.name, API_SERVER_KEY);
    refreshMap(apiMaps, AVOSCloud.SERVER_TYPE.STATS.name, STATS_SERVRE_KEY);
    refreshMap(apiMaps, AVOSCloud.SERVER_TYPE.ENGINE.name, ENGINE_SERVRE_KEY);
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

  /**
   * QCloud 节点的末尾是写死的，这里根据末尾后缀判断是否为 QCloud 节点
   *
   * @return
   */
  static boolean isQCloudApp(String appId) {
    return !AVUtils.isBlankString(appId) && appId.endsWith("9Nh9j0Va");
  }

  /**
   * 判断是否为 us 节点
   * @param appId
   * @return
   */
  static boolean isUsApp(String appId) {
    return !AVOSCloud.isCN() || (!AVUtils.isBlankString(appId) && appId.endsWith("MdYXbMMI"));
  }
}
