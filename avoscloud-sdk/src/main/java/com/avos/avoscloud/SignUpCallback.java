package com.avos.avoscloud;

/**
 * <p>
 * A SignUpCallback is used to run code after signing up a AVUser in a background thread.
 * </p>
 * <p>
 * The easiest way to use a SignUpCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the save is complete. The done function
 * will be run in the UI thread, while the signup happens in a background thread. This ensures that
 * the UI does not freeze while the signup happens.
 * </p>
 * <p>
 * For example, this sample code signs up the object myUser and calls a different function depending
 * on whether the signup succeeded or not.
 * </p>
 * 
 * <pre>
 * myUser.signUpInBackground(new SignUpCallback() {
 *   public void done(AVException e) {
 *     if (e == null) {
 *       myUserSignedUpSuccessfully();
 *     } else {
 *       myUserSignUpDidNotSucceed();
 *     }
 *   }
 * });
 * </pre>
 */
public abstract class SignUpCallback extends AVCallback<Void> {

  /**
   * Override this function with the code you want to run after the signUp is complete.
   * 
   * @param e The exception raised by the signUp, or null if it succeeded.
   */
  public abstract void done(AVException e);

  protected final void internalDone0(Void t, AVException avException) {
    this.done(avException);
  }

}
