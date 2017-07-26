package com.avos.avoscloud;

/**
 * <p>
 * A SaveCallback is used to run code after saving a AVObject in a background thread.
 * </p>
 * <p>
 * The easiest way to use a SaveCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the save is complete. The done function
 * will be run in the UI thread, while the save happens in a background thread. This ensures that
 * the UI does not freeze while the save happens.
 * </p>
 * <p>
 * For example, this sample code saves the object myObject and calls a different function depending
 * on whether the save succeeded or not.
 * </p>
 * 
 * <pre>
 * myObject.saveInBackground(new SaveCallback() {
 *   public void done(AVException e) {
 *     if (e == null) {
 *       myObjectSavedSuccessfully();
 *     } else {
 *       myObjectSaveDidNotSucceed();
 *     }
 *   }
 * });
 * </pre>
 */
public abstract class SaveCallback extends AVCallback<Void> {

  /**
   * Override this function with the code you want to run after the save is complete.
   * 
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(AVException e);

  protected final void internalDone0(java.lang.Void returnValue, AVException e) {
    done(e);
  }
}
