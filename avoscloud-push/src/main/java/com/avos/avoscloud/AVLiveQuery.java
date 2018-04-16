package com.avos.avoscloud;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.im.v2.AVIMBaseBroadcastReceiver;
import com.avos.avoscloud.im.v2.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by wli on 2017/5/25.
 */

public class AVLiveQuery {
  private static final String SUBSCRIBE_PATH = "LiveQuery/subscribe";
  private static final String UNSUBSCRIBE_PATH = "LiveQuery/unsubscribe";
  private static final String SUBSCRIBE_ID = "id";
  private static final String QUERY_ID = "query_id";
  private static final String SESSION_TOKEN = "sessionToken";
  private static final String QUERY = "query";
  private static final String OBJECT = "object";
  private static final String OP = "op";
  private static final String UPDATE_KEYS = "updatedKeys";
  static final String LIVEQUERY_PRIFIX = "live_query_";
  static final String ACTION_LIVE_QUERY_LOGIN = "action_live_query_login";

  private static String subscribeId;
  private String queryId;
  private AVQuery query;

  private static Set<AVLiveQuery> liveQuerySet = Collections.synchronizedSet(new HashSet<AVLiveQuery>());

  static void resumeSubscribeers() {
    for (AVLiveQuery query: liveQuerySet) {
      query.subscribeInBackground(null);
    }
  }

  private AVLiveQueryEventHandler eventHandler;

  public enum EventType {
    CREATE("create"), UPDATE("update"), ENTER("enter"), LEAVE("leave"), DELETE("delete"), LOGIN("login"), UNKONWN("unknown");

    private String event;

    public static EventType getType(String event) {
      if (CREATE.getContent().equals(event)) {
        return CREATE;
      } else if (UPDATE.getContent().equals(event)) {
        return UPDATE;
      } else if (ENTER.getContent().equals(event)) {
        return ENTER;
      } else if (LEAVE.getContent().equals(event)) {
        return LEAVE;
      } else if (DELETE.getContent().equals(event)) {
        return DELETE;
      } else if (LOGIN.getContent().equals(event)) {
        return LOGIN;
      }
      return UNKONWN;
    }

    EventType(String event) {
      this.event = event;
    }

    public String getContent() {
      return event;
    }
  }

  private AVLiveQuery(AVQuery query) {
    this.query = query;
  }

  /**
   * initialize AVLiveQuery with AVQuery
   * @param query
   * @return
   */
  public static AVLiveQuery initWithQuery(AVQuery query) {
    if (null == query) {
      throw new IllegalArgumentException("query cannot be null");
    }
    return new AVLiveQuery(query);
  }

