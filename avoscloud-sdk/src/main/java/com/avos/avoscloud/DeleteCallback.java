package com.avos.avoscloud;

/**
 * <p>
 * A DeleteCallback is used to run code after saving a AVObject in a background thread.
 * </p>
 * <p>
 * The easiest way to use a DeleteCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the delete is complete. The done function
 * will be run in the UI thread, while the delete happens in a background thread. This ensures that
 * the UI does not freeze while the delete happens.
 * </p>
 * <p>
 * For example, this sample code deletes the object myObject and calls a different function
 * depending on whether the save succeeded or not.
 * </p>
 * 
 * <pre>
 * myObject.deleteInBackground(new DeleteCallback() {
 *   public void done(AVException e) {
 *     if (e == null) {
 *       myObjectWasDeletedSuccessfully();
 *     } else {
 *       myObjectDeleteDidNotSucceed();
 *     }
 *   }
 * });
 * </pre>
 */
public abstract class DeleteCallback extends AVCallback<Void> {
  /**
   * Override this function with the code you want to run after the delete is complete.
   * 
   * @param e The exception raised by the delete, or null if it succeeded.
   */
  public abstract void done(AVException e);

  protected final void internalDone0(Void returnValue, AVException e) {
    done(e);
  }
}
