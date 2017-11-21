package com.avos.avoscloud.im.v2;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;

import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/20.
 */

public class AVIMReadonlyConversation extends AVIMConversation {
  protected AVIMReadonlyConversation(AVIMClient client, List<String> members,
                                      Map<String, Object> attributes) {
    super(client, members, attributes, false);
  }

  protected AVIMReadonlyConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
  }

  /**
   * 在对话中增加新的参与者（已经关闭这一操作）
   *
   * @param friendsList
   * @param callback
   */
  @Override
  public void addMembers(final List<String> friendsList, final AVIMConversationCallback callback) {
    if (null != callback) {
      callback.internalDone(null, new AVException(new UnsupportedOperationException("can't add members for readonly conversation.")));
    }
  }

  /**
   * 在对话中删除参与者（已经关闭这一操作）
   *
   * @param friendsList
   * @param callback
   */
  @Override
  public void kickMembers(final List<String> friendsList, final AVIMConversationCallback callback) {
    if (null != callback) {
      callback.internalDone(null, new AVException(new UnsupportedOperationException("can't kick members for readonly conversation.")));
    }
  }

  /**
   * 更新当前对话的属性至服务器端（已经关闭这一操作）
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
