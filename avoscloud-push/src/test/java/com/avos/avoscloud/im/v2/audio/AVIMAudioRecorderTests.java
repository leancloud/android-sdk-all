package com.avos.avoscloud.im.v2.audio;

import com.avos.avoscloud.BuildConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by fengjunwen on 2017/8/25.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVIMAudioRecorderTests {
  @Test
  public void testDummy() {
    AVIMAudioRecorder recorder = new AVIMAudioRecorder("./tmp.dat", null);
    Assert.assertNotNull(recorder);
  }
}
