package com.avos.avoscloud;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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
import com.avos.avoscloud.im.v2.AVIMBaseBroadcastReceiver;
import com.avos.avoscloud.im.v2.AVIMBinaryMessage;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMConversationEventHandler;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageManagerHelper;
import com.avos.avoscloud.im.v2.AVIMMessageOption;
import com.avos.avoscloud.im.v2.AVIMMessageQueryDirection;
import com.avos.avoscloud.im.v2.AVIMOptions;
import com.avos.avoscloud.im.v2.Conversation;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avoscloud.im.v2.callback.AVIMOperationFailure;
import com.avos.avoscloud.im.v2.conversation.AVIMConversationMemberInfo;
import com.avos.avoscloud.im.v2.conversation.ConversationMemberRole;
import com.avos.avospush.session.BlacklistCommandPacket;
import com.avos.avospush.session.CommandPacket;
import com.avos.avospush.session.ConversationControlPacket;
import com.avos.avospush.session.ConversationControlPacket.ConversationControlOp;
import com.avos.avospush.session.ConversationDirectMessagePacket;
import com.avos.avospush.session.ConversationMessageQueryPacket;
import com.avos.avospush.session.MessagePatchModifyPacket;
import com.avos.avospush.session.UnreadMessagesClearPacket;

@TargetApi(11)
class AVConversationHolder {
  AVSession session;
  String conversationId;
  int convType;

  // 服务器端为了兼容老版本，这里需要使用group的invite
  private static final String GROUP_INVITE = "invite";
  private static final String GROUP_KICK = "kick";
  private static final String BLOCK_MEMBER = "conversation-block-clients";
  private static final String UNBLOCK_MEMBER = "conversation-unblock-clients";

