package com.avos.avoscloud;

/**
 * 这个方法是在针对一个已经登陆用户修改密码请求
 * 
 */
public abstract class UpdatePasswordCallback extends AVCallback<Void> {
  /**
   * 请用您需要在修改密码完成以后的逻辑重载本方法
   * 
   * @param e 修改密码请求可能产生的异常
   * 
   */
  public abstract void done(AVException e);

  @Override
  protected final void internalDone0(Void t, AVException avException) {
    this.done(avException);
  }
}
