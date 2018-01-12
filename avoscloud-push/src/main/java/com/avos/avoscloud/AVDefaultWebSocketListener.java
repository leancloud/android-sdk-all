package com.avos.avoscloud;

import android.os.Bundle;

import java.util.List;
import com.avos.avoscloud.AVIMOperationQueue.Operation;
import com.avos.avoscloud.PendingMessageCache.Message;
import com.avos.avoscloud.im.v2.AVIMBinaryMessage;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.Conversation;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avospush.push.AVWebSocketListener;
import com.avos.avospush.session.BlacklistCommandPacket;
import com.avos.avospush.session.CommandPacket;
import com.avos.avospush.session.ConversationAckPacket;
import com.avos.avospush.session.ConversationControlPacket.ConversationControlOp;
import com.avos.avospush.session.MessageReceiptCache;
import com.avos.avospush.session.SessionAckPacket;
import com.avos.avospush.session.SessionControlPacket;
import com.avos.avospush.session.StaleMessageDepot;
import com.google.protobuf.ByteString;

class AVDefaultWebSocketListener implements AVWebSocketListener {

  AVSession session;
  private final StaleMessageDepot depot;
  private static final String SESSION_MESSASGE_DEPOT = "com.avos.push.session.message.";

  public AVDefaultWebSocketListener(AVSession session) {
    this.session = session;
    depot = new StaleMessageDepot(SESSION_MESSASGE_DEPOT + session.getSelfPeerId());
  }

  private static final int CODE_SESSION_SIGNATURE_FAILURE = 4102;
  private static final int CODE_SESSION_TOKEN_FAILURE = 4112;

