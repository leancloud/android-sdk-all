package com.avos.avoscloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by lbt05 on 2/11/15.
 */
class RealTimeRequestController extends BasicAnalyticsRequestDispatcher {

  RealTimeRequestController(final AnalyticsRequestDispatcher dispatcher) {
    super(dispatcher);
  }


  private final static int REQUEST_FOR_SEND = 19141010;
  private final static int REQUEST_END_SEND = 20141010;

  // 这个类基本上是给实时发送用的
  private final Handler reportRequestDispatcher = new Handler(Looper.getMainLooper()) {
    /*
     * 同一时间最多只有两个请求：1.正在发的请求 2.待发的请求
     * 请求发完以后检查是否要待发请求，有则发，无则停 有要发的请求，检查是否有正在发送的请求，有加入待发请求，没有则直接发送
     */
    boolean hasRequestForSend = false;
    boolean hasRequestSending = false;

    @Override
    public void handleMessage(Message msg) {

      switch (msg.what) {
        case REQUEST_FOR_SEND:
          if (hasRequestSending) {
            // cache this sending request if request is sending
            hasRequestForSend = true;
          } else {
            // send this sending request
            asyncHandler.sendEmptyMessage(REQUEST_FOR_SEND);
            hasRequestSending = true;
          }
          break;
        case REQUEST_END_SEND:
          if (hasRequestForSend) {
            // send this sending request
            asyncHandler.sendEmptyMessage(REQUEST_FOR_SEND);
            hasRequestForSend = false;
            hasRequestSending = true;
          } else {
            hasRequestSending = false;
          }
          break;
      }
    };
  };

  @Override
  public void prepareRequest() {
    if (AVOSCloud.isDebugLogEnabled() && AnalyticsImpl.enableDebugLog) {
      LogUtil.avlog.d("sent real time analytics request");
    }
  }

  @Override
  public void requestToSend(String currentSessionId) {
    reportRequestDispatcher.sendMessage(makeMessage());
  }

  private Message makeMessage() {
    Message msg = new Message();
    msg.what = REQUEST_FOR_SEND;
    return msg;
  }

  @Override
  public boolean requestValidate(Message msg) {
    return super.requestValidate(msg) && msg.what == REQUEST_FOR_SEND;
  }

  @Override
  public void onRequestDone() {
    reportRequestDispatcher.sendEmptyMessage(REQUEST_END_SEND);
  }

  @Override
  public void quit() {

  }
}
