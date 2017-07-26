package com.avos.avoscloud;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 7/11/13 Time: 10:05 AM To change this template
 * use File | Settings | File Templates.
 */

public class AVOperation {

  private AVOperationType type;
  private List batchRequest;
  private SaveCallback callback;
  private int sequence;
  private boolean last;

  public AVOperation() {
    batchRequest = new ArrayList();
    callback = null;
    last = true;
  }

  public void setLast(boolean l) {
    last = l;
  }

  public boolean getLast() {
    return last;
  }

  public List getBatchRequest() {
    return batchRequest;
  }

  public boolean isSnapshotRequest() {
    return type == AVOperationType.kAVOperationSnapshot;
  }

  public boolean isPendingRequest() {
    return type == AVOperationType.kAVOperationPendingOperation;
  }

  public void setCallback(SaveCallback cb) {
    callback = cb;
  }

  public SaveCallback getCallback() {
    return callback;
  }

  public void setSequence(int seq) {
    sequence = seq;
  }

  public int getSequence() {
    return sequence;
  }

  public void invokeCallback(AVException exception) {
    if (getCallback() != null) {
      getCallback().internalDone(exception);
    }
  }

  static AVOperation snapshotOperation(List request, SaveCallback cb) {
    return cloneOperation(request, cb, AVOperationType.kAVOperationSnapshot);
  }

  private static AVOperation cloneOperation(List request, SaveCallback cb, AVOperationType type) {
    AVOperation operation = new AVOperation();
    operation.batchRequest.addAll(request);
    operation.callback = cb;
    operation.type = type;
    return operation;
  }

  static AVOperation pendingOperation(List request, SaveCallback cb) {
    return cloneOperation(request, cb, AVOperationType.kAVOperationPendingOperation);
  }
}
