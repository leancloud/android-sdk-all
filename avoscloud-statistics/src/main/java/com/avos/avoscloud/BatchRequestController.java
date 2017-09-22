package com.avos.avoscloud;

import android.os.Message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lbt05 on 2/11/15.
 */
class BatchRequestController extends IntervalRequestController {

  private final AtomicInteger messageCount = new AtomicInteger(0);;
  // 消息数量的上限是30，但是由于start跟end事件都会触发，所以实际触发数量为消息数的2倍
  private final int messageCountThreshold = 60;

  BatchRequestController(String sessionId, final AnalyticsRequestDispatcher dispatcher,
      long defaultInterval) {
    super(sessionId, dispatcher, defaultInterval);
  }

  private int getMessageCount() {
    return messageCount.get();
  }

  private int incMessageCount() {
    return messageCount.incrementAndGet();
  }


  private void resetMessageCount() {
    resetMessageCount(0);
  }

  void resetMessageCount(int i) {
    messageCount.set(i);
  }

  @Override
  public void prepareRequest() {
    if (AVOSCloud.isDebugLogEnabled() && AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("send stats batch request");
    }
  }


  @Override
  public void requestToSend(String sessionId) {
    int count = incMessageCount();
    Message message = new Message();
    message.obj = sessionId;
    message.what = count;
    asyncHandler.sendMessage(message);
  }

  @Override
  public boolean requestValidate(Message msg) {
    return super.requestValidate(msg) || msg.what >= messageCountThreshold;
  }

  @Override
  public void appraisalSession(AnalyticsSession session) {
    if (session == null) {
      resetMessageCount();
    } else {
      resetMessageCount(session.getMessageCount());
    }
  }
}
