package com.avos.avoscloud;

import android.os.Message;

/**
 * 改变启动时发送的业务逻辑
 *
 * 原本的启动时发送是指在整个App被启动时才发送，一来不合理，二来不可控制
 *
 * 所以现在改成新起一个session就要发送
 *
 * Created by lbt05 on 2/13/15.
 */
class BoosterRequestController extends BasicAnalyticsRequestController {
  String currentSessionId;
  String tmpSessionId;

  BoosterRequestController(String sessionId, AnalyticsRequestDispatcher dispatcher) {
    super(dispatcher);
    this.currentSessionId = sessionId;
  }


  private Message makeMessage(String sessionId) {
    Message msg = new Message();
    msg.obj = sessionId;
    return msg;
  }

  @Override
  public void requestToSend(String sessionId) {
    asyncHandler.sendMessage(makeMessage(sessionId));
  }

  @Override
  public void quit() {

  }

  @Override
  public void prepareRequest() {
    if (AVOSCloud.isDebugLogEnabled() && AVOSCloud.showInternalDebugLog()) {
      LogUtil.avlog.d("sent analytics request on booster");
    }
  }

  @Override
  public boolean requestValidate(Message message) {
    tmpSessionId = (String) message.obj;
    return (!AVUtils.isBlankString(currentSessionId) && !currentSessionId.equals(tmpSessionId))
        && super.requestValidate(message);
  }

  @Override
  public void onRequestDone() {
    currentSessionId = tmpSessionId;

  }
}
