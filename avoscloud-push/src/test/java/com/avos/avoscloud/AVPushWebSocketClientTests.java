package com.avos.avoscloud;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Created by fengjunwen on 2017/9/4.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVPushWebSocketClientTests {

  private AVPushWebSocketClient.AVPacketParser listener = new AVPushWebSocketClient.AVPacketParser() {
    @Override
    public void loginCmd() {

    }

    @Override
    public void processCommand(ByteBuffer bytes) {

    }

    @Override
    public void processConnectionStatus(AVException e) {

    }

    @Override
    public void processRemoteServerNotAvailable() {

    }

    @Override
    public void processSessionsStatus(boolean closeEvent) {

    }
  };

  @Before
  public void setUp() {
    AVOSCloud.applicationContext = RuntimeEnvironment.application;
  }
  @Test
  public void testVPNUrl() {
    String pushServer = "wss://intviu-rtm.leancloud.cn/";
    AVPushWebSocketClient client = new AVPushWebSocketClient(URI.create(pushServer), listener, "lc.protobuf2.3", true);
    client.connect();
  }
}
