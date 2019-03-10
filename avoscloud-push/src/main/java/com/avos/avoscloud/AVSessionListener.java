package com.avos.avoscloud;

import com.avos.avospush.session.CommandPacket;

import android.content.Context;

import java.util.List;

public abstract class AVSessionListener {

  public abstract void onError(Context context, AVSession session, Throwable e, int sessionOperation,
      int requestId);

  public void onError(Context context, AVSession session, Throwable e) {
    this.onError(context, session, e, AVSession.OPERATION_UNKNOW, CommandPacket.UNSUPPORTED_OPERATION);
  }

  public abstract void onSessionOpen(Context context, AVSession session, int requestId);

  public abstract void onSessionClose(Context context, AVSession session, int requestId);

  public abstract void onSessionTokenRenewed(Context context, AVSession session, int requestId);

  public abstract void onSessionPaused(Context context, AVSession session);

  public abstract void onSessionResumed(Context context, AVSession session);

  public abstract void onOnlineQuery(Context context, AVSession session, List<String> onlinePeerIds,
      int requestCode);

  public abstract void onGoaway(Context context, AVSession session);

  /*
   * 这个方法主要是用来处理服务器端的主动登出当前用户的登录的
   */
  public abstract void onSessionClosedFromServer(Context context, AVSession session, int code);

}
