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

  public void requestToSend(String currentSessionId) {

  };

  public void quit() {

  };

  public boolean requestValidate(Message message) {
    return true;
  };

  public void onRequestDone() {

  };

  public void appraisalSession(AnalyticsSession session) {

  }

  interface AnalyticsRequestDispatcher {
    public void sendRequest();
  }
}
