package com.avos.avospush.session;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;
import com.avos.avoscloud.Messages.GenericCommand;

/**
 * Created by nsun on 4/24/14.
 */
public abstract class CommandPacket {

  private String cmd;

  /**
   * 只有在 login 时才需要 appId，其他时候不需要
   */
  private String appId;
  private int requestId = UNSUPPORTED_OPERATION;
  private String installationId;
  public static final int UNSUPPORTED_OPERATION = -65537;

  public int getRequestId() {
    return requestId;
  }

  public void setRequestId(int id) {
    this.requestId = id;
  }

  public String getCmd() {
    return cmd;
  }

  public void setCmd(String cmd) {
    this.cmd = cmd;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getInstallationId() {
    return installationId;
  }

  public void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  protected GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = GenericCommand.newBuilder();
    if (!AVUtils.isBlankContent(appId)) {
      builder.setAppId(appId);
    }

    builder.setCmd(Messages.CommandType.valueOf(getCmd()));
    if (getInstallationId() != null) {
      builder.setInstallationId(getInstallationId());
    }
    if (requestId > CommandPacket.UNSUPPORTED_OPERATION) {
      builder.setI(requestId);
    }
    return builder;
  }

  public GenericCommand getGenericCommand() {
    GenericCommand.Builder builder = getGenericCommandBuilder();
    return builder.build();
  }

  public int getLength() {
    return getGenericCommandBuilder().build().getSerializedSize();
  }
}
