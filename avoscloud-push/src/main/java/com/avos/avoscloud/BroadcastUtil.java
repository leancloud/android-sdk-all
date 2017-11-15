package com.avos.avoscloud;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.avos.avoscloud.im.v2.Conversation;

/**
 * Created by lbt05 on 7/17/15.
 */
public class BroadcastUtil {
  public static void sendIMLocalBroadcast(String clientId, String conversationId, int requestId,
                                          Conversation.AVIMOperation operation) {
    sendIMLocalBroadcast(clientId, conversationId, requestId, null, null, operation);
  }

  public static void sendIMLocalBroadcast(String clientId, String conversationId, int requestId,
                                          Throwable throwable, Conversation.AVIMOperation operation) {
    sendIMLocalBroadcast(clientId, conversationId, requestId, null, throwable, operation);
  }

  public static void sendIMLocalBroadcast(String clientId, String conversationId, int requestId,
                                          Bundle bundle, Conversation.AVIMOperation operation) {
    sendIMLocalBroadcast(clientId, conversationId, requestId, bundle, null, operation);
  }

  private static void sendIMLocalBroadcast(String clientId, String conversationId, int requestId,
                                           Bundle bundle, Throwable throwable, Conversation.AVIMOperation operation) {
    if (isOperationValid(operation)) {
      String keyHeader = operation.getOperation();

      Intent callbackIntent = new Intent(keyHeader + requestId);

      callbackIntent.putExtra(Conversation.callbackClientKey, clientId);
      if (!AVUtils.isBlankString(conversationId)) {
        callbackIntent.putExtra(Conversation.callbackConversationKey, conversationId);
      }

      if (null != throwable) {
        callbackIntent.putExtra(Conversation.callbackExceptionKey, throwable);
      }

      if (null != bundle) {
        callbackIntent.putExtras(bundle);
      }
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).sendBroadcast(callbackIntent);
    }
  }

  private static boolean isOperationValid(Conversation.AVIMOperation operation) {
    return null != operation &&
      Conversation.AVIMOperation.CONVERSATION_UNKNOWN.getCode() != operation.getCode();
  }
}
