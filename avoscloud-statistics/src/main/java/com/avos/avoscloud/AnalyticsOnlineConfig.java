package com.avos.avoscloud;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/27/13 Time: 1:39 PM To change this template use
 * File | Settings | File Templates.
 */


// fetch config from server
class AnalyticsOnlineConfig {
  private ReportPolicy reportPolicy = ReportPolicy.SEND_INTERVAL;
  private Map<String, String> config = new HashMap<String, String>();
  private final AnalyticsImpl parent;
  // 默认强制开启统计
  private boolean enableStats = true;

  AnalyticsOnlineConfig(AnalyticsImpl ref) {
    super();
    this.parent = ref;
  }

  /**
   * 请求自定义参数
   * @param context
   * @param callback
   */
  void update(final Context context, final AVCallback<Map<String, Object>> callback) {
    String endPoint = String.format("statistics/apps/%s/sendPolicy", AVOSCloud.applicationId);
    PaasClient.statistisInstance().getObject(endPoint, null, false, null,
      new GenericObjectCallback() {
        @Override
        public boolean isRequestStatisticNeed() {
          return false;
        }

        @Override
        public void onSuccess(String content, AVException e) {
          try {
            Map<String, Object> jsonMap = JSONHelper.mapFromString(content);
            if (null != callback) {
              Object parameters = jsonMap.get("parameters");
              if (parameters != null && parameters instanceof Map) {
                callback.internalDone((Map<String, Object>) parameters, e);
              } else {
                callback.internalDone(null, e);
              }
            }
            updateConfig(jsonMap, true);
          } catch (JSONException e1) {
            if (null != callback) {
              callback.internalDone(null, new AVException(e1));
            }
            e1.printStackTrace();
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          LogUtil.log.e("Failed " + content);
          if (null != callback) {
            callback.internalDone(null, new AVException(error));
          }
        }
      });
  }

  private void updateConfig(Map<String, Object> jsonMap, boolean updatePolicy) {
    if (null == jsonMap) {
      return;
    }
    try {
      Object parameters = jsonMap.get("parameters");
      boolean notifyListener = false;
      if (parameters != null && parameters instanceof Map) {
        Map newConfig = (Map) parameters;
        notifyListener = !config.equals(newConfig);
        config.clear();
        config.putAll(newConfig);
        parent.notifyOnlineConfigListener(new JSONObject(config));
      }
      if (updatePolicy) {
        Boolean enable = (Boolean) jsonMap.get("enable");
        if (enable != null) {
          // 服务端参数决定一切
          enableStats = enable;
        }
        Number policy = (Number) jsonMap.get("policy");
        if (policy != null) {
          ReportPolicy oldPolicy = reportPolicy;
          ReportPolicy newPolicy = ReportPolicy.valueOf(policy.intValue());
          if (oldPolicy != newPolicy || notifyListener) {
            parent.setReportPolicy(newPolicy);
          }
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  boolean isEnableStats() {
    return enableStats;
  }

  public void setEnableStats(boolean enableStats) {
    this.enableStats = enableStats;
  }

  boolean setReportPolicy(ReportPolicy p) {
    boolean policyUpdated = this.reportPolicy.value() != p.value();
    this.reportPolicy = p;
    return policyUpdated;
  }

  ReportPolicy getReportPolicy() {
    return reportPolicy;
  }

  String getConfigParams(String key) {
    Object object = config.get(key);
    if (object instanceof String) {
      return (String) object;
    }
    return null;
  }


}
