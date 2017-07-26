package com.avos.avoscloud;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 用户好友关系,同时包含用户的粉丝和用户的关注
 */
public class AVFriendship<T extends AVUser> {

  List<T> followers = new LinkedList<T>();
  List<T> followees = new LinkedList<T>();

  AVUser user;

  /**
   * 获取用户的粉丝
   * 
   * @return
   */
  public List<T> getFollowers() {
    return followers;
  }

  protected void setFollowers(List<T> followers) {
    this.followers = followers;
  }

  /**
   * 获取用户的关注
   * 
   * @return
   */
  public List<T> getFollowees() {
    return followees;
  }

  protected void setFollowees(List<T> followees) {
    this.followees = followees;
  }

  public AVUser getUser() {
    return user;
  }

  protected void setUser(AVUser user) {
    this.user = user;
  }

  static class AVFriendshipResponse {
    List<Map<String, Object>> followers;
    List<Map<String, Object>> followees;

    public List<Map<String, Object>> getFollowers() {
      return followers;
    }

    public void setFollowers(List<Map<String, Object>> followers) {
      this.followers = followers;
    }

    public List<Map<String, Object>> getFollowees() {
      return followees;
    }

    public void setFollowees(List<Map<String, Object>> followees) {
      this.followees = followees;
    }
  }
}
