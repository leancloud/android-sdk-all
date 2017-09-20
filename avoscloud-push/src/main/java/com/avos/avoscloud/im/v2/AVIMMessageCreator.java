package com.avos.avoscloud.im.v2;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

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
    AVIMMessage message = null;
    try {
      Constructor<T> ctor = mClazz.getDeclaredConstructor(Parcel.class);
      message = ctor.newInstance(source);
    } catch (Exception ex) {
      message = new AVIMMessage(source);
    }
    message = AVIMMessageManager.parseTypedMessage(message);
    return (T) message;
  }

  @Override
  public T[] newArray(int size) {
    return (T[]) Array.newInstance(mClazz, size);
  }
}
