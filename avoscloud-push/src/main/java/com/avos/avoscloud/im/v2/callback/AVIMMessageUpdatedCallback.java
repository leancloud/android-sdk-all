package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMMessage;

/**
 * Created by wli on 2017/6/29.
 */
public abstract class AVIMMessageUpdatedCallback extends AVCallback<AVIMMessage> {

  public abstract void done(AVIMMessage message, AVException e);

  @Override
  protected void internalDone0(AVIMMessage message, AVException avException) {
    done(message, avException);
  }
}