package com.avos.avoscloud;

/**
 * <p>
 * A RefreshCallback is used to run code after refresh is used to update a AVObject in a background
 * thread.
 * </p>
 * <p>
 * The easiest way to use a RefreshCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the refresh is complete. The done function
 * will be run in the UI thread, while the refresh happens in a background thread. This ensures that
 * the UI does not freeze while the refresh happens.
 * </p>
 * <p>
 * For example, this sample code refreshes an object of class "MyClass" and id myId. It calls a
 * different function depending on whether the refresh succeeded or not.
 * </p>
 * 
 * <pre>
 * object.refreshInBackground(new RefreshCallback() {
 *   public void done(AVObject object, AVException e) {
 *     if (e == null) {
 *       objectWasRefreshedSuccessfully(object);
 *     } else {
 *       objectRefreshFailed();
 *     }
 *   }
 * });
 * </pre>
 */
public abstract class RefreshCallback<T extends AVObject> extends AVCallback<T> {
  /**
   * Override this function with the code you want to run after the save is complete.
   * 
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(T object, AVException e);

  protected final void internalDone0(T returnValue, AVException e) {
    done(returnValue, e);
  }
}
