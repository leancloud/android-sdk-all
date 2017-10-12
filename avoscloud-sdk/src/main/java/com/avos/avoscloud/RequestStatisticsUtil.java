package com.avos.avoscloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import org.apache.http.Header;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by wli on 15/7/1.
 * 请求性能统计
 */
class RequestStatisticsUtil {
  private static final String REQUEST_DATA = "com.avos.avoscloud.RequestStatisticsUtil.data";
  private static final String LAST_SENDTIME = "lastSendTime";
  private static int TIME_INTERVAL = 24 * 3600 * 1000;
  private static RequestStatisticsUtil sInstance;
  public static Boolean REPORT_INTERNAL_STATS = true;

  /**
   * 最近一次更新成功的时间
   */
  private long lastSendTime = 0;

  /**
   * 用于存储收集的信息
   */
  private RequestStatistics requestStatistics;

  private RequestStatisticsUtil() {
    getLastSendTime();
    requestStatistics = new RequestStatistics();
  }

  public synchronized static RequestStatisticsUtil getInstance() {
    if (sInstance == null) {
      sInstance = new RequestStatisticsUtil();
    }
    return sInstance;
  }

  /**
   * 记录统计数据
   * 
   * @param statusCode http status code
   * @param isTimeOut 是否超时
   * @param time 请求所耗费的时间
   */
  public void recordRequestTime(int statusCode, boolean isTimeOut, long time) {
    if (!REPORT_INTERNAL_STATS) {
      return;
    }
    if (time > 0 && time < AVOSCloud.getNetworkTimeout() * 2) {
      requestStatistics.addRequestData(statusCode, isTimeOut, time);
      requestStatistics.saveToPreference();
    }
  }

  /**
   * 如果距离上次发送超过一天的时间，则将数据发送至服务器，同步发送
   */
  public void sendToServer() {
    if (!REPORT_INTERNAL_STATS) {
      return;
    }
    if (isNeedToSend()) {
      sendData(new RequestStatistics());
    }
  }

  private Map<String, Object> getClientInfo(Context context) {
    Map<String, Object> map = new HashMap<String, Object>();

    String packageName = context.getApplicationContext().getPackageName();
    map.put("platform", "Android");
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
      map.put("app_version", info.versionName);
      map.put("sdk_version", PaasClient.sdkVersion);
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    String macAddress = null;
    try {
      WifiManager wifiManager =
          (WifiManager) AVOSCloud.applicationContext.getSystemService(Context.WIFI_SERVICE);
      WifiInfo wInfo = wifiManager.getConnectionInfo();
      macAddress = wInfo.getMacAddress();
    } catch (Exception e) {}
    String androidId =
        Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    String deviceId =
        AVUtils.isBlankString(macAddress) ? androidId : AVUtils.md5(macAddress + androidId);
    map.put("id", deviceId);
    return map;
  }

  /**
   * 将数据发送至服务器，同步发送
   */
  private void sendData(final RequestStatistics requestData) {
    if (requestData.totalNum > 0) {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("client", getClientInfo(AVOSCloud.applicationContext));
      data.put("attributes", requestData.toPostDataMap());

      PaasClient client = PaasClient.statistisInstance();
      AVHttpClient asyncClient = AVHttpClient.clientInstance();
      String url = client.buildUrl("always_collect");
      try {
        Request.Builder builder = new Request.Builder();
        builder.url(url).put(RequestBody.create(AVHttpClient.JSON,
          AVUtils.jsonStringFromMapWithNull(data).getBytes("UTF-8")));
        client.updateHeaders(builder,null,false);
        asyncClient.execute(builder.build(), true   , new AsyncHttpResponseHandler() {
          @Override
          public void onSuccess(int statusCode, Header[] headers, byte[] body) {
            if (200 == statusCode) {
              updateLastSendTime();
              requestStatistics.minus(requestData);
              requestStatistics.saveToPreference();
            }
          }

          @Override
          public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
              Throwable error) {}
        });
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    } else {
      updateLastSendTime();
    }
  }

  private boolean isNeedToSend() {
    return System.currentTimeMillis() > lastSendTime + TIME_INTERVAL;
  }