  public AVConversationHolder(String conversationId, AVSession session, int convType) {
    this.session = session;
    this.conversationId = conversationId;
    this.conversationGene = getConversationGeneString();
    this.convType = convType;
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
        final SignatureFactory signatureFactory = AVIMOptions.getGlobalOptions().getSignatureFactory();
        if (null != signatureFactory) {
          // 服务器端为了兼容老版本，这里需要使用group的invite
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), members, GROUP_INVITE);
        }
        return null;
      }
    };
    new SignatureTask(callback).commit(session.getSelfPeerId());
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
        final SignatureFactory signatureFactory = AVIMOptions.getGlobalOptions().getSignatureFactory();
        if (signatureFactory != null) {
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), members, GROUP_KICK);
        }
        return null;
      }
    };
    new SignatureTask(callback).commit(session.getSelfPeerId());
  }

  // mute member
  // notice: not use signature factory.
  public void muteMembers(final List<String> members, final int requestId){
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_MUTE_MEMBER, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MUTE_MEMBER.getCode(), session.getSelfPeerId(),
        conversationId, requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(
        session.getSelfPeerId(), conversationId, members,
        ConversationControlOp.ADD_SHUTUP, null, null, requestId));
  }

  // unmute member
  // notice: not use signature factory.
  public void unmuteMembers(final List<String> members, final int requestId){
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UNMUTE_MEMBER, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_UNMUTE_MEMBER.getCode(), session.getSelfPeerId(),
        conversationId, requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(
        session.getSelfPeerId(), conversationId, members,
        ConversationControlOp.REMOVE_SHUTUP, null, null, requestId));
  }

  // block member
  public void blockMembers(final List<String> members, final int requestId){
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_BLOCK_MEMBER, requestId)) {
      return;
    }
    SignatureCallback callback = new SignatureCallback() {

      @Override
      public void onSignatureReady(Signature sig, AVException e) {
        if (e == null) {
          session.conversationOperationCache.offer(Operation.getOperation(
              AVIMOperation.CONVERSATION_BLOCK_MEMBER.getCode(), session.getSelfPeerId(),
              conversationId, requestId));
          PushService.sendData(BlacklistCommandPacket.genBlacklistCommandPacket(
              session.getSelfPeerId(), conversationId,
              BlacklistCommandPacket.BlacklistCommandOp.BLOCK, members, sig, requestId));
        } else {
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
              requestId, e, AVIMOperation.CONVERSATION_BLOCK_MEMBER);
        }
      }

      @Override
      public Signature computeSignature() throws SignatureException {
        final SignatureFactory signatureFactory = AVIMOptions.getGlobalOptions().getSignatureFactory();
        if (signatureFactory != null) {
          return signatureFactory.createBlacklistSignature(session.getSelfPeerId(), conversationId, members, BLOCK_MEMBER);
        }
        return null;
      }
    };
    new SignatureTask(callback).commit(session.getSelfPeerId());
  }

  // unblock member.
  public void unblockMembers(final List<String> members, final int requestId){
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UNBLOCK_MEMBER, requestId)) {
      return;
    }

    SignatureCallback callback = new SignatureCallback() {

      @Override
      public void onSignatureReady(Signature sig, AVException e) {
        if (e == null) {
          session.conversationOperationCache.offer(Operation.getOperation(
              AVIMOperation.CONVERSATION_UNBLOCK_MEMBER.getCode(), session.getSelfPeerId(),
              conversationId, requestId));
          PushService.sendData(BlacklistCommandPacket.genBlacklistCommandPacket(
              session.getSelfPeerId(), conversationId,
              BlacklistCommandPacket.BlacklistCommandOp.UNBLOCK, members, sig, requestId));
        } else {
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId,
              requestId, e, AVIMOperation.CONVERSATION_UNBLOCK_MEMBER);
        }
      }

      @Override
      public Signature computeSignature() throws SignatureException {
        final SignatureFactory signatureFactory = AVIMOptions.getGlobalOptions().getSignatureFactory();
        if (signatureFactory != null) {
          return signatureFactory.createBlacklistSignature(session.getSelfPeerId(), conversationId, members, UNBLOCK_MEMBER);
        }
        return null;
      }
    };
    new SignatureTask(callback).commit(session.getSelfPeerId());
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
        final SignatureFactory signatureFactory = AVIMOptions.getGlobalOptions().getSignatureFactory();
        if (null != signatureFactory) {
          // 服务器端为了兼容老版本，这里需要使用group的invite
          return signatureFactory.createConversationSignature(conversationId,
              session.getSelfPeerId(), Arrays.asList(session.getSelfPeerId()),
              GROUP_INVITE);
        }
        return null;
      }
    };
    new SignatureTask(callback).commit(session.getSelfPeerId());
  }

  public void queryMutedMembers(int offset, int limit, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    ConversationControlPacket packet = ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
        conversationId, null, ConversationControlOp.QUERY_SHUTUP, null, null, requestId);
    packet.setQueryOffset(offset);
    packet.setQueryLimit(limit);
    PushService.sendData(packet);
  }

  public void queryBlockedMembers(int offset, int limit, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    BlacklistCommandPacket packet = BlacklistCommandPacket.genBlacklistCommandPacket(session.getSelfPeerId(),
        conversationId, BlacklistCommandPacket.BlacklistCommandOp.QUERY, offset, limit, requestId);
    PushService.sendData(packet);
  }

  public void updateInfo(Map<String, Object> attr, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_UPDATE, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_UPDATE.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    PushService.sendData(ConversationControlPacket.genConversationCommand(session.getSelfPeerId(),
        conversationId, null, ConversationControlOp.UPDATE, attr, null, requestId));
  }

  public void promoteMember(Map<String, Object> member, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_PROMOTE_MEMBER, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_PROMOTE_MEMBER.getCode(), session.getSelfPeerId(), conversationId,
        requestId));
    ConversationControlPacket ccp = ConversationControlPacket.genConversationMemberCommand(session.getSelfPeerId(),
        conversationId, ConversationControlOp.MEMBER_UPDATE, member, null, requestId);
    PushService.sendData(ccp);
  }

  public void sendMessage(AVIMMessage message, int requestId, AVIMMessageOption messageOption) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_SEND_MESSAGE, requestId)) {
      return;
    }
    byte[] binaryMessage = null;
    if (message instanceof AVIMBinaryMessage) {
      binaryMessage = ((AVIMBinaryMessage) message).getBytes();
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
        message.getContent(), binaryMessage, message.isMentionAll(), message.getMentionList(),
        AVIMMessageManagerHelper.getMessageToken(message),
        messageOption,
        requestId));
  }


  public void patchMessage(PushServiceParcel pushServiceParcel, AVIMOperation operation, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_RECALL_MESSAGE, requestId)) {
      return;
    }

    session.conversationOperationCache.offer(Operation.getOperation(
      operation.getCode(), session.getSelfPeerId(), conversationId, requestId));

    if (operation.equals(AVIMOperation.CONVERSATION_RECALL_MESSAGE)) {
      String messageId = pushServiceParcel.getRecallMessage().getMessageId();
      long timeStamp = pushServiceParcel.getRecallMessage().getTimestamp();
      PushService.sendData(MessagePatchModifyPacket.getMessagePatchPacketForRecall(session.getSelfPeerId(), conversationId, messageId, timeStamp, requestId));
    } else if (operation.equals(AVIMOperation.CONVERSATION_UPDATE_MESSAGE)){
      String messageId = pushServiceParcel.getOldMessage().getMessageId();
      long timeStamp = pushServiceParcel.getOldMessage().getTimestamp();

      AVIMMessage newMessage = pushServiceParcel.getNewMessage();
      String data = newMessage.getContent();
      boolean mentionAll = newMessage.isMentionAll();
      List<String> mentionList = newMessage.getMentionList();
      byte[] binaryData = null;
      if (newMessage instanceof AVIMBinaryMessage) {
        binaryData = ((AVIMBinaryMessage) newMessage).getBytes();
      }

      PushService.sendData(MessagePatchModifyPacket.getMessagePatchPacketForUpdate(session.getSelfPeerId(), conversationId,
          messageId, data, binaryData, mentionAll, mentionList, timeStamp, requestId));
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
        AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromNewToOld.getCode(), limit, 0, requestId);
  }

  public void queryHistoryMessages(String msgId, long timestamp, boolean sclosed,
                                   String toMsgId, long toTimestamp, boolean toclosed,
                                   int direct, int limit, int msgType, int requestId) {
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_MESSAGE_QUERY, requestId)) {
      return;
    }
    session.conversationOperationCache.offer(Operation.getOperation(
        AVIMOperation.CONVERSATION_MESSAGE_QUERY.getCode(), session.getSelfPeerId(),
        conversationId, requestId));
    PushService.sendData(ConversationMessageQueryPacket.getConversationMessageQueryPacket(
        session.getSelfPeerId(), conversationId, msgId, timestamp, sclosed, toMsgId, toTimestamp, toclosed,
        direct, limit, msgType, requestId));
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
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY, requestId)) {
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
    if (!checkSessionStatus(AVIMOperation.CONVERSATION_READ, requestId)) {
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

  public void processConversationCommandFromClient(AVIMOperation imop, Map<String, Object> params,
      int requestId) {
    List<String> members = null != params ? ((List<String>) params.get(Conversation.PARAM_CONVERSATION_MEMBER)) : null;
    switch (imop) {
      case CONVERSATION_JOIN:
        join(requestId);
        break;
      case CONVERSATION_ADD_MEMBER:
        addMembers(members, requestId);
        break;
      case CONVERSATION_RM_MEMBER:
        kickMembers(members, requestId);
        break;
      case CONVERSATION_QUIT:
        quit(requestId);
        break;
      case CONVERSATION_UPDATE:
        Map<String, Object> attr =
            (Map<String, Object>) params.get(Conversation.PARAM_CONVERSATION_ATTRIBUTE);
        this.updateInfo(attr, requestId);
        break;
      case CONVERSATION_MUTE:
        mute(requestId);
        break;
      case CONVERSATION_UNMUTE:
        unmute(requestId);
        break;
      case CONVERSATION_MEMBER_COUNT_QUERY:
        getMemberCount(requestId);
        break;
      case CONVERSATION_FETCH_RECEIPT_TIME:
        getReceiptTime(requestId);
        break;
      case CONVERSATION_READ:
        String messageId = "";
        if (null != params && params.containsKey(Conversation.PARAM_MESSAGE_QUERY_MSGID)) {
          messageId = (String)params.get(Conversation.PARAM_MESSAGE_QUERY_MSGID);
        }
        long messageTS = 0;
        if (null != params && params.containsKey(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)) {
          messageTS = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)).longValue();
        }
        read(messageId, messageTS, requestId);
        break;
      case CONVERSATION_PROMOTE_MEMBER:
        Map<String, Object> memberInfo = null != params? (Map<String, Object>) params.get(Conversation.PARAM_CONVERSATION_MEMBER_DETAILS) : null;
        if (null != memberInfo) {
          promoteMember(memberInfo, requestId);
        }
        break;
      case CONVERSATION_MUTE_MEMBER:
        muteMembers(members, requestId);
        break;
      case CONVERSATION_UNMUTE_MEMBER:
        unmuteMembers(members, requestId);
        break;
      case CONVERSATION_BLOCK_MEMBER:
        blockMembers(members, requestId);
        break;
      case CONVERSATION_UNBLOCK_MEMBER:
        unblockMembers(members, requestId);
        break;
      case CONVERSATION_MUTED_MEMBER_QUERY:
        int offset = (Integer) params.get(Conversation.QUERY_PARAM_OFFSET);
        int sizeLimit = (Integer) params.get(Conversation.QUERY_PARAM_LIMIT);
        queryMutedMembers(offset, sizeLimit, requestId);
        break;
      case CONVERSATION_BLOCKED_MEMBER_QUERY:
        int blockedOffset = (Integer) params.get(Conversation.QUERY_PARAM_OFFSET);
        int blockedSizeLimit = (Integer) params.get(Conversation.QUERY_PARAM_LIMIT);
        queryBlockedMembers(blockedOffset, blockedSizeLimit, requestId);
        break;
      case CONVERSATION_MESSAGE_QUERY:
        // timestamp = 0时，原来的 (Long) 会发生强制转型错误(Integer cannot cast to Long)
        String msgId = (String) params.get(Conversation.PARAM_MESSAGE_QUERY_MSGID);
        long ts = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP)).longValue();
        boolean sclosed = (Boolean) params.get(Conversation.PARAM_MESSAGE_QUERY_STARTCLOSED);
        String toMsgId = (String) params.get(Conversation.PARAM_MESSAGE_QUERY_TO_MSGID);
        long tts = ((Number) params.get(Conversation.PARAM_MESSAGE_QUERY_TO_TIMESTAMP)).longValue();
        boolean tclosed = (Boolean) params.get(Conversation.PARAM_MESSAGE_QUERY_TOCLOSED);
        int direct = (Integer) params.get(Conversation.PARAM_MESSAGE_QUERY_DIRECT);
        int limit = (Integer) params.get(Conversation.PARAM_MESSAGE_QUERY_LIMIT);
        int msgType = (Integer) params.get(Conversation.PARAM_MESSAGE_QUERY_TYPE);
        queryHistoryMessages(msgId, ts, sclosed, toMsgId, tts, tclosed, direct, limit, msgType, requestId);
        break;
      default:
          break;
    }
  }

  public void processConversationCommandFromServer(AVIMOperation imop, String operation, int requestId, Messages.ConvCommand convCommand) {
    if (ConversationControlOp.STARTED.equals(operation)) {
      // need convCommand to instantiate conversation object.
      onConversationCreated(requestId, convCommand);
    } else if (ConversationControlOp.JOINED.equals(operation)) {
      String invitedBy = convCommand.getInitBy();
      // 这里是我自己邀请了我自己，这个事件会被忽略。因为伴随这个消息一起来的还有added消息
      if (invitedBy.equals(session.getSelfPeerId())) {
        return;
      } else if (!invitedBy.equals(session.getSelfPeerId())) {
        // need convCommand to instantiate conversation object.
        onInvitedToConversation(invitedBy, convCommand);
      }
    } else if (ConversationControlOp.REMOVED.equals(operation)) {
      if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        if (null == imop) {
          LogUtil.log.e("IllegalState. operation is null, excepted is QUIT / KICK, originalOp=" + operation);
        } else if (imop.getCode() == AVIMOperation.CONVERSATION_QUIT.getCode()) {
          onQuit(requestId);
        } else if (imop.getCode() == AVIMOperation.CONVERSATION_RM_MEMBER.getCode()) {
          onKicked(requestId);
        }
      }
    } else if (ConversationControlOp.ADDED.equals(operation)) {
      // 这里我们回过头去看发送的命令是什么，如果是join，则是自己把自己加入到某个conversation。否则是邀请成功
      if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        if (null == imop) {
          LogUtil.log.e("IllegalState. operation is null, excepted is JOIN / INVITE, originalOp=" + operation);
        } else if (imop.getCode() == AVIMOperation.CONVERSATION_JOIN.getCode()) {
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
      if (null == imop) {
        LogUtil.log.e("IllegalState. operation is null, excepted is MUTE / UNMUTE / UPDATE, originalOp=" + operation);
      } else if (AVIMOperation.CONVERSATION_MUTE.getCode() == imop.getCode()) {
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
    } else if (ConversationControlOp.MEMBER_UPDATED.equals(operation)) {
      onMemberUpdated(requestId);
    } else if (ConversationControlOp.SHUTUP_ADDED.equals(operation)
        || ConversationControlOp.SHUTUP_REMOVED.equals(operation)) {
      // 禁言/取消禁言的 response
      if (null == imop) {
        LogUtil.log.e("IllegalState. operation is null, excepted is member_shutupped / member_unshutuped, originalOp=" + operation);
      } else {
        onResponse4MemberMute(imop, operation, requestId, convCommand);
      }
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
    } else if (ConversationControlOp.MEMBER_INFO_CHANGED.equals(operation)) {
      String changedBy = convCommand.getInitBy();
      Messages.ConvMemberInfo member = convCommand.getInfo();
      onMemberChanged(changedBy, member);
    } else if (ConversationControlOp.SHUTUPED.equals(operation)
        || ConversationControlOp.UNSHUTUPED.equals(operation)) {
      String operator = convCommand.getInitBy();
      if (null != operator && operator.equals(session.getSelfPeerId())) {
        return;
      } else {
        onSelfShutupedNotify(ConversationControlOp.SHUTUPED.equals(operation), operator, convCommand);
      }
    } else if (ConversationControlOp.MEMBER_SHUTPED.equals(operation)
        || ConversationControlOp.MEMBER_UNSHUTUPED.equals(operation)) {
      String operator = convCommand.getInitBy();
      if (null != operator && operator.equals(session.getSelfPeerId())) {
        return;
      } else {
        onMemberShutupedNotify(ConversationControlOp.MEMBER_SHUTPED.equals(operation), operator, convCommand);
      }
    } else if (ConversationControlOp.BLOCKED.equals(operation)
        || ConversationControlOp.UNBLOCKED.equals(operation)) {
      String operator = convCommand.getInitBy();
      if (null != operator && operator.equals(session.getSelfPeerId())) {
        return;
      } else {
        onSelfBlockedNotify(ConversationControlOp.BLOCKED.equals(operation), operator, convCommand);
      }
    } else if (ConversationControlOp.MEMBER_BLOCKED_NOTIFY.equals(operation)
        || ConversationControlOp.MEMBER_UNBLOCKED_NOTIFY.equals(operation)) {
      String operator = convCommand.getInitBy();
      if (null != operator && operator.equals(session.getSelfPeerId())) {
        return;
      } else {
        onMemberBlockedNotify(ConversationControlOp.MEMBER_BLOCKED_NOTIFY.equals(operation), operator, convCommand);
      }
    }
  }

  void onResponse4MemberBlock(AVIMOperation imop, String operation, int reqeustId, Messages.BlacklistCommand blacklistCommand) {
    if (null == blacklistCommand) {
      return;
    }
    List<String> allowedList = blacklistCommand.getAllowedPidsList();
    List<Messages.ErrorCommand> errorCommandList = blacklistCommand.getFailedPidsList();
    Bundle bundle = genBundleFromPartiallyResult(allowedList, errorCommandList);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), blacklistCommand.getSrcCid(), reqeustId, bundle, imop);
  }

  void onResponse4MemberMute(AVIMOperation imop, String operation, int requestId, Messages.ConvCommand convCommand) {
    if (null == convCommand) {
      return;
    }
    List<String> allowedList = convCommand.getAllowedPidsList();
    List<Messages.ErrorCommand> errorCommandList = convCommand.getFailedPidsList();
    Bundle bundle = genBundleFromPartiallyResult(allowedList, errorCommandList);
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        bundle, imop);
  }

  private Bundle genBundleFromPartiallyResult(List<String> allowedList, List<Messages.ErrorCommand> errorCommandList) {
    String[] allowedMembers = new String[null == allowedList? 0 : allowedList.size()];
    if (null != allowedList) {
      allowedList.toArray(allowedMembers);
    }
    int errorCommandSize = (null == errorCommandList)? 0 : errorCommandList.size();
    ArrayList<AVIMOperationFailure> failedList = new ArrayList<>(errorCommandSize);
    if (null != errorCommandList) {
      for (Messages.ErrorCommand cmd: errorCommandList) {
        AVIMOperationFailure failure = new AVIMOperationFailure();
        failure.setCode(cmd.getCode());
        failure.setMemberIds(cmd.getPidsList());
        failure.setReason(cmd.getReason());
        failedList.add(failure);
      }
    }
    Bundle bundle = new Bundle();
    bundle.putStringArray(Conversation.callbackConvMemberMuted_SUCC, allowedMembers);
    bundle.putParcelableArrayList(Conversation.callbackConvMemberMuted_FAIL, failedList);
    return bundle;
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
      boolean isBinaryMsg = item.hasBin() && item.getBin();

      AVIMMessage message = null;
      if (isBinaryMsg && null != data) {
        message = new AVIMBinaryMessage(this.conversationId, from, timestamp, ackAt, readAt);
        ((AVIMBinaryMessage)message).setBytes(AVUtils.base64Decode(data.toString()));
      } else if (data instanceof String || data instanceof JSON) {
        message = new AVIMMessage(this.conversationId, from, timestamp, ackAt, readAt);
        message.setContent(data.toString());
      } else {
        continue;
      }
      message.setMessageId(msgId);
      message.setMentionAll(mentionAll);
      message.setMentionList(mentionList);

      message = AVIMMessageManagerHelper.parseTypedMessage(message);
      messageList.add(message);
    }
    onHistoryMessageQuery(messageList, requestKey, lastDeliveredAt, lastReadAt);
  }

  // 以下的方法都是主动方法，需要带着requestId当成面包屑来找到回家的路
  void onConversationCreated(int requestId, Messages.ConvCommand convCommand) {
    String createdAt = convCommand.getCdate();
    String cid = convCommand.getCid();
    int tempTTL = convCommand.hasTempConvTTL()? convCommand.getTempConvTTL(): 0;

    // they are not necessary for create-callback(isTemp, isTransient), except for tempTTL.
    Bundle bundle = new Bundle();
    bundle.putString(Conversation.callbackCreatedAt, createdAt);
    bundle.putString(Conversation.callbackConversationKey, cid);
    bundle.putInt(Conversation.callbackTemporaryTTL, tempTTL);
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

  private void onMemberUpdated(int requestId) {
    BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), conversationId, requestId,
        AVIMOperation.CONVERSATION_PROMOTE_MEMBER);
  }

  private void onMemberChanged(String operator, Messages.ConvMemberInfo member) {
    AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      AVIMConversation conversation = client.getConversation(this.conversationId);
      String objectId = member.getInfoId();
      String roleStr = member.getRole();
      String peerId = member.getPid();
      AVIMConversationMemberInfo memberInfo = new AVIMConversationMemberInfo(objectId, this.conversationId,
          peerId, ConversationMemberRole.fromString(roleStr));
      handler.processEvent(Conversation.STATUS_ON_MEMBER_INFO_CHANGED, operator, memberInfo, conversation);
    }
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
  void onInvitedToConversation(final String invitedBy, Messages.ConvCommand convCommand) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = parseConversation(client, convCommand);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_JOINED, invitedBy, null, conversation);
        }
      });
    }
  }

  void onKickedFromConversation(final String invitedBy) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
    final AVIMConversation conversation = client.getConversation(this.conversationId);
    if (handler != null) {
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_KICKED_FROM_CONVERSATION, invitedBy, null,
              conversation);
        }
      });
    }
    session.removeConversation(conversationId);
    AVIMMessageManagerHelper.removeConversationCache(conversation);
  }

  void onSelfShutupedNotify(final boolean isMuted, final String operator, Messages.ConvCommand convCommand) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = parseConversation(client, convCommand);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          if (isMuted) {
            handler.processEvent(Conversation.STATUS_ON_MUTED, operator, null, conversation);
          } else {
            handler.processEvent(Conversation.STATUS_ON_UNMUTED, operator, null, conversation);
          }
        }
      });
    }
  }

  void onMemberShutupedNotify(final boolean isMuted, final String operator, Messages.ConvCommand convCommand) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    List<String> members = convCommand.getMList();
    if (handler != null && null != members) {
      final List<String> copyMembers = new ArrayList<>(members);
      copyMembers.remove(session.getSelfPeerId());
      if (copyMembers.size() < 1) {
        // ignore self member_shutuped notify, bcz server sends both shutuped and member_shutuped notification.
        LogUtil.log.d("Notification --- ignore shutuped/unshutuped notify bcz duplicated.");
      } else {
        AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
        final AVIMConversation conversation = parseConversation(client, convCommand);
        refreshConversationThenNotify(conversation, new SimpleCallback() {
          @Override
          public void done() {
            if (isMuted) {
              handler.processEvent(Conversation.STATUS_ON_MEMBER_MUTED, operator, copyMembers, conversation);
            } else {
              handler.processEvent(Conversation.STATUS_ON_MEMBER_UNMUTED, operator, copyMembers, conversation);
            }
          }
        });
      }
    }
  }

  void onSelfBlockedNotify(final boolean isBlocked, final String operator, Messages.ConvCommand convCommand) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (null == handler) {
      return;
    }
    AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
    final AVIMConversation conversation = parseConversation(client, convCommand);
    refreshConversationThenNotify(conversation, new SimpleCallback() {
      @Override
      public void done() {
        if (isBlocked) {
          handler.processEvent(Conversation.STATUS_ON_BLOCKED, operator, null, conversation);
        } else {
          handler.processEvent(Conversation.STATUS_ON_UNBLOCKED, operator, null, conversation);
        }
      }
    });
  }

  void onMemberBlockedNotify(final boolean isBlocked, final String operator, Messages.ConvCommand convCommand) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    final List<String> members = convCommand.getMList();
    if (handler != null && null != members) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = parseConversation(client, convCommand);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          if (isBlocked) {
            handler.processEvent(Conversation.STATUS_ON_MEMBER_BLOCKED, operator, members, conversation);
          } else {
            handler.processEvent(Conversation.STATUS_ON_MEMBER_UNBLOCKED, operator, members, conversation);
          }
        }
      });
    }
  }

  private AVIMConversation parseConversation(AVIMClient client, Messages.ConvCommand convCommand) {
    if (null == client || null == convCommand) {
      return null;
    }
    boolean isTemp = convCommand.hasTempConv()? convCommand.getTempConv() : false;
    boolean isTransient = convCommand.hasTransient()? convCommand.getTransient() : false;
    int tempTTL = convCommand.hasTempConvTTL()?convCommand.getTempConvTTL() : 0;

    AVIMConversation conversation = client.getConversation(this.conversationId, isTransient, isTemp);
    conversation.setTemporaryExpiredat(System.currentTimeMillis()/1000 + tempTTL);
    return conversation;
  }

  void onMembersJoined(final List<String> members, final String invitedBy) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_MEMBERS_JOINED, invitedBy, members, conversation);
        }
      });
    }
  }

  void onMembersLeft(final List<String> members, final String removedBy) {

    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);

      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_MEMBERS_LEFT, removedBy, members, conversation);
        }
      });
    }
  }

  void onMessageUpdateEvent(final AVIMMessage message, final boolean isRecall) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          if (isRecall) {
            handler.processEvent(Conversation.STATUS_ON_MESSAGE_RECALLED, message, null, conversation);
          } else {
            handler.processEvent(Conversation.STATUS_ON_MESSAGE_UPDATED, message, null, conversation);
          }
        }
      });
    }
  }

  void onMessage(final AVIMMessage message, final boolean hasMore, final boolean isTransient) {
    message.setMessageIOType(AVIMMessage.AVIMMessageIOType.AVIMMessageIOTypeIn);
    message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSent);

    final AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
    refreshConversationThenNotify(message, new SimpleCallback() {
      @Override
      public void done() {
        AVIMMessageManagerHelper.processMessage(message, convType, client, hasMore, isTransient);
      }
    });
  }

  void onMessageReceipt(final AVIMMessage message) {
    refreshConversationThenNotify(message, new SimpleCallback() {
      @Override
      public void done() {
        AVIMMessageManagerHelper.processMessageReceipt(message, AVIMClient.getInstance(session.getSelfPeerId()));
      }
    });
  }

  private void refreshConversationThenNotify(final AVIMMessage message, final SimpleCallback callback) {
    if (null == message || null == callback) {
      return;
    }
    final AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
    final AVIMConversation conversation = client.getConversation(message.getConversationId(), convType);
    refreshConversationThenNotify(conversation, callback);
  }

  private void refreshConversationThenNotify(final AVIMConversation conversation, final SimpleCallback callback) {
    if (null == conversation) {
      return;
    }
    if (!conversation.isShouldFetch()) {
      callback.done();
    } else {
      LogUtil.log.d("try to query conversation info for id=" + conversation.getConversationId());
      Map<String, Object> fetchParams = conversation.getFetchRequestParams();
      Map<String, Object> params = JSON.parseObject(JSON.toJSONString(fetchParams), Map.class);

      final int requestId = AVUtils.getNextIMRequestId();
      AVIMOperation operation = AVIMOperation.CONVERSATION_QUERY;
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(new AVIMBaseBroadcastReceiver(null) {
        @Override
        public void execute(Intent intent, Throwable ex) {
          if (null == ex) {
            conversation.processQueryResult(intent.getExtras().getSerializable(Conversation.callbackData));
            LogUtil.log.d("updated conversation info. id=" + conversation.getConversationId());
          }
          callback.done();
        }
      }, new IntentFilter(operation.getOperation() + requestId));
      session.conversationQuery(params, requestId);
    }
  }

  /**
   * process the unread messages event
   */
  void onUnreadMessagesEvent(AVIMMessage message, int unreadCount, boolean mentioned) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);
      if (conversation.getUnreadMessagesCount() != unreadCount) {
        final Pair<Integer, Boolean> unreadInfo = new Pair<>(unreadCount, mentioned);
        if (null != message) {
          message.setMessageIOType(AVIMMessage.AVIMMessageIOType.AVIMMessageIOTypeIn);
          message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSent);
          message = AVIMMessageManagerHelper.parseTypedMessage(message);
        }
        final AVIMMessage msgCopy = message;

        refreshConversationThenNotify(conversation, new SimpleCallback() {
          @Override
          public void done() {
            handler.processEvent(Conversation.STATUS_ON_UNREAD_EVENT, msgCopy, unreadInfo, conversation);
          }
        });
      }
    }
  }

  void onConversationReadAtEvent(final long readAt) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_MESSAGE_READ, readAt, null, conversation);
        }
      });
    }
  }

  void onConversationDeliveredAtEvent(final long deliveredAt) {
    final AVIMConversationEventHandler handler = AVIMMessageManagerHelper.getConversationEventHandler();
    if (handler != null) {
      AVIMClient client = AVIMClient.getInstance(session.getSelfPeerId());
      final AVIMConversation conversation = client.getConversation(this.conversationId);
      refreshConversationThenNotify(conversation, new SimpleCallback() {
        @Override
        public void done() {
          handler.processEvent(Conversation.STATUS_ON_MESSAGE_DELIVERED, deliveredAt, null, conversation);
        }
      });
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

  private static abstract class SimpleCallback {
    public abstract void done();
  }
}
