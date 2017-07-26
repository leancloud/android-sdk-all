package com.avos.avospush.session;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;
import com.avos.avospush.session.LoginPacket;

/**
 * Created by wli on 2017/5/31.
 */

public class LiveQueryLoginPacket extends LoginPacket {

  public static final int SERVICE_LIVE_QUERY = 1;
  public static final int SERVICE_PUSH = 0;
  private String subscribeId;

  public void setSubscribeId(String subscribeId) {
    this.subscribeId = subscribeId;
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    if (!AVUtils.isBlankString(subscribeId)) {
      builder.setInstallationId(subscribeId);
      builder.setService(SERVICE_LIVE_QUERY);
    }
    return builder;
  }
}
