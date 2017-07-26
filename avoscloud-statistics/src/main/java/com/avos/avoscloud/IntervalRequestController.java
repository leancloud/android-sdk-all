package com.avos.avoscloud;

import android.os.Message;

/**
 *
 * Created by lbt05 on 2/9/15.
 */
class IntervalRequestController extends BoosterRequestController {

  IntervalTimer timer;

  IntervalRequestController(String sessionId, final AnalyticsRequestDispatcher dispatcher,
      long countDownInterval) {
    super(sessionId, dispatcher);
    timer =
        new IntervalTimer(AnalyticsRequestController.controllerThread.getLooper(),
            countDownInterval) {
          @Override
          public void onTrigger() {

            if (dispatcher != null) {
              if (AVOSCloud.isDebugLogEnabled()) {
                LogUtil.avlog.d("send stats interval request");
              }
              dispatcher.sendRequest();
            }
          }
        };
    timer.start();
  }

  public void quit() {
    timer.cancel();
  }

  private final void skip() {
    timer.skip();
  }

  @Override
  public void prepareRequest() {
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("send stats interval request for new session");
    }
  }

  @Override
  public void onRequestDone() {
    currentSessionId = tmpSessionId;
    this.skip();
  }
}
