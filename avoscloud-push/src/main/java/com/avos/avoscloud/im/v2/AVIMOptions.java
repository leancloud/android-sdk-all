package com.avos.avoscloud.im.v2;

import com.avos.avoscloud.SignatureFactory;

/**
 * Created by fengjunwen on 2017/8/31.
 */

public class AVIMOptions {
  private static final AVIMOptions globalOptions = new AVIMOptions();

  private String rtmServer = "";
  private SignatureFactory signatureFactory = null;
  private int timeoutInSecs = 15;

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

  /**
   * get signature factory
   * @return
   */
  public SignatureFactory getSignatureFactory() {
    return signatureFactory;
  }

  /**
   * set signature factory
   * @param signatureFactory
   */
  public void setSignatureFactory(SignatureFactory signatureFactory) {
    this.signatureFactory = signatureFactory;
  }

  /**
   * get timeout option.
   * @return
   */
  public int getTimeoutInSecs() {
    return timeoutInSecs;
  }

  /**
   * set timeout option.
   * @param timeoutInSecs
   */
  public void setTimeoutInSecs(int timeoutInSecs) {
    this.timeoutInSecs = timeoutInSecs;
  }

  private AVIMOptions() {
  }
}
