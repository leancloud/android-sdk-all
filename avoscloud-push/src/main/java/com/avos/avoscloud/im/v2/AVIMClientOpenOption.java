package com.avos.avoscloud.im.v2;

/**
 * Created by wli on 2017/2/7.
 */

public class AVIMClientOpenOption {

  /**
   * 是否强制单点登陆，默认为强制
   */
  private boolean isForceSingleLogin = true;
  private boolean isReconnect = false;

  public AVIMClientOpenOption() {
  }

  /**
   * 判断是否恢复重连
   * @return
   */
  public boolean isReconnect() {
    return this.isReconnect;
  }

  /**
   * 设置恢复重连标记
   *
   * @param reconnect
   */
  public void setReconnect(boolean reconnect) {
    this.isReconnect = reconnect;
  }

  /**
   * 是否强制单点登录
   *
   * @return
   * @since v4.6.3
   */
  @Deprecated
  public boolean isForceSingleLogin() {
    return isForceSingleLogin;
  }

  /**
   * 设置单点登录（已废除，请调用 #setReconnect 方法代替）
   *
   * @param forceSingleLogin
   * @since v4.6.3
   */
  @Deprecated
  public void setForceSingleLogin(boolean forceSingleLogin) {
    isForceSingleLogin = forceSingleLogin;
    isReconnect = !forceSingleLogin;
  }
}
