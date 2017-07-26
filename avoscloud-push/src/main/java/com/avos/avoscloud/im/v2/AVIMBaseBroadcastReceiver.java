package com.avos.avoscloud.im.v2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;

/**
 * Created by lbt05 on 3/19/15.
 */
public abstract class AVIMBaseBroadcastReceiver extends BroadcastReceiver {
  AVCallback callback;

  public AVIMBaseBroadcastReceiver(AVCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      Throwable error = null;
      if (null != intent && null != intent.getExtras() && intent.getExtras().containsKey(Conversation.callbackExceptionKey)) {
        error = (Throwable) intent.getExtras().getSerializable(Conversation.callbackExceptionKey);
      }
      execute(intent, error);
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext)
          .unregisterReceiver(this);
    } catch (Exception e) {
      if (callback != null) {
        callback.internalDone(null, new AVException(e));
      }
    }
  }

  public abstract void execute(Intent intent, Throwable error);
}
