package com.avos.avoscloud.im.v2.conversation;

/**
 * 成员角色的枚举类型
 *
 * Created by fengjunwen on 2017/12/14.
 */
public enum MemberRole {
  MANAGER("Manager"),  // 管理员
  MEMBER("Member"),    // 普通成员
  private String role;
  MemberRole(String role) {
    this.role = role;
  }

  /**
   * 获取角色名字
   * @return
   */
  public String getName() {
    return this.role;
  }

  /**
   * 从角色名字生成实例
   * @param role 角色名字
   * @return
   */
  public static MemberRole fromString(String role) {
    for(MemberRole mr: MemberRole.values()) {
      if (mr.role.equalsIgnoreCase(role)) {
        return mr;
      }
    }
    return null;
  }
}
