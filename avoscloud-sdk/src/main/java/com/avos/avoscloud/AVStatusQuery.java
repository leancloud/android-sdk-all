package com.avos.avoscloud;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class AVStatusQuery extends AVQuery<AVStatus> {

  private static final String END = "end";
  private long sinceId;
  private String inboxType;
  private long maxId;
  private boolean count;
  private AVUser owner;
  private boolean selfStatusQuery = false;

  public AVStatusQuery() {
    super("_Status", null);
    getInclude().add("source");
  }

  public void setSinceId(long sinceId) {
    this.sinceId = sinceId;
  }

  public long getSinceId() {
    return sinceId;
  }

  public void setInboxType(String inboxType) {
    this.inboxType = inboxType;
  }

  protected String getInboxType() {
    return this.inboxType;
  }

  public long getMaxId() {
    return maxId;
  }

  public void setMaxId(long maxId) {
    this.maxId = maxId;
  }

  public boolean isCount() {
    return count;
  }

  public void setCount(boolean count) {
    this.count = count;
  }

  public AVUser getOwner() {
    return owner;
  }

  public void setOwner(AVUser owner) {
    this.owner = owner;
  }

  protected void setSelfQuery(boolean isSelf) {
    this.selfStatusQuery = isSelf;
  }

  @Override
  public Map<String, String> assembleParameters() {
    if (selfStatusQuery && inboxType != null) {
      this.whereEqualTo("inboxType", inboxType);
    }
    super.assembleParameters();
    Map<String, String> p = this.getParameters();
    if (owner != null) {
      String ownerId = owner.getObjectId();
      Map<String, Object> ownerMap = AVUtils.mapFromUserObjectId(ownerId);
      p.put("owner", JSON.toJSONString(ownerMap));
    }
    if (sinceId > 0) {
      p.put("sinceId", String.valueOf(sinceId));
    }
    if (!AVUtils.isBlankString(inboxType) && !selfStatusQuery) {
      p.put("inboxType", inboxType);
    }
    if (maxId > 0) {
      p.put("maxId", String.valueOf(maxId));
    }
    if (count) {
      p.put("count", "1");
    }
    conditions.setParameters(p);
    return p;
  }

  @Override
  protected void processAdditionalInfo(String content, FindCallback<AVStatus> callback) {
    // TODO Auto-generated method stub
    if (InboxStatusFindCallback.class.isAssignableFrom(callback.getClass())) {
      InboxStatusFindCallback statusCallback = (InboxStatusFindCallback) callback;
      boolean v = false;
      try {
        com.alibaba.fastjson.JSONObject results = JSON.parseObject(content);
        if (results.containsKey(END)) {
          v = results.getBoolean(END);
        }
      } catch (Exception e) {
        LogUtil.avlog.e("Parsing json data error, " + content, e);
      }
      statusCallback.setEnd(v);
    }
  }
}
