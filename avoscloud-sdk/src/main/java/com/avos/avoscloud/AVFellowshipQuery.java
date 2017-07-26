package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;

import java.util.*;

/**
 * 这个类仅仅是作为单向好友关系查询的
 * Created with IntelliJ IDEA. User: zhuzeng Date: 1/26/14 Time: 11:28 AM To change this template
 * use File | Settings | File Templates.
 */
class AVFellowshipQuery<T extends AVUser> extends AVQuery<T> {
  private String friendshipTag;

  AVFellowshipQuery(String theClassName, Class<T> clazz) {
    super(theClassName, clazz);
  }

  void setFriendshipTag(final String tag) {
    friendshipTag = tag;
  }

  String getFriendshipTag() {
    return friendshipTag;
  }

  private void processResultList(Map[] results, List<T> list, String tag) throws Exception {
    for (Map item : results) {
      if (item != null && !item.isEmpty()) {
        T user;
        if (getClazz() != null) {
          user = getClazz().newInstance();
        } else {
          user = (T) AVUtils.objectFromClassName(getClassName());
        }
        if (item.get(tag) != null && !((Map<String, Object>) item.get(tag)).isEmpty()) {
          AVUtils.copyPropertiesFromMapToAVObject((Map<String, Object>) item.get(tag), user);
          list.add(user);
        }
      }
    }
  }

  private List<T> processResultByTag(String content, String tag) throws Exception {

    if (AVUtils.isBlankString(content)) {
      return Collections.EMPTY_LIST;
    }
    List<T> list = new LinkedList<T>();
    AVFollowResponse resp = new AVFollowResponse();
    resp = JSON.parseObject(content, resp.getClass());
    processResultList(resp.results, list, tag);
    return list;
  }

  @Override
  protected List<T> processResults(String content) throws Exception {
    if (AVUtils.isBlankContent(content)) {
      return Collections.emptyList();
    }
    return processResultByTag(content, friendshipTag);
  }
}
