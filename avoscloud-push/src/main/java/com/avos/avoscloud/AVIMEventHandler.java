package com.avos.avoscloud;

/**
 * Created by lbt05 on 1/30/15.
 */
public abstract class AVIMEventHandler {
  public void processEvent(final int operation, final Object operator, final Object operand,
      final Object eventScene) {
    if (!AVUtils.isMainThread()) {
      if (!AVOSCloud.handler.post(new Runnable() {
        @Override
        public void run() {
          processEvent0(operation, operator, operand, eventScene);
        }
      })) {
        LogUtil.log.e("Post runnable to handler failed.");
      }
    } else {
      processEvent0(operation, operator, operand, eventScene);
    }
  };

  protected abstract void processEvent0(int operation, Object operator, Object operand,
      Object eventScene);
}
