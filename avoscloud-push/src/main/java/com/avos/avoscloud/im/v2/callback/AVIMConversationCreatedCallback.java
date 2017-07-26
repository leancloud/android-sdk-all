package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;

public abstract class AVIMConversationCreatedCallback extends AVCallback<AVIMConversation> {
  public abstract void done(AVIMConversation conversation, AVIMException e);

  @Override
  protected final void internalDone0(AVIMConversation returnValue, AVException e) {
    done(returnValue, AVIMException.wrapperAVException(e));
  }
}
