package com.avos.avoscloud.test.mock;

import java.net.URLEncoder;

/**
 * Created by zhangxiaobo on 15/3/26.
 */
public class MockCloudQuery {
  public static final String QUERY = "select * from _User";

  public static class Url {
    public static final String GET =
        "https://api.leancloud.cn/1.1/cloudQuery?cql=select+*+from+_User";
  }

  public static class Result {
    public static final int GET_SIZE = 7;
    public static final String GET =
        "{\"results\":[{\"updatedAt\":\"2015-02-02T11:04:07.550Z\",\"objectId\":"
            + "\"54cf59a7e4b0fefed5715b8a\",\"username\":\"p4j5jnhi36ug5l1s9px3on2pn\","
            + "\"createdAt\":\"2015-02-02T11:04:07.550Z\",\"likes\":{\"__type\":\"Relation\","
            + "\"className\":\"Post\"},\"emailVerified\":false,\"authData\":{\"anonymous\":{\"id"
            + "\":\"4121b65d-f9e5-4aa5-9ff9-6b65283ee0a7\"}},\"mobilePhoneVerified\":false},"
            + "{\"updatedAt\":\"2015-02-02T11:06:44.197Z\",\"objectId\":\"54cf5a44e4b0fefed57165cb"
            + "\",\"username\":\"n2wdguw6jfyrvdf7ubjnc5wdr\",\"createdAt\":"
            + "\"2015-02-02T11:06:44.197Z\",\"likes\":{\"__type\":\"Relation\",\"className\":"
            + "\"Post\"},\"emailVerified\":false,\"authData\":{\"anonymous\":{\"id\":"
            + "\"7eb9e95a-603c-41f7-b64a-3a146a1685c8\"}},\"mobilePhoneVerified\":false},"
            + "{\"updatedAt\":\"2015-02-10T07:26:22.986Z\",\"objectId\":\"54d9b29ee4b0ba48c0a5deec"
            + "\",\"username\":\"ky00zmq0kwvzpqnn44j1frnzy\",\"createdAt\":"
            + "\"2015-02-10T07:26:22.986Z\",\"likes\":{\"__type\":\"Relation\",\"className\":\"Post"
            + "\"},\"emailVerified\":false,\"authData\":{\"anonymous\":{\"id\":"
            + "\"c430f1be-cb71-4f42-b044-47871056cad2\"}},\"mobilePhoneVerified\":false},{"
            + "\"updatedAt\":\"2015-02-10T07:26:25.710Z\",\"objectId\":\"54d9b2a1e4b0ba48c0a5df0b\""
            + ",\"username\":\"0oi03a5fw83rbn7wghnq6arzq\",\"createdAt\":\"2015-02-10T07:26:25.710Z"
            + "\",\"likes\":{\"__type\":\"Relation\",\"className\":\"Post\"},\"emailVerified\":"
            + "false,\"authData\":{\"anonymous\":{\"id\":\"65d37506-cb3a-4e55-9137-54382814af93\"}}"
            + ",\"mobilePhoneVerified\":false},{\"updatedAt\":\"2015-03-05T11:20:49.539Z\","
            + "\"objectId\":\"54f83c11e4b0c976f0403f7e\",\"username\":\"54ttch7c3ui05hxuubs26xavh\""
            + ",\"createdAt\":\"2015-03-05T11:20:49.539Z\",\"likes\":{\"__type\":\"Relation\","
            + "\"className\":\"Post\"},\"emailVerified\":false,\"authData\":{\"anonymous\":{\"id"
            + "\":\"7f898f16-54bc-4479-bb26-67307c2bea1b\"}},\"mobilePhoneVerified\":false},{\""
            + "updatedAt\":\"2015-03-11T09:19:18.024Z\",\"objectId\":\"54fff1ade4b04d02c7e22b2e\","
            + "\"username\":\"ly4l8xsfsnumj68pfasxs9puy\",\"createdAt\":\"2015-03-11T07:41:33.403Z"
            + "\",\"likes\":{\"__type\":\"Relation\",\"className\":\"Post\"},\"emailVerified\":"
            + "false,\"authData\":{\"weibo\":{\"access_token\":\"2.00J62yDCBiZo5E4a004dad36ppCcfD"
            + "\",\"expires_at\":\"2020-03-09T09:19:14.895Z\",\"uid\":\"1890976635\"}},"
            + "\"mobilePhoneVerified\":false},{\"updatedAt\":\"2015-03-12T06:30:47.835Z\","
            + "\"objectId\":\"54cf556ee4b0fefed5711558\",\"username\":\"xiaobo\",\"createdAt\":"
            + "\"2015-02-02T10:46:06.460Z\",\"likes\":{\"__type\":\"Relation\",\"className\":\"Post"
            + "\"},\"emailVerified\":false,\"authData\":{\"anonymous\":{\"id\":\""
            + "60854727-6554-4d5a-bf08-34c24c7c540c\"}},\"mobilePhoneVerified\":false}],"
            + "\"className\":\"_User\"}";
  }
}
