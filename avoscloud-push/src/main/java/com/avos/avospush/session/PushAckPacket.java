package com.avos.avospush.session;

import com.avos.avoscloud.Messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nsun on 4/24/14.
 */
public class PushAckPacket extends CommandPacket {
  List<String> ids;

  public PushAckPacket() {
    this.setCmd("ack");
  }

  public void setMessageIds(List<String> ids) {
    this.ids = ids;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setAckMessage(getAckCommand());
    return builder;
  }

  protected Messages.AckCommand getAckCommand() {
    Messages.AckCommand.Builder builder = Messages.AckCommand.newBuilder();
    builder.addAllIds(ids);
    return builder.build();
  }
}
