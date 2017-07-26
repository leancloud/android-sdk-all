package com.avos.avoscloud.ops;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.avos.avoscloud.AVObject;

public class NullOP implements AVOp {

  public static NullOP INSTANCE = new NullOP();

  public NullOP() {
    super();
  }

  @Override
  public <T extends AVOp> T cast(Class<T> clazz) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<AVOp> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String key() {
    return "__ALL_POSSIABLE_KEYS";
  }

  @Override
  public OpType type() {
    return OpType.Null;
  }

  @Override
  public Object apply(Object obj) {
    return obj;
  }

  @Override
  public AVOp merge(AVOp other) {
    return other;
  }

  @Override
  public Map<String, Object> encodeOp() {
    return Collections.emptyMap();
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public AVOp get(int idx) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AVOp remove(int idx) {
    // TODO Auto-generated method stub
    return null;
  }

  public Object getValues(){
      return null;
  }
}
