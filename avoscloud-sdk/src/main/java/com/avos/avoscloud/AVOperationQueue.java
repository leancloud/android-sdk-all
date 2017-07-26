package com.avos.avoscloud;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 7/11/13 Time: 10:30 AM To change this template
 * use File | Settings | File Templates.
 */
public class AVOperationQueue {
  private ConcurrentLinkedQueue<AVOperation> queue;
  private volatile int currentSequence;

  public AVOperationQueue() {
    queue = new ConcurrentLinkedQueue<AVOperation>();
  }

  public synchronized void increaseSequence() {
    currentSequence += 2;
  }

  public AVOperation addSnapshotOperation(List request, SaveCallback cb) {
    AVOperation operation = AVOperation.snapshotOperation(request, cb);
    operation.setSequence(currentSequence);
    queue.offer(operation);
    return operation;
  }

  public AVOperation addPendingOperation(List request, SaveCallback cb) {
    AVOperation operation = AVOperation.pendingOperation(request, cb);
    operation.setSequence(currentSequence);
    queue.offer(operation);
    return operation;
  }

  public AVOperation popHead() {
    return (AVOperation) queue.poll();
  }

  public boolean noPendingRequest() {
    return queue.isEmpty();
  }

  public void clearOperationWithSequence(int seq) {
    Iterator<AVOperation> iter = queue.iterator();
    while (iter.hasNext()) {
      AVOperation operation = iter.next();
      if (operation.getSequence() == seq) {
        iter.remove();
      }
    }
  }
}
