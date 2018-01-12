package com.avos.avoscloud.im.v2;

import com.avos.avoscloud.AVIMEventHandler;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;

public abstract class MessageHandler<T extends AVIMMessage> extends AVIMEventHandler {
  public abstract void onMessage(T message, AVIMConversation conversation, AVIMClient client);

  public abstract void onMessageReceipt(T message, AVIMConversation conversation, AVIMClient client);

  @Override
  protected final void processEvent0(final int operation, final Object operator, final Object operand,
                                     Object eventScene) {
    final AVIMConversation conversation = (AVIMConversation) eventScene;
//    if (conversation.isShouldFetch()) {
//      conversation.fetchInfoInBackground(new AVIMConversationCallback() {
//        @Override
//        public void done(AVIMException e) {
//          if (null != e && e.getCode() > 0) {
//            conversation.latestConversationFetch = System.currentTimeMillis();
//          }
//          processMessage(operation, operand, conversation);
//        }
//      });
//    } else {
      processMessage(operation, operand, conversation);
//    }
  }

  private void processMessage(int operation, final Object operand, AVIMConversation conversation) {
    switch (operation) {
      case Conversation.STATUS_ON_MESSAGE:
        onMessage((T) operand, conversation, conversation.client);
        break;
      case Conversation.STATUS_ON_MESSAGE_RECEIPTED:
        onMessageReceipt((T) operand, conversation, conversation.client);
        break;
    }
  }
}
