package com.avos.avoscloud;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;

@SuppressLint("NewApi")
public class ArchiveRequestTaskController {
  static ScheduledExecutorService scheduledExecutorService;
  static ScheduledFuture<?> scheduledFuture;
  private static Lock lock = new ReentrantLock();
  private static final long TIME_DELAY_FOR_ARCHIVEREQUEST = 30;

  public static synchronized void schedule() {
    boolean cancelled = true;
    if (scheduledExecutorService == null) {
      scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }
    if (scheduledFuture != null && !scheduledFuture.isDone()) {
      cancelled = scheduledFuture.cancel(false);
    }
    if (cancelled) {
      scheduledFuture =
          scheduledExecutorService.schedule(archiveRequestTask, TIME_DELAY_FOR_ARCHIVEREQUEST,
              TimeUnit.SECONDS);
    }

  }

  static Runnable archiveRequestTask = new Runnable() {
    @Override
    public void run() {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("trying to send archive request");
      }
      if (!AVUtils.isBlankString(AVOSCloud.applicationId) && AVOSCloud.applicationContext != null) {
        if (lock.tryLock()) {
          try {
            PaasClient.storageInstance().handleAllArchivedRequest(true);
            RequestStatisticsUtil.getInstance().sendToServer();
          } catch (Exception e) {
            LogUtil.log.e("Exception happended during processing archive requests", e);
          } finally {
            lock.unlock();
          }
        }
      } else {
        LogUtil.log.e("applicationContext is null, Please call AVOSCloud.initialize first");
      }
    }
  };
}
