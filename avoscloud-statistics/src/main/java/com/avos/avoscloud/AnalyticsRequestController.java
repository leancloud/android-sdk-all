package com.avos.avoscloud;

import android.os.HandlerThread;
import android.os.Message;

/**
 * Created by lbt05 on 2/11/15.
 */
abstract class AnalyticsRequestController {
  static HandlerThread controllerThread = new HandlerThread(
      "com.avos.avoscloud.AnalyticsRequestController");

  static {
    controllerThread.start();
  }

  /**
   * 申请准备发送 session 数据
   * @param currentSessionId
   */
  public void requestToSend(String currentSessionId) {
  };

  /**
   * 退出
   */
  public void quit() {
  };

  /**
   * 校验消息合法性
   * @param message
   * @return
   */
  public boolean requestValidate(Message message) {
    return true;
  };

  /**
   * 处理结束之后的附加操作
   */
  public void onRequestDone() {
  };

  /**
   * session 评估
   * @param session
   */
  public void appraisalSession(AnalyticsSession session) {
  }

  interface AnalyticsRequestDispatcher {
    /**
     * 发送数据
     */
    public void sendRequest();
  }
}
