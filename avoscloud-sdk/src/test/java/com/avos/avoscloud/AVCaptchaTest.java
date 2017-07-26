package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;

/**
 * Created by wli on 2017/5/10.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVCaptchaTest {

  private static String APP_ID = "a5CDnmOX94uSth8foK9mjHfq-gzGzoHsz";
  private static String APP_KEY = "Ue3h6la9zH0IxkUJmyhLjk9h";

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, APP_ID, APP_KEY);
  }

  @Test
  public void testRequestCaptchaInBackground() throws AVException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVCaptchaOption option = new AVCaptchaOption();
    option.setWidth(200);
    option.setHeight(100);
    AVCaptcha.requestCaptchaInBackground(option, new AVCallback<AVCaptchaDigest>() {
      @Override
      protected void internalDone0(AVCaptchaDigest avCaptchaDigest, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertNotNull(avCaptchaDigest);
        Assert.assertNotNull(avCaptchaDigest.getNonce());
        Assert.assertNotNull(avCaptchaDigest.getUrl());
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

  /**
   * 因为返回的是一张图片，这里没办法自动化测试，只能手动测试
   */
  @Ignore
  public void testVerifyCaptchaCodeInBackground() throws AVException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVCaptchaDigest digest = new AVCaptchaDigest();
    digest.setNonce("YGYKzr1h");
    AVCaptcha.verifyCaptchaCodeInBackground("owlg", digest, new AVCallback<String>() {
      @Override
      protected void internalDone0(String s, AVException avException) {
        Assert.assertNull(avException);
        Assert.assertNotNull(s);
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

}