  /**
   * subscribe the query
   * @param callback
   */
  public void subscribeInBackground(final AVLiveQuerySubscribeCallback callback) {
    Map<String, String> params = query.assembleParameters();
    params.put("className", query.getClassName());

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(QUERY, params);
    String session = getSessionToken();
    if (!AVUtils.isBlankString(session)) {
      dataMap.put(SESSION_TOKEN, session);
    }

    dataMap.put(SUBSCRIBE_ID, getSubscribeId());

    final String jsonString = AVUtils.jsonStringFromMapWithNull(dataMap);
    PaasClient.storageInstance().postObject(SUBSCRIBE_PATH, jsonString, false, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        JSONObject jsonObject = JSONObject.parseObject(content);
        if (null != jsonObject && jsonObject.containsKey(QUERY_ID)) {
          queryId = jsonObject.getString(QUERY_ID);
          liveQuerySet.add(AVLiveQuery.this);

          loginLiveQuery(callback);
        } else if (null != callback) {
          callback.internalDone(new AVException(e));
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (null != callback) {
          callback.internalDone(new AVException(error));
        }
      }
    });
  }

  public void setEventHandler(AVLiveQueryEventHandler eventHandler) {
    if (null == eventHandler) {
      throw new IllegalArgumentException("eventHandler can not be null.");
    }
    this.eventHandler = eventHandler;
  }

  private void loginLiveQuery(final AVLiveQuerySubscribeCallback callback) {
    int requestId = AVUtils.getNextIMRequestId();
    BroadcastReceiver loginReceiver = new AVIMBaseBroadcastReceiver(null) {
      @Override
      public void execute(Intent intent, Throwable error) {
        if (null != callback) {
          callback.internalDone(null == error ? null : new AVException(error));
        }
      }
    };

    LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(loginReceiver,
      new IntentFilter(LIVEQUERY_PRIFIX + requestId));

    try {
      Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
      i.setAction(ACTION_LIVE_QUERY_LOGIN);
      i.putExtra(SUBSCRIBE_ID, getSubscribeId());
      i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
      AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
    } catch (Exception ex) {
      LogUtil.avlog.e("failed to start PushServer. cause: " + ex.getMessage());
    }
  }

  /**
   * unsubscribe the query
   * @param callback
   */
  public void unsubscribeInBackground(final AVLiveQuerySubscribeCallback callback) {
    Map<String, String> map = new HashMap<>();
    map.put(SUBSCRIBE_ID, getSubscribeId());
    map.put(QUERY_ID, queryId);
    String jsonString = AVUtils.jsonStringFromMapWithNull(map);

    PaasClient.storageInstance().postObject(UNSUBSCRIBE_PATH, jsonString, false, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        if (null == e) {
          liveQuerySet.remove(AVLiveQuery.this);
          queryId = "";
          if (null != callback) {
            callback.internalDone(null);
          }
        } else if (null != callback) {
          callback.internalDone(e);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (null != callback) {
          callback.internalDone(new AVException(error));
        }
      }
    });
  }

  private String getSubscribeId() {
    if (AVUtils.isBlankString(subscribeId)) {
      final String SP_LIVEQUERY_KEY = "livequery_keyzone";
      final String SP_SUBSCRIBE_ID = "subscribeId";
      subscribeId = AVPersistenceUtils.sharedInstance().getPersistentSettingString(SP_LIVEQUERY_KEY,SP_SUBSCRIBE_ID, "");
      if (AVUtils.isBlankString(subscribeId)) {
        String packageName = AVOSCloud.applicationContext.getPackageName();
        String additionalStr = UUID.randomUUID().toString();
        subscribeId = AVUtils.md5(packageName + additionalStr);
        AVPersistenceUtils.sharedInstance().savePersistentSettingString(SP_LIVEQUERY_KEY, SP_SUBSCRIBE_ID, subscribeId);
      }
    }
    return subscribeId;
  }

  private String getSessionToken() {
    AVUser currentUser = AVUser.getCurrentUser();
    if (null != currentUser) {
      return currentUser.getSessionToken();
    }
    return "";
  }

  static void processData(ArrayList<String> dataList) {
    for (final String data : dataList) {
      AVOSCloud.handler.post(new Runnable() {
        @Override
        public void run() {
          try {
            JSONObject jsonObject = JSON.parseObject(data);
            String op = jsonObject.getString(OP);
            String queryId = jsonObject.getString(QUERY_ID);
            JSONObject object = jsonObject.getJSONObject(OBJECT);
            if (!AVUtils.isBlankString(queryId)) {
              ArrayList<String> updateKeyList = new ArrayList<String>();
              if (jsonObject.containsKey(UPDATE_KEYS)) {
                JSONArray jsonArray = jsonObject.getJSONArray(UPDATE_KEYS);
                for (Object item : jsonArray) {
                  updateKeyList.add((String)item);
                }
              }

              for (AVLiveQuery liveQuery : liveQuerySet) {
                if (queryId.equals(liveQuery.queryId) && null != liveQuery.eventHandler) {
                  liveQuery.eventHandler.done(EventType.getType(op), AVUtils.parseObjectFromMap(object), updateKeyList);
                }
              }
            }
          } catch (Exception e) {
            if (AVOSCloud.isDebugLogEnabled()) {
              LogUtil.avlog.e("Parsing json data error, ", e);
            }
          }
        }
      });
    }
  }
}
