package com.avos.avoscloud;

/**
 * <p>
 * A LogInCallback is used to run code after logging in a user.
 * </p>
 * <p>
 * The easiest way to use a LogInCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the login is complete. The done function
 * will be run in the UI thread, while the login happens in a background thread. This ensures that
 * the UI does not freeze while the save happens.
 * </p>
 * <p>
 * For example, this sample code logs in a user and calls a different function depending on whether
 * the login succeeded or not.
 * </p>
 * 
 * <pre>
 * AVUser.logInInBackground(&quot;username&quot;, &quot;password&quot;, new LogInCallback() {
 *   public void done(AVUser user, AVException e) {
 *     if (e == null &amp;&amp; user != null) {
 *       loginSuccessful();
 *     } else if (user == null) {
 *       usernameOrPasswordIsInvalid();
 *     } else {
 *       somethingWentWrong();
 *     }
 *   }
 * });
 * </pre>
 */
public abstract class LogInCallback<T extends AVUser> extends AVCallback<T> {
  /**
   * Override this function with the code you want to run after the save is complete.
   * 
   * @param user The user that logged in, if the username and password is valid.
   * @param e The exception raised by the login.
   */
  public abstract void done(T user, AVException e);

  protected final void internalDone0(T returnValue, AVException e) {
    done(returnValue, e);
  }
}
