package com.avos.avoscloud.im.v2;

/**
 * Created by wli on 2017/2/7.
 */

public class AVIMClientOpenOption {

  /**
   * 是否强制单点登陆，默认为非强制
   */
  private boolean isForceSingleLogin = false;

  public AVIMClientOpenOption() {

  }

  public boolean isForceSingleLogin() {
    return isForceSingleLogin;
  }

  public void setForceSingleLogin(boolean forceSingleLogin) {
    isForceSingleLogin = forceSingleLogin;
  }
}
