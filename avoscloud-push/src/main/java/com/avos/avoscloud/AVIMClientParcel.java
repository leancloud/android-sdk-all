package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by wli on 2017/7/26.
 * 主要用于 AVIMClient 与 PushService 传值使用，避免在添加参数时还需要在定义各种常量
 */
public class AVIMClientParcel implements Parcelable {

  /**
   * 用于标注客户端，以支持单点登录功能
   */
  private String clientTag = "";

  /**
   * 此值为与 AVIMClient 相关联的 AVUser 的 sessionToken
   */
  private String sessionToken = "";

  /**
   * 单点登录是否强制
   */
  private boolean isForceSingleLogin;


  public AVIMClientParcel() {}

  public AVIMClientParcel(Parcel in) {
    clientTag = in.readString();
    sessionToken = in.readString();
    isForceSingleLogin = in.readInt() == 1;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(clientTag);
    dest.writeString(sessionToken);
    dest.writeInt(isForceSingleLogin ? 1 : 0);
  }

  public static final Creator<AVIMClientParcel> CREATOR = new Creator<AVIMClientParcel>() {
    @Override
    public AVIMClientParcel createFromParcel(Parcel in) {
      return new AVIMClientParcel(in);
    }

    @Override
    public AVIMClientParcel[] newArray(int size) {
      return new AVIMClientParcel[size];
    }
  };

  public void setClientTag(String clientTag) {
    this.clientTag = clientTag;
  }

  public String getClientTag() {
    return clientTag;
  }

  public void setForceSingleLogin(boolean isForceSingleLogin) {
    this.isForceSingleLogin = isForceSingleLogin;
  }

  public boolean isForceSingleLogin() {
    return isForceSingleLogin;
  }

  public void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  public String getSessionToken() {
    return sessionToken;
  }
}
