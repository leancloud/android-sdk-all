package com.avos.avospush.push;

import com.avos.avoscloud.AVUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by yangchaozhong on 3/14/14.
 */
public class AVConnectivityReceiver extends BroadcastReceiver {

  private final AVConnectivityListener listener;

  public AVConnectivityReceiver(AVConnectivityListener listener) {
    this.listener = listener;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    int status = AVUtils.getConnectivityStatus(context);
    switch (status) {
      case AVUtils.TYPE_MOBILE:
        listener.onMobile(context);
        break;
      case AVUtils.TYPE_WIFI:
        listener.onWifi(context);
        break;
      case AVUtils.TYPE_OTHERS:
        listener.onOtherConnected(context);
        break;
      case AVUtils.TYPE_NOT_CONNECTED:
        listener.onNotConnected(context);
        break;
      default:
        break;
    }
  }
}
