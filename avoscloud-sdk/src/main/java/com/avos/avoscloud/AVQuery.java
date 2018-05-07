package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * The AVQuery class defines a query that is used to fetch AVObjects. The most common use case is
 * finding all objects that match a query through the findInBackground method, using a FindCallback.
 * For example, this sample code fetches all objects of class "MyClass". It calls a different
 * function depending on whether the fetch succeeded or not.
 * </p>
 *
 * <pre>
 *    AVQuery<AVObject> query = AVQuery.getQuery("MyClass");
 *    query.findInBackground(new FindCallback<AVObject>() {
 *        public void done(List<AVObject> objects, AVException e) {
 *            if (e == null) {
 *                objectsWereRetrievedSuccessfully(objects);
 *            } else {
 *                objectRetrievalFailed();
 *            }
 *        }
 *    }
 * </pre>
 * <p>
 * A AVQuery can also be used to retrieve a single object whose id is known, through the
 * getInBackground method, using a GetCallback. For example, this sample code fetches an object of
 * class "MyClass" and id myId. It calls a different function depending on whether the fetch
 * succeeded or not.
 * </p>
 *
 * <pre>
 *    AVQuery<AVObject> query = AVQuery.getQuery("MyClass");
 *    query.getInBackground(myId, new GetCallback<AVObject>() {
 *        public void done(AVObject object, AVException e) {
 *            if (e == null) {
 *                objectWasRetrievedSuccessfully(object);
 *            } else {
 *                objectRetrievalFailed();
 *            }
 *        }
 *    }
 * </pre>
 * <p>
 * A AVQuery can also be used to count the number of objects that match the query without retrieving
 * all of those objects. For example, this sample code counts the number of objects of the class
 * "MyClass".
 * </p>
 *
 * <pre>
 *    AVQuery<AVObject> query = AVQuery.getQuery("MyClass");
 *    query.countInBackground(new CountCallback() {
 *        public void done(int count, AVException e) {
 *            if (e == null) {
 *                objectsWereCounted(count);
 *            } else {
 *                objectCountFailed();
 *            }
 *        }
 *    }
 * </pre>
 * <p>
 * Using the callback methods is usually preferred because the network operation will not block the
 * calling thread. However, in some cases it may be easier to use the find, get or count calls,
 * which do block the calling thread. For example, if your application has already spawned a
 * background task to perform work, that background task could use the blocking calls and avoid the
 * code complexity of callbacks.
 * </p>
 */
public class AVQuery<T extends AVObject> {
  public enum CachePolicy {
    CACHE_ELSE_NETWORK, CACHE_ONLY, CACHE_THEN_NETWORK, IGNORE_CACHE, NETWORK_ELSE_CACHE,
    NETWORK_ONLY;
  }

  private static final String TAG = "com.avos.avoscloud.AVQuery";
  private final static String CLOUD_QUERY_PATH = "cloudQuery";

  private Class<T> clazz;
  private String className;
  private java.lang.Boolean isRunning;
  private CachePolicy cachePolicy = CachePolicy.IGNORE_CACHE;
  private long maxCacheAge = -1;

  private String queryPath;
  private boolean includeACL = false;

  // different from queryPath. externalQueryPath is used by caller directly to
  // create special end point for certain query.
  private String externalQueryPath;
  QueryConditions conditions;

  // getter/setter for fastjson.
  private AVQuery() {
    super();
  }

  Class<T> getClazz() {
    return clazz;
  }

  void setClazz(Class<T> clazz) {
    this.clazz = clazz;
  }

  List<String> getInclude() {
    return conditions.getInclude();
  }

  void setInclude(List<String> include) {
    conditions.setInclude(include);
  }

  Set<String> getSelectedKeys() {
    return conditions.getSelectedKeys();
  }

  void setSelectedKeys(Set<String> selectedKeys) {
    conditions.setSelectedKeys(selectedKeys);
  }

  java.lang.Boolean getIsRunning() {
    return isRunning;
  }

  void setIsRunning(java.lang.Boolean isRunning) {
    this.isRunning = isRunning;
  }

  Map<String, String> getParameters() {
    Map<String, String> result = conditions.getParameters();
    if (this.includeACL && null != result) {
      result.put("returnACL", "true");
    }
    return result;
  }

  void setParameters(Map<String, String> parameters) {
    conditions.setParameters(parameters);
  }

  String getQueryPath() {
    return queryPath;
  }

  void setQueryPath(String queryPath) {
    this.queryPath = queryPath;
  }

  String getExternalQueryPath() {
    return externalQueryPath;
  }

  void setExternalQueryPath(final String path) {
    externalQueryPath = path;
  }

  static String getTag() {
    return TAG;
  }

  Map<String, List<QueryOperation>> getWhere() {
    return conditions.getWhere();
  }

  // End of getter/setter

  /**
   * Accessor for the class name.
   */
  public String getClassName() {
    return className;
  }

  public AVQuery<T> setClassName(String className) {
    this.className = className;
    return this;
  }


  private void generateQueryPath() {
    // This method is only used for cache and currently only storage service use cache
    if (AVUtils.isBlankString(queryPath)) {
      conditions.assembleParameters();
      queryPath =
          PaasClient.storageInstance().buildUrl(queryPath(),
                  new AVRequestParams(getParameters()));
    }
  }

  /**
   * Accessor for the caching policy.
   */
  public CachePolicy getCachePolicy() {
    return cachePolicy;
  }

  /**
   * Change the caching policy of this query.
   *
   * @return this query.
   */
  public AVQuery<T> setCachePolicy(CachePolicy cachePolicy) {
    this.cachePolicy = cachePolicy;
    return this;
  }

  public CachePolicy getPolicy() {
    return cachePolicy;
  }

  /**
   * Change the caching policy of this query.
   *
   * @return this query.
   */
  public AVQuery<T> setPolicy(CachePolicy policy) {
    this.cachePolicy = policy;
    return this;
  }

  /**
   * Gets the maximum age of cached data that will be considered in this query. The returned value
   * is in milliseconds
   */
  public long getMaxCacheAge() {
    return maxCacheAge;
  }

  /**
   * Sets the maximum age of cached data that will be considered in this query.
   *
   * @return this query.
   */
  public AVQuery<T> setMaxCacheAge(long maxCacheAge) {
    this.maxCacheAge = maxCacheAge;
    return this;
  }

  public boolean isTrace() {
    return conditions.isTrace();
  }

  /**
   * Turn on performance tracing of finds. If performance tracing is already turned on this does
   * nothing. In general you don't need to call trace.
   *
   * @return this query.
   */
  public AVQuery<T> setTrace(boolean trace) {
    conditions.setTrace(trace);
    return this;
  }

  /**
   * Constructs a query. A default query with no further parameters will retrieve all AVObjects of
   * the provided class.
   *
   * @param theClassName The name of the class to retrieve AVObjects for.
   * @return the query object.
   */
  public AVQuery(String theClassName) {
    this(theClassName, null);
  }

  /**
   * clone a new query object, which fully same to this.
   *
   * @return a new AVQuery object.
   */
  public AVQuery clone() {
    AVQuery query = new AVQuery(this.className, this.clazz);

    query.isRunning = false;
    query.cachePolicy = this.cachePolicy;
    query.maxCacheAge = this.maxCacheAge;
    query.queryPath = this.queryPath;
    query.externalQueryPath = this.externalQueryPath;
    query.conditions = null != this.conditions? this.conditions.clone(): null;
    return query;
  }

  AVQuery(String theClassName, Class<T> clazz) {
    AVUtils.checkClassName(theClassName);
    this.className = theClassName;
    this.clazz = clazz;
    this.conditions = new QueryConditions();
  }

  /**
   * Constructs a query. A default query with no further parameters will retrieve all AVObjects of
   * the provided class.
   *
   * @param theClassName The name of the class to retrieve AVObjects for.
   * @return the query object.
   */
  public static <T extends AVObject> AVQuery<T> getQuery(String theClassName) {
    return new AVQuery<T>(theClassName);
  }

  /**
   * Create a AVQuery with special sub-class.
   *
   * @param clazz The AVObject subclass
   * @return The AVQuery
   */
  public static <T extends AVObject> AVQuery<T> getQuery(Class<T> clazz) {
    return new AVQuery<T>(AVObject.getSubClassName(clazz), clazz);
  }


  /**
   * Cancels the current network request (if one is running).
   */
  @Deprecated
  public void cancel() {

  }

  /**
   * Clears the cached result for all queries.
   */
  public static void clearAllCachedResults() {
    AVCacheManager.clearAllCache();
  }

  /**
   * Removes the previously cached result for this query, forcing the next find() to hit the
   * network. If there is no cached result for this query, then this is a no-op.
   */
  public void clearCachedResult() {
    generateQueryPath();
    if (!AVUtils.isBlankString(queryPath)) {
      AVCacheManager.sharedInstance().delete(queryPath);
    }
  }


  private String queryPath() {
    if (!AVUtils.isBlankString(externalQueryPath)) {
      return externalQueryPath;
    }
    return AVPowerfulUtils.getEndpoint(getClassName());
  }

  /**
   * Deprecated. Please use AVUser.getQuery() instead. Constructs a query for AVUsers
   *
   * @see AVUser#getQuery
   * @deprecated
   */
  @Deprecated
  public static AVQuery<AVUser> getUserQuery() {
    return AVUser.getQuery();
  }

  /**
   * Returns whether or not this query has a cached result.
   */
  public boolean hasCachedResult() {
    generateQueryPath();
    return !AVUtils.isBlankString(this.queryPath)
        && AVCacheManager.sharedInstance().hasCache(this.queryPath);
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
  public AVQuery<T> setLimit(int limit) {
    conditions.setLimit(limit);
    return this;
  }

  /**
   * @see #setLimit(int)
   * @param limit
   * @return this query.
   */
  public AVQuery<T> limit(int limit) {
    this.setLimit(limit);
    return this;
  }

  /**
   * @see #setSkip(int)
   * @param skip
   * @return this query.
   */
  public AVQuery<T> skip(int skip) {
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
  public AVQuery<T> setSkip(int skip) {
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
  public AVQuery<T> setOrder(String order) {
    conditions.setOrder(order);
    return this;
  }

  /**
   * @see #setOrder(String)
   * @param order
   * @return
   */
  public AVQuery<T> order(String order) {
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
  public AVQuery<T> addAscendingOrder(String key) {
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
  public AVQuery<T> addDescendingOrder(String key) {
    conditions.addDescendingOrder(key);
    return this;
  }

  /**
   * Include ACL attribute.
   *
   * @param includeACL need ACL attribute returned or not.
   */
  public AVQuery<T> includeACL(boolean includeACL) {
    this.includeACL = includeACL;
    return this;
  }

  /**
   * Include nested AVObjects for the provided key. You can use dot notation to specify which fields
   * in the included object that are also fetched.
   *
   * @param key The key that should be included.
   */
  public AVQuery<T> include(String key) {
    conditions.include(key);
    return this;
  }

  /**
   * Restrict the fields of returned AVObjects to only include the provided keys. If this is called
   * multiple times, then all of the keys specified in each of the calls will be included.
   *
   * @param keys The set of keys to include in the result.
   */
  public AVQuery<T> selectKeys(Collection<String> keys) {
    conditions.selectKeys(keys);
    return this;
  }

  /**
   * Sorts the results in ascending order by the given key.
   *
   * @param key The key to order by.
   * @return Returns the query, so you can chain this call.
   */
  public AVQuery<T> orderByAscending(String key) {
    conditions.orderByAscending(key);
    return this;
  }

  /**
   * Sorts the results in descending order by the given key.
   *
   * @param key The key to order by.
   * @return Returns the query, so you can chain this call.
   */
  public AVQuery<T> orderByDescending(String key) {
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
  public AVQuery<T> whereContainedIn(String key, Collection<? extends Object> values) {
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
  public AVQuery<T> whereContains(String key, String substring) {
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
  public AVQuery<T> whereSizeEqual(String key, int size) {
    conditions.whereSizeEqual(key, size);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value match another AVQuery.
   * This only works on keys whose values are AVObjects or lists of AVObjects. Add a constraint to
   * the query that requires a particular key's value to contain every one of the provided list of
   * values.
   *
   * @param key The key to check. This key's value must be an array.
   * @param values The values that will match.
   * @return Returns the query, so you can chain this call.
   */
  public AVQuery<T> whereContainsAll(String key, Collection<?> values) {
    conditions.whereContainsAll(key, values);
    return this;
  }

  /**
   * Add a constraint for finding objects that do not contain a given key.
   *
   * @param key The key that should not exist
   */
  public AVQuery<T> whereDoesNotExist(String key) {
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
  public AVQuery<T> whereEndsWith(String key, String suffix) {
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
  public AVQuery<T> whereEqualTo(String key, Object value) {
    conditions.whereEqualTo(key, value);
    return this;
  }

  private AVQuery<T> addWhereItem(QueryOperation op) {
    conditions.addWhereItem(op);
    return this;
  }

  private AVQuery<T> addOrItems(QueryOperation op) {
    conditions.addOrItems(op);
    return this;
  }

  private AVQuery<T> addAndItems(AVQuery query) {
    conditions.addAndItems(query.conditions);
    return this;
  }

  protected AVQuery<T> addWhereItem(String key, String op, Object value) {
    conditions.addWhereItem(key, op, value);
    return this;
  }

  /**
   * Add a constraint for finding objects that contain the given key.
   *
   * @param key The key that should exist.
   */
  public AVQuery<T> whereExists(String key) {
    conditions.whereExists(key);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value to be greater than the
   * provided value.
   *w
   * @param key The key to check.
   * @param value The value that provides an lower bound.
   * @return Returns the query, so you can chain this call.
   */
  public AVQuery<T> whereGreaterThan(String key, Object value) {
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
  public AVQuery<T> whereGreaterThanOrEqualTo(String key, Object value) {
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
  public AVQuery<T> whereLessThan(String key, Object value) {
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
  public AVQuery<T> whereLessThanOrEqualTo(String key, Object value) {
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
  public AVQuery<T> whereMatches(String key, String regex) {
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
  public AVQuery<T> whereMatches(String key, String regex, String modifiers) {
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
  public AVQuery<T> whereNear(String key, AVGeoPoint point) {
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
  public AVQuery<T> whereNotContainedIn(String key, Collection<? extends Object> values) {
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
  public AVQuery<T> whereNotEqualTo(String key, Object value) {
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
  public AVQuery<T> whereStartsWith(String key, String prefix) {
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
  public AVQuery<T> whereWithinGeoBox(String key, AVGeoPoint southwest, AVGeoPoint northeast) {
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
  public AVQuery<T> whereWithinKilometers(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinKilometers(key, point, maxDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the given ring area
   *
   * Radius of earth used is 6371.0 kilometers.
   * 
   * @param key The key that the AVGeoPoint is stored in.
   * @param point The reference AVGeoPoint that is used.
   * @param maxDistance outer radius of the given ring in kilometers
   * @param minDistance inner radius of the given ring in kilometers
   * @return
   */
  public AVQuery<T> whereWithinKilometers(String key, AVGeoPoint point, double maxDistance,
      double minDistance) {
    conditions.whereWithinKilometers(key, point, maxDistance, minDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the maximum distance given. Radius of earth used is 3958.8 miles.
   */
  public AVQuery<T> whereWithinMiles(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinMiles(key, point, maxDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point
   * given and within the given ring.
   * 
   * Radius of earth used is 3958.8 miles.
   * 
   * @param key
   * @param point
   * @param maxDistance
   * @param minDistance
   * @return
   */
  public AVQuery<T> whereWithinMiles(String key, AVGeoPoint point, double maxDistance,
      double minDistance) {
    conditions.whereWithinMiles(key, point, maxDistance, minDistance);
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
  public AVQuery<T> whereWithinRadians(String key, AVGeoPoint point, double maxDistance) {
    conditions.whereWithinRadians(key, point, maxDistance);
    return this;
  }

  /**
   * Add a proximity based constraint for finding objects with key point values near the point given
   * and within the maximum distance given.
   *
   * @param key
   * @param point
   * @param maxDistance
   * @param minDistance
   * @return
   */

  public AVQuery<T> whereWithinRadians(String key, AVGeoPoint point, double maxDistance,
      double minDistance) {
    conditions.whereWithinRadians(key, point, maxDistance, minDistance);
    return this;
  }

  /**
   * Constructs a query that is the or of the given queries.
   *
   * @param queries The list of AVQueries to 'or' together
   * @return A AVQuery that is the 'or' of the passed in queries
   */
  public static <T extends AVObject> AVQuery<T> or(List<AVQuery<T>> queries) {
    String className = null;
    if (queries.size() > 0) {
      className = queries.get(0).getClassName();
    }
    AVQuery<T> result = new AVQuery<T>(className);
    if (queries.size() > 1) {
      for (AVQuery<T> query : queries) {
        if (!className.equals(query.getClassName())) {
          throw new IllegalArgumentException("All queries must be for the same class");
        }
        result.addOrItems(new QueryOperation("$or", "$or", query.conditions
            .compileWhereOperationMap()));
      }
    } else {
      result.setWhere(queries.get(0).conditions.getWhere());
    }

    return result;
  }

  public static <T extends AVObject> AVQuery<T> and(List<AVQuery<T>> queries) {
    String className = null;
    if (queries.size() > 0) {
      className = queries.get(0).getClassName();
    }
    AVQuery<T> result = new AVQuery<T>(className);
    if (queries.size() > 1) {
      for (AVQuery<T> query : queries) {
        if (!className.equals(query.getClassName())) {
          throw new IllegalArgumentException("All queries must be for the same class");
        }
        result.addAndItems(query);
      }
    } else {
      result.setWhere(queries.get(0).conditions.getWhere());
    }

    return result;
  }

  // 'where={"belongTo":{"$select":{"query":{"className":"Person","where":{"gender":"Male"}},"key":"name"}}}'

  /**
   * Add a constraint to the query that requires a particular key's value matches a value for a key
   * in the results of another AVQuery
   *
   * @param key The key whose value is being checked
   * @param keyInQuery The key in the objects from the sub query to look in
   * @param query The sub query to run
   * @return Returns the query so you can chain this call.
   */
  public AVQuery<T> whereMatchesKeyInQuery(String key, String keyInQuery, AVQuery<?> query) {
    Map<String, Object> inner = new HashMap<String, Object>();
    inner.put("className", query.getClassName());
    inner.put("where", query.conditions.compileWhereOperationMap());
    if (query.conditions.getSkip() > 0) inner.put("skip", query.conditions.getSkip());
    if (query.conditions.getLimit() > 0) inner.put("limit", query.conditions.getLimit());
    if (!AVUtils.isBlankContent(query.getOrder())) inner.put("order", query.getOrder());

    Map<String, Object> queryMap = new HashMap<String, Object>();
    queryMap.put("query", inner);
    queryMap.put("key", keyInQuery);
    return addWhereItem(key, "$select", queryMap);
  }

  /**
   * Add a constraint to the query that requires a particular key's value match another AVQuery.
   * This only works on keys whose values are AVObjects or lists of AVObjects.
   *
   * @param key The key to check.
   * @param query The query that the value should match
   * @return Returns the query so you can chain this call.
   */
  public AVQuery<T> whereMatchesQuery(String key, AVQuery<?> query) {
    Map<String, Object> map =
        AVUtils.createMap("where", query.conditions.compileWhereOperationMap());
    map.put("className", query.className);
    if (query.conditions.getSkip() > 0) map.put("skip", query.conditions.getSkip());
    if (query.conditions.getLimit() > 0) map.put("limit", query.conditions.getLimit());
    if (!AVUtils.isBlankContent(query.getOrder())) map.put("order", query.getOrder());
    addWhereItem(key, "$inQuery", map);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value does not match any value
   * for a key in the results of another AVQuery.
   *
   * @param key The key whose value is being checked and excluded
   * @param keyInQuery The key in the objects from the sub query to look in
   * @param query The sub query to run
   * @return Returns the query so you can chain this call.
   */
  public AVQuery<T> whereDoesNotMatchKeyInQuery(String key, String keyInQuery, AVQuery<?> query) {
    Map<String, Object> map = AVUtils.createMap("className", query.className);
    map.put("where", query.conditions.compileWhereOperationMap());

    Map<String, Object> queryMap = AVUtils.createMap("query", map);
    queryMap.put("key", keyInQuery);

    addWhereItem(key, "$dontSelect", queryMap);
    return this;
  }

  /**
   * Add a constraint to the query that requires a particular key's value does not match another
   * AVQuery. This only works on keys whose values are AVObjects or lists of AVObjects.
   *
   * @param key The key to check.
   * @param query The query that the value should not match
   * @return Returns the query so you can chain this call.
   */
  public AVQuery<T> whereDoesNotMatchQuery(String key, AVQuery<?> query) {
    Map<String, Object> map = AVUtils.createMap("className", query.className);
    map.put("where", query.conditions.compileWhereOperationMap());

    addWhereItem(key, "$notInQuery", map);
    return this;
  }

  AVQuery<T> setWhere(Map<String, List<QueryOperation>> value) {
    conditions.setWhere(value);
    return this;
  }

  /**
   * 通过cql查询对象
   *
   * @param cql cql语句
   * @param params 查询参数列表
   * @return
   * @throws Exception
   */

  public static AVCloudQueryResult doCloudQuery(String cql, Object... params) throws Exception {
    final AVCloudQueryResult returnValue = new AVCloudQueryResult();
    doCloudQueryInBackground(cql, new CloudQueryCallback<AVCloudQueryResult>() {

      @Override
      public void done(AVCloudQueryResult result, AVException avException) {
        if (avException != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(avException, null));
        } else {
          returnValue.setCount(result.getCount());
          returnValue.setResults(result.getResults());
        }
      }
    }, AVObject.class, true, params);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return returnValue;
  }

  /**
   * 通过cql查询对象
   *
   * @param cql
   * @param clazz
   * @param params 查询参数列表
   * @return
   * @throws Exception
   */
  public static AVCloudQueryResult doCloudQuery(String cql, Class<? extends AVObject> clazz,
      Object... params) throws Exception {

    final AVCloudQueryResult returnValue = new AVCloudQueryResult();
    doCloudQueryInBackground(cql, new CloudQueryCallback<AVCloudQueryResult>() {

      @Override
      public void done(AVCloudQueryResult result, AVException avException) {
        if (avException != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(avException, null));
        } else {
          returnValue.setCount(result.getCount());
          returnValue.setResults(result.getResults());
        }
      }

    }, clazz, true, params);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return returnValue;

  }

  /**
   * 通过cql查询对象
   *
   * @param cql
   * @param callback
   * @param params 查询参数列表
   */

  public static void doCloudQueryInBackground(String cql,
      CloudQueryCallback<AVCloudQueryResult> callback, Object... params) {
    doCloudQueryInBackground(cql, callback, AVObject.class, false, params);
  }

  /**
   * 通过cql查询对象
   *
   * @param cql
   * @param callback
   * @param clazz
   * @param params
   */
  public static void doCloudQueryInBackground(String cql,
      CloudQueryCallback<AVCloudQueryResult> callback, Class<? extends AVObject> clazz,
      Object... params) {
    doCloudQueryInBackground(cql, callback, clazz, false, params);
  }

  /**
   * 通过cql查询对象
   *
   * 请在异步线程中调用此方法，在UI线程中，请调用doCloudQueryInBackground方法
   *
   * @param cql
   * @return
   * @throws Exception
   */
  public static AVCloudQueryResult doCloudQuery(String cql) throws Exception {
    return doCloudQuery(cql, AVObject.class);
  }

  /**
   * 通过cql查询对象
   *
   * 请在异步线程中调用此方法，在UI线程中，请调用doCloudQueryInBackground方法
   *
   * @param cql
   * @param clazz
   * @return
   * @throws Exception
   */
  public static AVCloudQueryResult doCloudQuery(String cql, Class<? extends AVObject> clazz)
      throws Exception {
    final AVCloudQueryResult returnValue = new AVCloudQueryResult();
    doCloudQueryInBackground(cql, new CloudQueryCallback<AVCloudQueryResult>() {

      @Override
      public void done(AVCloudQueryResult result, AVException avException) {
        if (avException != null) {
          AVExceptionHolder.add(AVErrorUtils.createException(avException, null));
        } else {
          returnValue.setCount(result.getCount());
          returnValue.setResults(result.getResults());
        }
      }

      @Override
      public boolean mustRunOnUIThread() {
        return false;
      }
    }, clazz, true, null);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return returnValue;
  }

  /**
   * 通过cql查询对象
   *
   * @param cql
   * @param callback
   */
  public static void doCloudQueryInBackground(String cql,
      CloudQueryCallback<AVCloudQueryResult> callback) {
    doCloudQueryInBackground(cql, callback, AVObject.class, false, null);
  }

  /**
   * 通过cql查询对象
   *
   * @param cql
   * @param callback
   * @param clazz
   */
  public static void doCloudQueryInBackground(String cql,
      CloudQueryCallback<AVCloudQueryResult> callback, Class<? extends AVObject> clazz) {
    doCloudQueryInBackground(cql, callback, clazz, false, null);
  }

  private static void doCloudQueryInBackground(String cql,
      final CloudQueryCallback<AVCloudQueryResult> callback, final Class<? extends AVObject> clazz,
      boolean sync, Object[] params) {
    List<Object> pvalue = new LinkedList<Object>();
    if (params != null) {
      for (Object o : params) {
        pvalue.add(o);
      }
    }

    AVRequestParams p = new AVRequestParams();
    p.put("cql", cql);
    if (!AVUtils.isEmptyList(pvalue)) {
      p.put("pvalues", AVUtils.jsonStringFromObjectWithNull(pvalue));
    }
    PaasClient.storageInstance().getObject(CLOUD_QUERY_PATH, p, sync, null,
        new GenericObjectCallback() {
          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }

          @Override
          public void onSuccess(String content, AVException e) {
            try {
              AVCloudQueryResult result = processCloudResults(content, clazz);
              if (callback != null) {
                callback.internalDone(result, null);
              }
            } catch (Exception processException) {
              if (callback != null) {
                callback.internalDone(null, AVErrorUtils.createException(processException, null));
              }
            }
          }
        });
  }

  private static <T extends AVObject> AVCloudQueryResult processCloudResults(String content,
      Class<T> clazz) throws Exception {
    AVCloudQueryResult result = new AVCloudQueryResult();
    if (AVUtils.isBlankContent(content)) {
      List<AVObject> emptyResult = Collections.emptyList();
      result.setResults(emptyResult);
      return result;
    }
    AVResponse resp = new AVResponse();
    resp = JSON.parseObject(content, resp.getClass());

    List<T> objects = new LinkedList<T>();
    if (resp.results != null) {
      for (Map item : resp.results) {
        if (item != null && !item.isEmpty()) {
          AVObject object;
          if (clazz != null) {
            object = clazz.newInstance();
            if (AVUtils.isBlankString(object.getClassName())) {
              object.setClassName(resp.className);
            }
          } else {
            object = new AVObject(resp.className);
          }
          AVUtils.copyPropertiesFromMapToAVObject(item, object);
          objects.add((T) object);
        }
      }
    }
    result.setCount(resp.count);
    result.setResults(objects);
    return result;
  }

  // this is for AVStatusQuery
  protected void processAdditionalInfo(String content, FindCallback<T> callback) {

  }

  @SuppressWarnings("unchecked")
  protected List<T> processResults(String content) throws Exception {
    if (AVUtils.isBlankContent(content)) {
      return Collections.emptyList();
    }
    AVResponse resp = new AVResponse();
    resp = JSON.parseObject(content, resp.getClass());

    List<T> result = new LinkedList<T>();
    for (Map item : resp.results) {
      if (item != null && !item.isEmpty()) {
        AVObject object;
        if (clazz != null) {
          object = clazz.newInstance();
        } else {
          object = AVUtils.newAVObjectByClassName(resp.className, this.getClassName());
        }
        AVUtils.copyPropertiesFromMapToAVObject(item, object);
        object.rebuildInstanceData();
        result.add((T) object);
      }
    }
    return result;
  }

  /**
   * Retrieves a list of AVObjects that satisfy this query from the server in a background thread.
   * This is preferable to using find(), unless your code is already running in a background thread.
   *
   * @param callback callback.done(objectList, e) is called when the find completes.
   */
  public void findInBackground(FindCallback<T> callback) {
    assembleParameters();
    final FindCallback<T> internalCallback = callback;

    String path = queryPath();
    queryPath =
        PaasClient.storageInstance().getObject(path, new AVRequestParams(getParameters()), false,
            null, new GenericObjectCallback() {
              @Override
              public void onSuccess(String content, AVException e) {
                try {
                  List<T> result = AVQuery.this.processResults(content);
                  processAdditionalInfo(content, internalCallback);
                  if (internalCallback != null) {
                    internalCallback.internalDone(result, null);
                  }
                } catch (Exception ex) {
                  if (internalCallback != null) {
                    internalCallback.internalDone(null, AVErrorUtils.createException(ex, null));
                  }
                }
              }

              @Override
              public void onFailure(Throwable error, String content) {
                if (internalCallback != null) {
                  internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
                }
              }
            }, cachePolicy, this.maxCacheAge);
  }

  /**
   * Constructs a AVObject whose id is already known by fetching data from the server. This mutates
   * the AVQuery.
   *
   * @param theObjectId Object id of the AVObject to fetch.
   */
  @SuppressWarnings("unchecked")
  public T get(String theObjectId) throws AVException {
    final Object[] result = {null};
    this.getInBackground(theObjectId, true, new GetCallback<T>() {
      @Override
      public void done(T object, AVException e) {
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
    return (T) result[0];
  }

  /**
   * Retrieves at most one AVObject that satisfies this query. Uses the network and/or the cache,
   * depending on the cache policy. This mutates the AVQuery.
   *
   * @return A AVObject obeying the conditions set in this query, or null if none found.
   */
  @SuppressWarnings("unchecked")
  public T getFirst() throws AVException {
    final Object[] result = {null};
    getFirstInBackground(true, new GetCallback<T>() {
      @Override
      public void done(AVObject object, AVException e) {
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
    return (T) result[0];
  }

  /**
   * Retrieves at most one AVObject that satisfies this query from the server in a background
   * thread. This is preferable to using getFirst(), unless your code is already running in a
   * background thread. This mutates the AVQuery.
   *
   * @param callback callback.done(object, e) is called when the find completes.
   */
  public void getFirstInBackground(GetCallback<T> callback) {
    getFirstInBackground(false, callback);
  }

  private void getFirstInBackground(boolean sync, GetCallback<T> callback) {
    this.assembleParameters();
    Map<String, String> parameters = getParameters();
    parameters.put("limit", Integer.toString(1));

    final GetCallback<T> internalCallback = callback;
    PaasClient.storageInstance().getObject(queryPath(), new AVRequestParams(getParameters()), sync,
        null, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            try {
              List<T> result = AVQuery.this.processResults(content);
              if (internalCallback != null) {
                T first = null;
                AVException error = null;
                if (result.size() > 0) {
                  first = result.get(0);
                } else {
                  first = null;
                }
                internalCallback.internalDone(first, error);
              }
            } catch (Exception ex) {
              if (internalCallback != null) {
                internalCallback.internalDone(null, AVErrorUtils.createException(ex, null));
              }
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

  /**
   * Constructs a AVObject whose id is already known by fetching data from the server in a
   * background thread. This does not use caching. This is preferable to using the
   * AVObject(className, objectId) constructor, unless your code is already running in a background
   * thread.
   *
   * @param objectId Object id of the AVObject to fetch
   * @param callback callback.done(object, e) will be called when the fetch completes.
   */
  public void getInBackground(String objectId, GetCallback<T> callback) {
    final GetCallback<T> internalCallback = callback;
    this.getInBackground(objectId, false, new GetCallback<T>() {
      @Override
      public void done(T object, AVException e) {
        if (internalCallback != null) {
          internalCallback.internalDone(object, e);
        }
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void getInBackground(String objectId, boolean sync, GetCallback<T> callback) {
    String path = AVPowerfulUtils.getEndpointByAVClassName(getClassName(), objectId);
    final GetCallback<T> internalCallback = callback;
    this.assembleParameters();
    PaasClient.storageInstance().getObject(path, new AVRequestParams(getParameters()), sync, null,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            T object = null;
            AVException error = e;
            if (AVUtils.isBlankContent(content)) {
              object = null;
              error = new AVException(AVException.OBJECT_NOT_FOUND, "Object is not found.");
            } else {
              try {
                if (AVQuery.this.clazz != null) {
                  try {
                    object = clazz.newInstance();
                  } catch (Exception er) {
                    internalCallback.internalDone(AVErrorUtils.createException(er,
                        "Please create non-params constructor"));
                  }
                } else {
                  object =
                      (T) AVUtils.newAVObjectByClassName(AVQuery.this.getClassName());
                }
                AVUtils.copyPropertiesFromJsonStringToAVObject(content, object);
                object.rebuildInstanceData();
              } catch (Exception exception) {
                if (internalCallback != null) {
                  internalCallback.internalDone(object, new AVException(exception));
                }
              }
            }
            if (internalCallback != null) {
              internalCallback.internalDone(object, error);
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

  /**
   * Counts the number of objects that match this query. This does not use caching.
   */
  public int count() throws AVException {
    final int[] value = {0};
    countInBackground(true, new CountCallback() {
      @Override
      public void done(int count, AVException e) {
        if (e == null) {
          value[0] = count;
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
    return value[0];
  }

  protected int count(boolean needsLock) {
    final int[] value = {0};
    countInBackground(true, new CountCallback() {
      @Override
      public void done(int count, AVException e) {
        value[0] = count;
      }
    });
    return value[0];
  }

  /**
   * Counts the number of objects that match this query in a background thread. This does not use
   * caching.
   *
   * @param callback callback.done(count, e) will be called when the count completes.
   */
  public void countInBackground(CountCallback callback) {
    this.countInBackground(false, callback);
  }

  private void countInBackground(boolean sync, CountCallback callback) {
    conditions.assembleParameters();
    Map<String, String> parameters = conditions.getParameters();
    parameters.put("count", "1");
    parameters.put("limit", "0");
    final CountCallback internalCallback = callback;
    String path = queryPath();
    queryPath =
        PaasClient.storageInstance().getObject(path, new AVRequestParams(parameters), sync,
            null, new GenericObjectCallback() {
              @Override
              public void onSuccess(String content, AVException e) {
                try {
                  AVResponse resp = JSON.parseObject(content, AVResponse.class);
                  if (internalCallback != null) {
                    internalCallback.internalDone(resp.count, null);
                  }
                } catch (Exception jsonParseException) {
                  internalCallback.internalDone(AVErrorUtils.createException(jsonParseException,
                      "Exception during response parse"));
                }
              }

              @Override
              public void onFailure(Throwable error, String content) {
                if (internalCallback != null) {
                  internalCallback.internalDone(0, AVErrorUtils.createException(error, content));
                }
              }
            }, cachePolicy, maxCacheAge);
  }

  /**
   * Retrieves a list of AVObjects that satisfy this query. Uses the network and/or the cache,
   * depending on the cache policy.
   *
   * @return A list of all AVObjects obeying the conditions set in this query.
   */
  public List<T> find() throws AVException {
    String path = queryPath();
    this.assembleParameters();
    final List<T> result = new ArrayList<T>();
    queryPath =
        PaasClient.storageInstance().getObject(path, new AVRequestParams(getParameters()), true,
            null, new GenericObjectCallback() {
              @Override
              public void onSuccess(String content, AVException e) {
                try {
                  result.addAll(AVQuery.this.processResults(content));
                } catch (Exception ex) {
                  AVExceptionHolder.add(AVErrorUtils.createException(ex, null));
                }
              }

              @Override
              public void onFailure(Throwable error, String content) {
                AVExceptionHolder.add(AVErrorUtils.createException(error, content));
              }
            }, cachePolicy, this.maxCacheAge);
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return result;
  }


  /**
   * Delete all objects that are retrieved by this query.
   *
   * @since 1.4.0
   * @throws AVException
   */
  public void deleteAll() throws AVException {
    AVObject.deleteAll(this.find());
  }

  /**
   * Delete all objects that are retrieved by this query in background.
   *
   * @since 1.4.0
   * @param cb The delete callback.
   */
  public void deleteAllInBackground(final DeleteCallback cb) {
    this.findInBackground(new FindCallback<T>() {

      @Override
      public void done(List<T> avObjects, AVException avException) {
        if (avException != null) {
          cb.internalDone(null, avException);
        } else {
          AVObject.deleteAllInBackground(avObjects, cb);
        }

      }

    });
  }

  protected Map<String, String> assembleParameters() {
    return conditions.assembleParameters();
  }
}
