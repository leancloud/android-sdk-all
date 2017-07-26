package com.avos.avospush.session;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.Messages;

public class ConversationAckPacket extends PeerBasedCommandPacket {

  public ConversationAckPacket() {
    super.setCmd("ack");
  }

  String conversationId;
  String messageId;
  Long largestTimeStamp;

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  /**
   * 设置需要 ack 的 timestamps
   * @param largestTimeStamp
   */
  public void setTimestamp(long largestTimeStamp) {
    this.largestTimeStamp = largestTimeStamp;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setAckMessage(getAckCommand());
    return builder;
  }

  protected Messages.AckCommand getAckCommand() {
    Messages.AckCommand.Builder builder = Messages.AckCommand.newBuilder();
    if (!TextUtils.isEmpty(messageId)) {
      builder.setMid(messageId);
    }
    if (null != largestTimeStamp) {
      builder.setTots(largestTimeStamp);
    }
    if (!TextUtils.isEmpty(conversationId)) {
      builder.setCid(conversationId);
    }
    return builder.build();
  }

  public static ConversationAckPacket getConversationAckPacket(String peerId,
      String conversationId, String messageId) {
    ConversationAckPacket cap = new ConversationAckPacket();
    cap.setPeerId(peerId);
    cap.setConversationId(conversationId);
    cap.setMessageId(messageId);
    return cap;
  }

  /**
   * 根据 timestamps 获取 ack 的 Packet
   * 用于离线消息仅返回 count 时通知 server 该 timestamps 以前的消息均已收到
   * 一般情况下只需要发送最大的 timestamp 即可，不需要发送全部
   * @param peerId
   * @param conversationId
   * @param timestamp
   * @return
   */
  public static ConversationAckPacket getConversationAckPacket(String peerId,
      String conversationId, Long timestamp) {
    ConversationAckPacket cap = new ConversationAckPacket();
    cap.setPeerId(peerId);
    cap.setConversationId(conversationId);
    cap.setTimestamp(timestamp);
    return cap;
  }
}
