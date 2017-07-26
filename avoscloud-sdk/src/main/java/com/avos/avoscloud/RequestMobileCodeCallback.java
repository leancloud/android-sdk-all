package com.avos.avoscloud;

/**
 * <p>
 * RequestMobilePhoneVerify 用来验证用户的手机号码
 * </p>
 * <p>
 * 　调用的范例如下
 * </p>
 * 
 * <pre>
 * AVUser.requestMobilePhoneVerifyInBackgroud(&quot;12345678901&quot;,
 *     new RequestMobileCodeCallback() {
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
public abstract class RequestMobileCodeCallback extends AVCallback<Void> {

  public abstract void done(AVException e);

  @Override
  protected final void internalDone0(Void t, AVException avException) {
    this.done(avException);
  }
}
