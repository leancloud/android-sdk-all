package com.avos.avoscloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA. User: tangxiaomin Date: 4/12/13 Time: 6:41 PM
 */
public class AVBroadcastReceiver extends BroadcastReceiver {


  @Override
  public void onReceive(Context context, Intent intent) {
    // intent完全有可能是null的情况，就太糟糕了
    // 难道刚刚开机的时候移动ISP还没有识别出来的时候就不去尝试连接了么？
    // if (AVUtils.isConnected(context)) {
    try {
      context.startService(new Intent(context, com.avos.avoscloud.PushService.class));
    } catch (Exception ex) {
      LogUtil.log.e("failed to start PushService. cause: " + ex.getMessage());
    }
    // }
  }
}
