package com.avos.avoscloud;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

/**
 * Created by lbt05 on 2/11/15.
 * 一个类似于CountDownTimer的小实现
 * 用于实现一个简单的循环定时器
 */
abstract class IntervalTimer {
  /**
   * The interval in millis that the user receives callbacks
   */
  private final long mCountdownInterval;

  private volatile long mTriggerTimeInFuture;

  /**
   * boolean representing if the timer was cancelled
   */
  private volatile boolean mCancelled = false;

  public IntervalTimer(long countDownInterval) {
    this(null, countDownInterval);
  }

  public IntervalTimer(Looper looper, long countDownInterval) {
    this.mCountdownInterval = countDownInterval;
    mHandler = new Handler(looper == null ? Looper.getMainLooper() : looper) {

      @Override
      public void handleMessage(Message msg) {

        synchronized (IntervalTimer.this) {
          if (mCancelled) {
            return;
          }

          switch (msg.what) {
            case MSG:
              final long millisLeft = mTriggerTimeInFuture - SystemClock.elapsedRealtime();
              if (millisLeft <= 0) {
                onTrigger();
                // set next trigger timestamp
                mTriggerTimeInFuture = mTriggerTimeInFuture + mCountdownInterval - millisLeft;
                sendMessageDelayed(obtainMessage(MSG), mCountdownInterval);
                // onFinish();
              } else if (millisLeft <= mCountdownInterval) {
                // no tick, just delay until done
                sendMessageDelayed(obtainMessage(MSG), millisLeft);
              }
              break;
            case SKIP:
              mTriggerTimeInFuture = SystemClock.elapsedRealtime() + mCountdownInterval;

              break;
          }
        }
      }
    };
  }

  /**
   * 停止计时器
   */
  public synchronized void cancel() {
    mCancelled = true;
    mHandler.removeMessages(MSG);
    mHandler.removeMessages(SKIP);
  }

  public final void skip() {
    mHandler.sendEmptyMessage(SKIP);
  }

  public abstract void onTrigger();

  /**
   * 开始计时器
   */
  protected synchronized final IntervalTimer start() {
    mCancelled = false;
    mTriggerTimeInFuture = SystemClock.elapsedRealtime() + mCountdownInterval;
    mHandler.sendMessage(mHandler.obtainMessage(MSG));
    return this;
  }

  private static final int MSG = 1;
  private static final int SKIP = -1;

  // handles counting down
  private Handler mHandler;
}
