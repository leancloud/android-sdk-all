package com.avos.avoscloud.im.v2;

import java.util.List;
import java.util.Map;

/**
 * Created by fengjunwen on 2017/11/2.
 */

public class AVIMChatRoom extends AVIMConversation {
  protected AVIMChatRoom(AVIMClient client, String conversationId){
    super(client, conversationId);
    setTransientForInit(true);
  }

  protected AVIMChatRoom(AVIMClient client, List<String> members,
                             Map<String, Object> attributes) {
    super(client, members, attributes, true);
  }
}
