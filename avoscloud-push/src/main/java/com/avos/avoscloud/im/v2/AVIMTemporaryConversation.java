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

public class AVIMTemporaryConversation extends AVIMReadonlyConversation {
  protected AVIMTemporaryConversation(AVIMClient client, List<String> members,
                             Map<String, Object> attributes) {
    super(client, members, attributes);
    isTemporary = true;
  }

  protected AVIMTemporaryConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
    isTemporary = true;
  }

  /*
   * 判断当前临时对话是否已经过期
   */
  public boolean isExpired() {
    long now = System.currentTimeMillis() / 1000;
    return now > this.temporaryExpiredat;
  }
}
