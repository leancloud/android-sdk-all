package com.avos.avoscloud;

/**
 * Created by lbt05 on 4/22/15.
 */
abstract class GenericRetryCallback extends GenericObjectCallback {
  GenericObjectCallback callback;

  public GenericRetryCallback(GenericObjectCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onSuccess(String content, AVException e) {
    if (callback != null) {
      callback.onSuccess(content, e);
    }
  }

  @Override
  public void onFailure(Throwable error, String content) {
    if (callback != null) {
      callback.onFailure(error, content);
    }
  }

  @Override
  public boolean isRequestStatisticNeed() {
    return callback.isRequestStatisticNeed();
  }
}
