package com.avos.avoscloud.im.v2.callback;

import java.util.List;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;

/**
 * 从AVIMClient查询AVIMConversation时的回调抽象类
 */
public abstract class AVIMConversationQueryCallback
    extends AVCallback<List<AVIMConversation>> {

  public abstract void done(List<AVIMConversation> conversations, AVIMException e);

  @Override
  protected final void internalDone0(List<AVIMConversation> returnValue, AVException e) {
    done(returnValue, AVIMException.wrapperAVException(e));
  }

}
