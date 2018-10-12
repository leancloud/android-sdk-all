package com.avos.avospush.push;

import android.content.Context;

/**
 * Created by yangchaozhong on 3/14/14.
 */
public interface AVConnectivityListener {
  void onMobile(Context context);
  void onWifi(Context context);
  void onOtherConnected(Context context);
  void onNotConnected(Context context);
}
