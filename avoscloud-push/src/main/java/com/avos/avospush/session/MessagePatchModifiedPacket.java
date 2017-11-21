package com.avos.avospush.session;

import com.avos.avoscloud.Messages;
import com.avos.avoscloud.im.v2.AVIMClient;

/**
 * Created by wli on 2017/6/30.
 */
public class MessagePatchModifiedPacket extends PeerBasedCommandPacket {

  private long lastPatchTime;

  public MessagePatchModifiedPacket() {
    this.setCmd("patch");
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setOp(Messages.OpType.modified);
    builder.setPatchMessage(getPatchCommand());
    return builder;
  }

  protected Messages.PatchCommand getPatchCommand() {
    Messages.PatchCommand.Builder builder = Messages.PatchCommand.newBuilder();
    builder.setLastPatchTime(lastPatchTime);
    return builder.build();
  }

  public static MessagePatchModifiedPacket getPatchMessagePacket(String peerId, long lastPatchTime) {
    MessagePatchModifiedPacket messagePatchModifiedPacket = new MessagePatchModifiedPacket();
    if (AVIMClient.getClientsCount() > 1) {
      // peerId is necessary only when more than 1 client logined.
      messagePatchModifiedPacket.setPeerId(peerId);
    }
    messagePatchModifiedPacket.lastPatchTime = lastPatchTime;
    return messagePatchModifiedPacket;
  }
}
