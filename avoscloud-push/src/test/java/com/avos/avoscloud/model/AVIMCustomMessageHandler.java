package com.avos.avoscloud.model;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMTypedMessageHandler;

/**
 * Created by wli on 2017/8/29.
 */

public class AVIMCustomMessageHandler extends AVIMTypedMessageHandler<AVIMCustomMessage> {


  public AVIMCustomMessageHandler() {
  }

  @Override
  public void onMessage(AVIMCustomMessage message, AVIMConversation conversation, AVIMClient client) {

  }

  @Override
  public void onMessageReceipt(AVIMCustomMessage message, AVIMConversation conversation, AVIMClient client) {
    super.onMessageReceipt(message, conversation, client);
  }
}
