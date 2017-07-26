package com.avos.avoscloud.test.mock;

/**
 * Created by zhangxiaobo on 15/2/9.
 */
public class MockObject {
  private MockObject() {}

  public static final String OBJECT_ID = "54d8972ee4b0ba48c09be767";
  public static final String CLASS_NAME = "GameScore";
  public static final String TEST_KEY = "test";
  public static final String TEST_VALUE = "hello world";

  public static class Url {
    public static final String BASE = "https://api.leancloud.cn/1.1/classes/GameScore/";
    public static final String REST = BASE + "54d8972ee4b0ba48c09be767";
  }

  public class Result {

    public static final String BATCH_SAVE =
        "{\"%s\":{\"createdAt\":"
            + "\"2015-03-05T11:28:18.260Z\",\"objectId\":\"54d8972ee4b0ba48c09be767\"}}";
    public static final String QUERY =
        "{\"createdAt\":\"2015-02-09T11:17:02.387Z\",\"updatedAt\":"
            + "\"2015-02-09T11:17:02.387Z\",\"objectId\":\"54d8972ee4b0ba48c09be767\",\"test\":"
            + "\"hello world\"}";
    public static final String FETCH =
        "{\"score\":1200,\"playerNames\":\"steve\",\"history\":"
            + "[1000,1100],\"keyByte\":{\"__type\":\"Bytes\",\"base64\":\"amFjb2I=\"},\"birthday\":"
            + "{\"iso\":\"1986-02-27T08:52:08.684Z\",\"__type\":\"Date\"},\"pie\":3.14,\"winner\":"
            + "{\"name\":\"jacob\",\"sex\":\"male\"},\"game\":"
            + "{\"__type\":\"Pointer\",\"className\":\"Game\",\"objectId\":"
            + "\"54fd7b55e4b08f77533f2d86\"},\"createdAt\":\"2015-03-09T10:08:38.477Z\",\"updatedAt"
            + "\":\"2015-03-09T10:08:38.477Z\",\"objectId\":\"54d8972ee4b0ba48c09be767\"}";
    public static final String FETCH_LIST =
        "{\"score\":1200,\"history\":[1000,1100],\"games\":[{\"name\":\"COC\",\"createdAt\":"
            + "\"2015-03-16T11:07:31.224Z\",\"updatedAt\":\"2015-03-16T11:07:31.224Z\","
            + "\"objectId\":\"5506b973e4b03b1912ea4d1e\",\"__type\":\"Pointer\",\"className\":"
            + "\"Game\"},{\"name\":\"candy crush\",\"createdAt\":\"2015-03-16T11:07:31.536Z\","
            + "\"updatedAt\":\"2015-03-16T11:07:31.536Z\",\"objectId\":\"5506b973e4b03b1912ea4d25\""
            + ",\"__type\":\"Pointer\",\"className\":\"Game\"},{\"name\":\"sims\",\"createdAt\":"
            + "\"2015-03-16T11:07:31.812Z\",\"updatedAt\":\"2015-03-16T11:07:31.812Z\",\"objectId\""
            + ":\"5506b973e4b03b1912ea4d2a\",\"__type\":\"Pointer\",\"className\":\"Game\"}],"
            + "\"createdAt\":\"2015-03-09T10:08:38.477Z\",\"updatedAt"
            + "\":\"2015-03-09T10:08:38.477Z\",\"objectId\":\"54d8972ee4b0ba48c09be767\"}";

    public static final String FETCH_INCLUDE = "{\"score\":1200,\"playerNames\":\"steve\","
        + "\"history\":[1000,1100],\"game\":{\"name\":\"COC\",\"createdAt\":"
        + "\"2015-03-09T10:52:05.595Z\",\"updatedAt\":\"2015-03-09T10:52:05.595Z\",\"objectId\""
        + ":\"54fd7b55e4b08f77533f2d86\",\"__type\":\"Pointer\",\"className\":\"GameScore\"},"
        + "\"createdAt\":\"2015-03-09T10:52:05.609Z\",\"updatedAt\":\"2015-03-09T10:52:05.609Z"
        + "\",\"objectId\":\"54d8972ee4b0ba48c09be767\"}";
  }
}
