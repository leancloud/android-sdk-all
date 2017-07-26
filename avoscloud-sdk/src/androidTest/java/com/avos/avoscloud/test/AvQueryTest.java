package com.avos.avoscloud.test;

import com.avos.avoscloud.AVCloudQueryResult;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.test.base.NetworkTestBase;
import com.avos.avoscloud.test.mock.MockCloudQuery;

import junit.framework.Assert;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.HttpResponseGenerator;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

/**
 * Created by zhangxiaobo on 15/3/26.
 */
@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class AvQueryTest extends NetworkTestBase {
  @Test
  public void testCloudQuery() throws Exception {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockCloudQuery.Url.GET, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockCloudQuery.Result.GET, buildJsonHeader());
      }
    });

    AVCloudQueryResult avCloudQueryResult = AVQuery.doCloudQuery(MockCloudQuery.QUERY);
    Assert.assertEquals(MockCloudQuery.Result.GET_SIZE, avCloudQueryResult.getCount());
    Assert.assertEquals(MockCloudQuery.Result.GET_SIZE, avCloudQueryResult.getResults().size());
  }
}
