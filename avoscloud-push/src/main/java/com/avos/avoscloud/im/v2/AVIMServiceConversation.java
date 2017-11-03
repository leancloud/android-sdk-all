package com.avos.avoscloud.im.v2;

import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/2.
 */

public class AVIMServiceConversation extends AVIMConversation {
  protected AVIMServiceConversation(AVIMClient client, List<String> members,
                                      Map<String, Object> attributes) {
    super(client, members, attributes, false);
  }
  protected AVIMServiceConversation(AVIMClient client, String conversationId) {
    super(client, conversationId);
  }
}
