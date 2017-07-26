package com.avos.avoscloud.ops;

import java.util.Map;

import com.avos.avoscloud.AVObject;

/**
 * A avoscloud operation
 * 
 * @author apple
 * 
 */
public interface AVOp extends Iterable<AVOp> {

  public static enum OpType {
    Set, Increment, AddUnique, Add, Remove, AddRelation, RemoveRelation, Delete, Null, Compound
  }

  public <T extends AVOp> T cast(Class<T> clazz);

  public String key();

  public OpType type();

  public Object apply(Object obj);

  public AVOp merge(AVOp other);

  public int size();

  public AVOp get(int idx);

  public AVOp remove(int idx);

  public Map<String, Object> encodeOp();

  public Object getValues();
}
