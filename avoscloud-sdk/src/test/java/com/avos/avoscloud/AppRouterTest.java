package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;

/**
 * Created by wli on 2017/4/20.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AppRouterTest {




  @Test
  public void testUCloudFetchRouter() throws Exception {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_UCLOUD_APP_ID, TestConfig.TEST_UCLOUD_APP_KEY);

    syncFetchRouter();

    String server = "https://%s.%s.lncld.net";
    String appIdPrefix = TestConfig.TEST_UCLOUD_APP_ID.substring(0, 8);
    Assert.assertTrue(AppRouterManager.getInstance().getStorageServer().equals(String.format(server, appIdPrefix, "api")));
    Assert.assertTrue(AppRouterManager.getInstance().getRtmRouterServer().equals(String.format(server, appIdPrefix, "rtm")));
    Assert.assertTrue(AppRouterManager.getInstance().getStatsServer().equals(String.format(server, appIdPrefix, "stats")));
    Assert.assertTrue(AppRouterManager.getInstance().getEngineServer().equals(String.format(server, appIdPrefix, "engine")));

    testAVObjectSave();
  }

  @Test
  public void testQCloudFetchRouter() throws Exception {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_QCLOUD_APP_ID, TestConfig.TEST_QCLOUD_APP_KEY);
    syncFetchRouter();

    Assert.assertTrue(AppRouterManager.getInstance().getStorageServer().equals("https://e1-api.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getRtmRouterServer().equals("https://router-q0-push.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getStatsServer().equals("https://e1-api.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getEngineServer().equals("https://e1-api.leancloud.cn"));
    testAVObjectSave();
  }

  @Test
  public void testUSFetchRouter() throws Exception {
    AVOSCloud.useAVCloudUS();
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_US_APP_ID, TestConfig.TEST_US_APP_KEY);

    syncFetchRouter();

    Assert.assertTrue(AppRouterManager.getInstance().getStorageServer().equals("https://us-api.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getRtmRouterServer().equals("https://router-a0-push.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getStatsServer().equals("https://us-api.leancloud.cn"));
    Assert.assertTrue(AppRouterManager.getInstance().getEngineServer().equals("https://us-api.leancloud.cn"));
    testAVObjectSave();
  }

  private void syncFetchRouter() {
    final CountDownLatch latch = new CountDownLatch(1);
    AppRouterManager.getInstance().fetchRouter(true, new AVCallback() {
      @Override
      protected void internalDone0(Object o, AVException avException) {
        Assert.assertNull(avException);
        latch.countDown();
      }

      @Override
      protected boolean mustRunOnUIThread() {
        return false;
      }
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  private void testAVObjectSave() {
    AVObject object = new AVObject("justTest");
    object.put("key1", "value1");
    try {
      object.save();
      Assert.assertNotNull(object.getObjectId());
    } catch (AVException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSetServer() {
    final String testAPIServer = "testAPIServer";
    final String testRtmServer = "testRtmServer";
    final String testPushServer = "testPushServer";
    final String testEngineServer = "testEngineServer";
    final String testStatsServer = "testStatsServer";

    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.API, testAPIServer);
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.RTM, testRtmServer);
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.PUSH, testPushServer);
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.ENGINE, testEngineServer);
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.STATS, testStatsServer);

    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_UCLOUD_APP_ID, TestConfig.TEST_UCLOUD_APP_KEY);

    Assert.assertEquals(testEngineServer, AppRouterManager.getInstance().getEngineServer());
    Assert.assertEquals(testPushServer, AppRouterManager.getInstance().getPushServer());
    Assert.assertEquals(testRtmServer, AppRouterManager.getInstance().getRtmRouterServer());
    Assert.assertEquals(testStatsServer, AppRouterManager.getInstance().getStatsServer());
    Assert.assertEquals(testAPIServer, AppRouterManager.getInstance().getStorageServer());
  }
}