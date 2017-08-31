package com.avos.avoscloud.im.v2;

/**
 * Created by fengjunwen on 2017/8/31.
 */

public class AVIMOptions {
  private static final AVIMOptions globalOptions = new AVIMOptions();

  private String rtmServer = "";

  /**
   * get global options instance.
   * @return
   */
  public static AVIMOptions getGlobalOptions() {
    return globalOptions;
  }

  /**
   * set rtm server address.
   * @param server
   */
  public void setRTMServer(String server) {
    rtmServer = server;
  }

  /**
   * get rtm server address.
   * @return
   */
  public String getRTMServer() {
    return rtmServer;
  }

  private AVIMOptions() {
  }
}
