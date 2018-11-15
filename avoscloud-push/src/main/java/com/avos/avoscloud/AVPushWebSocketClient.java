package com.avos.avoscloud;

import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;

//import com.avos.avoscloud.java_websocket.WebSocket;
//import com.avos.avoscloud.java_websocket.client.WebSocketClient;
//import com.avos.avoscloud.java_websocket.drafts.Draft_17;
//import com.avos.avoscloud.java_websocket.framing.CloseFrame;
//import com.avos.avoscloud.java_websocket.framing.Framedata;
//import com.avos.avoscloud.java_websocket.framing.FramedataImpl1;
//import com.avos.avoscloud.java_websocket.handshake.ServerHandshake;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.PingFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;

import com.avos.avospush.session.CommandPacket;

import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
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
  public static final String SUB_PROTOCOL_2_1 = "lc.protobuf2.1";
  public static final String SUB_PROTOCOL_2_3 = "lc.protobuf2.3";

  private static final String HEADER_SUB_PROTOCOL = "Sec-WebSocket-Protocol";
  private static final String HEADER_SNI_HOST = "Host";
  private static final int PING_TIMEOUT_CODE = 3000;

  private HeartBeatPolicy heartBeatPolicy;

  private final long RECONNECT_INTERVAL = 10 * 1000;

  Runnable reconnectTask = new Runnable() {
    @Override
    public void run() {
      autoReconnect();
    }
  };

  AVPacketParser receiver;
  SSLSessionCache sessionCache;

  private static ArrayList<IProtocol> protocols = new ArrayList<IProtocol>();
  static {
    protocols.add(new Protocol(SUB_PROTOCOL_2_3));
  }

  public AVPushWebSocketClient(URI serverURI, AVPacketParser parser,
                               final String subProtocol, boolean secEnabled) {
    super(serverURI, new Draft_6455(Collections.<IExtension>emptyList(), protocols), new HashMap<String, String>() {
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
    this.receiver = parser;
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
    if (receiver != null) {
      receiver.loginCmd();
      receiver.processConnectionStatus(null);
      receiver.processSessionsStatus(false);
    }
    LogUtil.avlog.d("onOpen()");

    // resume livequery if necessary.
    AVLiveQuery.resumeSubscribeers();
  }

  @Override
  public void onMessage(ByteBuffer byteBuffer) {
    if (receiver != null) {
      receiver.processCommand(byteBuffer);
    }
  }

  public void onMessage(String msg) {
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    heartBeatPolicy.stopHeartbeat();
    if (receiver != null) {
      receiver.processSessionsStatus(true);
    }
    if (receiver != null) {
      receiver.processConnectionStatus(new AVException(code, reason));
    }
    LogUtil.avlog.d("onClose(). local disconnection code=" + code + ", reason=" + reason + ", remote=" + remote);
    switch (code) {
      case -1:
        LogUtil.avlog.d("connection refused");
        if(remote){
          if(receiver !=null){
            receiver.processRemoteServerNotAvailable();
          }
        }else{
          scheduleReconnect();
        }
        break;
      case PING_TIMEOUT_CODE:
        LogUtil.avlog.d("connection unhealthy");
        autoReconnect();
        break;
      default:
        scheduleReconnect();
        break;
    }
  }

  @Override
  public void onError(Exception ex) {
    ex.printStackTrace();
    if (receiver != null && AVUtils.isConnected(AVOSCloud.applicationContext)) {
      receiver.processRemoteServerNotAvailable();
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
    LogUtil.avlog.d("connection destroyed");
  }

  protected boolean isDestroyed() {
    return this.destroyed.get();
  }

  protected synchronized void autoReconnect() {
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
    PingFrame frame = new PingFrame();
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

  public interface AVPacketParser {
    void loginCmd();

    void processCommand(ByteBuffer bytes);

    void processConnectionStatus(AVException e);

    void processRemoteServerNotAvailable();

    void processSessionsStatus(boolean closeEvent);
  }
}
