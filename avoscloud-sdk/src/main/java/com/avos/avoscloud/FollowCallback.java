package com.avos.avoscloud;



/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 12/25/13 Time: 10:05 AM To change this template
 * use File | Settings | File Templates.
 */

public abstract class FollowCallback<T extends AVObject> extends AVCallback<T> {
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
