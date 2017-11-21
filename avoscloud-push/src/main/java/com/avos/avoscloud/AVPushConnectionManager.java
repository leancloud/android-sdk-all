package com.avos.avoscloud;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.java_websocket.framing.CloseFrame;
import com.avos.avospush.session.LiveQueryLoginPacket;
import com.avos.avospush.push.AVPushRouter;
import com.avos.avospush.session.CommandPacket;
import com.avos.avospush.session.LoginPacket;
import com.avos.avospush.session.MessagePatchModifiedPacket;
import com.avos.avospush.session.PushAckPacket;
import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by yangchaozhong on 3/14/14.
 */
class AVPushConnectionManager implements AVPushWebSocketClient.AVSocketListener {
  private static AVPushConnectionManager instance = null;

  private volatile AVPushWebSocketClient socketClient;

  private static final Map<String, AVSession> peerIdEnabledSessions = Collections
      .synchronizedMap(new HashMap<String, AVSession>());

  private List<AVCallback> connectionCallbacks = new LinkedList<AVCallback>();
  private AVPushRouter router;

  private static String liveQuerySubscribeId = "";

  private AVPushConnectionManager(Context ctx) {
    LogUtil.log.d("begin to invoke AVPushConnectionManager(Context)");
    router = new AVPushRouter(ctx, new AVPushRouter.RouterResponseListener() {
      @Override
      public void onServerAddress(String address) {
        if (!AVUtils.isBlankString(address)) {
          createNewWebSocket(address);
        } else {
          if (AVOSCloud.isDebugLogEnabled()) {
            LogUtil.avlog.d("push server address is null");
          }
        }
      }
    });

    initSessionsIfExists();
    boolean initializeConn = true;
    if (AVOSCloud.isGcmOpen()) {
      try {
        Class<?> gcmManagerClass = Class.forName("com.avos.avoscloud.AVGcmManager");
        Method getTokenMethod = gcmManagerClass.getMethod("getGcmTokenInBackground", Context.class);
        getTokenMethod.invoke(gcmManagerClass, ctx);
        initializeConn = false;
      } catch (Exception e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.i("gcm library not started since not included");
        }
      }
    }
    if (initializeConn) {
      initConnection();
    } else {
      LogUtil.log.d("skip initialize connection bcz of GCM Push using");
    }
    LogUtil.log.d("end of AVPushConnectionManager(Context)");
  }

  private void initSessionsIfExists() {
    Map<String, String> cachedSessions = AVSessionCacheHelper.getTagCacheInstance().getAllSession();
    for (Map.Entry<String, String> entry : cachedSessions.entrySet()) {
      AVSession s = this.getOrCreateSession(entry.getKey());
      s.sessionResume.set(true);
      s.tag = entry.getValue();
    }
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d(cachedSessions.size() + " sessions recovered");
    }
  }

  public synchronized static AVPushConnectionManager getInstance(Context ctx) {
    if (instance == null) {
      instance = new AVPushConnectionManager(ctx);
    }

    return instance;
  }

  public void initConnection() {
    this.initConnection(null);
  }

  public void initConnection(final AVCallback cl) {
    if (socketClient != null && socketClient.isOpen()) {
      LogUtil.log.d("push connection is open");
      return;
    } else if (socketClient != null) {
      socketClient.cancelReconnect();
    }
    LogUtil.log.d("try to query connection server via push router.");
    router.fetchPushServer();
    if (null != cl) {
      connectionCallbacks.add(cl);
    }
  }

  public boolean isConnectedToPushServer() {
    return (socketClient != null && socketClient.isOpen());
  }

  public void stop() {
    cleanupSocketConnection();
  }

  public AVSession getOrCreateSession(String peerId) {
    try {
      // 据说这行有NPE，所以不得不catch起来避免app崩溃
      boolean newAdded = !peerIdEnabledSessions.containsKey(peerId);
      AVSession session = null;
      if (newAdded) {
        session = new AVSession(peerId, new AVDefaultSessionListener(this));
        peerIdEnabledSessions.put(peerId, session);
        session.getWebSocketListener().onListenerAdded(
            (this.socketClient != null && socketClient.isOpen()));
      } else {
        session = peerIdEnabledSessions.get(peerId);
      }
      return session;
    } catch (Exception e) {
      return null;
    }
  }

  public void removeSession(String peerId) {
    AVSession session = peerIdEnabledSessions.remove(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onListenerRemoved();
    }
  }

  public void sendData(final CommandPacket packet) {
    if (socketClient != null && socketClient.isOpen()) {
      socketClient.send(packet);
    }
  }

  public void cleanupSocketConnection() {
    this.cleanupSocketConnection(CloseFrame.NORMAL, "");
  }

  public void cleanupSocketConnection(final int code, final String message) {
    if (socketClient != null && (socketClient.isConnecting() || socketClient.isOpen())) {
      try {
        socketClient.close(code, message);
        socketClient.destroy();
      } catch (Exception e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.e("Close socket client failed.", e);
        }
      }
    } else if (socketClient != null && socketClient.isClosing()) {
      socketClient.destroy();
      socketClient = null;
    }
  }

  private synchronized void createNewWebSocket(final String pushServer) {
    if (socketClient == null || socketClient.isClosed()) {
      // 由于需要链接到新的server address上,原来的client就要被抛弃了,抛弃前需要取消自动重连的任务
      if (socketClient != null) {
        LogUtil.log.d("destroy socketClient first which is closed.");
        socketClient.destroy();
      }

      if (AVSession.isOnlyPushCount()) {
        socketClient =
            new AVPushWebSocketClient(URI.create(pushServer), this, "lc.protobuf2.3", true);
      } else {
        socketClient =
            new AVPushWebSocketClient(URI.create(pushServer), this, "lc.protobuf2.1", true);
      }

      socketClient.connect();
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.d("connect to server: " + pushServer);
      }
    } else {
      LogUtil.log.d("skip create socketClient.");
    }
  }


  private void processLiveQueryData(Messages.DataCommand dataCommand) {
    List<String> messageIds = dataCommand.getIdsList();
    List<Messages.JsonObjectMessage> messages = dataCommand.getMsgList();

    ArrayList<String> dataList = new ArrayList<>();
    for (int i = 0; i < messages.size() && i < messageIds.size(); i++) {
      Messages.JsonObjectMessage message = messages.get(i);
      if (null != message) {
        dataList.add(message.getData());
      }
    }
    AVLiveQuery.processData(dataList);
  }

  private void processDataCommand(Messages.DataCommand dataCommand) {
    List<String> messageIds = dataCommand.getIdsList();
    List<Messages.JsonObjectMessage> messages = dataCommand.getMsgList();
    for (int i = 0; i < messages.size() && i < messageIds.size(); i++) {
      if (null != messages.get(i)) {
        AVNotificationManager.getInstance().processPushMessage(messages.get(i).getData(), messageIds.get(i));
      }
    }
    PushAckPacket pap = new PushAckPacket();
    pap.setInstallationId(AVInstallation.getCurrentInstallation().getInstallationId());
    pap.setMessageIds(messageIds);
    sendData(pap);
  }

  public void processCommand(ByteBuffer bytes) {
    try {
      Messages.GenericCommand command = Messages.GenericCommand.parseFrom(bytes.array());
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.d("downlink : " + command.toString());
      }

      String peerId = command.getPeerId();
      Integer requestKey = command.hasI() ? command.getI() : null;
      if (AVUtils.isBlankString(peerId)) {
        // in case that only 1 client loggined, downlink doesn't contains peerId.
        peerId = AVIMClient.getDefaultClient();
      }

      if (command.getCmd().getNumber() == Messages.CommandType.loggedin_VALUE) {
        if (LiveQueryLoginPacket.SERVICE_LIVE_QUERY == command.getService()) {
          processLoggedinCommand(requestKey);
        }
      } else if (!peerIdEnabledSessions.isEmpty()
          || command.getCmd().getNumber() == Messages.CommandType.data_VALUE) {
        switch (command.getCmd().getNumber()) {
          case Messages.CommandType.data_VALUE:
            if (command.hasService()) {
              final int service = command.getService();
              if (LiveQueryLoginPacket.SERVICE_PUSH == service) {
                processDataCommand(command.getDataMessage());
              } else if (LiveQueryLoginPacket.SERVICE_LIVE_QUERY == service) {
                processLiveQueryData(command.getDataMessage());
              }
            } else {
              processDataCommand(command.getDataMessage());
            }
            break;
          case Messages.CommandType.direct_VALUE:
            processDirectCommand(peerId, command.getDirectMessage());
            break;
          case Messages.CommandType.session_VALUE:
            processSessionCommand(peerId, command.getOp().name(), requestKey,
                command.getSessionMessage());
            break;
          case Messages.CommandType.ack_VALUE:
            processAckCommand(peerId, requestKey, command.getAckMessage());
            break;
          case Messages.CommandType.rcp_VALUE:
            processRpcCommand(peerId, command.getRcpMessage());
            break;
          case Messages.CommandType.conv_VALUE:
            processConvCommand(peerId, command.getOp().name(), requestKey,
                command.getConvMessage());
            break;
          case Messages.CommandType.error_VALUE:
            processErrorCommand(peerId, requestKey, command.getErrorMessage());
            break;
          case Messages.CommandType.logs_VALUE:
            processLogsCommand(peerId, requestKey, command.getLogsMessage());
            break;
          case Messages.CommandType.unread_VALUE:
            processUnreadCommand(peerId, command.getUnreadMessage());
            break;
          case Messages.CommandType.patch_VALUE:
            if(command.getOp().equals(Messages.OpType.modify)) {
              // modify 为服务器端主动推送的 patch 消息
              processPatchCommand(peerId, true, requestKey, command.getPatchMessage());
            } else if (command.getOp().equals(Messages.OpType.modified)) {
              // modified 代表的是服务器端对于客户端请求的相应
              processPatchCommand(peerId, false, requestKey, command.getPatchMessage());
            }
            break;
          default:
            break;
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  /**
   * 清空 connection list，因为会有在重连时发送多个请求的情况，所以需要一个 list 来存储
   * 主要逻辑运行在主线程
   */
  @Override
  public synchronized void processConnectionStatus(final AVException e) {
    router.processSocketConnectionResult(e);
    AVOSCloud.handler.post(new Runnable() {
      @Override
      public void run() {
        if (null != connectionCallbacks && connectionCallbacks.size() > 0) {
          Iterator<AVCallback> iterator = connectionCallbacks.iterator();
          while (iterator.hasNext()) {
            AVCallback callback = iterator.next();
            if (null != callback) {
              callback.internalDone(null, e);
            }
            iterator.remove();
          }
        }
      }
    });
  }

  private void processSessionCommand(String peerId, String op, Integer requestId,
                                     Messages.SessionCommand command) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onSessionCommand(op, requestId, command);
    }
  }

  private void processDirectCommand(String peerId, Messages.DirectCommand directCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onDirectCommand(directCommand);
    }
  }

  private void processRpcCommand(String peerId, Messages.RcpCommand command) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      if (command.hasRead()) {
        session.getWebSocketListener().onReadCmdReceipt(command);
      } else {
        session.getWebSocketListener().onMessageReceipt(command);
      }
    }
  }

  private void processAckCommand(String peerId, Integer requestKey, Messages.AckCommand command) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onAckCommand(requestKey, command);
    }
  }

  private void processConvCommand(String peerId, String operation, Integer requestKey,
                                  Messages.ConvCommand convCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onConversationCommand(operation, requestKey, convCommand);
    }
  }

  private void processErrorCommand(String peerId, Integer requestKey,
                                   Messages.ErrorCommand errorCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onError(requestKey, errorCommand);
    }
  }

  private void processLogsCommand(String peerId, Integer requestKey,
                                  Messages.LogsCommand logsCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    session.getWebSocketListener().onHistoryMessageQuery(requestKey, logsCommand);
  }

  private void processUnreadCommand(String peerId, Messages.UnreadCommand unreadCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (session != null && session.getWebSocketListener() != null) {
      session.getWebSocketListener().onUnreadMessagesCommand(unreadCommand);
    }
  }

  private void processPatchCommand(String peerId, boolean isModify, Integer requestKey, Messages.PatchCommand patchCommand) {
    AVSession session = peerIdEnabledSessions.get(peerId);
    if (null != session && null != session.getWebSocketListener()) {
      session.getWebSocketListener().onMessagePatchCommand(isModify, requestKey, patchCommand);
    }

    if (isModify) {
      long lastPatchTime = 0;
      for (Messages.PatchItem item : patchCommand.getPatchesList()) {
        if (item.getPatchTimestamp() > lastPatchTime) {
          lastPatchTime = item.getPatchTimestamp();
        }
      }
      sendData(MessagePatchModifiedPacket.getPatchMessagePacket(peerId, lastPatchTime));
    }
  }

  private void processLoggedinCommand(Integer requestKey) {
    if (null != requestKey) {
      Intent intent = new Intent();
      intent.setAction(AVLiveQuery.LIVEQUERY_PRIFIX + requestKey);
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).sendBroadcast(intent);
    }
  }

  public void sendLiveQueryLoginCmd(String subscribeId, int requestId) {
    if (!AVUtils.isBlankString(subscribeId)) {
      liveQuerySubscribeId = subscribeId;
      LiveQueryLoginPacket lp = new LiveQueryLoginPacket();
      lp.setSubscribeId(subscribeId);
      if (0 != requestId) {
        lp.setRequestId(requestId);
      }
      socketClient.send(lp);
    }
  }

  @Override
  public void loginCmd() {
    LoginPacket lp = new LoginPacket();
    lp.setAppId(AVOSCloud.applicationId);
    lp.setInstallationId(AVInstallation.getCurrentInstallation().getInstallationId());
    socketClient.send(lp);

    if (!AVUtils.isBlankString(liveQuerySubscribeId)) {
      sendLiveQueryLoginCmd(liveQuerySubscribeId, 0);
    }
  }

  @Override
  public void processSessionsStatus(boolean closeEvent) {
    for (AVSession session : peerIdEnabledSessions.values()) {
      if (session.getWebSocketListener() != null) {
        if (closeEvent) {
          session.getWebSocketListener().onWebSocketClose();
        } else {
          session.getWebSocketListener().onWebSocketOpen();
        }
      }
    }
  }


  @Override
  public void processRemoteServerNotAvailable() {
    router.fetchPushServer();
  }
}