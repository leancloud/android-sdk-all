package com.avos.avoscloud;

/**
 * <p>
 * A ProgressCallback is used to get progress of an operation.
 * </p>
 * <p>
 * The easiest way to use a ProgressCallback is through an anonymous inner class.
 * </p>
 */
public abstract class ProgressCallback extends AVCallback<Integer> {
  public abstract void done(Integer percentDone);

  /**
   * Override this function with your desired callback.
   */
  protected final void internalDone0(Integer returnValue, AVException e) {
    done(returnValue);
  }
}
