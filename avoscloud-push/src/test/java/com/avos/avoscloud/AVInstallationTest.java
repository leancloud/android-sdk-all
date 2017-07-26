package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by wli on 2017/7/6.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVInstallationTest {

  private static final String TEST_ORDINARY_KEY = "ordinary_key";
  private static final String TEST_ORDINARY_VALUE = "ordinary_value";

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testEndPoint() {
    final String objectId = "objectId";
    AVInstallation installation = new AVInstallation();
    Assert.assertEquals(AVInstallation.AVINSTALLATION_ENDPOINT, AVPowerfulUtils.getEndpoint(installation));
    installation.setObjectId(objectId);
    Assert.assertEquals(String.format("%s/%s", AVInstallation.AVINSTALLATION_ENDPOINT, objectId), AVPowerfulUtils.getEndpoint(installation));
  }

  @Test
  public void testAVInstallationUpdate() throws AVException {
    AVInstallation currentInstallation = AVInstallation.getCurrentInstallation();
    currentInstallation.put(TEST_ORDINARY_KEY, TEST_ORDINARY_VALUE);
    currentInstallation.save();

    Assert.assertNotNull(currentInstallation.getObjectId());
    Assert.assertEquals(currentInstallation.get(TEST_ORDINARY_KEY), TEST_ORDINARY_VALUE);

    currentInstallation.put(TEST_ORDINARY_KEY, "newValue");
    currentInstallation.save();
    Assert.assertNotNull(currentInstallation.getObjectId());
    Assert.assertEquals(currentInstallation.get(TEST_ORDINARY_KEY), "newValue");
  }

  @Test
  public void testAVInstallationFetch() throws AVException {
    AVInstallation currentInstallation = AVInstallation.getCurrentInstallation();
    currentInstallation.save();
    currentInstallation.fetch();
    Assert.assertTrue(!currentInstallation.containsKey(TEST_ORDINARY_KEY));
    Assert.assertNotNull(currentInstallation.getObjectId());

    AVObject object = AVObject.createWithoutData("_Installation", currentInstallation.getObjectId());
    object.put(TEST_ORDINARY_KEY, TEST_ORDINARY_VALUE);
    object.save();

    currentInstallation.fetch();
    Assert.assertEquals(currentInstallation.get(TEST_ORDINARY_KEY), TEST_ORDINARY_VALUE);
  }

  @Test
  public void testAVInstallationDelete() throws AVException {
    final AVInstallation currentInstallation = AVInstallation.getCurrentInstallation();
    currentInstallation.put("remove", "123");
    currentInstallation.save();

    currentInstallation.remove("remove");
    currentInstallation.save();

    AVObject object = AVObject.createWithoutData("_Installation", currentInstallation.getObjectId());
    object.fetch();
    Assert.assertNull(object.get("remove"));
  }
}
