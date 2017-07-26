package com.avos.avoscloud.feedback.test.mock;

/**
 * Created by zhangxiaobo on 15/3/25.
 */
public class MockFeedbackThread {
  private MockFeedbackThread() {}

  public static final String OBJECT_ID = "55122ba7e4b0e3088f997550";

  public static class Url {
    public static final String FEEDBACK = "https://api.leancloud.cn/1.1/feedback";
    public static final String THREAD = "https://api.leancloud.cn/1.1/feedback/" + OBJECT_ID
        + "/threads";
  }

  public static class Result {
    public static final String GET =
        "{\"results\":[{\"content\":\"" + MockComment.CONTENT
            + "\",\"type\":\"user\",\"feedback\":{\"__type\":\"Pointer\",\"className\":"
            + "\"UserFeedback\",\"objectId\":\"55122ba7e4b0e3088f997550\"},\"createdAt\":"
            + "\"2015-03-25T03:38:06.960Z\",\"updatedAt\":\"2015-03-25T03:38:06.960Z\",\""
            + "objectId\":\"55122d9ee4b0e3088f998774\"}]}";
    public static final String POST_FEEDBACK =
        "{\"createdAt\":\"2015-03-25T03:29:43.871Z\",\"objectId\":\"" + OBJECT_ID + "\"}";
  }
}
