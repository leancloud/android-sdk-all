package com.avos.avoscloud.feedback.test.base;

import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.feedback.test.utils.TestExecutorService;

import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.robolectric.Robolectric;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangxiaobo on 15/2/6.
 */
public class NetworkTestBase extends TestBase {
  @Before
  public void setup() {
    super.setup();
    BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
    TestExecutorService pool = new TestExecutorService(10, 10, 10, TimeUnit.MILLISECONDS, queue);

    // TestExecutorService to run on UI thread, submit immediately.
    PaasClient.storageInstance().clientInstance(false).setThreadPool(pool);
    PaasClient.storageInstance().clientInstance(true).setThreadPool(pool);

    Robolectric.getFakeHttpLayer().logHttpRequests();
  }

  protected BasicHeader buildJsonHeader() {
    return new BasicHeader("Content-Type", "application/json");
  }

}
