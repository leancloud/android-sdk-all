package com.avos.avoscloud.im.v2;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by wli on 16/9/19.
 */
public class AVIMMessageOption implements Parcelable {

  /**
   * 消息等级
   */
  private MessagePriority priority = null;

  /**
   * 是否为暂态消息，默认值为 false
   */
  private boolean isTransient = false;

  /**
   * 是否需要回执，默认为 false
   */
  private boolean isReceipt = false;

  /**
   * 是否设置该消息为下线通知消息
   */
  private boolean isWill = false;

  private String pushData;

  public AVIMMessageOption() {

  }

  public void setPriority(MessagePriority priority) {
    this.priority = priority;
  }

  public MessagePriority getPriority() {
    return priority;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean isTransient) {
    this.isTransient = isTransient;
  }

  public boolean isReceipt() {
    return isReceipt;
  }

  public void setReceipt(boolean receipt) {
    isReceipt = receipt;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(null != priority ? priority.getNumber() : -1);
    out.writeInt(isTransient ? 1 : 0);
    out.writeInt(isReceipt ? 1 : 0);
    out.writeString(pushData);
  }


  public AVIMMessageOption(Parcel in) {
    priority = MessagePriority.getProiority(in.readInt());
    isTransient = in.readInt() == 1;
    isReceipt = in.readInt() == 1;
    pushData = in.readString();
  }

  public static final Creator<AVIMMessageOption> CREATOR = new Creator<AVIMMessageOption>() {
    @Override
    public AVIMMessageOption createFromParcel(Parcel in) {
      return new AVIMMessageOption(in);
    }

    @Override
    public AVIMMessageOption[] newArray(int size) {
      return new AVIMMessageOption[size];
    }
  };

  public String getPushData() {
    return pushData;
  }

  public void setPushData(String pushData) {
    this.pushData = pushData;
  }

  public boolean isWill() {
    return isWill;
  }

  /**
   * 设置该消息是否为下线通知消息
   * @param will 若为 true 的话，则为下线通知消息
   */
  public void setWill(boolean will) {
    isWill = will;
  }

  /**
   * 消息优先级的枚举，仅针对聊天室有效
   */
  public enum MessagePriority {
    High(1),
    Normal(2),
    Low(3);

    private int priorityIndex;

    public static MessagePriority getProiority(int index) {
      switch (index) {
        case 1:
          return High;
        case 2:
          return Normal;
        case 3:
          return Low;
        default:
          return null;
      }
    }

    MessagePriority(int priority) {
      this.priorityIndex = priority;
    }

    public int getNumber() {
      return priorityIndex;
    }
  }
}
