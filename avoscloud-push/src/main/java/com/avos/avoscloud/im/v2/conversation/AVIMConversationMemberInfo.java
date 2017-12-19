package com.avos.avoscloud.im.v2.conversation;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by fengjunwen on 2017/12/14.
 */
public class AVIMConversationMemberInfo {
  public static String ATTR_OJBECTID = "infoId";
  public static String ATTR_CONVID = "conversationId";
  public static String ATTR_MEMBERID = "peerId";
  public static String ATTR_ROLE = "role";
  public static String ATTR_CREATEDAT = "createdAt";
  public static String ATTR_NICKNAME = "nickname";
  public static String ATTR_INVITER = "inviter";

  private String conversationId = null;
  private String memberId = null;
  private MemberRole role;
  private String createdAt = null;
  private String objectId = null;
  private String inviter = null;
  private String nickname = null;

  public AVIMConversationMemberInfo(String conversationId, String memberId, MemberRole role) {
    this(null, conversationId, memberId, role);
  }

  public AVIMConversationMemberInfo(String objectId, String conversationId, String memberId, MemberRole role) {
    this.objectId = objectId;
    this.conversationId = conversationId;
    this.memberId = memberId;
    this.role = role;
  }

  public String getConversationId() {
    return conversationId;
  }

  public String getMemberId() {
    return this.memberId;
  }

  public MemberRole getRole() {
    return this.role;
  }

  public void setRole(MemberRole role) {
    this.role = role;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getObjectId() {
    return objectId;
  }

  public String getInviter() {
    return inviter;
  }

  public void setInviter(String inviter) {
    this.inviter = inviter;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public Map<String,String> getUpdateAttrs() {
    HashMap<String, String> attrs = new HashMap<>();
    attrs.put(ATTR_MEMBERID, getMemberId());
    attrs.put(ATTR_ROLE, getRole().getName());
    attrs.put(ATTR_OJBECTID, getObjectId());
    return attrs;
  }

  public static AVIMConversationMemberInfo createInstance(Map<String, Object> data) {
    if (null == data) {
      return null;
    }
    String conversationId = (String)data.get(ATTR_CONVID);
    String memberId = (String)data.get(ATTR_MEMBERID);
    String roleStr = (String)data.get(ATTR_ROLE);
    String objectId = (String)data.get(ATTR_OJBECTID);
    MemberRole role = MemberRole.valueOf(roleStr);
    return new AVIMConversationMemberInfo(objectId, conversationId, memberId, role);
  }
}
