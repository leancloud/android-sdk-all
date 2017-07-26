package com.avos.avospush.push;

import android.content.Context;

/**
 * Created by yangchaozhong on 3/14/14.
 */
public interface AVConnectivityListener {
  public void onMobile(Context context);

  public void onWifi(Context context);

  public void onNotConnected(Context context);
}
