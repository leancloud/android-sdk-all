package com.avos.avoscloud;

/**
 * <p>
 * A GetDataCallback is used to run code after a AVFile fetches its data on a background thread.
 * </p>
 * <p>
 * The easiest way to use a GetDataCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the fetch is complete. The done function
 * will be run in the UI thread, while the fetch happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * 
 * <pre>
 * file.getDataInBackground(new GetDataCallback() {
 *   public void done(byte[] data, AVException e)
 *   {
 *     // ...
 *   }
 * });
 * </pre>
 */
public abstract class GetDataCallback extends AVCallback<byte[]> {
  public abstract void done(byte[] data, AVException e);

  protected final void internalDone0(byte[] returnValue, AVException e) {
    done(returnValue, e);
  }
}
