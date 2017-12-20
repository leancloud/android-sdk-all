package com.avos.avoscloud.im.v2.callback;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fengjunwen on 2017/12/20.
 */

public class AVIMConversationFailure implements Parcelable {
  String reason = "";
  int code = 0;
  List<String> memberIds = null;

  public AVIMConversationFailure() {
    ;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public List<String> getMemberIds() {
    return memberIds;
  }

  public void setMemberIds(List<String> memberIds) {
    this.memberIds = memberIds;
  }
  public int getMemberIdCount() {
    return (null == this.memberIds)? 0 : this.memberIds.size();
  }

  public void writeToParcel(Parcel dest, int flags) {
    int memberCount = getMemberIdCount();
    dest.writeInt(this.code);
    dest.writeString(this.reason);
    dest.writeInt(memberCount);
    if (memberCount > 0) {
      dest.writeStringList(this.memberIds);
    }
  }
  public int describeContents() {
    return 0;
  }
  public static final Parcelable.Creator<AVIMConversationFailure> CREATOR = new Creator() {
    @Override
    public AVIMConversationFailure createFromParcel(Parcel source) {
      AVIMConversationFailure result = new AVIMConversationFailure();
      result.setCode(source.readInt());
      result.setReason(source.readString());
      int memberCount = source.readInt();
      if (memberCount > 0) {
        List<String> memberList = new ArrayList<>(memberCount);
        source.readStringList(memberList);
        result.setMemberIds(memberList);
      }
      return result;
    }
    @Override
    public AVIMConversationFailure[] newArray(int size) {
      return new AVIMConversationFailure[size];
    }
  };
}
