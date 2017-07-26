package com.avos.avospush.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;

/**
 * Created by nsun on 5/13/14.
 */
public class SessionAckPacket extends PeerBasedCommandPacket {

  List<String> ids;

  public SessionAckPacket() {
    this.setCmd("ack");
  }

  public void setMessageId(String id) {
    ids = new ArrayList<String>(1);
    ids.add(id);
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setAckMessage(getAckCommand());
    return builder;
  }

  protected Messages.AckCommand getAckCommand() {
    Messages.AckCommand.Builder builder = Messages.AckCommand.newBuilder();
    if (!AVUtils.isEmptyList(ids)) {
      builder.addAllIds(ids);
    }
    return builder.build();
  }
}
