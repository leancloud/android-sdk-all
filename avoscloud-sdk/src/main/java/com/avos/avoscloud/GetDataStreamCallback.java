package com.avos.avoscloud;

import java.io.InputStream;

/**
 * <p>
 * A GetDataStreamCallback is used to run code after a AVFile fetches its data on a background thread.
 * </p>
 * <p>
 * The easiest way to use a GetDataStreamCallback is through an anonymous inner class. Override the done
 * function to specify what the callback should do after the fetch is complete. The done function
 * will be run in the UI thread, while the fetch happens in a background thread. This ensures that
 * the UI does not freeze while the fetch happens.
 * </p>
 * 
 * <pre>
 * file.getDataStreamInBackground(new GetDataStreamCallback() {
 *   public void done(InputStream data, AVException e)
 *   {
 *     // ...
 *   }
 * });
 * </pre>
 */
public abstract class GetDataStreamCallback extends AVCallback<InputStream> {
  public abstract void done(InputStream data, AVException e);

  protected final void internalDone0(InputStream returnValue, AVException e) {
    done(returnValue, e);
  }
}
