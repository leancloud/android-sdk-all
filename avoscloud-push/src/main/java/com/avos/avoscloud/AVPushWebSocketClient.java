package com.avos.avoscloud;

import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;

import com.avos.avoscloud.java_websocket.WebSocket;
import com.avos.avoscloud.java_websocket.client.WebSocketClient;
import com.avos.avoscloud.java_websocket.drafts.Draft_17;
import com.avos.avoscloud.java_websocket.framing.CloseFrame;
import com.avos.avoscloud.java_websocket.framing.Framedata;
import com.avos.avoscloud.java_websocket.framing.FramedataImpl1;
import com.avos.avoscloud.java_websocket.handshake.ServerHandshake;
import com.avos.avospush.session.CommandPacket;

import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * Created by lbt05 on 5/20/16.
 */
public class AVPushWebSocketClient extends WebSocketClient {
  private static final String HEADER_SUB_PROTOCOL = "Sec-WebSocket-Protocol";
  private static final String HEADER_SNI_HOST = "Host";
  private static final int PING_TIMEOUT_CODE = 3000;

  private HeartBeatPolicy heartBeatPolicy;

  private final long RECONNECT_INTERVAL = 10 * 1000;

  Runnable reconnectTask = new Runnable() {
    @Override
    public void run() {
      reconnect();
    }
  };

  AVSocketListener listener;
  SSLSessionCache sessionCache;

  public AVPushWebSocketClient(URI serverURI, AVSocketListener listener,
                               final String subProtocol, boolean secEnabled) {
    super(serverURI, new Draft_17(), new HashMap<String, String>() {
      {
        put(HEADER_SUB_PROTOCOL, subProtocol);
      }
    }, 0);
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("trying to connect " + serverURI + ", subProtocol=" + subProtocol);
    }
    initHeartBeatPolicy();
    if (secEnabled) {
      setSocket();
    }
    this.listener = listener;
  }

  private void initHeartBeatPolicy() {
    heartBeatPolicy = new HeartBeatPolicy() {
      @Override
      public void onTimeOut() {
        closeConnection(PING_TIMEOUT_CODE, "No response for ping");
      }

      @Override
      public void sendPing() {
        ping();
      }
    };
  }

  private void setSocket() {
    try {
      String url = getURI().toString();
      if (!AVUtils.isBlankContent(url)) {
        if (url.startsWith("wss")) {

          if (null == sessionCache) {
            sessionCache = new SSLSessionCache(AVOSCloud.applicationContext);
          }
          SSLCertificateSocketFactory socketFactory = (SSLCertificateSocketFactory)SSLCertificateSocketFactory.getDefault(5000, sessionCache);
          Socket socket = socketFactory.createSocket();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            socketFactory.setUseSessionTickets(socket, true);
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && socket instanceof SSLSocket) {
            try {
              SNIHostName serverName = new SNIHostName(getURI().getHost());
              List<SNIServerName> serverNames = new ArrayList<>(1);
              serverNames.add(serverName);

              SSLParameters params = ((SSLSocket)socket).getSSLParameters();
              params.setServerNames(serverNames);
              ((SSLSocket)socket).setSSLParameters(params);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }

          setSocket(socket);

        } else {
          SocketFactory socketFactory = SocketFactory.getDefault();
          setSocket(socketFactory.createSocket());
        }
      }
    } catch (Exception e) {
      LogUtil.avlog.e("Socket Error", new AVException(e));
    }
  }

  @Override
  public void onOpen(ServerHandshake handshakedata) {
    this.cancelReconnect();
    heartBeatPolicy.startHeartbeat();
    if (listener != null) {
      listener.loginCmd();
      listener.processConnectionStatus(null);
      listener.processSessionsStatus(false);
    }
    LogUtil.avlog.d("AVPushWebSocketClient#onOpen() finished.");
  }

  @Override
  public void onMessage(ByteBuffer byteBuffer) {
    if (listener != null) {
      listener.processCommand(byteBuffer);
    }
  }

  @Override
  public void onMessage(String msg) {
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    heartBeatPolicy.stopHeartbeat();
    if (listener != null) {
      listener.processSessionsStatus(true);
    }
    if (listener != null) {
      listener.processConnectionStatus(new AVException(code, reason));
    }
    LogUtil.avlog.d("AVPushWebSocketClient#onClose(). local disconnection:" + code + "  " + reason + " :" + remote);
    switch (code) {
      case -1:
        LogUtil.avlog.d("connection refused");
        if(remote){
          if(listener!=null){
            listener.processRemoteServerNotAvailable();
          }
        }else{
          scheduleReconnect();
        }
        break;
      case CloseFrame.ABNORMAL_CLOSE:
        scheduleReconnect();
        break;
      case PING_TIMEOUT_CODE:
        LogUtil.avlog.d("connection unhealthy");
        reconnect();
        break;
      default:
        scheduleReconnect();
        break;
    }
  }

  @Override
  public void onError(Exception ex) {
    LogUtil.avlog.e("socket error.", ex);
    if (listener != null && AVUtils.isConnected(AVOSCloud.applicationContext)) {
      listener.processRemoteServerNotAvailable();
    }
  }

  protected void scheduleReconnect() {
    if (RECONNECT_INTERVAL > 0) {
      AVOSCloud.handler.postDelayed(reconnectTask, RECONNECT_INTERVAL);
    }
  }

  protected void cancelReconnect() {
    AVOSCloud.handler.removeCallbacks(this.reconnectTask);
  }
  AtomicBoolean destroyed = new AtomicBoolean(false);
  protected void destroy(){
    destroyed.set(true);
    cancelReconnect();
    heartBeatPolicy.stopHeartbeat();
  }

  protected synchronized void reconnect() {
    if (this.isConnecting() || this.isOpen()) {
      // 已经是健康的状态了就没必要再发了
      return;
    } else if (AVUtils.isConnected(AVOSCloud.applicationContext)) {
      this.connect();
    } else if(!destroyed.get()){
      // 网络状态有问题,我们延期再尝试吧
      scheduleReconnect();
    }
  }

  protected void ping() {
    FramedataImpl1 frame = new FramedataImpl1(Framedata.Opcode.PING);
    frame.setFin(true);
    this.sendFrame(frame);
  }

  public void send(CommandPacket packet) {
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("uplink : " + packet.getGenericCommand().toString());
    }
    try {
      send(packet.getGenericCommand().toByteArray());
    } catch (Exception e) {
      LogUtil.avlog.e(e.getMessage());
    }
  }

  @Override
  public void onWebsocketPong(WebSocket conn, Framedata f) {
    super.onWebsocketPong(conn, f);
    heartBeatPolicy.onPong();
  }

  public interface AVSocketListener {
    void loginCmd();

    void processCommand(ByteBuffer bytes);

    void processConnectionStatus(AVException e);

    void processRemoteServerNotAvailable();

    void processSessionsStatus(boolean closeEvent);
  }
}
