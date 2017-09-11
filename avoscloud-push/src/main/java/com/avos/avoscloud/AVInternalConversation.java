package com.avos.avoscloud;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVIMOperationQueue.Operation;
import com.avos.avoscloud.PendingMessageCache.Message;
import com.avos.avoscloud.SignatureFactory.SignatureException;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMConversationEventHandler;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageManagerHelper;
import com.avos.avoscloud.im.v2.AVIMMessageOption;
import com.avos.avoscloud.im.v2.AVIMMessageQueryDirection;
import com.avos.avoscloud.im.v2.Conversation;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avospush.session.CommandPacket;
import com.avos.avospush.session.ConversationControlPacket;
import com.avos.avospush.session.ConversationControlPacket.ConversationControlOp;
import com.avos.avospush.session.ConversationDirectMessagePacket;
import com.avos.avospush.session.ConversationMessageQueryPacket;
import com.avos.avospush.session.MessagePatchModifyPacket;
import com.avos.avospush.session.UnreadMessagesClearPacket;

@TargetApi(11)
class AVInternalConversation {
  AVSession session;
  String conversationId;

  // 服务器端为了兼容老版本，这里需要使用group的invite
  private static final String GROUP_INVITE = "invite";
  private static final String GROUP_KICK = "kick";

  public AVInternalConversation(String conversationId, AVSession session) {
    this.session = session;
    this.conversationId = conversationId;
    this.conversationGene = getConversationGeneString();
  }

