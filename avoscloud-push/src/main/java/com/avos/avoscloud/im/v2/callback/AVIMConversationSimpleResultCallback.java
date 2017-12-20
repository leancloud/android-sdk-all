package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMException;

import java.util.List;

/**
 * Created by fengjunwen on 2017/12/20.
 */

public abstract class AVIMConversationSimpleResultCallback extends AVCallback<List<String>> {
  /**
   * 结果处理函数
   * @param memberIdList  成员的 client id 列表
   * @param e             异常
   */
  public abstract void done(List<String> memberIdList, AVIMException e);

  @Override
  protected final void internalDone0(List<String> returnValue, AVException e) {
    done(returnValue, AVIMException.wrapperAVException(e));
  }
}
