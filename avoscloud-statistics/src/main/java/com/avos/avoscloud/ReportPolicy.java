package com.avos.avoscloud;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/27/13 Time: 4:49 PM
 * http://mta.qq.com/mta/setting/ctr_transmission_strategy?app_id=1
 */
public enum ReportPolicy {
  REALTIME(0), // 实时发送，app每产生一条消息都会发送到服务器, debug only
  BATCH(1), // 批量发送，默认当消息数量达到30条时发送一次。
  SENDDAILY(4), // 每日发送
  SENDWIFIONLY(5), // 仅在WIFI下启动时发送 , debug only.
  SEND_INTERVAL(6), // 间隔一段时间发送，每隔一段时间一次性发送到服务器。30 seconds
  SEND_ON_EXIT(7); // 只在启动时发送，本次产生的所有数据在下次启动时发送。

  private int value = 0;

  private ReportPolicy(int value) {
    this.value = value;
  }

  public static ReportPolicy valueOf(int value) {
    switch (value) {
      case 0:
        return REALTIME;
      case 1:
        return BATCH;
      case 4:
        return SENDDAILY;
      case 5:
        return SENDWIFIONLY;
      case 6:
        return SEND_INTERVAL;
      case 7:
        return SEND_ON_EXIT;
      default:
        return null;
    }
  }

  public int value() {
    return this.value;
  }
}
