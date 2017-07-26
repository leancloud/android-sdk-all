package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

import com.avos.avoscloud.im.v2.AVIMMessage;

/**
 * Created by wli on 2017/6/28.
 */

public class PushServiceParcel implements Parcelable {

  private AVIMMessage oldMessage;
  private AVIMMessage newMessage;

  private AVIMMessage recallMessage;

  public PushServiceParcel() {}

  public PushServiceParcel(Parcel in) {
    oldMessage = in.readParcelable(AVIMMessage.CREATOR.getClass().getClassLoader());
    newMessage = in.readParcelable(AVIMMessage.CREATOR.getClass().getClassLoader());
    recallMessage = in.readParcelable(AVIMMessage.CREATOR.getClass().getClassLoader());
  }


  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(oldMessage, 0);
    dest.writeParcelable(newMessage, 0);
    dest.writeParcelable(recallMessage, 0);
  }

  public static final Creator<PushServiceParcel> CREATOR = new Creator<PushServiceParcel>() {
    @Override
    public PushServiceParcel createFromParcel(Parcel in) {
      return new PushServiceParcel(in);
    }

    @Override
    public PushServiceParcel[] newArray(int size) {
      return new PushServiceParcel[size];
    }
  };

  public AVIMMessage getRecallMessage() {
    return recallMessage;
  }

  public void setRecallMessage(AVIMMessage recallMessage) {
    this.recallMessage = recallMessage;
  }

  public AVIMMessage getOldMessage() {
    return oldMessage;
  }

  public void setOldMessage(AVIMMessage oldMessage) {
    this.oldMessage = oldMessage;
  }

  public AVIMMessage getNewMessage() {
    return newMessage;
  }

  public void setNewMessage(AVIMMessage newMessage) {
    this.newMessage = newMessage;
  }
}
