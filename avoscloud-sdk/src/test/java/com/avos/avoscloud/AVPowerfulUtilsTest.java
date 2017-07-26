package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by wli on 2017/7/5.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVPowerfulUtilsTest {
  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testGetEndpoint() {
    final String objectId = "objectId";
    AVUser user = new AVUser();
    Assert.assertEquals(AVUser.AVUSER_ENDPOINT, AVPowerfulUtils.getEndpoint(user));
    user.setObjectId(objectId);
    Assert.assertEquals(String.format("%s/%s", AVUser.AVUSER_ENDPOINT, objectId), AVPowerfulUtils.getEndpoint(user));

    AVStatus status = new AVStatus();
    Assert.assertEquals(AVStatus.STATUS_ENDPOINT, AVPowerfulUtils.getEndpoint(status));
    status.setObjectId(objectId);
    Assert.assertEquals(String.format("%s/%s", AVStatus.STATUS_ENDPOINT, objectId), AVPowerfulUtils.getEndpoint(status));

    AVRole role = new AVRole();
    Assert.assertEquals(AVRole.AVROLE_ENDPOINT, AVPowerfulUtils.getEndpoint(role));
    role.setObjectId(objectId);
    Assert.assertEquals(String.format("%s/%s", AVRole.AVROLE_ENDPOINT, objectId), AVPowerfulUtils.getEndpoint(role));

    // AVFile 没有 fetch
    AVFile file = new AVFile();
    Assert.assertEquals(AVFile.AVFILE_ENDPOINT, AVPowerfulUtils.getEndpoint(file));

    final String NORMAL_OBJECT_NAME = "normalObject";
    AVObject normalObject = new AVObject("normalObject");
    Assert.assertEquals("classes/" + NORMAL_OBJECT_NAME, AVPowerfulUtils.getEndpoint(normalObject));
    normalObject.setObjectId(objectId);
    Assert.assertEquals(String.format("classes/%s/%s", NORMAL_OBJECT_NAME, objectId), AVPowerfulUtils.getEndpoint(normalObject));
  }
}
