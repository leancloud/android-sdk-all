package com.avos.avoscloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by lbt05 on 2/13/15.
 */
abstract class BasicAnalyticsRequestController extends AnalyticsRequestController {

  Handler asyncHandler;

  BasicAnalyticsRequestController(
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

  /**
   * prepare request to send
   */
  public void prepareRequest() {}

  /**
   * after request sent.
   */
  public void onRequestDone() {
  }
}
