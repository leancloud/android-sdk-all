package com.avos.avoscloud.test;

import android.os.Looper;

import com.avos.avoscloud.test.utils.TestExecutorService;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangxiaobo on 15/2/5.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
public class HttpRequestTest {

  protected static final int HTTP_OK = 200;
  protected static final int HTTP_NOT_FOUND = 404;
  protected AsyncHttpClient httpClient = new AsyncHttpClient();

  @Before
  public void setup() {
    BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
    TestExecutorService pool = new TestExecutorService(10, 10, 10, TimeUnit.MILLISECONDS, queue);
    httpClient.setThreadPool(pool);
  }

  @Test
  public void testHttp() throws InterruptedException {
    Robolectric.addPendingHttpResponse(HTTP_OK, "ok", new BasicHeader("Content-Type",
        "\"application/json\""));
    AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
      @Override
      public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Assert.assertEquals(statusCode, HTTP_OK);
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
          Throwable error) {
        Assert.assertEquals(statusCode, HTTP_NOT_FOUND);
      }
    };

    httpClient.get("http://baidu.com", null, handler);

  }
}
