package com.avos.avospush.session;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;

/**
 * Created by wli on 15/10/15.
 * 清除离线消息通知
 */
public class UnreadMessagesClearPacket extends PeerBasedCommandPacket {

  String conversationId;
  String messageId;
  long messageTS;

  public UnreadMessagesClearPacket() {
    this.setCmd("read");
  }

  String getConversationId() {
    return conversationId;
  }

  void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  void setMessageTS(long timestamp) {
    this.messageTS = timestamp;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setReadMessage(getReadCommand());
    return builder;
  }

  protected Messages.ReadCommand getReadCommand() {
    Messages.ReadCommand.Builder builder = Messages.ReadCommand.newBuilder();
    Messages.ReadTuple.Builder readTupleBuilder = builder.addConvsBuilder();
    if (!AVUtils.isBlankString(messageId)) {
      readTupleBuilder.setMid(messageId);
    }
    if (messageTS > 0) {
      readTupleBuilder.setTimestamp(messageTS);
    }
    readTupleBuilder.setCid(conversationId);
    return builder.build();
  }

  public static UnreadMessagesClearPacket getUnreadClearPacket(String peerId,
      String conversationId, String messageId, long timeStamp, int requestId) {
    UnreadMessagesClearPacket packet = new UnreadMessagesClearPacket();
    packet.setPeerId(peerId);
    packet.setConversationId(conversationId);
    packet.setRequestId(requestId);
    packet.setMessageId(messageId);
    packet.setMessageTS(timeStamp);
    return packet;
  }
}
