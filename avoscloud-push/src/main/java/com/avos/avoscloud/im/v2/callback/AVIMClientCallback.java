package com.avos.avoscloud.im.v2.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMException;

/**
 * Created by lbt05 on 3/19/15.
 */
public abstract class AVIMClientCallback extends AVCallback<AVIMClient> {
  public abstract void done(AVIMClient client, AVIMException e);

  @Override
  protected void internalDone0(AVIMClient client, AVException avException) {
    done(client, AVIMException.wrapperAVException(avException));
  }
}
