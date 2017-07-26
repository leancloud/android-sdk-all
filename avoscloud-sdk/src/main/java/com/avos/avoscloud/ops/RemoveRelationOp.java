package com.avos.avoscloud.ops;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;

public class RemoveRelationOp extends CollectionOp {
  private Set<AVObject> values = new HashSet<AVObject>();

  public RemoveRelationOp(String key, AVObject... values) {
    super(key, OpType.RemoveRelation);
    if (values != null) {
      for (AVObject obj : values) {
        this.values.add(obj);
      }
    }
  }

  public Set<AVObject> getValues() {
    return values;
  }

  public RemoveRelationOp() {
    super();
  }


  @Override
  public Object apply(Object oldValue) {
    return oldValue;
  }

  @Override
  public Map<String, Object> encodeOp() {
    return AVUtils.createPointerArrayOpMap(key, "RemoveRelation", getValues());
  }

  @Override
  public AVOp merge(AVOp other) {
    assertKeyEquals(other);
    switch (other.type()) {
      case Null:
        return this;
      case Set:
        return other;
      case RemoveRelation:
        this.values.addAll(other.cast(RemoveRelationOp.class).values);
        return this;
      case AddUnique:
      case Remove:
      case Add:
      case AddRelation:
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
