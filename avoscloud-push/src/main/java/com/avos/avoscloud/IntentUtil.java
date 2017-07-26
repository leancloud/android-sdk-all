package com.avos.avoscloud;

import android.content.Intent;
import android.os.Build;

public class IntentUtil {
  // http://stackoverflow.com/questions/9783704/broadcast-receiver-onreceive-never-called
  public static Intent setupIntentFlags(Intent i) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
      i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    }
    return i;
  }
}
