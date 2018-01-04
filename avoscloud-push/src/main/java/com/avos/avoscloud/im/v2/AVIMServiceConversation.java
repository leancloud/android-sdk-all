package com.avos.avoscloud.im.v2;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVErrorUtils;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVRequestParams;
import com.avos.avoscloud.AVResponse;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationMemberCountCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/2.
 */

public class AVIMServiceConversation extends AVIMReadonlyConversation {
  private static final String SUBSCRIBERCOUNT_PATH = "rtm/conversation/subscribed";

  protected AVIMServiceConversation(AVIMClient client, List<String> members,
                                      Map<String, Object> attributes) {
    super(client, members, attributes);
    isSystem = true;
  }

  protected AVIMServiceConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
    isSystem = true;
  }

  /**
   * 订阅该服务号
   *
   * @param callback  结果回调函数
   */
  public void subscribe(final AVIMConversationCallback callback) {
    join(callback);
  }

  /**
   * 退订该服务号
   *
   * @param callback   结果回调函数
   */
  public void unsubscribe(final AVIMConversationCallback callback) {
    quit(callback);
  }

  /**
   * 查询本服务号的订阅用户数
   *
   * @param callback   结果回调函数
   */
  public void getSubscriberCount(final AVIMConversationMemberCountCallback callback) {
    if (null == callback) {
      return;
    }
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("conv_id", this.conversationId);
    AVRequestParams params = new AVRequestParams(queryParams);
    PaasClient.storageInstance().getObject(SUBSCRIBERCOUNT_PATH, params, false,
        null, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (null != e) {
              callback.internalDone(null, new AVIMException(e));
            } else {
              int result = 0;
              if (AVUtils.isBlankContent(content)) {
                LogUtil.log.e("response is empty for request " + SUBSCRIBERCOUNT_PATH);
              } else {
                try {
                  AVResponse resp = new AVResponse();
                  resp = JSON.parseObject(content, resp.getClass());
                  result = resp.count;
                } catch (Exception ex) {
                  LogUtil.log.e("failed to parse result for request " + SUBSCRIBERCOUNT_PATH, ex);
                }
              }
              callback.internalDone(result, null);
            }
          }
          @Override
          public void onFailure(Throwable error, String content) {
            LogUtil.log.e("failed to query " + SUBSCRIBERCOUNT_PATH + " cause: " + error.getMessage());
            callback.internalDone(null, new AVIMException(content, error));
          }
    }, AVQuery.CachePolicy.NETWORK_ONLY, 86400000);
  }
}
