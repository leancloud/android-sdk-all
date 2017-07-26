package com.avos.avoscloud;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import com.alibaba.fastjson.annotation.JSONType;

/**
 * <p>
 * A class that is used to access all of the children of a many-to-many relationship. Each instance
 * of AVOSCloud.Relation is associated with a particular parent object and key.
 * </p>
 */
@JSONType(ignores = {"query","parent"})
public class AVRelation<T extends AVObject> {
  private String key;
  private AVObject parent;
  private String targetClass;

  public AVRelation() {
    super();
  }

  AVRelation(AVObject parent, String key) {
    super();
    this.parent = parent;
    this.key = key;
  }

  AVRelation(String targetClass) {
    this(null, null);
    this.targetClass = targetClass;
  }

  /**
   * Adds an object to this relation.
   * 
   * @param object The object to add to this relation.
   */
  public void add(T object) {
    if (object == null) throw new IllegalArgumentException("null AVObject");
    if (AVUtils.isBlankString(targetClass)) {
      targetClass = object.getClassName();
    }
    if (!AVUtils.isBlankString(targetClass) && !targetClass.equals(object.getClassName())) {
      throw new IllegalArgumentException("Could not add class '" + object.getClassName()
          + "' to this relation,expect class is '" + targetClass + "'");
    }
    parent.addRelation(object, key, true);
  }

  /**
   * Adds many objects to this relation.
   * 
   * @param objects The objects to add to this relation.
   */
  public void addAll(Collection<T> objects) {
    if (objects != null) {
      for (T obj : objects) {
        add(obj);
      }
    }
  }

  /**
   * Removes an object from this relation.
   * 
   * @param object The object to remove from this relation.
   */
  public void remove(AVObject object) {
    parent.removeRelation(object, key, true);
  }

  /**
   * Gets a query that can be used to query the objects in this relation.
   * 
   * @return A AVQuery that restricts the results to objects in this relations.
   */
  public AVQuery<T> getQuery() {
    return this.getQuery(null);
  }

  /**
   * Gets a query that can be used to query the subclass objects in this relation.
   * 
   * @param clazz The AVObject subclass.
   * @return A AVQuery that restricts the results to objects in this relations.
   */
  public AVQuery<T> getQuery(Class<T> clazz) {
    if (getParent() == null || AVUtils.isBlankString(getParent().getObjectId())) {
      throw new IllegalStateException("unable to encode an association with an unsaved AVObject");
    }

    Map<String, Object> map = new HashMap<String, Object>() {
      {
        put("object", AVUtils.mapFromPointerObject(AVRelation.this.getParent()));
        put("key", AVRelation.this.getKey());
      }
    };

    Map<String, Object> result = new HashMap<String, Object>();
    result.put("$relatedTo", map);
    String targetClassName = getTargetClass();

    if (AVUtils.isBlankString(getTargetClass())) {
      targetClassName = getParent().getClassName();
    }
    AVQuery<T> query = new AVQuery<T>(targetClassName, clazz);
    query.addWhereItem("$relatedTo", null, map);
    if (AVUtils.isBlankString(getTargetClass())) {
      query.getParameters().put("redirectClassNameForKey", this.getKey());
    }

    return query;
  }

  /**
   * Create a query that can be used to query the parent objects in this relation.
   * 
   * @param parentClassName The parent class name
   * @param relationKey The relation field key in parent
   * @param child The child object.
   * @return A AVQuery that restricts the results to parent objects in this relations.
   */
  public static <M extends AVObject> AVQuery<M> reverseQuery(String parentClassName,
      String relationKey, AVObject child) {
    AVQuery<M> query = new AVQuery<M>(parentClassName);
    query.whereEqualTo(relationKey, AVUtils.mapFromPointerObject(child));
    return query;
  }

  /**
   * Create a query that can be used to query the parent objects in this relation.
   * 
   * @param theParentClazz The parent subclass.
   * @param relationKey The relation field key in parent
   * @param child The child object.
   * @return A AVQuery that restricts the results to parent objects in this relations.
   */
  public static <M extends AVObject> AVQuery<M> reverseQuery(Class<M> theParentClazz,
      String relationKey, AVObject child) {
    AVQuery<M> query = new AVQuery<M>(AVObject.getSubClassName(theParentClazz), theParentClazz);
    query.whereEqualTo(relationKey, AVUtils.mapFromPointerObject(child));
    return query;
  }

  public String getKey() {
    return key;
  }

  void setKey(String newKey) {
    key = newKey;
  }

  void setParent(AVObject newParent) {
    parent = newParent;
  }

  public AVObject getParent() {
    return parent;
  }

  /**
   * 当targetClass为null时，手动设置targetClass值
   * 
   */
  public void setTargetClass(String newTargetClass) {
    targetClass = newTargetClass;
  }

  public String getTargetClass() {
    return targetClass;
  }
}
