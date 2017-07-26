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
public class AVObjectTest {

  private static final String TEST_TABLE_NAME = "AVObjectTest";
  private static final String TEST_ORDINARY_KEY = "ordinary_key";
  private static final String TEST_ORDINARY_VALUE = "ordinary_value";

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testAVObjectSave() throws AVException {
    AVObject object = new AVObject(TEST_TABLE_NAME);
    object.put(TEST_ORDINARY_KEY, TEST_ORDINARY_VALUE);
    object.save();
    Assert.assertNotNull(object.getObjectId());
    Assert.assertEquals(object.get(TEST_ORDINARY_KEY), TEST_ORDINARY_VALUE);
  }

  @Test
  public void testAVObjectFetch() throws AVException {
    AVObject object = new AVObject(TEST_TABLE_NAME);
    object.put(TEST_ORDINARY_KEY, TEST_ORDINARY_VALUE);
    object.save();

    AVObject objectNeedFetch = AVObject.createWithoutData(TEST_TABLE_NAME, object.getObjectId());
    objectNeedFetch.fetch();
    Assert.assertEquals(objectNeedFetch.getObjectId(), object.getObjectId());
    Assert.assertEquals(objectNeedFetch.get(TEST_ORDINARY_KEY), object.get(TEST_ORDINARY_KEY));
  }

  @Test
  public void testAVObjectUpdate() throws AVException {
    AVObject object = new AVObject(TEST_TABLE_NAME);
    object.put(TEST_ORDINARY_KEY, TEST_ORDINARY_VALUE);
    object.save();

    final String newValue = "newValue";
    object.put(TEST_ORDINARY_KEY, newValue);
    object.save();
    Assert.assertNotNull(object.getObjectId());
    Assert.assertEquals(object.get(TEST_ORDINARY_KEY), newValue);
  }
}
