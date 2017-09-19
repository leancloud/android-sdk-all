package com.avos.avoscloud;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by fengjunwen on 2017/9/19.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVUtilsBase64Test {
  @Test
  public void testDecode() {
    String data = "k7H16jjMiHfll6UuAoMK/OilCZc=";
    byte[] decodeData = AVUtils.base64Decode(data);//.decodeBase64(data);
  }
}
