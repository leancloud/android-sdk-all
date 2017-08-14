package com.avos.avoscloud;

/**
 * User: summer Date: 13-4-11 Time: PM3:46
 */
public abstract class AVCallback<T> {
  public void internalDone(final T t, final AVException avException) {
    if (mustRunOnUIThread() && !AVUtils.isMainThread()) {
      if (!AVOSCloud.handler.post(new Runnable() {
        @Override
        public void run() {
          internalDone0(t, avException);
        }
      })) {
        LogUtil.log.e("Post runnable to handler failed.");
      }
    } else {
      internalDone0(t, avException);
    }
  }

  protected boolean mustRunOnUIThread() {
    return true;
  }

  public void internalDone(final AVException avException) {
    this.internalDone(null, avException);
  }

  protected abstract void internalDone0(T t, AVException avException);
}
