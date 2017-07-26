package com.avos.avoscloud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avos.avospush.session.MessageQueue.HasId;

import com.avos.avospush.session.MessageQueue;

class PendingMessageCache<E extends HasId> {

  private Map<String, E> msgMapping;
  private MessageQueue<E> messages;

  PendingMessageCache(String peerId, Class<E> type) {
    this.messages = new MessageQueue<E>(peerId, type);
    this.setupMapping();
  }

  private void setupMapping() {
    msgMapping = new ConcurrentHashMap<String, E>();
    for (E msg : messages) {
      if (!AVUtils.isBlankString(msg.getId())) {
        msgMapping.put(msg.getId(), msg);
      }
    }
  }

  void offer(E msg) {
    if (!AVUtils.isBlankString(msg.getId())) {
      msgMapping.put(msg.getId(), msg);
    }
    messages.offer(msg);
  }

  E poll(String msgId) {
    if (!AVUtils.isBlankString(msgId) && msgMapping.containsKey(msgId)) {
      E returnValue = msgMapping.remove(msgId);
      messages.remove(returnValue);
      return returnValue;
    }
    return this.poll();
  }

  E poll() {
    return messages.poll();
  }

  public void clear() {
    messages.clear();
    msgMapping.clear();
  }

  public boolean isEmpty() {
    return messages.isEmpty();
  }

  public static class Message implements HasId {
    public String msg;
    public String id;
    public long timestamp;

    boolean requestReceipt;
    String cid;

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    public static Message getMessage(String msg, String id,
                                     boolean requestReceipt, String cid) {
      Message m = new Message();
      m.msg = msg;
      m.setId(id);
      m.requestReceipt = requestReceipt;
      m.cid = cid;
      return m;
    }
  }
}
