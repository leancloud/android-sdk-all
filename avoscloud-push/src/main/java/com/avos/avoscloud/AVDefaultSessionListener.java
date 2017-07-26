package com.avos.avoscloud;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMClientEventHandler;
import com.avos.avoscloud.im.v2.AVIMMessageManagerHelper;
import com.avos.avoscloud.im.v2.Conversation;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avospush.session.CommandPacket;

public class AVDefaultSessionListener extends AVInternalSessionListener {
  // 所有的返回不再直接在listener中间处理，而是抛出一个broadcast出来，让用户自定义的receiver来处理
  AVPushConnectionManager manager;

  public AVDefaultSessionListener(AVPushConnectionManager manager) {
    this.manager = manager;
  }

  @Override
  public void onSessionOpen(Context context, AVSession session, int requestId) {

    // 既然已经成功了，就往缓存里面添加一条记录
    AVSessionCacheHelper.getTagCacheInstance().addSession(session.getSelfPeerId(), session.tag);
    // 这里需要给AVIMClient那边发一个LocalBoardcastMessage
    if (requestId > CommandPacket.UNSUPPORTED_OPERATION) {
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId,
          AVIMOperation.CLIENT_OPEN);
    }
  }

  @Override
  public void onSessionPaused(Context context, AVSession session) {
    AVIMClientEventHandler handler = AVIMMessageManagerHelper.getClientEventHandler();
    if (handler != null) {
      handler.processEvent(Conversation.STATUS_ON_CONNECTION_PAUSED, null, null,
        AVIMClient.getInstance(session.getSelfPeerId()));
    }
  }

  @Override
  public void onSessionResumed(Context context, AVSession session) {
    AVIMClientEventHandler handler = AVIMMessageManagerHelper.getClientEventHandler();
    if (handler != null) {
      handler.processEvent(Conversation.STATUS_ON_CONNECTION_RESUMED, null, null,
        AVIMClient.getInstance(session.getSelfPeerId()));
    }
  }

  @Override
  public void onSessionClosedFromServer(Context context, AVSession session, int code) {
    cleanSession(session);
    AVIMClientEventHandler handler = AVIMMessageManagerHelper.getClientEventHandler();
    if (handler != null) {
      handler.processEvent(Conversation.STATUS_ON_CLIENT_OFFLINE, null, code,
          AVIMClient.getInstance(session.getSelfPeerId()));
    }
  }

  @Override
  public void onError(Context context, AVSession session, Throwable e, int sessionOperation,
      int requestId) {
    if (AVOSCloud.isDebugLogEnabled() || AVOSCloud.showInternalDebugLog()) {
      LogUtil.log.e("session error:" + e);
    }
    if (requestId > CommandPacket.UNSUPPORTED_OPERATION) {
      switch (sessionOperation) {
        case AVSession.OPERATION_OPEN_SESSION:
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId, e,
              Conversation.AVIMOperation.CLIENT_OPEN);
          break;
        case AVSession.OPERATION_CLOSE_SESSION:
          BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId, e,
              Conversation.AVIMOperation.CLIENT_DISCONNECT);
          break;
      }
      if (sessionOperation == AVIMOperation.CONVERSATION_CREATION.getCode()) {
        BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId, e,
            Conversation.AVIMOperation.CONVERSATION_CREATION);
      }
    }
  }

  @Override
  public void onSessionClose(Context context, AVSession session, int requestId) {
    manager.removeSession(session.getSelfPeerId());
    if (requestId > CommandPacket.UNSUPPORTED_OPERATION) {
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestId,
        AVIMOperation.CLIENT_DISCONNECT);
    }
  }

  private void cleanSession(AVSession session) {
    AVSessionCacheHelper.getTagCacheInstance().removeSession(session.getSelfPeerId());
    session.sessionOpened.set(false);
    // 如果session都已不在，缓存消息静静地等到桑田沧海
    session.cleanUp();
    manager.removeSession(session.getSelfPeerId());
  }

  @Override
  public void onOnlineQuery(Context context, AVSession session, List<String> onlinePeerIds,
      int requestCode) {
    if (requestCode != CommandPacket.UNSUPPORTED_OPERATION) {
      Bundle bundle = new Bundle();
      bundle.putStringArrayList(Conversation.callbackOnlineClients, new ArrayList<String>(
        onlinePeerIds));
      BroadcastUtil.sendIMLocalBroadcast(session.getSelfPeerId(), null, requestCode,
        bundle, AVIMOperation.CLIENT_ONLINE_QUERY);
    }
  }
}
