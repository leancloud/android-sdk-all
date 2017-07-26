package com.avos.avoscloud.test.mock;

import android.os.Environment;

/**
 * Created by zhangxiaobo on 15/2/6.
 */
public class MockFile {
  private MockFile() {}

  public static final String BUCKET = "vglxjn9o";
  public static final String OBJECT_ID = "54d3046ae4b0a342000cac34";
  public static final String CONTENT = "hello world";
  public static final int SIZE = 11;
  public static final String MD5 = "5eb63bbbe01eeed093cb22bb8f5acdc3";
  public static final String UPLOAD_KEY = "GbPx7p8VoOp8ofGFzJQ2Hm8oJxSBvjkUV5AqgKAb";
  public static final String NAME = "abc.txt";
  public static final String PATH = Environment.getExternalStorageDirectory() + "/" + NAME;
  public static final String URL = "http://ww1.sinaimg.cn/large/57361f0cjw1e36pys52zij.jpg";

  public static class Url {
    public static final String GET =
        "http://ac-vglxjn9o.clouddn.com/0vDrdKtV8oVrpSBwgBn7dbo4gMBGSaKrSCVGWRCW.jpg";
    public static final String QINIU = "https://api.leancloud.cn/1.1/qiniu";
    public static final String Files = "https://api.leancloud.cn/1.1/files";
    public static final String THUMBNAILL = GET + "?imageView/1/w/200/h/100/q/100/format/png";
    public static final String QINIU_HOST = "http://upload.qiniu.com";
    public static final String QINIU_CREATE_BLOCK = String.format(QINIU_HOST + "/mkblk/%d", SIZE);
    public static final String QINIU_MKFILE = String.format(QINIU_HOST + "/mkfile/%d/key/", SIZE);
    public static final String DELETE =
        "https://api.leancloud.cn/1.1/classes/_File/54d3046ae4b0a342000cac34";
  }

  public static class Result {
    // http response content
    public static final String GET =
        "{\"mime_type\":\"image\\/jpeg\",\"updatedAt\":\"2015-02-05T05:49:30.246Z\","
            + "\"key\":\"0vDrdKtV8oVrpSBwgBn7dbo4gMBGSaKrSCVGWRCW.jpg\",\"name\":\"dongtai0.jpg\","
            + "\"objectId\":\"54d3046ae4b0a342000cac34\",\"createdAt\":\"2015-02-05T05:49:30.246Z\""
            + ",\"__type\":\"File\",\"url\":"
            + "\"http:\\/\\/ac-vglxjn9o.clouddn.com\\/"
            + "0vDrdKtV8oVrpSBwgBn7dbo4gMBGSaKrSCVGWRCW.jpg\","
            + "\"metaData\":{\"_checksum\":\"5eb63bbbe01eeed093cb22bb8f5acdc3\","
            + "\"_name\":\"dongtai0.jpg\",\"owner\":\"54d8972ee4b0ba48c09be767\",\"size\":11},"
            + "\"bucket\":\"vglxjn9o\"}";
    public static final String SAVE =
        "{\"bucket\":\"1ha2c8py\",\"url\":"
            + "\"http:\\/\\/ac-1ha2c8py.clouddn.com\\/D4lv0oGZTcLpsIGG0dOrgFadPSRSrMgVeH9yJ2Gw\","
            + "\"token\":\"w6ZYeC-arS2makzcotrVJGjQvpsCQeHcPseFRDzJ:p8L9TIT35H3CpZycsvdX8y7veP8=:"
            + "eyJzY29wZSI6IjFoYTJjOHB5IiwiaW5zZXJ0T25seSI6MSwiZGVhZGxpbmUiOjE0MjM1Mzg1Njl9\","
            + "\"createdAt\":\"2015-02-10T03:12:49.572Z\","
            + "\"objectId\":\"54d97731e4b0ba48c0a33602\"}";
    public static final String UPLOAD =
        "{\"hash\":\"FiqubDXJT8-0FdvpX0CLnOke6Ebt\","
            + "\"key\":\"%s\"}";
    public static final String CREATE_BLOCK =
        "{\"ctx\":\"XBItlB_I1HEDlJkljVkju3PQyXRWlafUvnuYHy-C3UIAASNFZ4mrze_-3LqYdlQyEPDh0sNoZWxsb"
            + "yB3b3JsZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "CwAAAAsAAAAAAAAAMwoAABxN2FQLAAAACwAAAMCoAbuSEwIAfwD_____AQA=\",\"checksum\":"
            + "\"Kq5sNclPz7QV2-lfQIuc6R7oRu0=\",\"crc32\":222957957,\"offset\":11,\"host\":"
            + "\"http://upload.qiniu.com\"}";
    public static final String METADATA_WIDTH = "\"width\":100";
    public static final String METADATA_HEIGHT = "\"height\":200";
  }
}