  public void addMembers(final List<String> members, final int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_ADD_MEMBER, requestId)) {
      return;
    }
    SignatureCallback callback = new SignatureCallback() {

      @Override
      public void onSignatureReady(Signature sig, AVException e) {
        if (e == null) {
          session.conversationOperationCache.offer(Operation.getOperation(
            AVIMOperation.CONVERSATION_ADD_MEMBER.getCode(), session.getSelfPeerId(),
            conversationId, requestId));
          PushService.sendData(ConversationControlPacket.genConversationCommand(
            session.getSelfPeerId(), conversationId, members,
            ConversationControlOp.ADD, null, sig, requestId));
        } else {
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
              requestId, e, AVIMOperation.CONVERSATION_ADD_MEMBER);
        }
      }

      @Override
      public Signature computeSignature() throws SignatureException {
        final SignatureFactory signatureFactory = AVSession.getSignatureFactory();
        if (null != signatureFactory) {
          // 服务器端为了兼容老版本，这里需要使用group的invite
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), members, GROUP_INVITE);
        }
        return null;
      }
    };
    new SignatureTask(callback).execute(session.getSelfPeerId());
  }

  public void kickMembers(final List<String> members, final int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_RM_MEMBER, requestId)) {
      return;
    }
    SignatureCallback callback = new SignatureCallback() {

      @Override
      public void onSignatureReady(Signature sig, AVException e) {
        if (e == null) {
          session.conversationOperationCache.offer(Operation.getOperation(
            AVIMOperation.CONVERSATION_RM_MEMBER.getCode(), session.getSelfPeerId(),
            conversationId, requestId));
          PushService.sendData(ConversationControlPacket.genConversationCommand(
            session.getSelfPeerId(), conversationId, members,
            ConversationControlOp.REMOVE, null, sig, requestId));
        } else {
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
              requestId, e, AVIMOperation.CONVERSATION_RM_MEMBER);
        }
      }

      @Override
      public Signature computeSignature() throws SignatureException {
        // 服务器端为兼容老版本，签名使用kick的action
        final SignatureFactory signatureFactory = AVSession.getSignatureFactory();
        if (signatureFactory != null) {
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), members, GROUP_KICK);
        }
        return null;
      }
    };
    new SignatureTask(callback).execute(session.getSelfPeerId());
  }

  public void join(final int requestId) {

    SignatureCallback callback = new SignatureCallback() {

      @Override
      public void onSignatureReady(Signature sig, AVException e) {
        if (e == null) {
          session.conversationOperationCache.offer(Operation.getOperation(
            AVIMOperation.CONVERSATION_JOIN.getCode(), session.getSelfPeerId(), conversationId,
            requestId));
          PushService.sendData(ConversationControlPacket.genConversationCommand(
            session.getSelfPeerId(), conversationId, Arrays.asList(session.getSelfPeerId()),
            ConversationControlOp.ADD, null, sig, requestId));
        } else {
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
              requestId, e, AVIMOperation.CONVERSATION_JOIN);
        }
      }

      @Override
      public Signature computeSignature() throws SignatureException {
        final SignatureFactory signatureFactory = AVSession.getSignatureFactory();
        if (null != signatureFactory) {
          // 服务器端为了兼容老版本，这里需要使用group的invite
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), Arrays.asList(session.getSelfPeerId()),
              GROUP_INVITE);
        }
        return null;
      }
    };
    new SignatureTask(callback).execute(session.getSelfPeerId());
  }

  public void updateInfo(Map<String, Object> attr, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_SEND_MESSAGE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_UPDATE.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
        conversationId, null, ConversationControlOp.UPDATE, attr, null, requestId));
  }

  public void sendMessage(AVIMMessage message, int requestId, AVIMMessageOption messageOption) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_SEND_MESSAGE, requestId)) {
      return;
    }

    if (!messageOption.isTransient()) {
      session.storeMessage((Message.getMessage(message.getContent(),
        String.valueOf(requestId), messageOption.isReceipt(), conversationId)), requestId);
    } else {
      // 暂态消息服务器也不会返回 ack，所以这里不需要等待网络返回，直接返回成功即可
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
          requestId, AVIMOperation.CONVERSATION_SEND_MESSAGE);
    }

    PushService
      .sendData(ConversationDirectMessagePacket.getConversationMessagePacket(
        session.getSelfPeerId(),
        conversationId,
        message.getContent(), message.isMentionAll(), message.getMentionList(),
        AVIMMessageManagerHelper.getMessageToken(message),
        messageOption,
        requestId));
  }


  public void patchMessage(PushServiceParcel pushServiceParcel, AVIMOperation operation, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_SEND_MESSAGE, requestId)) {
      return;
    }

    session.conversationOperationCache.offer(Operation.getOperation(
      operation.getCode(), session.getSelfPeerId(), conversationId, requestId));

    if (operation.equals(AVIMOperation.CONVERSATION_RECALL_MESSAGE)) {
      String messageId = pushServiceParcel.getRecallMessage().getMessageId();
      long timeStamp = pushServiceParcel.getRecallMessage().getTimestamp();
      PushService.sendData(MessagePatchModifyPacket.getMessagePatchPacketForRecall(session.getSelfPeerId(), conversationId, messageId, timeStamp, requestId));
    } else {
      String messageId = pushServiceParcel.getOldMessage().getMessageId();
      long timeStamp = pushServiceParcel.getOldMessage().getTimestamp();
      String data = pushServiceParcel.getNewMessage().getContent();
      PushService.sendData(MessagePatchModifyPacket.getMessagePatchPacketForUpdate(session.getSelfPeerId(), conversationId, messageId, data, timeStamp, requestId));
    }
  }

  public void quit(final int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_QUIT, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
      AVIMOperation.CONVERSATION_QUIT.getCode(), session.getSelfPeerId(), conversationId,
      requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(
      session.getSelfPeerId(), conversationId, Arrays.asList(session.getSelfPeerId()),
      ConversationControlOp.REMOVE, null, null, requestId));
  }

  public void queryHistoryMessages(String msgId, long timestamp, int limit, String toMsgId,
      long toTimestamp, int requestId) {
    queryHistoryMessages(msgId, timestamp, false, toMsgId, toTimestamp, false,
        AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromNewToOld.getCode(), limit, requestId);
  }

  public void queryHistoryMessages(String msgId, long timestamp, boolean sclosed,
                                   String toMsgId, long toTimestamp, boolean toclosed,
                                   int direct, int limit, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_QUIT, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MESSAGE_QUERY.getCode(), session.getSelfPeerId(),
        conversationId, requestId));
    PushService.sendData(ConversationMessageQueryPacket.getConversationMessageQueryPacket(
        session.getSelfPeerId(), conversationId, msgId, timestamp, sclosed, toMsgId, toTimestamp, toclosed,
        direct, limit, requestId));
  }

  public void mute(int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_MUTE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MUTE.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
      conversationId, null, ConversationControlOp.MUTE, null, null, requestId));
  }

  public void unmute(int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UNMUTE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_UNMUTE.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
        conversationId, null, ConversationControlOp.UNMUTE, null, null, requestId));
  }

  public void getMemberCount(int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UNMUTE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY.getCode(), session.getSelfPeerId(),
        conversationId,
        requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
        conversationId, null, ConversationControlOp.COUNT, null, null, requestId));
  }

  private void getReceiptTime(int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
      AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME.getCode(), session.getSelfPeerId(), conversationId,
      requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
      conversationId, null, ConversationControlOp.MAX_READ, null, null, requestId));
  }

  private void read(String msgId, long timestamp, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UNMUTE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
      AVIMOperation.CONVERSATION_READ.getCode(), session.getSelfPeerId(), conversationId, requestId));

    UnreadMessagesClearPacket packet =
      UnreadMessagesClearPacket.getUnreadClearPacket(session.getSelfPeerId(), conversationId, msgId, timestamp, requestId);
    PushService.sendData(packet);

    // 因为没有返回值，所以在发送 command 后直接置 unreadCount 为 0 并发送事件
    onUnreadMessagesEvent(null, 0, false);
  }

  private boolean checkSessionStatus(AVIMOperation operation, int requestId) {
    if (session.sessionPaused.get()) {
      RuntimeException se = new RuntimeException("Connection Lost");
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
          requestId, se, operation);
      return false;
    } else {
      return true;
    }
  }

  public void processConversationCommandFromClient(int operationCode, Map<String, Object> params,
      int requestId) {
    if (operationCode == AVIMOperation.CONVERSATION_JOIN.getCode()) {
      this.join(requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_ADD_MEMBER.getCode()) {
      List<String> members = (List<String>) params.get(Conversation.PARAM_CONVERSATION_MEMBER);
      this.addMembers(members, requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_RM_MEMBER.getCode()) {
      List<String> members = (List<String>) params.get(Conversation.PARAM_CONVERSATION_MEMBER);
      this.kickMembers(members, requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_QUIT.getCode()) {
      this.quit(requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_UPDATE.getCode()) {
      Map<String, Object> attr =
          (Map<String, Object>) params.get(Conversation.PARAM_CONVERSATION_ATTRIBUTE);
      this.updateInfo(attr, requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_MESSAGE_QUERY.getCode()) {
      // timestamp = 0时，原来的 (Long) 会发生强制转型错误(Integer cannot cast to Long)
      String msgId = (String) params.get(Conversation.PARAM_MESSAGE_QUERY_MSGID);
      long ts = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)).longValue();
      boolean sclosed = (Boolean) params.get(Conversation.PARAM_MESSAGE_QUERY_STARTCLOSED);
      String toMsgId = (String) params.get(Conversation.PARAM_MESSAGE_QUERY_TO_MSGID);
      long tts = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TO_TIMESTAMP)).longValue();
      boolean tclosed = (Boolean) params.get(Conversation.PARAM_MESSAGE_QUERY_TOCLOSED);
      int direct = (Integer) params.get(Conversation.PARAM_MESSAGE_QUERY_DIRECT);
      int limit = (Integer) params.get(Conversation.PARAM_MESSAGE_QUERY_LIMIT);
      this.queryHistoryMessages(msgId, ts, sclosed, toMsgId, tts, tclosed, direct, limit, requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_MUTE.getCode()) {
      mute(requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_UNMUTE.getCode()) {
      unmute(requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY.getCode()) {
      getMemberCount(requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_READ.getCode()) {
      String messageId = "";
      if (null != params && params.containsKey(Conversation.PARAM_MESSAGE_QUERY_MSGID)) {
        messageId = (String)params.get(Conversation.PARAM_MESSAGE_QUERY_MSGID);
      }
      long messageTS = 0;
      if (null != params && params.containsKey(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)) {
        messageTS = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)).longValue();
      }
      read(messageId, messageTS, requestId);
    } else if (operationCode == AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME.getCode()) {
      getReceiptTime(requestId);
    }
  }

  public void processConversationCommandFromServer(AVIMOperation imop, String operation, int requestId, Messages.ConvCommand convCommand) {
    if (ConversationControlOp.STARTED.equals(operation)) {
      onConversationCreated(requestId, convCommand.getCdate());
    } else if (ConversationControlOp.JOINED.equals(operation)) {
      String invitedBy = convCommand.getInitBy();
      // 这里是我自己邀请了我自己，这个事件会被忽略。因为伴随这个消息一起来的还有added消息
      if (invitedBy.equals(session.getSelfPeerId())) {
        return;
      } else if (!invitedBy.equals(session.getSelfPeerId())) {
        onInvitedToConversation(invitedBy);
      }
    } else if (ConversationControlOp.REMOVED.equals(operation)) {
      if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        if (imop.getCode() == AVIMOperation.CONVERSATION_QUIT.getCode()) {
          onQuit(requestId);
        } else if (imop.getCode() == AVIMOperation.CONVERSATION_RM_MEMBER.getCode()) {
          onKicked(requestId);
        }
      }
    } else if (ConversationControlOp.ADDED.equals(operation)) {
      // 这里我们回过头去看发送的命令是什么如果是join，则是自己把自己加入到某个conversation。否则是邀请成功
      if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        if (imop.getCode() == AVIMOperation.CONVERSATION_JOIN.getCode()) {
          onJoined(requestId);
        } else if (imop.getCode() == AVIMOperation.CONVERSATION_ADD_MEMBER.getCode()) {
          onInvited(requestId);
        }
      }
    } else if (ConversationControlOp.LEFT.equals(operation)) {
      String invitedBy = convCommand.getInitBy();
      if (invitedBy != null && !invitedBy.equals(session.getSelfPeerId())) {
        this.onKickedFromConversation(invitedBy);
      }
    } else if (ConversationControlOp.UPDATED.equals(operation)) {
      if (AVIMOperation.CONVERSATION_MUTE.getCode() == imop.getCode()) {
        onMuted(requestId);
      } else if (AVIMOperation.CONVERSATION_UNMUTE.getCode() == imop.getCode()) {
        onUnmuted(requestId);
      } else if (AVIMOperation.CONVERSATION_UPDATE.getCode() == imop.getCode()) {
        onInfoUpdated(requestId, convCommand.getUdate());
      }
    } else if (ConversationControlOp.MEMBER_COUNT_QUERY_RESULT.equals(operation)) {
      int memberCount = convCommand.getCount();
      onMemberCount(memberCount, requestId);
    } else if (ConversationControlOp.MAX_READ.equals(operation)) {
      long receiptTime = convCommand.getMaxAckTimestamp();
      long readTime = convCommand.getMaxReadTimestamp();
      onTimesReceipt(requestId,  receiptTime, readTime);
    }
    // 下面都是被动
    else if (ConversationControlOp.MEMBER_JOINED.equals(operation)) {
      String invitedBy = convCommand.getInitBy();
      List<String> joinedMembers = convCommand.getMList();
      onMembersJoined(joinedMembers, invitedBy);
    } else if (ConversationControlOp.MEMBER_LEFTED.equals(operation)) {
      String removedBy = convCommand.getInitBy();
      List<String> leftMembers = convCommand.getMList();
      onMembersLeft(leftMembers, removedBy);
    }
  }

  public void processMessages(Integer requestKey, List<Messages.LogItem> logItems) {
    ArrayList<AVIMMessage> messageList = new ArrayList<AVIMMessage>();

    //这里记录的是对方 ack 及 read 的时间，而非自己 
    long lastDeliveredAt = -1;
    long lastReadAt = -1;
      for (Messages.LogItem item : logItems) {
        long ackAt = item.hasAckAt() ? -1 : item.getAckAt();
        long readAt = item.hasReadAt() ?-1 : item.getReadAt();
        if (lastDeliveredAt < ackAt) {
          lastDeliveredAt = ackAt;
        }
        if (lastReadAt < readAt) {
          lastReadAt = readAt;
        }

        String from = item.getFrom();
        Object data = item.getData();
        long timestamp = item.getTimestamp();
        String msgId = item.getMsgId();
        boolean mentionAll = item.hasMentionAll()? item.getMentionAll():false;
        List<String> mentionList = item.getMentionPidsList();

        AVIMMessage message = new AVIMMessage(this.conversationId, from, timestamp, ackAt, readAt);
        message.setMessageId(msgId);
        message.setMentionAll(mentionAll);
        message.setMentionList(mentionList);

        if (data instanceof String || data instanceof JSON) {
          message.setContent(data.toString());
        } else {
          continue;
        }
        message = AVIMMessageManagerHelper.parseTypedMessage(message);
        messageList.add(message);
      }
      onHistoryMessageQuery(messageList, requestKey, lastDeliveredAt, lastReadAt);
  }

  // 以下的方法都是主动方法，需要带着requestId当成面包屑来找到回家的路
  void onConversationCreated(int requestId, String createdAt) {
    Bundle bundle = new Bundle();
    bundle.putString(Conversation.callbackCreatedAt, createdAt);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId, bundle,
        AVIMOperation.CONVERSATION_CREATION);
  }

  void onJoined(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_JOIN);
  }

  void onInvited(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_ADD_MEMBER);
  }

  void onKicked(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_RM_MEMBER);
  }

  void onQuit(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_QUIT);
  }

  private void onInfoUpdated(int requestId, String updatedAt) {
    Bundle bundle = new Bundle();
    bundle.putString(Conversation.callbackUpdatedAt, updatedAt);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId, bundle,
        AVIMOperation.CONVERSATION_UPDATE);
  }

  void onMuted(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_MUTE);
  }

  void onUnmuted(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_UNMUTE);
  }

  void onMemberCount(int count, int requestId) {
    Bundle bundle = new Bundle();
    bundle.putInt(Conversation.callbackMemberCount, count);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        bundle, AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY);
  }

  void onMessageSent(int requestId, String msgId, long timestamp) {
    Bundle bundle = new Bundle();
    bundle.putLong(Conversation.callbackMessageTimeStamp, timestamp);
    bundle.putString(Conversation.callbackMessageId, msgId);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        bundle, AVIMOperation.CONVERSATION_SEND_MESSAGE);
  }

  void onHistoryMessageQuery(ArrayList<AVIMMessage> messages, int requestId, long deliveredAt, long readAt) {
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Conversation.callbackHistoryMessages, messages);
    bundle.putLong(Conversation.callbackDeliveredAt, deliveredAt);
    bundle.putLong(Conversation.callbackReadAt, readAt);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
      bundle, AVIMOperation.CONVERSATION_MESSAGE_QUERY);
    session.sendUnreadMessagesAck(messages, conversationId);
  }

  void onTimesReceipt(int requestId, long deliveredAt, long readAt) {
    Bundle bundle = new Bundle();
    bundle.putLong(Conversation.callbackReadAt, readAt);
    bundle.putLong(Conversation.callbackDeliveredAt, deliveredAt);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
      bundle, AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME);
  }

  // 以下的所有内容都是从服务器端来得，客户端是被动接受的
  void onInvitedToConversation(String invitedBy) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      handler.processEvent(Conversation.STATUS_ON_JOINED, invitedBy, null, conversation);
    }
  }

  void onKickedFromConversation(String invitedBy) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
    AVIMConversation conversation = client.getConversation(this.conversationId);
    if (handler != null) {
      handler.processEvent(Conversation.STATUS_ON_KICKED_FROM_CONVERSATION, invitedBy, null,
          conversation);
    }
    session.removeConversation(conversationId);
    AVIMMessageManagerHelper.removeConversationCache(conversation);
  }


  void onMembersJoined(List<String> members, String invitedBy) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      handler.processEvent(Conversation.STATUS_ON_MEMBERS_JOINED, invitedBy, members, conversation);
    }
  }

  void onMembersLeft(List<String> members, String removedBy) {

    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);

      handler.processEvent(Conversation.STATUS_ON_MEMBERS_LEFT, removedBy, members, conversation);
    }
  }

  void onMessageUpdateEvent(AVIMMessage message, boolean isRecall) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      if (isRecall) {
        handler.processEvent(Conversation.STATUS_ON_MESSAGE_RECALLED, message, null, conversation);
      } else {
        handler.processEvent(Conversation.STATUS_ON_MESSAGE_UPDATED, message, null, conversation);
      }
    }
  }

  void onMessage(AVIMMessage message, boolean hasMore, boolean isTransient) {
    message.setMessageIOType(AVIMMessage.AVIMMessageIOType.AVIMMessageIOTypeIn);
    message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSent);

    AVIMMessageManagerHelper.processMessage(message,
        AVIMClient.getInstance(session.getSelfPeerId()), hasMore, isTransient);
  }

  void onMessageReceipt(AVIMMessage message) {
    AVIMMessageManagerHelper.processMessageReceipt(message,
        AVIMClient.getInstance(session.getSelfPeerId()));
  }

  /**
   * process the unread messages event
   */
  void onUnreadMessagesEvent(AVIMMessage message, int unreadCount, boolean mentioned) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      if (conversation.getUnreadMessagesCount() != unreadCount) {
        Pair<Integer, Boolean> unreadInfo = new Pair<>(unreadCount, mentioned);
        handler.processEvent(Conversation.STATUS_ON_UNREAD_EVENT, message, unreadInfo, conversation);
      }
    }
  }

  void onConversationReadAtEvent(long readAt) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      handler.processEvent(Conversation.STATUS_ON_MESSAGE_READ, readAt, null, conversation);
    }
  }

  void onConversationDeliveredAtEvent(long deliveredAt) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      handler.processEvent(Conversation.STATUS_ON_MESSAGE_DELIVERED, deliveredAt, null, conversation);
    }
  }

  private String conversationGene = null;

  private String getConversationGeneString() {
    if (AVUtils.isBlankString(conversationGene)) {
      HashMap<String, String> conversationGeneMap = new HashMap<String, String>();
      conversationGeneMap.put(Conversation.INTENT_KEY_CLIENT, session.getSelfPeerId());
      conversationGeneMap.put(Conversation.INTENT_KEY_CONVERSATION, this.conversationId);
      conversationGene = JSON.toJSONString(conversationGeneMap);
    }
    return conversationGene;
  }
}
