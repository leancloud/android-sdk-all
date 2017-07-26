package com.avos.avoscloud;

/**
 * Created with IntelliJ IDEA. User: dennis (xzhuang@avos.com) Date: 13-7-22 Time: 上午11:37
 */
public final class AVExceptionHolder {

  private static final ThreadLocal<AVException> local = new ThreadLocal<AVException>() {
    @Override
    protected AVException initialValue() {
      return null;
    }
  };

  public final static void add(AVException e) {
    local.set(e);
  }

  public final static boolean exists() {
    return local.get() != null;
  }

  public final static AVException remove() {
    AVException e = local.get();
    local.remove();
    return e;
  }
}
