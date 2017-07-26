package com.avos.avoscloud;

public abstract class AVMobilePhoneVerifyCallback extends AVCallback<Void> {



  /**
   * <p>
   * AVMobilePhoneVerifyCallback 用来验证用户的手机号码
   * </p>
   * <p>
   * 　调用的范例如下
   * </p>
   * 
   * <pre>
   * AVUser.verifyMobilePhoneInBackgroud(&quot;123456&quot;,
   *     new AVMobilePhoneVerifyCallback() {
   *       public void done(AVException e) {
   *         if (e == null) {
   *           requestedSuccessfully();
   *         } else {
   *           requestDidNotSucceed();
   *         }
   *       }
   *     });
   * </pre>
   */
  @Override
  protected final void internalDone0(Void t, AVException avException) {
    this.done(avException);
  }

  public abstract void done(AVException e);
}
