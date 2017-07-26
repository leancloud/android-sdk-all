package com.avos.avoscloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by lbt05 on 2/13/15.
 */
abstract class BasicAnalyticsRequestDispatcher extends AnalyticsRequestController {

  Handler asyncHandler;

  BasicAnalyticsRequestDispatcher(
      final AnalyticsRequestController.AnalyticsRequestDispatcher dispatcher) {

    asyncHandler = new android.os.Handler(controllerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        if (dispatcher != null && requestValidate(msg)) {
          prepareRequest();
          dispatcher.sendRequest();
        }
        onRequestDone();
      }
    };
  }

  public void prepareRequest() {}

  public boolean requestValidate(Message message) {
    return true;
  }

  public void onRequestDone() {

  }
}
