package com.avos.avoscloud.im.v2;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Array;

/**
 * Created by lbt05 on 1/6/16.
 */
public class AVIMMessageCreator<T extends AVIMMessage> implements Parcelable.Creator<T> {

  Class mClazz;

  public AVIMMessageCreator(Class<T> clazz) {
    this.mClazz = clazz;
  }

  @Override
  public T createFromParcel(Parcel source) {
    AVIMMessage message = new AVIMMessage(source);
    message = AVIMMessageManager.parseTypedMessage(message);
    return (T) message;
  }

  @Override
  public T[] newArray(int size) {
    return (T[]) Array.newInstance(mClazz, size);
  }
}
