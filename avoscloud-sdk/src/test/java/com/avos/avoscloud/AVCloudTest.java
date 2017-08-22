package com.avos.avoscloud;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by wli on 2017/8/4.
 */

public class AVCloudTest {
  @Test
  public void testConvertCloudResponse() {
    String content = "{\"result\":{\"content\":\"2222我若是写代码。\",\"ACL\":{\"*\":{\"read\":true},\"59229e282f301e006b1b637e\":{\"read\":true}},\"number\":123,\"userName\":{\"__type\":\"Pointer\",\"className\":\"_User\",\"objectId\":\"59631dab128fe1507271d9b7\"},\"asdfds\":{\"__type\":\"Pointer\",\"className\":\"_File\",\"objectId\":\"5875e04a61ff4b005c5c28ba\"},\"objectId\":\"56de3e5aefa631005ec03f67\",\"createdAt\":\"2016-03-08T02:52:10.733Z\",\"updatedAt\":\"2017-08-02T10:58:05.630Z\",\"__type\":\"Object\",\"className\":\"Comment\"}}";
    AVObject object = (AVObject) AVCloud.convertCloudResponse(content);

    Assert.assertEquals(object.getClassName(), "Comment");
    Assert.assertEquals(object.get("content"), "2222我若是写代码。");
    Assert.assertEquals(object.getInt("number"), 123);

    Assert.assertEquals(object.getObjectId(), "56de3e5aefa631005ec03f67");
    Assert.assertNotNull(object.getCreatedAt());
    Assert.assertNotNull(object.getUpdatedAt());
    Assert.assertNotNull(object.getACL());
    Assert.assertNotNull(object.get("asdfds"));
  }
}
