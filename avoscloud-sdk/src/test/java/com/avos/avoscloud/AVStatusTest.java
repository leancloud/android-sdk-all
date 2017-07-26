package com.avos.avoscloud;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;

/**
 * Created by wli on 2017/4/27.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVStatusTest {

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testResetUnreadStatusesCount() throws AVException, InterruptedException {
    AVUser user = new AVUser();
    user.logIn(TestConfig.TEST_USER_NAME, TestConfig.TEST_USER_PWD);
    final CountDownLatch latch = new CountDownLatch(1);
    AVStatus.resetUnreadStatusesCount("private", new AVCallback() {
      @Override
      protected void internalDone0(Object o, AVException avException) {
        latch.countDown();
        Assert.assertNull(avException);
      }
    });
    latch.await();
  }
}
