package com.avos.avoscloud;

/**
 * <p>
 * CloudQueryCallback 是通过AVQuery直接调用cloudQuery查询之后被调用的回调函数
 * </p>
 */

public abstract class CloudQueryCallback<T extends AVCloudQueryResult>
    extends AVCallback<AVCloudQueryResult> {
  public abstract void done(AVCloudQueryResult result, AVException avException);

  @Override
  protected final void internalDone0(AVCloudQueryResult returnValue, AVException e) {
    done(returnValue, e);
  }
}
