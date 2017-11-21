package com.avos.avospush.session;

import com.avos.avoscloud.Messages;
import com.avos.avoscloud.im.v2.AVIMClient;

/**
 * Created by nsun on 4/24/14.
 */
public class PeerBasedCommandPacket extends CommandPacket {

  private String peerId;

  public String getPeerId() {
    return peerId;
  }

  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    if (null != getPeerId()) {
      builder.setPeerId(getPeerId());
    }
    return builder;
  }
}
