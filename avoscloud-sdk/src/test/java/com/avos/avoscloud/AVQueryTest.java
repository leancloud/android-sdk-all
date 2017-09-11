package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by fengjunwen on 2017/9/11.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVQueryTest {
  @Test
  public void testCloneMethod() {
    AVQuery<AVObject> query = new AVQuery<>("Person");
    AVQuery newQuery = query.clone();
    Assert.assertNotNull(newQuery);
    Assert.assertTrue("Person".equals(newQuery.getClassName()));
  }
}
