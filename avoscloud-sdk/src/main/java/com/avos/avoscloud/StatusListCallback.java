package com.avos.avoscloud;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 1/3/14 Time: 4:25 PM To change this template use
 * File | Settings | File Templates.
 */
public abstract class StatusListCallback extends AVCallback<java.util.List<AVStatus>> {
  /**
   * Override this function with the code you want to run after the fetch is complete.
   * 
   * @param statusObjects The objects matching the query, or null if it failed.
   * @param avException The exception raised by the find, or null if it succeeded.
   */
  public abstract void done(List<AVStatus> statusObjects, AVException avException);

  protected final void internalDone0(List<AVStatus> returnValue, AVException e) {
    done(returnValue, e);
  }
}
