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
   * 在聊天对话中间增加新的参与者（临时对话已经关闭这一操作）
   *
   * @param friendsList
   * @param callback
   */
  @Override
  public void addMembers(final List<String> friendsList, final AVIMConversationCallback callback) {
    if (null != callback) {
      callback.internalDone(null, new AVException(new UnsupportedOperationException("can't add members for temporary conversation.")));
    }
  }

  /**
   * 更新当前对话的属性至服务器端（临时对话已经关闭这一操作）
   *
   * @param callback
   */
  @Override
  public void updateInfoInBackground(AVIMConversationCallback callback) {
    if (null != callback) {
      callback.internalDone(null, new AVException(new UnsupportedOperationException("can't update anything for temporary conversation.")));
    }
  }
}