  private void updateLastSendTime() {
    lastSendTime = System.currentTimeMillis();
    SharedPreferences preferences =
        AVOSCloud.applicationContext.getSharedPreferences(REQUEST_DATA, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putLong(LAST_SENDTIME, lastSendTime);
    editor.commit();
  }

  private void getLastSendTime() {
    SharedPreferences preferences =
        AVOSCloud.applicationContext.getSharedPreferences(REQUEST_DATA, Context.MODE_PRIVATE);
    lastSendTime = preferences.getLong(LAST_SENDTIME, 0);
    lastSendTime = preferences.getLong(LAST_SENDTIME, 0);
  }

  /**
   * 收集信息属性的类
   */
  private static class RequestStatistics {
    private static final String REQUEST_TOTAL_NUM = "request_total_num";
    private static final String REQUEST_TIMEOUT_NUM = "request_timeout_num";
    private static final String REQUEST_2xx_TOTAL_TIME = "request_2xx_total_time";
    private static final String REQUEST_2xx_NUM = "request_2xx_num";
    private static final String REQUEST_4xx_NUM = "request_4xx_num";
    private static final String REQUEST_5xx_NUM = "request_5xx_num";

    /**
     * 总请求的数量
     */
    private int totalNum = 0;

    /**
     * 超时请求的数量
     */
    private int timeoutNum = 0;

    /**
     * 请求为2xx的数量
     */
    private int request2xxNum = 0;

    /**
     * 请求为4xx的数量
     */
    private int request4xxNum = 0;

    /**
     * 请求为5xx的数量
     */
    private int request5xxNum = 0;

    /**
     * 请求为2xx的总时间
     */
    private long request2xxTotalTime = 0;

    public RequestStatistics() {
      updateFromPreference();
    }

    public synchronized void addRequestData(int statusCode, boolean isTimeOut, long time) {
      if (isTimeOut) {
        ++totalNum;
        ++timeoutNum;
      } else {
        if (statusCode > 100) {
          int beginCode = statusCode / 100;
          if (beginCode == 2) {
            ++totalNum;
            ++request2xxNum;
            request2xxTotalTime += time;
          } else if (beginCode == 4) {
            ++totalNum;
            ++request4xxNum;
          } else if (beginCode == 5) {
            ++totalNum;
            ++request5xxNum;
          }
        }
      }
    }

    public synchronized void minus(RequestStatistics requestStatistics) {
      if (requestStatistics != null) {
        totalNum -= requestStatistics.totalNum;
        timeoutNum -= requestStatistics.timeoutNum;
        request2xxNum -= requestStatistics.request2xxNum;
        request4xxNum -= requestStatistics.request4xxNum;
        request5xxNum -= requestStatistics.request5xxNum;
        request2xxTotalTime -= requestStatistics.request2xxTotalTime;
      }
    }

    public synchronized void saveToPreference() {
      SharedPreferences preferences =
          AVOSCloud.applicationContext.getSharedPreferences(REQUEST_DATA, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putInt(REQUEST_TIMEOUT_NUM, timeoutNum);
      editor.putInt(REQUEST_TOTAL_NUM, totalNum);
      editor.putInt(REQUEST_2xx_NUM, request2xxNum);
      editor.putInt(REQUEST_4xx_NUM, request4xxNum);
      editor.putInt(REQUEST_5xx_NUM, request5xxNum);
      editor.putLong(REQUEST_2xx_TOTAL_TIME, request2xxTotalTime);
      editor.commit();
    }

    public synchronized Map<String, Object> toPostDataMap() {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("total", totalNum);
      map.put("timeout", timeoutNum);
      map.put("2xx", request2xxNum);
      map.put("4xx", request4xxNum);
      map.put("5xx", request5xxNum);
      map.put("avg", 0 == request2xxNum ? 0 : (request2xxTotalTime / request2xxNum));
      return map;
    }

    private synchronized void updateFromPreference() {
      SharedPreferences preferences =
          AVOSCloud.applicationContext.getSharedPreferences(REQUEST_DATA, Context.MODE_PRIVATE);
      timeoutNum = preferences.getInt(REQUEST_TIMEOUT_NUM, 0);
      totalNum = preferences.getInt(REQUEST_TOTAL_NUM, 0);
      request2xxNum = preferences.getInt(REQUEST_2xx_NUM, 0);
      request4xxNum = preferences.getInt(REQUEST_4xx_NUM, 0);
      request5xxNum = preferences.getInt(REQUEST_5xx_NUM, 0);
      request2xxTotalTime = preferences.getLong(REQUEST_2xx_TOTAL_TIME, 0);
    }
  }
}
