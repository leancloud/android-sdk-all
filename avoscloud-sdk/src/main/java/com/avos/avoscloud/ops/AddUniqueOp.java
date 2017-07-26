package com.avos.avoscloud.ops;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;

public class AddUniqueOp extends CollectionAddOp {
  private Set<Object> values = new HashSet<Object>();

  public Set<Object> getValues() {
    return values;
  }

  public AddUniqueOp() {
    super();
  }

  public AddUniqueOp(String key, Object... values) {
    super(key, OpType.AddUnique);
    if (values != null) {
      for (Object obj : values) {
        this.values.add(obj);
      }
    }
  }

  @Override
  public Map<String, Object> encodeOp() {
    return AVUtils.createArrayOpMap(key, this.type.name(), getParsedValues());
  }

  public Object apply(Object oldValue) {
    Set<Object> result = new HashSet<Object>();
    if (oldValue != null) {
      result.addAll((Collection) oldValue);
    }
    if (getValues() != null) {
      result.addAll(getValues());
    }
    return new LinkedList<Object>(result);
  }

  @Override
  public AVOp merge(AVOp other) {
    assertKeyEquals(other);
    switch (other.type()) {
      case Null:
        return this;
      case Set:
        return other;
      case AddUnique:
        this.values.addAll(other.cast(AddUniqueOp.class).values);
        return this;
      case AddRelation:
      case Remove:
      case Add:
      case RemoveRelation:
        return new CompoundOp(key, this, other);
      case Increment:
        throw new UnsupportedOperationException("Could not increment an non-numberic value.");
      case Delete:
        return other;
      case Compound:
        other.cast(CompoundOp.class).addFirst(this);
        return other;
      default:
        throw new IllegalStateException("Unknow op type " + other.type());
    }
  }
}
