package com.avos.avospush.session;

import com.avos.avoscloud.Messages;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.im.v2.AVIMMessageOption;

public class ConversationDirectMessagePacket extends PeerBasedCommandPacket {
  String conversationId;
  String message;
  String messageToken;
  AVIMMessageOption messageOption;

  public ConversationDirectMessagePacket() {
    this.setCmd("direct");
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  private void setMessageOption(AVIMMessageOption option) {
    this.messageOption = option;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setDirectMessage(getDirectCommand());
    if (null != messageOption) {
      if (null != messageOption.getPriority()) {
        builder.setPriority(messageOption.getPriority().getNumber());
      }
    }
    return builder;
  }

  protected Messages.DirectCommand getDirectCommand() {
    Messages.DirectCommand.Builder builder = Messages.DirectCommand.newBuilder();
    builder.setMsg(message);
    builder.setCid(conversationId);

    if (null != messageOption) {
      if (messageOption.isReceipt()) {
        builder.setR(true);
      }
      if (messageOption.isTransient()) {
        builder.setTransient(true);
      }

      String pushData = messageOption.getPushData();
      if (!AVUtils.isBlankString(pushData)) {
        builder.setPushData(pushData);
      }

      if (messageOption.isWill()) {
        builder.setWill(true);
      }
    }

    if (!AVUtils.isBlankString(messageToken)) {
      builder.setDt(messageToken);
    }
    return builder.build();
  }

  public static ConversationDirectMessagePacket getConversationMessagePacket(String peerId,
                                                                             String conversationId, String msg, AVIMMessageOption messageOption, int requestId) {
    ConversationDirectMessagePacket cdmp = new ConversationDirectMessagePacket();
    cdmp.setPeerId(peerId);
    cdmp.setConversationId(conversationId);
    cdmp.setRequestId(requestId);
    cdmp.setMessageOption(messageOption);
    cdmp.setMessage(msg);
    return cdmp;
  }

  public static ConversationDirectMessagePacket getConversationMessagePacket(String peerId,
                                                                             String conversationId, String msg, String messageToken, AVIMMessageOption option, int requestId) {
    ConversationDirectMessagePacket cdmp =
      getConversationMessagePacket(peerId, conversationId, msg, option, requestId);
    cdmp.messageToken = messageToken;
    return cdmp;
  }
}
