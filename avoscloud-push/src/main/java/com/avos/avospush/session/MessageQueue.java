package com.avos.avospush.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Handler;
import android.os.HandlerThread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.AVPersistenceUtils;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.LogUtil;

public class MessageQueue<E> implements Queue<E> {

  Queue<E> messages;
  private final String queueKey;
  private static final String MESSAGE_ZONE = "com.avoscloud.chat.message";
  private static final String QUEUE_KEY = "com.avoscloud.chat.message.queue";
  private final Class<E> type;
  static HandlerThread serializeThread = new HandlerThread("com.avos.avoscloud.push.messagequeue");
  static {
    serializeThread.start();
  }
  static Handler serializeHanlder = new Handler(serializeThread.getLooper());

  public MessageQueue(String peerId, Class<E> type) {
    messages = new ConcurrentLinkedQueue<E>();
    this.type = type;
    queueKey = QUEUE_KEY + "." + peerId;
    LinkedList<E> storedMessages = restoreMessageQueue();
    if (storedMessages != null && storedMessages.size() > 0) {
      messages.addAll(storedMessages);
    }
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    boolean result = messages.addAll(collection);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public void clear() {
    messages.clear();
    this.storeMessageQueue();
  }

  @Override
  public boolean contains(Object object) {
    return messages.contains(object);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    return messages.containsAll(collection);
  }

  @Override
  public boolean isEmpty() {
    return messages.isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return messages.iterator();
  }

  @Override
  public boolean remove(Object object) {
    boolean result = messages.remove(object);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    boolean result = messages.removeAll(collection);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    boolean result = messages.retainAll(collection);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public int size() {
    return messages.size();
  }

  @Override
  public Object[] toArray() {
    return messages.toArray();
  }

  @Override
  public <T> T[] toArray(T[] array) {
    return messages.toArray(array);
  }

  @Override
  public boolean add(E e) {
    boolean result = messages.add(e);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public boolean offer(E e) {
    boolean result = messages.offer(e);
    this.storeMessageQueue();
    return result;
  }

  @Override
  public E remove() {
    E result = messages.remove();
    this.storeMessageQueue();
    return result;
  }

  @Override
  public E poll() {
    E result = messages.poll();
    this.storeMessageQueue();
    return result;
  }

  @Override
  public E element() {
    E result = messages.element();
    this.storeMessageQueue();
    return result;
  }

  @Override
  public E peek() {
    return messages.peek();
  }

  private void storeMessageQueue() {
    // 异步序列化，保证效率
    serializeHanlder.post(new Runnable() {

      @Override
      public void run() {
        String queueString =
            JSON.toJSONString(messages, SerializerFeature.SkipTransientField,
                SerializerFeature.WriteClassName, SerializerFeature.QuoteFieldNames,
                SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse);
        AVPersistenceUtils.sharedInstance().savePersistentSettingString(MESSAGE_ZONE, queueKey,
            queueString);

      }
    });
  }

  private synchronized LinkedList<E> restoreMessageQueue() {
    LinkedList<E> storedMessages = new LinkedList<E>();
    String queueString =
        AVPersistenceUtils.sharedInstance()
            .getPersistentSettingString(MESSAGE_ZONE, queueKey, null);
    if (!AVUtils.isBlankString(queueString)) {
      try {
        storedMessages.addAll(JSON.parseArray(queueString, type));
      } catch (Exception e) {
        // clean it since there's parse exception
        AVPersistenceUtils.sharedInstance().removePersistentSettingString(MESSAGE_ZONE, queueKey);
        LogUtil.log.e(e.getMessage());
      }
    }
    return storedMessages;
  }

  public interface HasId {
    public String getId();

    public void setId(String id);
  }
}
