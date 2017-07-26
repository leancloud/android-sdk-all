package com.avos.avoscloud;

/**
 * Created by wli on 2017/5/31.
 */

public abstract class AVLiveQuerySubscribeCallback extends AVCallback<Void> {

  public abstract void done(AVException e);

  @Override
  protected void internalDone0(Void aVoid, AVException avException) {
    done(avException);
  }
}
