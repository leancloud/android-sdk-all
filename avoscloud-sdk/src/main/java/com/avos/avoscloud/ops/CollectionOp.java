package com.avos.avoscloud.ops;


import com.alibaba.fastjson.annotation.JSONType;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lbt05 on 5/28/15.
 */
@JSONType(ignores = {"parsedValues"})
public abstract class CollectionOp extends BaseOp {
  public CollectionOp() {
    super();
  }

  public CollectionOp(String key, OpType type) {
    super(key, type);
  }

  public void setValues(Collection values) {
    this.getValues().clear();
    this.getValues().addAll(values);
  };

  @Override
  public abstract Collection getValues();

  public List getParsedValues() {
    List<Object> result = new LinkedList<Object>();
    for (Object v : getValues()) {
      if (v instanceof AVObject) {
        Object realValue = AVUtils.mapFromPointerObject((AVObject) v);
        result.add(realValue);
      } else if (v instanceof AVFile) {
        Object realValue = AVUtils.mapFromFile((AVFile) v);
        result.add(realValue);
      } else {
        result.add(v);
      }
    }
    return result;
  }
}
