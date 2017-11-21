package com.avos.avoscloud.im.v2;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;

import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/2.
 */

public class AVIMServiceConversation extends AVIMReadonlyConversation {
  protected AVIMServiceConversation(AVIMClient client, List<String> members,
                                      Map<String, Object> attributes) {
    super(client, members, attributes);
    isSystem = true;
  }

  protected AVIMServiceConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
    isSystem = true;
  }
}
