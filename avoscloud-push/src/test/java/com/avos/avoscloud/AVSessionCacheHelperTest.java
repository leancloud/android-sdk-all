package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

/**
 * Created by wli on 2017/6/15.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVSessionCacheHelperTest {

  String testClientId = "clientId";
  String testClientTag = "tag";

  @Before
  public void initAvos() {

    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testRemoveNotExistTag() {
    AVSessionCacheHelper.getTagCacheInstance().addSession(testClientId, testClientTag);
    AVSessionCacheHelper.getTagCacheInstance().removeSession("test11");
    Map<String, String> map = AVSessionCacheHelper.getTagCacheInstance().getAllSession();
    Assert.assertTrue(map.size() == 1);
    Assert.assertTrue(testClientTag.equals(map.get(testClientId)));
  }

  @Test
  public void testRemoveExistTag() {
    AVSessionCacheHelper.getTagCacheInstance().addSession(testClientId, testClientTag);
    AVSessionCacheHelper.getTagCacheInstance().removeSession(testClientId);
    Map<String, String> map = AVSessionCacheHelper.getTagCacheInstance().getAllSession();
    Assert.assertTrue(map.size() == 0);
  }

  @Test
  public void testTagCacheAdd() {
    AVSessionCacheHelper.getTagCacheInstance().addSession(testClientId, testClientTag);
    Map<String, String> clientMap = AVSessionCacheHelper.getTagCacheInstance().getAllSession();
    Assert.assertTrue(clientMap.size() == 1);
    Assert.assertTrue(testClientTag.equals(clientMap.get(testClientId)));
  }

  @Test
  public void testUpdateTag() {
    String updateTag = "updateTag";
    AVSessionCacheHelper.getTagCacheInstance().addSession(testClientId, testClientTag);
    AVSessionCacheHelper.getTagCacheInstance().addSession(testClientId, updateTag);
    Map<String, String> clientMap = AVSessionCacheHelper.getTagCacheInstance().getAllSession();
    Assert.assertTrue(clientMap.size() == 1);
    Assert.assertTrue(updateTag.equals(clientMap.get(testClientId)));
  }
}
