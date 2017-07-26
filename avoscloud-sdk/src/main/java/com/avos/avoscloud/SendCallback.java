package com.avos.avoscloud;

/**
 * <p>
 * A SendCallback is used to run code after sending a AVPush in a background thread.
 * </p>
 * <p>
 * The easiest way to use a SendCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the send is complete. The done function
 * will be run in the UI thread, while the send happens in a background thread. This ensures that
 * the UI does not freeze while the send happens.
 * </p>
 * <p>
 * For example, this sample code sends the message "Hello world" on the "hello" channel and logs
 * whether the send succeeded.
 * </p>
 * 
 * <pre>
 * AVPush push = new AVPush();
 * push.setChannel(&quot;hello&quot;);
 * push.setMessage(&quot;Hello world!&quot;);
 * push.sendInBackground(new SendCallback() {
 *   public void done(AVException e) {
 *     if (e == null) {
 *       Log.d(&quot;push&quot;, &quot;success!&quot;);
 *     } else {
 *       Log.d(&quot;push&quot;, &quot;failure&quot;);
 *     }
 *   }
 * });
 * </pre>
 */

public abstract class SendCallback extends com.avos.avoscloud.AVCallback<java.lang.Void> {
  public SendCallback() { /* compiled code */}

  /**
   * Override this function with the code you want to run after the send is complete.
   * 
   * @param e The exception raised by the send, or null if it succeeded.
   */
  public abstract void done(com.avos.avoscloud.AVException e);

  protected final void internalDone0(java.lang.Void returnValue, com.avos.avoscloud.AVException e) {
    this.done(e);
  }
}
