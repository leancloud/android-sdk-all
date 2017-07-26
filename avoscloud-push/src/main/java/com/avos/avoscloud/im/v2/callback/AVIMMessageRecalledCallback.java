package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.messages.AVIMRecalledMessage;

/**
 * Created by wli on 2017/6/28.
 */

public abstract class AVIMMessageRecalledCallback extends AVCallback<AVIMRecalledMessage> {

  public abstract void done(AVIMRecalledMessage recalledMessage, AVException e);

  @Override
  protected void internalDone0(AVIMRecalledMessage avimRecalledMessage, AVException avException) {
    done(avimRecalledMessage, avException);
  }
}
