package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.conversation.AVIMConversationMemberInfo;

import java.util.List;

/**
 * Created by fengjunwen on 2017/12/17.
 */

public abstract class AVIMConversationMemberQueryCallback extends AVCallback<List<AVIMConversationMemberInfo>> {
  public abstract void done(List<AVIMConversationMemberInfo> memberInfoList, AVIMException e);

  @Override
  protected final void internalDone0(List<AVIMConversationMemberInfo> returnValue, AVException e) {
    done(returnValue, AVIMException.wrapperAVException(e));
  }
}
