package com.avos.avospush.session;

import com.avos.avoscloud.Messages;

/**
 * Created by nsun on 4/24/14.
 */
public class LoginPacket extends CommandPacket {

  public LoginPacket() {
    this.setCmd("login");
  }

  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setCmd(Messages.CommandType.login);
    return builder;
  }
}
