package com.avos.avoscloud;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wli on 2017/5/10.
 * 这里需要手机接收短信，所以只能手动测试
 */
@Ignore
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVSMSTest {

  private static final String PHONE = "19900000000";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String TEMPLATE_NAME = "templateName";
  private static final String OPERATION = "operation";
  private static final String CODE = "code";
  private static final String SIGNATURE_NAME = "signatureName";
  private static int TTL = 100;
  private static final Map<String, Object> ENV_MAP = new HashMap<>();

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
    ENV_MAP.put("key1", "value1");
  }

  @Test
  public void testRequestSMSCode() throws AVException {
    AVOSCloud.requestSMSCode(PHONE);
  }

  @Test
  public void testRequestSMSCode1() throws AVException {
    AVOSCloud.requestSMSCode(PHONE, APPLICATION_NAME, OPERATION, TTL);
  }

  @Test
  public void testRequestSMSCode2() throws AVException {
    AVOSCloud.requestSMSCode(PHONE, TEMPLATE_NAME, ENV_MAP);
  }

  @Test
  public void testRequestSMSCode3() throws AVException {
    AVOSCloud.requestSMSCode(PHONE, TEMPLATE_NAME, SIGNATURE_NAME, ENV_MAP);
  }

  @Test
  public void testRequestSMSCodeInBackground() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.requestSMSCodeInBackground(PHONE, APPLICATION_NAME, OPERATION, TTL, new RequestMobileCodeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testRequestSMSCodeInBackground1() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.requestSMSCodeInBackground(PHONE, TEMPLATE_NAME, ENV_MAP, new RequestMobileCodeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testRequestSMSCodeInBackground2() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.requestSMSCodeInBackground(PHONE, TEMPLATE_NAME, SIGNATURE_NAME, ENV_MAP, new RequestMobileCodeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testRequestSMSCodeInBackground3() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.requestSMSCodeInBackground(PHONE, new RequestMobileCodeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testRequestVoiceCode() throws AVException {
    AVOSCloud.requestVoiceCode(PHONE);
  }

  @Test
  public void testRequestVoiceCodeInBackground() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.requestVoiceCodeInBackground(PHONE, new RequestMobileCodeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testVerifySMSCode() throws AVException {
    AVOSCloud.verifySMSCode(CODE, PHONE);
  }

  @Test
  public void testVerifyCode() throws AVException {
    AVOSCloud.verifyCode(CODE, PHONE);
  }

  @Test
  public void testVerifySMSCodeInBackground() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.verifySMSCodeInBackground(CODE, PHONE, new AVMobilePhoneVerifyCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test
  public void testVerifyCodeInBackground() throws AVException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    AVOSCloud.verifyCodeInBackground(CODE, PHONE, new AVMobilePhoneVerifyCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
      }
    });
    latch.await();
  }
}
