package com.avos.avoscloud.im.v2.callback;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fengjunwen on 2017/12/20.
 */

public class AVIMOperationFailure implements Parcelable {
  String reason = "";
  int code = 0;
  List<String> memberIds = null;

  /**
   * 默认构造函数
   */
  public AVIMOperationFailure() {
    ;
  }

  /**
   * 返回错误原因描述
   * @return
   */
  public String getReason() {
    return reason;
  }

  /**
   * 设置错误原因描述
   * @param reason
   */
  public void setReason(String reason) {
    this.reason = reason;
  }

  /**
   * 返回错误码
   * @return
   */
  public int getCode() {
    return code;
  }

  /**
   * 设置错误码
   * @param code
   */
  public void setCode(int code) {
    this.code = code;
  }

  /**
   * 获取出错的 member id 列表
   * @return
   */
  public List<String> getMemberIds() {
    return memberIds;
  }

  /**
   * 设置出错的 member id 列表
   * @param memberIds
   */
  public void setMemberIds(List<String> memberIds) {
    this.memberIds = memberIds;
  }

  /**
   * 返回出错的 member id 列表长度
   * @return
   */
  public int getMemberIdCount() {
    return (null == this.memberIds)? 0 : this.memberIds.size();
  }

  /**
   * Parcelable 接口实现
   */
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

  public static final Parcelable.Creator<AVIMOperationFailure> CREATOR = new Creator() {
    @Override
    public AVIMOperationFailure createFromParcel(Parcel source) {
      AVIMOperationFailure result = new AVIMOperationFailure();
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
    public AVIMOperationFailure[] newArray(int size) {
      return new AVIMOperationFailure[size];
    }
  };
}
