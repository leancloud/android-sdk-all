package com.avos.avoscloud.im.v2;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.BuildConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by fengjunwen on 2017/8/18.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "AndroidManifest.xml", sdk = 23)
public class AVIMMessageStorageTests {
  private String defaultClient = "ThisIsATestUserA";
  @Before
  public void setup() {
    AVOSCloud.applicationContext = new Activity();
  }
  @Test
  public void testDummy() {
    long currentThreadId = Thread.currentThread().getId();
    long mainThreadId = Looper.getMainLooper().getThread().getId();
    System.out.println("mainThreadId=" + mainThreadId + ", currentThreadId=" + currentThreadId);
    AVIMMessageStorage.DBHelper helper = new AVIMMessageStorage.DBHelper(AVOSCloud.applicationContext, defaultClient);
    Assert.assertNotNull(helper);
    helper.getReadableDatabase().execSQL("select * from messages");
    AVIMMessageStorage storage = AVIMMessageStorage.getInstance(defaultClient);
    Assert.assertNotNull(storage);
  }
}
