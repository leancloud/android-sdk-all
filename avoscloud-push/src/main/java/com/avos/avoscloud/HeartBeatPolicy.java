package com.avos.avoscloud;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wli on 2017/6/7.
 */

abstract class HeartBeatPolicy {

  /**
   * 定时任务
   */
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

  private Future healthFuture;

  /**
   * 最近收到 ping 的时间
   */
  private long lastPongTS;

  /**
   * 心跳设置为 180s
   */
  private final long HEART_BEAT_INTERNAL = 180 * 1000;

  /**
   * 超时时长设置为两个心跳
   */
  private final long HEALTHY_THRESHOLD = HEART_BEAT_INTERNAL * 2;

  private Runnable healthMonitor = new Runnable() {
    @Override
    public void run() {
      if (System.currentTimeMillis() - lastPongTS > HEALTHY_THRESHOLD) {
        onTimeOut();
      } else {
        sendPing();
      }
    }
  };

  synchronized void onPong() {
    lastPongTS = System.currentTimeMillis();
  }

  synchronized void startHeartbeat() {
    stopHeartbeat();
    lastPongTS = System.currentTimeMillis();
    healthFuture = executor.scheduleAtFixedRate(healthMonitor, HEART_BEAT_INTERNAL, HEART_BEAT_INTERNAL,
      TimeUnit.MILLISECONDS);
  }

   synchronized void stopHeartbeat() {
    if (null != healthFuture) {
      healthFuture.cancel(true);
    }
  }

  public abstract void onTimeOut();

  public abstract void sendPing();
}
