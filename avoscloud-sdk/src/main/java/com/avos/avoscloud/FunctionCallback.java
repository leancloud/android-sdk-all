package com.avos.avoscloud;


/**
 * <p>
 * A FunctionCallback is used to run code after AVCloud.callFunction(java.lang.String,
 * java.util.Map) is used to run a Cloud Function in a background thread.
 * </p>
 * <p>
 * The easiest way to use a FunctionCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the cloud function is complete. The done
 * function will be run in the UI thread, while the fetch happens in a background thread. This
 * ensures that the UI does not freeze while the fetch happens.
 * </p>
 * <p>
 * For example, this sample code calls a cloud function "MyFunction" with params and calls a
 * different function depending on whether the function succeeded.
 * </p>
 * 
 * <pre>
 *
 *  AVCloud.callFunctionInBackground("MyFunction"new, params, FunctionCallback() {
 *         public void done(AVObject object, AVException e) {
 *             if (e == null) {
 *                 cloudFunctionSucceeded(object);
 *             } else {
 *                 cloudFunctionFailed();
 *             }
 *         }
 *     });
 *
 * </pre>
 */

public abstract class FunctionCallback<T> extends AVCallback<T> {
  /**
   * Override this function with the code you want to run after the cloud function is complete.
   * 
   * @param object The object that was returned by the cloud function.
   * @param e The exception raised by the cloud call, or null if it succeeded.
   */
  public abstract void done(T object, AVException e);

  protected final void internalDone0(T returnValue, AVException e) {
    done(returnValue, e);
  }
}
