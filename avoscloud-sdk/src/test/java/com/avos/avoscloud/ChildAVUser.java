package com.avos.avoscloud;

/**
 * Created by wli on 2017/3/14.
 */

@AVClassName("ChildAVUser")
public class ChildAVUser extends AVUser {

  public void setNickName(String name) {
    this.put("nickName", name);
  }

  public String getNickName() {
    return this.getString("nickName");
  }
}