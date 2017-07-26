package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMException;

import java.util.List;

/**
 *
 * 作为AVIMClient方法中checkOnlineClients的回调方法
 */
public abstract class AVIMOnlineClientsCallback extends AVCallback<List<String>> {
  public abstract void done(List<String> object, AVIMException e);

  @Override
  protected final void internalDone0(List<String> object, AVException error) {
    this.done(object, AVIMException.wrapperAVException(error));
  }
}
