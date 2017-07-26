package com.avos.avoscloud.test.mock;

/**
 * Created by zhangxiaobo on 15/3/2.
 */
public class MockUser {
  public static final String NAME = "xiaobo";
  public static final String PASSWORD = "123456";
  public static final String NICKNAME = "jacob";

  public static class Result {
    public static final String LOGIN =
        "{\"sessionToken\":\"4mg9cvexc1mp10y1yqzn0z9p9\",\"updatedAt\":\"2015-02-13T08:09:09.253Z\""
            + ",\"objectId\":\"54cf556ee4b0fefed5711558\",\"username\":\"xiaobo\",\"createdAt\":"
            + "\"2015-02-02T10:46:06.460Z\",\"emailVerified\":false,\"authData\":{\"anonymous\":"
            + "{\"id\":\"60854727-6554-4d5a-bf08-34c24c7c540c\"}},\"mobilePhoneVerified\":false}";
  }
}