  @Override
  public void onWebSocketOpen() {
    if (session.sessionOpened.get() || session.sessionResume.get()) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("web socket opened, send session open.");
      }
      session.reopen();
    }
  }

  @Override
  public void onWebSocketClose() {
    if (!session.sessionPaused.getAndSet(true)) {
      try {
        session.sessionListener.onSessionPaused(AVOSCloud.applicationContext, session);
        // 这里给所有的消息发送失败消息
        if (session.pendingMessages != null && !session.pendingMessages.isEmpty()) {
          while (!session.pendingMessages.isEmpty()) {
            Message m = session.pendingMessages.poll();
            if (!AVUtils.isBlankString(m.cid)) {
              AVConversationHolder conversation = session.getConversationHolder(m.cid, Conversation.CONV_TYPE_NORMAL);
              BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(),
                  conversation.conversationId, Integer.parseInt(m.id), new RuntimeException(
                      "Connection Lost"),
                  AVIMOperation.CONVERSATION_SEND_MESSAGE);
            }
          }
        }
        if (session.conversationOperationCache != null
            && !session.conversationOperationCache.isEmpty()) {
          for (int i = 0; i < session.conversationOperationCache.cache.size(); i++) {
            int requestId = session.conversationOperationCache.cache.keyAt(i);
            Operation op = session.conversationOperationCache.poll(requestId);
            BroadcastUtil.sendIMLocalBroadcast(op.sessionId, op.conversationId, requestId,
                new IllegalStateException("Connection Lost"),
                AVIMOperation.getAVIMOperation(op.operation));
          }
        }
      } catch (Exception e) {
        session.sessionListener.onError(AVOSCloud.applicationContext, session, e);
      }
    }
  }

  @Override
  public void onDirectCommand(Messages.DirectCommand directCommand) {
    final String msg = directCommand.getMsg();
    final ByteString binaryMsg = directCommand.getBinaryMsg();
    final String fromPeerId = directCommand.getFromPeerId();
    final String conversationId = directCommand.getCid();
    final Long timestamp = directCommand.getTimestamp();
    final String messageId = directCommand.getId();
    int convType = directCommand.hasConvType()? directCommand.getConvType() : Conversation.CONV_TYPE_NORMAL;
    final boolean isTransient = directCommand.hasTransient() && directCommand.getTransient();
    final boolean hasMore = directCommand.hasHasMore() && directCommand.getHasMore();
    final long patchTimestamp = directCommand.getPatchTimestamp();
    final boolean mentionAll = directCommand.hasMentionAll()? directCommand.getMentionAll() : false;
    final List<String> mentionList = directCommand.getMentionPidsList();

    try {
      if (!isTransient) {
        if (!AVUtils.isBlankString(conversationId)) {
          PushService.sendData(ConversationAckPacket.getConversationAckPacket(
            session.getSelfPeerId(), conversationId, messageId));
        } else {
          PushService.sendData(genSessionAckPacket(messageId));
        }
      }

      if (depot.putStableMessage(messageId) && !AVUtils.isBlankString(conversationId)) {
        AVConversationHolder conversation = session.getConversationHolder(conversationId, convType);
        AVIMMessage message = null;
        if (AVUtils.isBlankString(msg) && null != binaryMsg) {
          message = new AVIMBinaryMessage(conversationId, fromPeerId, timestamp, -1);
          ((AVIMBinaryMessage)message).setBytes(binaryMsg.toByteArray());
        } else {
          message = new AVIMMessage(conversationId, fromPeerId, timestamp, -1);
          message.setContent(msg);
        }
        message.setMessageId(messageId);
        message.setUpdateAt(patchTimestamp);
        message.setMentionAll(mentionAll);
        message.setMentionList(mentionList);
        conversation.onMessage(message, hasMore, isTransient);
      }
    } catch (Exception e) {
      session.sessionListener.onError(AVOSCloud.applicationContext, session, e);
    }
  }

  @Override
  public void onSessionCommand(String op, Integer requestKey, Messages.SessionCommand command) {
    int requestId = (null != requestKey ? requestKey : CommandPacket.UNSUPPORTED_OPERATION);

    if (op.equals(SessionControlPacket.SessionControlOp.OPENED)) {
      try {
        session.sessionOpened.set(true);
        session.sessionResume.set(false);

        if (!session.sessionPaused.getAndSet(false)) {
          if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
            session.conversationOperationCache.poll(requestId);
          }
          session.sessionListener.onSessionOpen(AVOSCloud.applicationContext, session, requestId);
        } else {
          if (AVOSCloud.showInternalDebugLog()) {
            LogUtil.avlog.d("session resumed");
          }
          session.sessionListener.onSessionResumed(AVOSCloud.applicationContext, session);
        }
        if (command.hasSt() && command.hasStTtl()) {
          session.updateRealtimeSessionToken(command.getSt(), Integer.valueOf(command.getStTtl()));
        }
      } catch (Exception e) {
        session.sessionListener.onError(AVOSCloud.applicationContext, session, e);
      }
    } else if (op.equals(SessionControlPacket.SessionControlOp.RENEWED_RTMTOKEN)) {
      if (command.hasSt() && command.hasStTtl()) {
        session.updateRealtimeSessionToken(command.getSt(), Integer.valueOf(command.getStTtl()));
      }
      session.sessionListener.onSessionTokenRenewed(AVOSCloud.applicationContext, session, requestId);
    }else if (op.equals(SessionControlPacket.SessionControlOp.QUERY_RESULT)) {
      final List<String> sessionPeerIds = command.getOnlineSessionPeerIdsList();
      session.sessionListener.onOnlineQuery(AVOSCloud.applicationContext, session, sessionPeerIds,
          requestId);

    } else if (op.equals(SessionControlPacket.SessionControlOp.CLOSED)) {
      if (command.hasCode()) {
        session.sessionListener.onSessionClosedFromServer(AVOSCloud.applicationContext, session,
            command.getCode());
      } else {
        if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
          session.conversationOperationCache.poll(requestId);
        }
        session.sessionListener.onSessionClose(AVOSCloud.applicationContext, session, requestId);
      }
    }
  }

  private void onAckError(Integer requestKey, Messages.AckCommand command, Message m) {
    Operation op = session.conversationOperationCache.poll(requestKey);
    if (op.operation == AVIMOperation.CLIENT_OPEN.getCode()) {
      session.sessionOpened.set(false);
      session.sessionResume.set(false);
    }
    AVIMOperation operation = AVIMOperation.getAVIMOperation(op.operation);
    int code = command.getCode();
    int appCode = (command.hasAppCode() ? command.getAppCode() : 0);
    String reason = command.getReason();
    AVException error = new AVIMException(code, appCode, reason);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), op.conversationId,
      requestKey, error, operation);
  }

  @Override
  public void onAckCommand(Integer requestKey, Messages.AckCommand ackCommand) {
    session.setServerAckReceived(System.currentTimeMillis() / 1000);
    long timestamp = ackCommand.getT();
    final Message m = session.pendingMessages.poll(String.valueOf(requestKey));
    if (ackCommand.hasCode()) {
      // 这里是发送消息异常时的ack
      this.onAckError(requestKey, ackCommand, m);
    } else {
      if (null != m && !AVUtils.isBlankString(m.cid)) {
        AVConversationHolder conversation = session.getConversationHolder(m.cid, Conversation.CONV_TYPE_NORMAL);
        session.conversationOperationCache.poll(requestKey);
        String msgId = ackCommand.getUid();
        conversation.onMessageSent(requestKey, msgId, timestamp);
        if (m.requestReceipt) {
          m.timestamp = timestamp;
          m.id = msgId;
          MessageReceiptCache.add(session.getSelfPeerId(), msgId, m);
        }
      }
    }
  }

  @Override
  public void onListenerAdded(boolean open) {
    if (open) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("web socket opened, send session open.");
      }
      this.onWebSocketOpen();
    }
  }

  @Override
  public void onListenerRemoved() {

  }

  @Override
  public void onMessageReceipt(Messages.RcpCommand rcpCommand) {
    try {
      if (rcpCommand.hasT()) {
        final Long timestamp = rcpCommand.getT();
        final String conversationId = rcpCommand.getCid();
        int convType = Conversation.CONV_TYPE_NORMAL; // RcpCommand doesn't include convType, so we use default value.
        // Notice: it becomes a problem only when server send RcpCommand to a new device for the logined user.

        if (!AVUtils.isBlankString(conversationId)) {
          processConversationDeliveredAt(conversationId, convType, timestamp);
          processMessageReceipt(rcpCommand.getId(), conversationId, convType, timestamp);
        }
      }
    } catch (Exception e) {
      session.sessionListener.onError(AVOSCloud.applicationContext, session, e);
    }
  }

  /**
   * 处理 v2 版本中 conversation 的 deliveredAt 事件
   * @param conversationId
   * @param timestamp
   */
  private void processConversationDeliveredAt(String conversationId, int convType, long timestamp) {
    AVConversationHolder conversation = session.getConversationHolder(conversationId, convType);
    conversation.onConversationDeliveredAtEvent(timestamp);
  }

  /**
   * 处理 v2 版本中 message 的 rcp 消息
   * @param msgId
   * @param conversationId
   * @param timestamp
   */
  private void processMessageReceipt(String msgId, String conversationId, int convType, long timestamp) {
    Object messageCache =
      MessageReceiptCache.get(session.getSelfPeerId(), msgId);
    if (messageCache == null) {
      return;
    }
    Message m = (Message) messageCache;
    AVIMMessage msg =
      new AVIMMessage(conversationId, session.getSelfPeerId(), m.timestamp, timestamp);
    msg.setMessageId(m.id);
    msg.setContent(m.msg);
    msg.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
    AVConversationHolder conversation = session.getConversationHolder(conversationId, convType);
    conversation.onMessageReceipt(msg);
  }

  @Override
  public void onReadCmdReceipt(Messages.RcpCommand rcpCommand) {
    if (rcpCommand.hasRead() && rcpCommand.hasCid()) {
      final Long timestamp = rcpCommand.getT();
      String conversationId = rcpCommand.getCid();
      AVConversationHolder conversation = session.getConversationHolder(conversationId, Conversation.CONV_TYPE_NORMAL);
      conversation.onConversationReadAtEvent(timestamp);
    }
  }

  @Override
  public void onBlacklistCommand(String operation, Integer requestKey, Messages.BlacklistCommand blacklistCommand) {
    if (BlacklistCommandPacket.BlacklistCommandOp.QUERY_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null == op || op.operation != AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY.getCode()) {
        LogUtil.log.w("not found requestKey: " + requestKey);
      } else {
        List<String> result = blacklistCommand.getBlockedPidsList();
        String[] resultArray = new String[null == result ? 0: result.size()];
        if (null != result) {
          result.toArray(resultArray);
        }
        String cid = blacklistCommand.getSrcCid();
        Bundle bundle = new Bundle();
        bundle.putStringArray(Conversation.callbackData, resultArray);
        BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), cid, requestKey,
            bundle, AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY);
      }
    } else if (BlacklistCommandPacket.BlacklistCommandOp.BLOCKED.equals(operation)
        || BlacklistCommandPacket.BlacklistCommandOp.UNBLOCKED.equals(operation)){
      // response for block/unblock reqeust.
      String conversationId = blacklistCommand.getSrcCid();
      AVConversationHolder internalConversation = session.getConversationHolder(conversationId, Conversation.CONV_TYPE_NORMAL);
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null == op || null == internalConversation) {
        // warning.
      } else {
        AVIMOperation originOperation = AVIMOperation.getAVIMOperation(op.operation);
        internalConversation.onResponse4MemberBlock(originOperation, operation, requestKey, blacklistCommand);
      }
    }
  }
  @Override
  public void onConversationCommand(String operation, Integer requestKey, Messages.ConvCommand convCommand) {
    if (ConversationControlOp.QUERY_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == AVIMOperation.CONVERSATION_QUERY.getCode()) {
        String result = convCommand.getResults().getData();
        Bundle bundle = new Bundle();
        bundle.putString(Conversation.callbackData, result);
        BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestKey,
            bundle, AVIMOperation.CONVERSATION_QUERY);
        // verified: it's not need to update local cache?
      } else {
        LogUtil.log.w("not found requestKey: " + requestKey);
      }
    } else if (ConversationControlOp.QUERY_SHUTUP_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY.getCode()) {
        List<String> result = convCommand.getMList(); // result stored in m field.
        String[] resultMembers = new String[null == result? 0 : result.size()];
        if (null != result) {
          result.toArray(resultMembers);
        }
        Bundle bundle = new Bundle();
        bundle.putStringArray(Conversation.callbackData, resultMembers);
        BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestKey,
            bundle, AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY);
      } else {
        LogUtil.log.w("not found requestKey: " + requestKey);
      }
    } else {
      String conversationId = null;
      int requestId = (null != requestKey ? requestKey : CommandPacket.UNSUPPORTED_OPERATION);
      AVIMOperation originOperation = null;
      if ((operation.equals(ConversationControlOp.ADDED)
          || operation.equals(ConversationControlOp.REMOVED)
          || operation.equals(ConversationControlOp.UPDATED)
          || operation.equals(ConversationControlOp.MEMBER_COUNT_QUERY_RESULT)
          || operation.equals(ConversationControlOp.SHUTUP_ADDED)
          || operation.equals(ConversationControlOp.SHUTUP_REMOVED)
          || operation.equals(ConversationControlOp.MEMBER_UPDATED))
          && requestId != CommandPacket.UNSUPPORTED_OPERATION) {

        Operation op = session.conversationOperationCache.poll(requestId);
        originOperation = AVIMOperation.getAVIMOperation(op.operation);
        conversationId = op.conversationId;
      } else {
        if (operation.equals(ConversationControlOp.STARTED)) {
          session.conversationOperationCache.poll(requestId);
        }
        conversationId = convCommand.getCid();
      }
      int convType = Conversation.CONV_TYPE_NORMAL;
      if (convCommand.hasTempConv() && convCommand.getTempConv()) {
        convType = Conversation.CONV_TYPE_TEMPORARY;
      } else if (convCommand.hasTransient() && convCommand.getTransient()) {
        convType = Conversation.CONV_TYPE_TRANSIENT;
      }
      if (!AVUtils.isBlankString(conversationId)) {
        AVConversationHolder conversation = session.getConversationHolder(conversationId, convType);
        conversation.processConversationCommandFromServer(originOperation, operation, requestId, convCommand);
      }
    }
  }

  private SessionAckPacket genSessionAckPacket(String messageId) {
    SessionAckPacket sap = new SessionAckPacket();
    sap.setPeerId(session.getSelfPeerId());
    if (!AVUtils.isBlankString(messageId)) {
      sap.setMessageId(messageId);
    }

    return sap;
  }

  @Override
  public void onError(Integer requestKey, Messages.ErrorCommand errorCommand) {
    if (null != requestKey && requestKey != CommandPacket.UNSUPPORTED_OPERATION) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == AVIMOperation.CLIENT_OPEN.getCode()) {
        session.sessionOpened.set(false);
        session.sessionResume.set(false);
      }
      int code = errorCommand.getCode();
      int appCode = (errorCommand.hasAppCode() ? errorCommand.getAppCode() : 0);
      String reason = errorCommand.getReason();
      AVIMOperation operation = (null != op)? AVIMOperation.getAVIMOperation(op.operation): null;
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestKey,
          new AVIMException(code, appCode, reason), operation);
    }

    // 如果遇到signature failure的异常,清除缓存
    if (null == requestKey) {
      int code = errorCommand.getCode();
      // 如果遇到signature failure的异常,清除缓存
      if (CODE_SESSION_SIGNATURE_FAILURE == code) {
        AVSessionCacheHelper.getTagCacheInstance().removeSession(session.getSelfPeerId());
      } else if (CODE_SESSION_TOKEN_FAILURE == code) {
        // 如果遇到session token 失效或者过期的情况，先是清理缓存，然后再重新触发一次自动登录
        session.updateRealtimeSessionToken("", 0);
        this.onWebSocketOpen();
      }
    }
  }

  @Override
  public void onHistoryMessageQuery(Integer requestKey, Messages.LogsCommand command) {
    if (null != requestKey && requestKey != CommandPacket.UNSUPPORTED_OPERATION) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      int convType = Conversation.CONV_TYPE_NORMAL;
      if (command.getLogsCount() > 0) {
        Messages.LogItem item = command.getLogs(0);
        if (null != item && item.hasConvType()) {
          convType = item.getConvType();
        }
      }
      AVConversationHolder conversation = session.getConversationHolder(op.conversationId, convType);
      conversation.processMessages(requestKey, command.getLogsList());
    }
  }

  @Override
  public void onUnreadMessagesCommand(Messages.UnreadCommand unreadCommand) {
    session.updateLastNotifyTime(unreadCommand.getNotifTime());
    if (unreadCommand.getConvsCount() > 0) {

      List<Messages.UnreadTuple> unreadTupleList = unreadCommand.getConvsList();
      if (null != unreadTupleList) {
        for (Messages.UnreadTuple unreadTuple : unreadTupleList) {
          String msgId = unreadTuple.getMid();
          String msgContent = unreadTuple.getData();
          long ts = unreadTuple.getPatchTimestamp();
          long updateTS = unreadTuple.getPatchTimestamp();
          String conversationId = unreadTuple.getCid();
          boolean mentioned = unreadTuple.getMentioned();
          ByteString binaryMsg = unreadTuple.getBinaryMsg();
          String from = unreadTuple.getFrom();
          int convType = unreadTuple.hasConvType()? unreadTuple.getConvType() : Conversation.CONV_TYPE_NORMAL;

          AVIMMessage message = null;
          if (AVUtils.isBlankString(msgContent) && null != binaryMsg) {
            message = new AVIMBinaryMessage(conversationId, from, ts, -1);
            ((AVIMBinaryMessage)message).setBytes(binaryMsg.toByteArray());
          } else {
            message = new AVIMMessage(conversationId, from, ts, -1);
            message.setContent(msgContent);
          }
          message.setMessageId(msgId);
          message.setUpdateAt(updateTS);

          AVConversationHolder conversation = session.getConversationHolder(conversationId, convType);
          conversation.onUnreadMessagesEvent(message, unreadTuple.getUnread(), mentioned);
        }
      }
    }
  }

  @Override
  public void onMessagePatchCommand(boolean isModify, Integer requestKey, Messages.PatchCommand patchCommand) {
    updateLocalPatchTime(isModify, patchCommand);
    if (isModify) {
      if (patchCommand.getPatchesCount() > 0) {
        for (Messages.PatchItem patchItem : patchCommand.getPatchesList()) {
          AVIMMessage message = AVIMTypedMessage.getMessage(patchItem.getCid(), patchItem.getMid(), patchItem.getData(), patchItem.getFrom(), patchItem.getTimestamp(), 0, 0);
          message.setUpdateAt(patchItem.getPatchTimestamp());
          AVConversationHolder conversation = session.getConversationHolder(patchItem.getCid(), Conversation.CONV_TYPE_NORMAL);
          conversation.onMessageUpdateEvent(message, patchItem.getRecall());
        }
      }
    } else {
      Operation op = session.conversationOperationCache.poll(requestKey);
      AVIMOperation operation = AVIMOperation.getAVIMOperation(op.operation);
      Bundle bundle = new Bundle();
      bundle.putLong(Conversation.PARAM_MESSAGE_PATCH_TIME, patchCommand.getLastPatchTime());
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestKey, bundle, operation);
    }
  }

  private void updateLocalPatchTime(boolean isModify, Messages.PatchCommand patchCommand) {
    if (isModify) {
      long lastPatchTime = 0;
      for (Messages.PatchItem item : patchCommand.getPatchesList()) {
        if (item.getPatchTimestamp() > lastPatchTime) {
          lastPatchTime = item.getPatchTimestamp();
        }
      }
      session.updateLastPatchTime(lastPatchTime);
    } else {
      session.updateLastPatchTime(patchCommand.getLastPatchTime());
    }
  }
}
