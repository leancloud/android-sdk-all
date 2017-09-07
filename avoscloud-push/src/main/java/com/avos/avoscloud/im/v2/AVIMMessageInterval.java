package com.avos.avoscloud.im.v2;

/**
 * Created by fengjunwen on 2017/9/5.
 */

public class AVIMMessageInterval {
  public AVIMMessageIntervalBound startIntervalBound;
  public AVIMMessageIntervalBound endIntervalBound;

  public static class AVIMMessageIntervalBound {
    public String messageId;
    public long timestamp;
    public boolean closed;
    private AVIMMessageIntervalBound(String messageId, long timestamp, boolean closed) {
      this.messageId = messageId;
      this.timestamp = timestamp;
      this.closed = closed;
    }
  }

  public static AVIMMessageIntervalBound createBound(String messageId, long timestamp, boolean closed) {
    return new AVIMMessageIntervalBound(messageId, timestamp, closed);
  }

  public AVIMMessageInterval(AVIMMessageIntervalBound start, AVIMMessageIntervalBound end) {
    this.startIntervalBound = start;
    this.endIntervalBound = end;
  }
}
