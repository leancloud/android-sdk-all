package com.avos.avospush.session;

import java.util.List;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;

/**
 * Created by wli on 2017/6/28.
 */

public class MessagePatchModifyPacket extends PeerBasedCommandPacket {

  public MessagePatchModifyPacket() {
    setCmd("patch");
  }

  private String conversationId;
  private String messageId;
  private long timestamp;
  private String messageData;
  private boolean isRecall;
  private boolean mentionAll;
  private List<String> mentionList;

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setOp(Messages.OpType.modify);
    builder.setPatchMessage(getPatchCommand());
    return builder;
  }

  private Messages.PatchCommand getPatchCommand() {
    Messages.PatchCommand.Builder builder = Messages.PatchCommand.newBuilder();
    Messages.PatchItem.Builder patchItemBuilder = Messages.PatchItem.newBuilder();
    if (timestamp > 0) {
      patchItemBuilder.setTimestamp(timestamp);
    }
    if (!AVUtils.isBlankString(messageId)) {
      patchItemBuilder.setMid(messageId);
    }
    if (!AVUtils.isBlankString(conversationId)) {
      patchItemBuilder.setCid(conversationId);
    }
    if (!AVUtils.isBlankString(messageData)) {
      patchItemBuilder.setData(messageData);
    }
    patchItemBuilder.setMentionAll(mentionAll);
    if (null != mentionList) {
      patchItemBuilder.addAllMentionPids(mentionList);
    }
    patchItemBuilder.setRecall(isRecall);
    builder.addPatches(patchItemBuilder.build());
    return builder.build();
  }

  public static MessagePatchModifyPacket getMessagePatchPacketForUpdate(String peerId, String conversationId,
                                                                        String messageId, String data, boolean mentionAll, List<String> mentionList,
                                                                        long timestamp, int requestId) {
    MessagePatchModifyPacket packet = new MessagePatchModifyPacket();
    packet.conversationId = conversationId;
    packet.messageId = messageId;
    packet.timestamp = timestamp;
    packet.messageData = data;
    packet.isRecall = false;
    packet.mentionAll = mentionAll;
    packet.mentionList = mentionList;
    packet.setRequestId(requestId);
    packet.setPeerId(peerId);
    return packet;
  }

  public static MessagePatchModifyPacket getMessagePatchPacketForRecall(String peerId, String conversationId, String messageId, long timestamp, int requestId) {
    MessagePatchModifyPacket packet = new MessagePatchModifyPacket();
    packet.conversationId = conversationId;
    packet.messageId = messageId;
    packet.timestamp = timestamp;
    packet.isRecall = true;
    packet.setRequestId(requestId);
    packet.setPeerId(peerId);
    return packet;
  }
}
