package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by wli on 2017/6/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class FastJsonTest {

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testFastJson() {
    final String key = "test";
    final String oldValue = "11111";
    final String newValue = "22222";
    AVObject oldObject = new AVObject("fastjson_test");
    oldObject.put(key, oldValue);
    Assert.assertTrue(oldValue.equals(oldObject.get(key)));

    String content = JSON.toJSONString(oldObject);
    AVObject newObject = (AVObject) JSON.parse(content);

    Assert.assertTrue(oldValue.equals(newObject.get(key)));

    newObject.put(key, newValue);
    Assert.assertTrue(newValue.equals(newObject.get(key)));
  }

}
