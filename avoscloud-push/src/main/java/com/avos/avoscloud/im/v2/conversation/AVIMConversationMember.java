package com.avos.avoscloud.im.v2.conversation;

/**
 * Created by fengjunwen on 2017/12/14.
 */
public class AVIMConversationMember {
  private String peerId;
  private MemberRole role;
  private String createdAt;
  private String objectId;
  private String inviter;
  private String nickname;

  public AVIMConversationMember(String peerId, MemberRole role) {
    this(null, peerId, role);
  }

  public AVIMConversationMember(String objectId, String peerId, MemberRole role) {
    this.objectId = objectId;
    this.peerId = peerId;
    this.role = role;
  }

  public String getPeerId() {
    return this.peerId;
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
}
