package com.avos.avoscloud.im.v2.callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.Conversation;

/**
 * Created by fengjunwen on 2017/12/20.
 */

public abstract class AVIMConversationPartiallySucceededCallback extends AVCallback<Map<String, Object>> {
  public abstract void done(AVIMException e, List<String> successfulClientIds, List<AVIMOperationFailure> failures);

  @Override
  protected final void internalDone0(Map<String, Object> returnValue, AVException e) {
    String[] allowed = (String[]) returnValue.get(Conversation.callbackConvMemberMuted_SUCC);
    ArrayList<AVIMOperationFailure> failed = (ArrayList<AVIMOperationFailure>)returnValue.get(Conversation.callbackConvMemberMuted_FAIL);

    done(AVIMException.wrapperAVException(e), Arrays.asList(allowed), failed);
  }
}
