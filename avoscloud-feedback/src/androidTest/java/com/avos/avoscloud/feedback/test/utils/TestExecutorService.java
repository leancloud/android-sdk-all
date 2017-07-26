package com.avos.avoscloud.feedback.test.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangxiaobo on 15/2/6.
 */
public class TestExecutorService extends ThreadPoolExecutor {
  public TestExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
  }

  @Override
  public Future<?> submit(Runnable runnable) {
    FutureTask futureTask = new FutureTask(runnable, null);
    futureTask.run();
    return futureTask;
  }
}
