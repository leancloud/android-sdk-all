package com.avos.avoscloud.im.v2.conversation;

import com.avos.avoscloud.AVObject;

/**
 * Created by fengjunwen on 2017/12/14.
 */
public class AVIMConversationMember extends AVObject {
  static String ATTR_ROLE = "role";
  static String ATTR_PEERID = "peerId";
  transient private MemberRole role;

  public AVIMConversationMember(MemberRole role, String peerId) {
    super("_ConversationMember");
    this.role = role;
    put(ATTR_PEERID, peerId);
    put(ATTR_ROLE, role.getName());
  }
  public String getPeerId() {
    return this.getString(ATTR_PEERID);
  }
  public MemberRole getRole() {
    return this.role;
  }

}
