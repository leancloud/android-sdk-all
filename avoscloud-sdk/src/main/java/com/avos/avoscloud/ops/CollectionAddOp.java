package com.avos.avoscloud.ops;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lbt05 on 5/28/15.
 */
public abstract class CollectionAddOp extends CollectionOp {

  public CollectionAddOp() {
    super();
  }

  public CollectionAddOp(String key, OpType type) {
    super(key, type);
  }

  public Object apply(Object oldValue) {
    List<Object> result = new LinkedList<Object>();
    if (oldValue != null) {
      result.addAll((Collection) oldValue);
    }
    if (getValues() != null) {
      result.addAll(getValues());
    }
    return result;
  }
}
