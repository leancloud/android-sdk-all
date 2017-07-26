package com.avos.avoscloud.ops;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.avos.avoscloud.AVUtils;

public class AddOp extends CollectionAddOp {

  private List<Object> values = new LinkedList<Object>();

  public AddOp() {
    super();
  }

  public AddOp(String key, OpType type) {
    super(key, type);
  }

  public List<Object> getValues() {
    return values;
  }

  public AddOp(String key, Object... values) {
    super(key, OpType.Add);
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

  @Override
  public AVOp merge(AVOp other) {
    assertKeyEquals(other);
    switch (other.type()) {
      case Null:
        return this;
      case Set:
        return other;
      case Add:
        this.values.addAll(other.cast(AddOp.class).values);
        return this;
      case AddUnique:
      case Remove:
      case AddRelation:
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
