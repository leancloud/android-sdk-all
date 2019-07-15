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
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.API, "avoscloud.com");
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.ENGINE, "avoscloud.com");
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.PUSH, "avoscloud.com");
    AVOSCloud.setServer(AVOSCloud.SERVER_TYPE.RTM, "avoscloud.com");
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  @Test
  public void testAVObjectDeserialize() {
    String data = "{\n" +
        "        \"image\": {\n" +
        "                \"bucket\": \"xtuccgoj\",\n" +
        "                \"metaData\": {\n" +
        "                        \"owner\": \"unknown\",\n" +
        "                        \"size\": 12382\n" +
        "                },\n" +
        "                \"createdAt\": \"2019-06-22T07:18:09.584Z\",\n" +
        "                \"mime_type\": \"image/jpeg\",\n" +
        "                \"__type\": \"File\",\n" +
        "                \"name\": \"shop_vip_qq.jpg\",\n" +
        "                \"url\": \"http://file2.i7play.com/8de5fdb6cf3f8e91010f/shop_vip_qq.jpg\",\n" +
        "                \"objectId\": \"5d0dd631eaa375007402d28a\",\n" +
        "                \"updatedAt\": \"2019-06-22T07:18:09.584Z\"\n" +
        "        },\n" +
        "        \"createdAt\": \"2019-06-22T06:48:28.395Z\",\n" +
        "        \"__type\": \"Object\",\n" +
        "        \"name\": \"腾讯会员月卡\",\n" +
        "        \"end\": \"-1\",\n" +
        "        \"className\": \"ShopItem\",\n" +
        "        \"type\": 1,\n" +
        "        \"value\": 19000,\n" +
        "        \"objectId\": \"5d0dcf3c43e78c0073024a19\",\n" +
        "        \"updatedAt\": \"2019-06-22T12:11:50.924Z\"\n" +
        "}";
    AVObject obj = JSON.parseObject(data, AVObject.class);
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
