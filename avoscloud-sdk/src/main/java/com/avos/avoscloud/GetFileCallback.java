package com.avos.avoscloud;

/**
 * 
 * <p>
 * A GetFileCallback is used to run code after a AVFile.parseFileWithObjectId is used to fetch a
 * AVFile by object id in a background thread.
 * </p>
 * <p>
 * The easiest way to use a GetFileCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the fetch is complete. The done function
 * will be run in the UI thread, while the fetch happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * <p>
 * For example, this sample code fetches an AVFile with object id 'myId':
 * </p>
 * 
 * <pre>
 * AVFile.parseFileWithObjectId(myId, new GetFileCallback&lt;AVFile&gt;() {
 *   public void done(AVFile file, AVException e) {
 *     if (e == null) {
 *       fileWasRetrievedSuccessfully(object);
 *     } else {
 *       fileRetrievalFailed();
 *     }
 *   }
 * });
 * </pre>
 */

public abstract class GetFileCallback<T extends AVFile> extends AVCallback<T> {
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
