package com.avos.avoscloud;

/**
 * 
 * <p>
 * A GetCallback is used to run code after a AVQuery is used to fetch a AVObject in a background
 * thread.
 * </p>
 * <p>
 * The easiest way to use a GetCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the fetch is complete. The done function
 * will be run in the UI thread, while the fetch happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * <p>
 * For example, this sample code fetches an object of class "MyClass" and id myId. It calls a
 * different function depending on whether the fetch succeeded or not.
 * </p>
 * 
 * <pre>
 * AVQuery&lt;AVObject&gt; query = AVQuery.getQuery(&quot;MyClass&quot;);
 * query.getInBackground(myId, new GetCallback&lt;AVObject&gt;() {
 *   public void done(AVObject object, AVException e) {
 *     if (e == null) {
 *       objectWasRetrievedSuccessfully(object);
 *     } else {
 *       objectRetrievalFailed();
 *     }
 *   }
 * });
 * </pre>
 */

public abstract class GetCallback<T extends AVObject> extends AVCallback<T> {
  /**
   * Override this function with the code you want to run after the fetch is complete.
   *
   * @param object The object that was retrieved, or null if it did not succeed.
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(T object, AVException e);

  protected final void internalDone0(T returnValue, AVException e) {
    done(returnValue, e);
  }
}
