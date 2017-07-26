package com.avos.avospush.session;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;

/**
 * Created by lbt05 on 2/3/15.
 */
public class ConversationMessageQueryPacket extends PeerBasedCommandPacket {
  public ConversationMessageQueryPacket() {
    setCmd("logs");
  }

  String msgId;
  int limit;
  long timestamp;
  String conversationId;
  int requestId;
  String toMsgId;
  long toTimestamp;

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getMsgId() {
    return msgId;
  }

  public void setMsgId(String msgId) {
    this.msgId = msgId;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getToMsgId() {
    return toMsgId;
  }

  public void setToMsgId(String toMsgId) {
    this.toMsgId = toMsgId;
  }

  public long getToTimestamp() {
    return toTimestamp;
  }

  public void setToTimestamp(long toTimestamp) {
    this.toTimestamp = toTimestamp;
  }

  @Override
  public int getRequestId() {
    return requestId;
  }

  @Override
  public void setRequestId(int requestId) {
    this.requestId = requestId;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setLogsMessage(getLogsCommand());
    builder.setI(requestId);
    return builder;
  }

  protected Messages.LogsCommand getLogsCommand() {
    Messages.LogsCommand.Builder builder = Messages.LogsCommand.newBuilder();
    builder.setCid(conversationId);
    builder.setLimit(limit);
    if (!AVUtils.isBlankString(msgId)) {
      builder.setMid(msgId);
    }

    if (timestamp > 0) {
      builder.setT(timestamp);
    }

    if (!AVUtils.isBlankString(toMsgId)) {
      builder.setTmid(toMsgId);
    }

    if (toTimestamp > 0) {
      builder.setTt(toTimestamp);
    }
    return builder.build();
  }

  public static ConversationMessageQueryPacket getConversationMessageQueryPacket(String peerId,
      String conversationId, String msgId,
      long timestamp, int limit, String toMsgId, long toTimestamp, int requestId) {
    ConversationMessageQueryPacket cqp = new ConversationMessageQueryPacket();
    cqp.setPeerId(peerId);

    cqp.setConversationId(conversationId);
    cqp.setMsgId(msgId);
    cqp.setLimit(limit);
    cqp.setTimestamp(timestamp);
    cqp.setRequestId(requestId);

    cqp.setToMsgId(toMsgId);
    cqp.setToTimestamp(toTimestamp);

    return cqp;
  }
}
