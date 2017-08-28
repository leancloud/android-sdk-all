package com.avos.avoscloud.im.v2.video;

import android.app.Activity;

import com.avos.avoscloud.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by fengjunwen on 2017/8/25.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVIMVideoCaptureTests {
  @Test
  public void testSystemAppMethod() {
    Activity test = new Activity();
    AVIMVideoCapture.dispatchTakeVideoIntent(test);
  }
}
