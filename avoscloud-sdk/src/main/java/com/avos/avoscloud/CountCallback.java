package com.avos.avoscloud;

/**
 * <p>
 * A CountCallback is used to run code after a AVQuery is used to count objects matching a query in
 * a background thread.
 * </p>
 * 
 * <p>
 * The easiest way to use a GetCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the count is complete. The done function
 * will be run in the UI thread, while the count happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * 
 * <p>
 * For example, this sample code counts objects of class "MyClass". It calls a different function
 * depending on whether the count succeeded or not.
 * </p>
 * 
 * <pre>
 * AVQuery&lt;AVObject&gt; query = AVQuery.getQuery(&quot;MyClass&quot;);
 * query.countInBackground(new CountCallback() {
 *   public void done(int count, AVException e) {
 *     if (e == null) {
 *       objectsWereCountedSuccessfully(count);
 *     } else {
 *       objectCountingFailed();
 *     }
 *   }
 * });
 * </pre>
 */

public abstract class CountCallback extends AVCallback<Integer> {

  /**
   * Override this function with the code you want to run after the count is complete.
   * 
   * @param count The number of objects matching the query, or -1 if it failed.
   * @param e The exception raised by the count, or null if it succeeded.
   */
  public abstract void done(int count, AVException e);

  /**
   * 
   * @param returnValue
   * @param e
   */
  protected final void internalDone0(Integer returnValue, AVException e) {
    done(returnValue == null ? -1 : returnValue, e);
  }
}
