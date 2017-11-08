package com.avos.avoscloud.im.v2;

import com.alibaba.fastjson.JSON;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/2.
 */

public class AVIMTemporaryConversation extends AVIMConversation {
  protected AVIMTemporaryConversation(AVIMClient client, List<String> members,
                             Map<String, Object> attributes) {
    super(client, members, attributes, false);
    isTemporary = true;
  }
  protected AVIMTemporaryConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
    isTemporary = true;
  }

  /**
   * 从服务器同步对话的属性
   *
   * @param callback
   */

  public void fetchInfoInBackground(final AVIMConversationCallback callback) {
    if (AVUtils.isBlankString(conversationId)) {
      if (callback != null) {
        callback.internalDone(null, new AVException(AVException.INVALID_QUERY, "ConversationId is empty"));
      } else {
        LogUtil.avlog.e("ConversationId is empty");
      }
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.QUERY_PARAM_TEMPCONV, conversationId);
    sendCMDToPushService(JSON.toJSONString(params), Conversation.AVIMOperation.CONVERSATION_QUERY, callback);
  }
}
