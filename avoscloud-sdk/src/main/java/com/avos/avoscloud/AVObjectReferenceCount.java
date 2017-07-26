package com.avos.avoscloud;

import java.util.concurrent.atomic.AtomicInteger;

public class AVObjectReferenceCount {
  AVObject value;
  AtomicInteger count;

  public AVObjectReferenceCount(AVObject o) {
    this.value = o;
    count = new AtomicInteger(1);
  }

  public int increment() {
    return count.incrementAndGet();
  }

  public int desc() {
    return count.decrementAndGet();
  }

  public int getCount() {
    return count.get();
  }

  public AVObject getValue() {
    return value;
  }
}
