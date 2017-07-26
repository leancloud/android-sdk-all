package com.avos.avoscloud;


public abstract class GenericObjectCallback {
  public void onSuccess(String content, AVException e) {}

  public void onFailure(int statusCode, Throwable error, String content) {
    if (isRetryNeeded(statusCode, error)) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("retry this request");
      }
      retry(error, content);
    } else {
      onFailure(error, content);
    }
  }

  public void onFailure(Throwable error, String content) {

  }


  public void onGroupRequestFinished(int left, int total, AVObject object) {

  }

  public boolean isRetryNeeded(int statusCode, Throwable error) {
    return false;
  }

  public void retry(Throwable error, String content) {

  }

  public boolean isRequestStatisticNeed() {
    return true;
  }
}
