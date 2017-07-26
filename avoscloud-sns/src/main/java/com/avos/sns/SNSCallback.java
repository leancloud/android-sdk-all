package com.avos.sns;

/**
 * User: summer Date: 13-4-11 Time: PM3:46
 */
public abstract class SNSCallback {


  /**
   * Override this function with the code you want to run after the save is complete.
   * 
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(SNSBase base, SNSException e);

  final void internalDone(SNSBase base, SNSException e) {
    done(base, e);
  }
}
