package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.callback.AVFriendshipCallback;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用于查询用户的好友关系的Query对象，同时包含用户的粉丝和用户的关注
 */
public class AVFriendshipQuery<T extends AVUser> {
  String userId;
  Class<T> userClazz;
  QueryConditions conditions;

  AVFriendshipQuery(String userId) {
    this(userId, null);
  }

  AVFriendshipQuery(String userId, Class<T> clazz) {
    this.userId = userId;
    this.userClazz = clazz;
    conditions = new QueryConditions();
  }

  protected void getInBackground(String objectId, boolean sync, AVFriendshipCallback callback) {
    String path = String.format("users/%s/followersAndFollowees", objectId);
    final AVFriendshipCallback internalCallback = callback;
    conditions.assembleParameters();
    PaasClient.storageInstance().getObject(path, new AVRequestParams(conditions.getParameters()),
        sync, null,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            AVFriendship<T> friendship = null;
            AVException error = e;
            if (AVUtils.isBlankContent(content)) {
              friendship = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "Object is not found.");
            } else {
              try {
                friendship = new AVFriendship<T>();
                AVFriendship.AVFriendshipResponse response =
                    JSON.parseObject(content, AVFriendship.AVFriendshipResponse.class);
                if (AVFriendshipQuery.this.userClazz != null) {
                  List<T> followers = friendship.getFollowers();
                  List<T> followees = friendship.getFollowees();
                  for (Map<String, Object> followerShip : response.followers) {
                    T follower = AVUser.newAVUser(userClazz, null);
                    AVUtils.copyPropertiesFromMapToAVObject(
                        (Map<String, Object>) followerShip.get("follower"), follower);
                    followers.add(follower);
                    if (friendship.getUser() == null) {
                      T user = AVUser.newAVUser(userClazz, null);
                      AVUtils.copyPropertiesFromMapToAVObject(
                          (Map<String, Object>) followerShip.get("user"), user);
                      friendship.setUser(user);
                    }
                  }
                  friendship.setFollowers(followers);
                  for (Map<String, Object> followeeShip : response.followees) {
                    T followee = AVUser.newAVUser(userClazz, null);
                    AVUtils.copyPropertiesFromMapToAVObject(
                        (Map<String, Object>) followeeShip.get("followee"), followee);
                    followees.add(followee);
                  }
                  friendship.setFollowees(followees);
                }
                // AVUtils.copyPropertiesFromJsonStringToAVObject(content, object);
              } catch (Exception e1) {
                if (internalCallback != null) {
                  internalCallback.internalDone(null, AVErrorUtils.createException(e1, content));
                }
              }
            }
            if (internalCallback != null) {
              internalCallback.internalDone(friendship, error);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
            }

          }
        });
  }

  public AVFriendship get() throws AVException {
    final Object[] result = {null};
    this.getInBackground(this.userId, true, new AVFriendshipCallback() {
      @Override
      public void done(AVFriendship object, AVException e) {
        if (e == null) {
          result[0] = object;
        } else {
          AVExceptionHolder.add(e);
        }
      }

      @Override
      protected boolean mustRunOnUIThread() {
        return false;
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return (AVFriendship) result[0];
  }

  /**
   * 
   * @param callback
   */
  public void getInBackground(AVFriendshipCallback callback) {
    this.getInBackground(this.userId, false, callback);
  }

  /**
   * Accessor for the limit.
   */
  public int getLimit() {
    return conditions.getLimit();
  }

  /**
   * Controls the maximum number of results that are returned. Setting a negative limit denotes
   * retrieval without a limit. The default limit is 100, with a maximum of 1000 results being
   * returned at a time.
   *
   * @return this query.
   */
  public AVFriendshipQuery<T> setLimit(int limit) {
    conditions.setLimit(limit);
    return this;
  }

  /**
   * @see #setLimit(int)
   * @param limit
   * @return this query.
   */
  public AVFriendshipQuery<T> limit(int limit) {
    this.setLimit(limit);
    return this;
  }

  /**
   * @see #setSkip(int)
   * @param skip
   * @return this query.
   */
  public AVFriendshipQuery<T> skip(int skip) {
    setSkip(skip);
    return this;
  }

  /**
   * Accessor for the skip value.
   */
  public int getSkip() {
    return conditions.getSkip();
  }

  /**
   * Controls the number of results to skip before returning any results. This is useful for
   * pagination. Default is to skip zero results.
   *
   * @return this query
   */
  public AVFriendshipQuery<T> setSkip(int skip) {
    conditions.setSkip(skip);
    return this;
  }

  public String getOrder() {
    return conditions.getOrder();
  }

  /**
   * Set query order fields.
   *
   * @param order
   * @return this query.
   */
  public AVFriendshipQuery<T> setOrder(String order) {
    conditions.setOrder(order);
    return this;
  }

  /**
   * @see #setOrder(String)
   * @param order
   * @return
   */
  public AVFriendshipQuery<T> order(String order) {
    setOrder(order);
    return this;
  }

  /**
   * Also sorts the results in ascending order by the given key. The previous sort keys have
   * precedence over this key.
   *
   * @param key The key to order by
   * @return Returns the query so you can chain this call.
   */
  public AVFriendshipQuery<T> addAscendingOrder(String key) {
    conditions.addAscendingOrder(key);
    return this;
  }

  /**
   * Also sorts the results in descending order by the given key. The previous sort keys have
   * precedence over this key.
   *
   * @param key The key to order by
   * @return Returns the query so you can chain this call.
   */
  public AVFriendshipQuery<T> addDescendingOrder(String key) {
    conditions.addDescendingOrder(key);
    return this;
  }

  /**
   * Include nested AVObjects for the provided key. You can use dot notation to specify which fields
   * in the included object that are also fetched.
   *
   * @param key The key that should be included.
   */
  public AVFriendshipQuery<T> include(String key) {
    conditions.include(key);
    return this;
  }

  /**
   * Restrict the fields of returned AVObjects to only include the provided keys. If this is called
   * multiple times, then all of the keys specified in each of the calls will be included.
   *
   * @param keys The set of keys to include in the result.
   */
  public AVFriendshipQuery<T> selectKeys(Collection<String> keys) {
    conditions.selectKeys(keys);
    return this;
  }

  /**
   * Sorts the results in ascending order by the given key.
   *
   * @param key The key to order by.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> orderByAscending(String key) {
    conditions.orderByAscending(key);
    return this;
  }

  /**
   * Sorts the results in descending order by the given key.
   *
   * @param key The key to order by.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> orderByDescending(String key) {
    conditions.orderByDescending(key);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be contained in the
   * provided list of values.
   *
   * @param key The key to check.
   * @param values The values that will match.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereContainedIn(String key, Collection<? extends Object> values) {
    conditions.whereContainedIn(key, values);
    return this;
  }

  /**
   * Add a constraint for finding string values that contain a provided string. This will be slow
   * for large datasets.
   *
   * @param key The key that the string to match is stored in.
   * @param substring The substring that the value must contain.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereContains(String key, String substring) {
    conditions.whereContains(key, substring);
    return this;
  }

  /**
   * 添加查询约束条件，查找key类型是数组，该数组的长度匹配提供的数值。
   *
   * @since 2.0.2
   * @param key 查询的key
   * @param size 数组的长度
   * @return
   */
  public AVFriendshipQuery<T> whereSizeEqual(String key, int size) {
    conditions.whereSizeEqual(key, size);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value match another
   * AVFriendshipQuery.
   * This only works on keys whose values are AVObjects or lists of AVObjects. Add a constraint to
   * the query that requires a particular key's value to contain every one of the provided list of
   * values.
   *
   * @param key The key to check. This key's value must be an array.
   * @param values The values that will match.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereContainsAll(String key, Collection<?> values) {
    conditions.whereContainsAll(key, values);
    return this;
  }

  /**
   * Add a constraint for finding objects that do not contain a given key.
   *
   * @param key The key that should not exist
   */
  public AVFriendshipQuery<T> whereDoesNotExist(String key) {
    conditions.whereDoesNotExist(key);
    return this;
  }



  /**
   * Add a constraint for finding string values that end with a provided string. This will be slow
   * for large datasets.
   *
   * @param key The key that the string to match is stored in.
   * @param suffix The substring that the value must end with.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereEndsWith(String key, String suffix) {
    conditions.whereEndsWith(key, suffix);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be equal to the
   * provided value.
   *
   * @param key The key to check.
   * @param value The value that the AVObject must contain.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereEqualTo(String key, Object value) {
    if (value instanceof AVObject) {
      addWhereItem(key, QueryOperation.EQUAL_OP, AVUtils.mapFromPointerObject((AVObject) value));
    } else {
      addWhereItem(key, QueryOperation.EQUAL_OP, value);
    }
    return this;
  }

  private AVFriendshipQuery<T> addWhereItem(QueryOperation op) {
    conditions.addWhereItem(op);
    return this;
  }

  private AVFriendshipQuery<T> addOrItems(QueryOperation op) {
    conditions.addOrItems(op);
    return this;
  }



  protected AVFriendshipQuery<T> addWhereItem(String key, String op, Object value) {
    return addWhereItem(new QueryOperation(key, op, value));
  }

  /**
   * Add a constraint for finding objects that contain the given key.
   *
   * @param key The key that should exist.
   */
  public AVFriendshipQuery<T> whereExists(String key) {
    conditions.whereExists(key);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be greater than the
   * provided value.
   *
   * @param key The key to check.
   * @param value The value that provides an lower bound.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereGreaterThan(String key, Object value) {
    conditions.whereGreaterThan(key, value);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be greater or equal to
   * than the provided value.
   *
   * @param key The key to check.
   * @param value The value that provides an lower bound.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereGreaterThanOrEqualTo(String key, Object value) {
    conditions.whereGreaterThanOrEqualTo(key, value);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be less than the
   * provided value.
   *
   * @param key The key to check.
   * @param value The value that provides an upper bound.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereLessThan(String key, Object value) {
    conditions.whereLessThan(key, value);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be less or equal to
   * than the provided value.
   *
   * @param key The key to check.
   * @param value The value that provides an lower bound.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereLessThanOrEqualTo(String key, Object value) {
    conditions.whereLessThanOrEqualTo(key, value);
    return this;
  }

  /**
   * Add a regular expression constraint for finding string values that match the provided regular
   * expression. This may be slow for large datasets.
   *
   * @param key The key that the string to match is stored in.
   * @param regex The regular expression pattern to match.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereMatches(String key, String regex) {
    conditions.whereMatches(key, regex);
    return this;
  }

  /**
   * Add a regular expression constraint for finding string values that match the provided regular
   * expression. This may be slow for large datasets.
   *
   * @param key The key that the string to match is stored in.
   * @param regex The regular expression pattern to match.
   * @param modifiers Any of the following supported PCRE modifiers: i - Case insensitive search m -
   *          Search across multiple lines of input
   * @return
   */
  public AVFriendshipQuery<T> whereMatches(String key, String regex, String modifiers) {
    conditions.whereMatches(key, regex, modifiers);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point
   * given.
   *
   * @param key The key that the AVGeoPoint is stored in.
   * @param point The reference AVGeoPoint that is used.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereNear(String key, AVGeoPoint point) {
    conditions.whereNear(key, point);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value not be contained in the
   * provided list of values.
   *
   * @param key The key to check.
   * @param values The values that will not match.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereNotContainedIn(String key, Collection<? extends Object> values) {
    conditions.whereNotContainedIn(key, values);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be not equal to the
   * provided value.
   *
   * @param key The key to check.
   * @param value The value that must not be equalled.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereNotEqualTo(String key, Object value) {
    conditions.whereNotEqualTo(key, value);
    return this;
  }

  /**
   * Add a constraint for finding string values that start with a provided string. This query will
   * use the backend index, so it will be fast even for large datasets.
   *
   * @param key The key that the string to match is stored in.
   * @param prefix The substring that the value must start with.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereStartsWith(String key, String prefix) {
    conditions.whereStartsWith(key, prefix);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's coordinates be contained within
   * a given rectangular geographic bounding box.
   *
   * @param key The key to be constrained.
   * @param southwest The lower-left inclusive corner of the box.
   * @param northeast The upper-right inclusive corner of the box.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereWithinGeoBox(String key, AVGeoPoint southwest,
      AVGeoPoint northeast) {
    conditions.whereWithinGeoBox(key, southwest, northeast);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the maximum distance given. Radius of earth used is 6371.0 kilometers.
   *
   * @param key The key that the AVGeoPoint is stored in.
   * @param point The reference AVGeoPoint that is used.
   * @param maxDistance Maximum distance (in kilometers) of results to return.
   * @return
   */
  public AVFriendshipQuery<T> whereWithinKilometers(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinKilometers(key, point, maxDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the maximum distance given. Radius of earth used is 3958.8 miles.
   */
  public AVFriendshipQuery<T> whereWithinMiles(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinMiles(key, point, maxDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the maximum distance given.
   *
   * @param key The key that the AVGeoPoint is stored in.
   * @param point The reference AVGeoPoint that is used.
   * @param maxDistance Maximum distance (in radians) of results to return.
   * @return Returns the query, so you can chain this call.
   */
  public AVFriendshipQuery<T> whereWithinRadians(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinRadians(key, point, maxDistance);
    return this;
  }
}
