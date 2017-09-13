package com.avos.avoscloud;

import com.avos.avoscloud.im.v2.AVIMOptions;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avospush.session.CommandPacket;
import com.avos.avospush.session.MessageQueue;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.SparseArray;

import java.util.concurrent.ConcurrentHashMap;

class AVIMOperationQueue {

  public static class Operation {
    int requestId;
    int operation;
    String sessionId;
    String conversationId;

    public static Operation getOperation(int operation, String sessionId, String conversationId,
        int requestId) {
      Operation op = new Operation();
      op.conversationId = conversationId;
      op.sessionId = sessionId;
      op.operation = operation;
      op.requestId = requestId;
      return op;
    }
  }

  static ConcurrentHashMap<Integer, Runnable> timeoutCache =
      new ConcurrentHashMap<Integer, Runnable>();
  static HandlerThread timeoutHandlerThread = new HandlerThread(
      "com.avos.avoscloud.im.v2.timeoutHandlerThread");

  static {
    timeoutHandlerThread.start();
  }

  static Handler timeoutHandler = new Handler(timeoutHandlerThread.getLooper());

  SparseArray<Operation> cache = new SparseArray<AVIMOperationQueue.Operation>();
  MessageQueue<Operation> operationQueue;

  public AVIMOperationQueue(String key) {
    operationQueue =
        new MessageQueue<AVIMOperationQueue.Operation>("operation.queue." + key, Operation.class);
    setupCache();
  }

  private void setupCache() {
    for (Operation op : operationQueue) {
      if (op.requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        cache.put(op.requestId, op);
      }
    }
  }
  //offer 应该永远比发送数据早执行
  public void offer(final Operation op) {
    if (op.requestId != CommandPacket.UNSUPPORTED_OPERATION) {
      cache.put(op.requestId, op);
      Runnable timeoutTask = new Runnable() {

        @Override
        public void run() {
          Operation polledOP = poll(op.requestId);
          if (polledOP != null) {
            AVIMOperation operation = AVIMOperation.getAVIMOperation(polledOP.operation);
            BroadcastUtil.sendIMLocalBroadcast(polledOP.sessionId, polledOP.conversationId,
                polledOP.requestId, new AVException(AVException.TIMEOUT, "Timeout Exception"),
                operation);
          }
        }
      };
      timeoutCache.put(op.requestId, timeoutTask);
      timeoutHandler.postDelayed(timeoutTask, AVIMOptions.getGlobalOptions().getTimeoutInSecs() * 1000);
    }
    operationQueue.offer(op);
  }

  public Operation poll(int requestId) {
    if (requestId != CommandPacket.UNSUPPORTED_OPERATION && cache.get(requestId) != null) {
      Operation returnValue = cache.get(requestId);
      cache.remove(requestId);
      operationQueue.remove(returnValue);
      Runnable timeoutTask = timeoutCache.get(requestId);
      timeoutCache.remove(requestId);
      if (timeoutTask != null) {
        timeoutHandler.removeCallbacks(timeoutTask);
      }
      return returnValue;
    }
    return this.poll();
  }

  public Operation poll() {
    return operationQueue.poll();
  }

  public void clear() {
    operationQueue.clear();
    cache.clear();
  }

  public boolean isEmpty() {
    return operationQueue.isEmpty();
  }

}
