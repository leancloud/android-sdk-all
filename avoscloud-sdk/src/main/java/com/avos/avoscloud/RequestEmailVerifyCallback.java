package com.avos.avoscloud;

/**
 * <p>
 * RequestEmailVerifyCallback 用来验证用户的邮箱.
 * </p>
 * <p>
 * The easiest way to use a RequestEmailVerifyCallback is through an anonymous inner class. Override
 * the done function to specify what the callback should do after the request is complete. The done
 * function will be run in the UI thread, while the request happens in a background thread. This
 * ensures that the UI does not freeze while the request happens.
 * </p>
 * <p>
 * For example, this sample code requests an email verify for a user and calls a different function
 * depending on whether the request succeeded or not.
 * </p>
 * 
 * <pre>
 * AVUser.requestEmailVerfiyInBackground(&quot;forgetful@example.com&quot;,
 *     new RequestEmailVerifyCallback() {
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
public abstract class RequestEmailVerifyCallback extends AVCallback<Void> {
  /**
   * Override this function with the code you want to run after the request is complete.
   * 
   * @param e The exception raised by the save, or null if no account is associated with the email
   *          address.
   */
  public abstract void done(AVException e);

  @Override
  protected void internalDone0(Void t, AVException avException) {
    this.done(avException);
  }
}
