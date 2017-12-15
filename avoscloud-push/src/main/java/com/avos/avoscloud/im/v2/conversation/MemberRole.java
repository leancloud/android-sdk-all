package com.avos.avoscloud.im.v2.conversation;

/**
 * Created by fengjunwen on 2017/12/14.
 */

public enum MemberRole {
  CREATOR("Creator"),
  MODERATOR("Moderator"),
  MEMBER("Member"),
  VISITOR("Visitor"),
  FORBIDDEN("Forbidden");
  private String role;
  MemberRole(String role) {
    this.role = role;
  }

  public String getName() {
    return this.role;
  }
}
