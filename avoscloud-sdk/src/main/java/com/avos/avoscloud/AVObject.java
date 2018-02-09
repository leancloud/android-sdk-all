package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.ops.AVOp;
import com.avos.avoscloud.ops.AddOp;
import com.avos.avoscloud.ops.AddRelationOp;
import com.avos.avoscloud.ops.AddUniqueOp;
import com.avos.avoscloud.ops.CollectionOp;
import com.avos.avoscloud.ops.CompoundOp;
import com.avos.avoscloud.ops.DeleteOp;
import com.avos.avoscloud.ops.IncrementOp;
import com.avos.avoscloud.ops.RemoveOp;
import com.avos.avoscloud.ops.RemoveRelationOp;
import com.avos.avoscloud.ops.SetOp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * The AVObject is a local representation of data that can be saved and retrieved from the AVOSCloud
 * cloud.
 * </p>
 * <p>
 * The basic workflow for creating new data is to construct a new AVObject, use put() to fill it
 * with data, and then use save() to persist to the database.
 * </p>
 * <p>
 * The basic workflow for accessing existing data is to use a AVQuery to specify which existing data
 * to retrieve.
 * </p>
 */
public class AVObject implements Parcelable {

  public static final String CREATED_AT = "createdAt";
  public static final String UPDATED_AT = "updatedAt";
  public static final String OBJECT_ID = "objectId";

  protected boolean requestStatistic = true;

  static {
    JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  }

  private static final String LOGTAG = AVObject.class.getName();
  static final int UUID_LEN = UUID.randomUUID().toString().length();

  private final class FetchObjectCallback extends GenericObjectCallback {
    private final AVCallback<AVObject> internalCallback;

    private FetchObjectCallback(AVCallback<AVObject> internalCallback) {
      this.internalCallback = internalCallback;
    }

    @Override
    public void onSuccess(String content, AVException e) {
      AVException error = e;
      AVObject object = AVObject.this;
      if (!AVUtils.isBlankContent(content)) {
        AVUtils.copyPropertiesFromJsonStringToAVObject(content, object);
        AVObject.this.isDataReady = true;
        AVObject.this.onDataSynchronized();
      } else {
        object = null;
        error = new AVException(AVException.OBJECT_NOT_FOUND, "The object is not Found");
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
  }

  private String className;
  protected String objectId;
  protected String updatedAt;
  protected String createdAt;
  private String uuid;
  private volatile boolean fetchWhenSave = false;

  @JSONField
  private boolean isDataReady;
  transient protected AVACL acl;
  private transient volatile boolean running;
  Map<String, Object> serverData;
  Map<String, AVOp> operationQueue;
  Map<String, Object> instanceData;
  Map<String, AVOp> tempDataForServerSaving;

  public AVObject() {
    super();
    serverData = new ConcurrentHashMap<>();
    operationQueue = new ConcurrentHashMap<String, AVOp>();
    instanceData = new ConcurrentHashMap<String, Object>();
    tempDataForServerSaving = new ConcurrentHashMap<String, AVOp>();
    className = getSubClassName(this.getClass());
    init();
  }

  @Override
  public String toString() {
    return JSON.toJSONString(this, ObjectValueFilter.instance,
        SerializerFeature.WriteClassName,
        SerializerFeature.DisableCircularReferenceDetect);
  }

  /**
   * 将本对象转化为一个jsonObject
   *
   * @return
   */
  public JSONObject toJSONObject() {
    Map<String, Object> dataMap = new HashMap<String, Object>();
    for (Map.Entry<String, Object> entry : instanceData.entrySet()) {
      dataMap.put(entry.getKey(), parseObject(entry.getValue()));
    }
    dataMap.put(OBJECT_ID, this.objectId);
    dataMap.put(CREATED_AT, this.createdAt);
    dataMap.put(UPDATED_AT, this.updatedAt);
    dataMap.put("className", this.className);

    return new JSONObject(dataMap);
  }

  private static Object parseObject(Object object) {
    if (object == null) {
      return null;
    } else if (object instanceof Map) {
      return getParsedMap((Map<String, Object>) object);
    } else if (object instanceof Collection) {
      return getParsedList((Collection) object);
    } else if (object instanceof AVObject) {
      return ((AVObject) object).toJSONObject();
    } else if (object instanceof AVGeoPoint) {
      return AVUtils.mapFromGeoPoint((AVGeoPoint) object);
    } else if (object instanceof Date) {
      return AVUtils.mapFromDate((Date) object);
    } else if (object instanceof byte[]) {
      return AVUtils.mapFromByteArray((byte[]) object);
    } else if (object instanceof AVFile) {
      return ((AVFile) object).toJSONObject();
    } else if (object instanceof org.json.JSONObject) {
      return JSON.parse(object.toString());
    } else if (object instanceof org.json.JSONArray) {
      return JSON.parse(object.toString());
    } else {
      return object;
    }
  }

  private static List getParsedList(Collection list) {
    List newList = new ArrayList(list.size());

    for (Object o : list) {
      newList.add(parseObject(o));
    }

    return newList;
  }

  private static Map<String, Object> getParsedMap(Map<String, Object> map) {
    Map newMap = new HashMap<String, Object>(map.size());

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      Object o = entry.getValue();
      newMap.put(key, parseObject(o));
    }

    return newMap;
  }

  /**
   * internal method for fastjson getter/setter
   *
   * @return
   */

  Map<String, Object> getServerData() {
    return serverData;
  }

  /**
   * internal method for fastjson getter/setter
   *
   * @param serverData
   */
  void setServerData(Map<String, Object> serverData) {
    this.serverData.clear();
    this.serverData.putAll(serverData);
  }

  /**
   * internal method for fastjson getter/setter
   *
   * @return
   */
  Map<String, AVOp> getOperationQueue() {
    return operationQueue;
  }

  /**
   * internal method for fastjson getter/setter
   *
   * @param operationQueue
   */
  void setOperationQueue(Map<String, AVOp> operationQueue) {
    this.operationQueue.clear();
    this.operationQueue.putAll(operationQueue);
  }

  /**
   * Internal usage.You SHOULD NOT invoke this method.
   *
   * @return
   */
  boolean isDataReady() {
    return isDataReady;
  }

  /**
   * Internal usesage.You SHOULD NOT invoke this method.
   *
   * @return
   */
  void setDataReady(boolean isDataReady) {
    this.isDataReady = isDataReady;
  }

  /**
   * Internal usesage.You SHOULD NOT invoke this method.
   *
   * @return
   */
  void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Internal usesage.You SHOULD NOT invoke this method.
   *
   * @return
   */
  void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Internal usesage.You SHOULD NOT invoke this method.
   *
   * @return
   */
  void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public boolean isFetchWhenSave() {
    return fetchWhenSave;
  }

  public void setFetchWhenSave(boolean fetchWhenSave) {
    this.fetchWhenSave = fetchWhenSave;
  }

  // end of getter/setters for fastjson.

  public String getUuid() {
    if (AVUtils.isBlankString(this.uuid)) {
      this.uuid = UUID.randomUUID().toString().toLowerCase();
    }
    return this.uuid;
  }

  private final static Map<String, Class<? extends AVObject>> SUB_CLASSES_MAP =
      new HashMap<String, Class<? extends AVObject>>();
  private final static Map<Class<? extends AVObject>, String> SUB_CLASSES_REVERSE_MAP =
      new HashMap<Class<? extends AVObject>, String>();

  static Class<? extends AVObject> getSubClass(String className) {
    return SUB_CLASSES_MAP.get(className);
  }

  static String getSubClassName(Class<? extends AVObject> clazz) {
    if (AVUser.class.isAssignableFrom(clazz)) {
      return AVUser.userClassName();
    } else if (AVRole.class.isAssignableFrom(clazz)) {
      return AVRole.className;
    } else if (AVStatus.class.isAssignableFrom(clazz)) {
      return AVStatus.userClassName();
    } else {
      return SUB_CLASSES_REVERSE_MAP.get(clazz);
    }
  }

  /**
   * Register subclass to AVOSCloud SDK.It must be invocated before AVOSCloud.initialize.
   *
   * @param clazz The subclass.
   * @since 1.3.6
   */
  public static <T extends AVObject> void registerSubclass(Class<T> clazz) {
    AVClassName avClassName = clazz.getAnnotation(AVClassName.class);
    if (avClassName == null) {
      throw new IllegalArgumentException("The class is not annotated by @AVClassName");
    }
    String className = avClassName.value();
    AVUtils.checkClassName(className);
    SUB_CLASSES_MAP.put(className, clazz);
    SUB_CLASSES_REVERSE_MAP.put(clazz, className);
    ParserConfig.getGlobalInstance().putDeserializer(clazz, AVObjectDeserializer.instance);
    SerializeConfig.getGlobalInstance().put(clazz, AVObjectSerializer.instance);
  }

  /**
   * <p>
   * Constructs a new AVObject with no data in it. A AVObject constructed in this way will not have
   * an objectId and will not persist to the database until save() is called.
   * </p>
   * <p>
   * Class names must be alphanumerical plus underscore, and start with a letter. It is recommended
   * to name classes in CamelCaseLikeThis.
   * </p>
   *
   * @param theClassName The className for this AVObject.
   */
  public AVObject(String theClassName) {
    this();
    AVUtils.checkClassName(theClassName);
    className = theClassName;

  }

  private void init() {
    objectId = "";
    isDataReady = false;
    if (getPaasClientInstance().getDefaultACL() != null) {
      acl = new AVACL(getPaasClientInstance().getDefaultACL());
    }
    running = false;
  }

  /**
   * Atomically adds an object to the end of the array associated with a given key.
   *
   * @param key   The key.
   * @param value The object to add.
   */
  public void add(String key, Object value) {
    this.addObjectToArray(key, value, false);
  }

  /**
   * Atomically adds the objects contained in a Collection to the end of the array associated with a
   * given key.
   *
   * @param key    The key.
   * @param values The objects to add.
   */
  public void addAll(String key, Collection<?> values) {
    for (Object item : values) {
      this.addObjectToArray(key, item, false);
    }
  }

  /**
   * Create a AVQuery with special sub-class.
   *
   * @param clazz The AVObject subclass
   * @return The AVQuery
   */
  public static <T extends AVObject> AVQuery<T> getQuery(Class<T> clazz) {
    return new AVQuery<T>(getSubClassName(clazz), clazz);
  }

  /**
   * Atomically adds the objects contained in a Collection to the array associated with a given key,
   * only adding elements which are not already present in the array. The position of the insert is
   * not guaranteed.
   *
   * @param key    The key.
   * @param values The objects to add.
   */
  public void addAllUnique(String key, Collection<?> values) {
    for (Object item : values) {
      this.addObjectToArray(key, item, true);
    }
  }

  /**
   * Atomically adds an object to the array associated with a given key, only if it is not already
   * present in the array. The position of the insert is not guaranteed.
   *
   * @param key   The key.
   * @param value The object to add.
   */
  public void addUnique(String key, Object value) {
    this.addObjectToArray(key, value, true);
  }

  /**
   * Whether this object has a particular key. Same as 'has'.
   *
   * @param key The key to check for
   * @return Returns whether this object contains the key
   */
  public boolean containsKey(String key) {
    return (get(key) != null);
  }

  /**
   * Creates a new AVObject based upon a class name. If the class name is a special type (e.g. for
   * AVUser), then the appropriate type of AVObject is returned.
   *
   * @param className The class of object to create.
   * @return A new AVObject for the given class name.
   */
  public static AVObject create(String className) {
    return new AVObject(className);
  }

  /**
   * 通过解析AVObject.toString得到的String对象来获取AVObject对象
   *
   * @param avObjectString
   * @return
   * @throws Exception
   */
  public static AVObject parseAVObject(String avObjectString) throws Exception {
    AVObject object = (AVObject) JSON.parse(avObjectString);
    // 针对于子类的反序列化中间可能没有在第一步就走AVObjectDeserializer的解析
    if (object instanceof AVObject && !AVObject.class.equals(object.getClass())) {
      object.rebuildInstanceData();
    }
    return object;
  }

  /**
   * Creates a reference to an existing AVObject for use in creating associations between AVObjects.
   * Calling AVObject.isDataAvailable() on this object will return false until
   * AVObject.fetchIfNeeded() or AVObject.refresh() has been called. No network request will be
   * made.
   *
   * @param className The object's class.
   * @param objectId  The object id for the referenced object.
   * @return A AVObject without data.
   */
  public static AVObject createWithoutData(String className, String objectId) {
    AVObject object = new AVObject(className);
    object.setObjectId(objectId);
    return object;
  }


  void setClassName(String className) {
    this.className = className;
  }

  /**
   * Creates a reference to an existing AVObject subclass instance for use in creating associations
   * between AVObjects. Calling AVObject.isDataAvailable() on this object will return false until
   * AVObject.fetchIfNeeded() or AVObject.refresh() has been called. No network request will be
   * made.
   *
   * @param clazz    The object's class.
   * @param objectId The object id for the referenced object.
   * @return A AVObject without data.
   */
  public static <T extends AVObject> T createWithoutData(Class<T> clazz, String objectId)
      throws AVException {
    try {
      T result = clazz.newInstance();
      result.setClassName(getSubClassName(clazz));
      result.setObjectId(objectId);
      return result;
    } catch (Exception e) {
      throw new AVException("Create subclass instance failed.", e);
    }
  }

  /**
   * Deletes this object on the server. This does not delete or destroy the object locally.
   *
   * @throws AVException Throws an error if the object does not exist or if the internet fails.
   */
  public void delete() throws AVException {
    delete(null);
  }

  /**
   * Deletes this object on the server. This does not delete or destroy the object locally.
   *
   * @param option options for server to
   * @throws Exception AVException Throws an error if the object does not exist or if the internet fails.
   */
  public void delete(AVDeleteOption option) throws AVException {
    delete(true, false, option, new DeleteCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
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
  }

  /**
   * Delete AVObject in batch.The objects class name must be the same.
   *
   * @param objects the AVObject list to be deleted.
   * @throws AVException
   * @since 1.4.0
   */
  public static void deleteAll(Collection<? extends AVObject> objects) throws AVException {
    deleteAll(true, false, objects, new DeleteCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
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
  }

  /**
   * Delete AVObject in batch with callback in background.The objects class name must be the same.
   *
   * @param objects
   * @param deleteCallback
   * @throws AVException
   * @since 1.4.0
   */
  public static void deleteAllInBackground(Collection<? extends AVObject> objects,
                                           DeleteCallback deleteCallback) {
    deleteAll(false, false, objects, deleteCallback);
  }

  private static void deleteAll(boolean sync, boolean isEventually,
                                Collection<? extends AVObject> objects, DeleteCallback callback) {
    if (objects == null || objects.isEmpty()) {
      callback.internalDone(null, null);
      return;
    }
    if (isEventually) {
      for (AVObject object : objects) {
        if (object != null) object.deleteEventually(callback);
      }
    } else {
      String className = null;
      boolean wasFirst = true;
      StringBuilder sb = new StringBuilder();
      for (AVObject object : objects) {
        if (AVUtils.isBlankString(object.getClassName()) || AVUtils.isBlankString(object.objectId)) {
          throw new IllegalArgumentException(
              "Invalid AVObject, the class name or objectId is blank.");
        }
        if (className == null) {
          className = object.getClassName();
        } else if (!className.equals(object.getClassName())) {
          throw new IllegalArgumentException("The objects class name must be the same.");
        }
        if (wasFirst) {
          sb.append(AVPowerfulUtils.getEndpoint(object));
          wasFirst = false;
        } else {
          sb.append(",").append(object.getObjectId());
        }
      }

      final DeleteCallback internalCallback = callback;
      String endpoint = sb.toString();
      PaasClient.storageInstance().deleteObject(endpoint, sync, false, new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, null);
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
          }
        }
      }, null, null);
    }
  }

  /**
   * Deletes this object from the server at some unspecified time in the future, even if AVOSCloud
   * is currently inaccessible. Use this when you may not have a solid network connection, and don't
   * need to know when the delete completes. If there is some problem with the object such that it
   * can't be deleted, the request will be silently discarded. Delete requests made with this method
   * will be stored locally in an on-disk cache until they can be transmitted to AVOSCloud. They
   * will be sent immediately if possible. Otherwise, they will be sent the next time a network
   * connection is available. Delete instructions saved this way will persist even after the app is
   * closed, in which case they will be sent the next time the app is opened. If more than 10MB of
   * commands are waiting to be sent, subsequent calls to deleteEventually or saveEventually will
   * cause old instructions to be silently discarded until the connection can be re-established, and
   * the queued objects can be saved.
   */
  public void deleteEventually() {
    deleteEventually(null);
  }

  /**
   * Deletes this object from the server at some unspecified time in the future, even if AVOSCloud
   * is currently inaccessible. Use this when you may not have a solid network connection, and don't
   * need to know when the delete completes. If there is some problem with the object such that it
   * can't be deleted, the request will be silently discarded. Delete requests made with this method
   * will be stored locally in an on-disk cache until they can be transmitted to AVOSCloud. They
   * will be sent immediately if possible. Otherwise, they will be sent the next time a network
   * connection is available. Delete instructions saved this way will persist even after the app is
   * closed, in which case they will be sent the next time the app is opened. If more than 10MB of
   * commands are waiting to be sent, subsequent calls to deleteEventually or saveEventually will
   * cause old instructions to be silently discarded until the connection can be re-established, and
   * the queued objects can be saved.
   *
   * @param callback callback which will be called if the delete completes before the app exits.
   */
  public void deleteEventually(DeleteCallback callback) {
    delete(false, true, null, callback);
  }

  /**
   * Deletes this object on the server in a background thread. Does nothing in particular when the
   * save completes. Use this when you don't care if the delete works.
   */
  public void deleteInBackground() {
    deleteInBackground(null, null);
  }

  /**
   * Deletes this object on the server in a background thread. Does nothing in particular when the
   * save completes. Use this when you don't care if the delete works.
   *
   * @param option
   */
  public void deleteInBackground(AVDeleteOption option) {
    deleteInBackground(option, null);
  }

  /**
   * Deletes this object on the server in a background thread. Does nothing in particular when the
   * save completes. Use this when you don't care if the delete works.
   *
   * @param option
   * @param callback
   */
  public void deleteInBackground(AVDeleteOption option, DeleteCallback callback) {
    delete(false, false, option, callback);
  }

  /**
   * Deletes this object on the server in a background thread. This is preferable to using delete(),
   * unless your code is already running from a background thread.
   *
   * @param callback callback.done(e) is called when the save completes.
   */
  public void deleteInBackground(DeleteCallback callback) {
    delete(false, false, null, callback);
  }

  private void delete(boolean sync, boolean isEventually, AVDeleteOption option, DeleteCallback callback) {
    final DeleteCallback internalCallback = callback;
    String url = AVPowerfulUtils.getEndpoint(this);
    Map<String, String> params = null;
    if (option != null && option.matchQuery != null) {
      if (this.getClassName() != null && !this.getClassName().equals(option.matchQuery.getClassName())) {
        callback.internalDone(new AVException(0, "AVObject class inconsistant with AVQuery in AVDeleteOption"));
        return;
      }
      Map<String, Object> whereOperationMap = null;
      whereOperationMap = option.matchQuery.conditions.compileWhereOperationMap();
      Map<String, Object> whereMap = new HashMap<>();
      if ((whereOperationMap != null && !whereOperationMap.isEmpty())) {
        whereMap.put("where", AVUtils.restfulServerData(whereOperationMap));
      }
      url = AVUtils.addQueryParams(url, whereMap);
    }

    getPaasClientInstance().deleteObject(url, sync,
        isEventually, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, null);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (internalCallback != null) {
              internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        }, this.getObjectId(), internalId());
  }

  public AVObject fetch() throws AVException {
    return this.fetch(null);
  }

  public AVObject fetch(String includeKeys) throws AVException {
    fetchInBackground(true, includeKeys, new GetCallback<AVObject>() {
      @Override
      public void done(AVObject object, AVException e) {
        if (e != null) {
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
    return this;
  }

  /**
   * Fetches all the objects in the provided list.
   *
   * @param objects The list of objects to fetch.
   * @return The list passed in.
   * @throws AVException Throws an exception if the server returns an error or is inaccessible.
   */
  public static List<AVObject> fetchAll(List<AVObject> objects) throws AVException {
    List<AVObject> results = new LinkedList<AVObject>();
    for (AVObject o : objects) {
      results.add(o.fetch());
    }
    return results;
  }

  /**
   * Fetches all the objects that don't have data in the provided list.
   *
   * @param objects The list of objects to fetch.
   * @throws AVException Throws an exception if the server returns an error or is inaccessible.
   */
  public static List<AVObject> fetchAllIfNeeded(List<AVObject> objects) throws AVException {
    List<AVObject> results = new LinkedList<AVObject>();
    for (AVObject o : objects) {
      results.add(o.fetchIfNeeded());
    }
    return results;
  }

  /**
   * Fetches all the objects that don't have data in the provided list in the background
   *
   * @param objects  The list of objects to fetch.
   * @param callback callback.done(result, e) is called when the fetch completes.
   */
  public static void fetchAllIfNeededInBackground(List<AVObject> objects,
                                                  FindCallback<AVObject> callback) {
    final FindCallback<AVObject> internalCallback = callback;
    final List<AVObject> result = new ArrayList<AVObject>();
    fetchAllInBackground(true, objects, new GenericObjectCallback() {
      @Override
      public void onGroupRequestFinished(int left, int total, AVObject object) {
        if (object != null) {
          result.add(object);
        }
        if (left <= 0 && internalCallback != null) {
          internalCallback.internalDone(result, null);
        }
      }
    });
  }

  /**
   * Fetches all the objects in the provided list in the background
   *
   * @param objects  The list of objects to fetch.
   * @param callback callback.done(result, e) is called when the fetch completes.
   */
  public static void fetchAllInBackground(List<AVObject> objects,
                                          final FindCallback<AVObject> callback) {
    final List<AVObject> result = new ArrayList<AVObject>();
    fetchAllInBackground(false, objects, new GenericObjectCallback() {
      @Override
      public void onGroupRequestFinished(int left, int total, AVObject object) {
        if (object != null) {
          result.add(object);
        }
        if (left <= 0 && callback != null) {
          callback.internalDone(result, null);
        }
      }
    });
  }

  private static void fetchAllInBackground(boolean check, List<AVObject> objects,
                                           final GenericObjectCallback callback) {
    final int total = objects.size();
    final AtomicInteger counter = new AtomicInteger(objects.size());
    for (AVObject object : objects) {
      if (!check || !object.isDataAvailable()) {
        object.fetchInBackground(false, null, new GetCallback<AVObject>() {
          @Override
          public void done(AVObject object, AVException e) {
            if (callback != null) {
              callback.onGroupRequestFinished(counter.decrementAndGet(), total, object);
            }
          }
        });
      } else if (callback != null) {
        callback.onGroupRequestFinished(counter.decrementAndGet(), total, object);
      }
    }

    if (objects.size() <= 0 && callback != null) {
      callback.onGroupRequestFinished(0, 0, null);
    }
  }

  public AVObject fetchIfNeeded() throws AVException {
    return this.fetchIfNeeded(null);
  }

  public AVObject fetchIfNeeded(String includeKeys) throws AVException {
    if (!isDataAvailable()) {
      fetchInBackground(true, includeKeys, new GetCallback<AVObject>() {
        @Override
        public void done(AVObject object, AVException e) {
          if (e != null) {
            AVExceptionHolder.add(e);
          }
        }

        @Override
        protected boolean mustRunOnUIThread() {
          return false;
        }
      });
    }
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return this;
  }

  /**
   * If this AVObject has not been fetched (i.e. AVObject.isDataAvailable() returns false), fetches
   * this object with the data from the server in a background thread. This is preferable to using
   * AVObject.fetchIfNeeded(), unless your code is already running from a background thread.
   *
   * @param callback callback.done(object, e) is called when the fetch completes.
   */
  public void fetchIfNeededInBackground(GetCallback<AVObject> callback) {
    this.fetchIfNeededInBackground(null, callback);
  }

  /**
   * If this AVObject has not been fetched (i.e. AVObject.isDataAvailable() returns false), fetches
   * this object with the data from the server in a background thread. This is preferable to using
   * AVObject.fetchIfNeeded(), unless your code is already running from a background thread.
   *
   * @param includeKeys 以逗号隔开的include字段列表组成的字符串，形如'author,comment'
   * @param callback    callback.done(object, e) is called when the fetch completes.
   * @since 2.0.2
   */
  public void fetchIfNeededInBackground(String includeKeys, GetCallback<AVObject> callback) {
    if (!isDataAvailable()) {
      fetchInBackground(includeKeys, callback);
    } else if (callback != null) {
      callback.internalDone(this, null);
    }
  }

  /**
   * Fetches this object with the data from the server in a background thread. This is preferable to
   * using fetch(), unless your code is already running from a background thread.
   *
   * @param callback callback.done(object, e) is called when the fetch completes.
   */
  public void fetchInBackground(GetCallback<AVObject> callback) {
    fetchInBackground(null, callback);
  }

  /**
   * Fetches this object with the data from the server in a background thread. This is preferable to
   * using fetch(), unless your code is already running from a background thread.
   *
   * @param includeKeys 以逗号隔开的include字段列表组成的字符串，例如'author,comment'
   * @param callback    callback.done(object, e) is called when the fetch completes.
   */
  public void fetchInBackground(String includeKeys, GetCallback<AVObject> callback) {
    fetchInBackground(false, includeKeys, callback);
  }

  private void fetchInBackground(boolean sync, String includeKeys, GetCallback<AVObject> callback) {
    if (AVUtils.isBlankString(getObjectId())) {
      if (callback != null) {
        AVException exception =
            AVErrorUtils.createException(AVErrorUtils.MISSING_OBJECTID, "Missing objectId");
        callback.internalDone(null, exception);
      }
      return;
    }
    final Map<String, String> params = new HashMap<String, String>();
    if (!AVUtils.isBlankString(includeKeys)) {
      params.put("include", includeKeys);
    }
    getPaasClientInstance().getObject(AVPowerfulUtils.getEndpoint(this),
        new AVRequestParams(params), sync, headerMap(), new FetchObjectCallback(callback));
  }

  /**
   * Access a value. In most cases it is more convenient to use a helper function such as getString
   * or getInt.
   *
   * @param key The key to access the value for.
   * @return null if there is no such key.
   */
  public Object get(String key) {
    if (CREATED_AT.equals(key)) {
      return getCreatedAt();
    }
    if (UPDATED_AT.equals(key)) {
      return getUpdatedAt();
    }
    return instanceData.get(key);
  }

  /**
   * Access the AVACL governing this object.
   */
  public AVACL getACL() {
    return acl;
  }

  /**
   * Access a boolean value.
   *
   * @param key The key to access the value for.
   * @return Returns false if there is no such key or if it is not a boolean.
   */
  public boolean getBoolean(String key) {
    Boolean b = (Boolean) get(key);
    return b == null ? false : b;
  }

  /**
   * Access a byte array value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a byte array.
   */
  public byte[] getBytes(String key) {
    return (byte[]) (get(key));
  }

  /**
   * Accessor to the class name.
   */
  public String getClassName() {
    if (AVUtils.isBlankString(className)) {
      className = getSubClassName(this.getClass());
    }
    return className;
  }

  /**
   * This reports time as the server sees it, so that if you create a AVObject, then wait a while,
   * and then call save(), the creation time will be the time of the first save() call rather than
   * the time the object was created locally.
   *
   * @return The first time this object was saved on the server.
   */
  public Date getCreatedAt() {
    return AVUtils.dateFromString(createdAt);
  }

  /**
   * Access a Date value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a Date.
   */
  public Date getDate(String key) {
    return (Date) get(key);
  }

  /**
   * Access a double value.
   *
   * @param key The key to access the value for.
   * @return Returns 0 if there is no such key or if it is not a double.
   */
  public double getDouble(String key) {
    Number number = (Number) get(key);
    if (number != null) return number.doubleValue();
    return 0;
  }

  /**
   * Access an int value.
   *
   * @param key The key to access the value for.
   * @return Returns 0 if there is no such key or if it is not a JSONObject.
   */
  public int getInt(String key) {
    Number v = (Number) get(key);
    if (v != null) return v.intValue();
    return 0;
  }

  /**
   * Access a JSONArray value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a JSONArray.
   */
  public JSONArray getJSONArray(String key) {
    Object list = get(key);
    if (list == null) return null;
    if (list instanceof JSONArray) return (JSONArray) list;
    if (list instanceof Collection<?>) {
      JSONArray array = new JSONArray((Collection<?>) list);
      return array;
    }
    if (list instanceof Object[]) {
      JSONArray array = new JSONArray();
      for (Object obj : (Object[]) list) {
        array.put(obj);
      }
      return array;
    }
    return null;
  }

  /**
   * Access a JSONObject value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a JSONObject.
   */
  public JSONObject getJSONObject(String key) {
    Object object = get(key);
    if (object instanceof JSONObject) {
      return (JSONObject) object;
    }
    String jsonString = JSON.toJSONString(object);
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(jsonString);
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid json string", exception);
    }
    return jsonObject;
  }

  /**
   * Access a List value
   *
   * @param key The key to access the value for
   * @return Returns null if there is no such key or if the value can't be converted to a List.
   */
  public List getList(String key) {
    return (List) get(key);
  }

  /**
   * 获得一个指定类型的List值
   *
   * @param key
   * @param clazz
   * @return
   */

  public <T extends AVObject> List<T> getList(String key, Class<T> clazz) {
    List<AVObject> list = this.getList(key);
    List<T> returnList = null;
    if (list != null) {
      returnList = new LinkedList<T>();
      try {
        for (AVObject item : list) {
          returnList.add(null != item ? AVObject.cast(item, clazz) : null);
        }
      } catch (Exception e) {
        LogUtil.log.e("ClassCast Exception", e);
      }
    }
    return returnList;
  }

  /**
   * Access a long value.
   *
   * @param key The key to access the value for.
   * @return Returns 0 if there is no such key or if it is not a long.
   */
  public long getLong(String key) {
    Number number = (Number) get(key);
    if (number != null) return number.longValue();
    return 0L;
  }

  /**
   * Access a Map value
   *
   * @param key The key to access the value for
   * @return Returns null if there is no such key or if the value can't be converted to a Map.
   */
  public <V> Map<String, V> getMap(String key) {
    return (Map<String, V>) this.get(key);
  }

  /**
   * Access a numerical value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a Number.
   */
  public Number getNumber(String key) {
    Number number = (Number) get(key);
    return number;
  }

  /**
   * Accessor to the object id. An object id is assigned as soon as an object is saved to the
   * server. The combination of a className and an objectId uniquely identifies an object in your
   * application.
   *
   * @return The object id.
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * Access a AVFile value. This function will not perform a network request. Unless the AVFile has
   * been downloaded (e.g. by calling AVFile.getData()), AVFile.isDataAvailable() will return false.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a AVFile.
   */
  @SuppressWarnings("unchecked")
  public <T extends AVFile> T getAVFile(String key) {
    return (T) get(key);
  }

  /**
   * Access a AVGeoPoint value.
   *
   * @param key The key to access the value for
   * @return Returns null if there is no such key or if it is not a AVGeoPoint.
   */
  public AVGeoPoint getAVGeoPoint(String key) {
    return (AVGeoPoint) get(key);
  }

  /**
   * Access a AVObject value. This function will not perform a network request. Unless the AVObject
   * has been downloaded (e.g. by a AVQuery.include(String) or by calling AVObject.fetchIfNeeded()
   * or AVObject.refresh()), AVObject.isDataAvailable() will return false.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a AVObject.
   */
  @SuppressWarnings("unchecked")
  public <T extends AVObject> T getAVObject(String key) {
    return (T) get(key);
  }


  public <T extends AVObject> T getAVObject(String key, Class<T> clazz) throws Exception {
    AVObject object = getAVObject(key);
    if (object == null) {
      return null;
    } else {
      if (clazz.isInstance(object)) {
        return (T) object;
      } else {
        return AVObject.cast(this, clazz);
      }
    }
  }

  /**
   * Access a AVUser value. This function will not perform a network request. Unless the AVObject
   * has been downloaded (e.g. by a AVQuery.include(String) or by calling AVObject.fetchIfNeeded()
   * or AVObject.refresh()), AVObject.isDataAvailable() will return false.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a AVUser.
   */
  @SuppressWarnings("unchecked")
  public <T extends AVUser> T getAVUser(String key) {
    return (T) get(key);
  }

  /**
   * Access a AVUser subclass value. This function will not perform a network request. Unless the
   * AVObject has been downloaded (e.g. by a AVQuery.include(String) or by calling
   * AVObject.fetchIfNeeded() or AVObject.refresh()), AVObject.isDataAvailable() will return false.
   *
   * @param key   The key to access the value for.
   * @param clazz subclass of AVUser as the class of return value
   * @return Returns null if there is no such key or if it is not a AVUser.
   */
  public <T extends AVUser> T getAVUser(String key, Class<T> clazz) {
    AVUser user = (AVUser) get(key);
    return user == null ? null : AVUser.cast(user, clazz);
  }

  /**
   * Access or create a Relation value for a key
   *
   * @param key The key to access the relation for.
   * @return the AVRelation object if the relation already exists for the key or can be created for
   * this key.
   */
  public <T extends AVObject> AVRelation<T> getRelation(String key) {
    if (checkKey(key)) {
      Object object = this.get(key);
      if (object != null && object instanceof AVRelation) {
        ((AVRelation) object).setParent(this);
        return (AVRelation) object;
      } else {
        AVRelation<T> relation = new AVRelation<T>(this, key);
        instanceData.put(key, relation);
        return relation;
      }
    }
    return null;
  }

  /**
   * Access a string value.
   *
   * @param key The key to access the value for.
   * @return Returns null if there is no such key or if it is not a String.
   */
  public String getString(String key) {
    Object obj = get(key);
    if (obj instanceof String)
      return (String) obj;
    else
      return null;
  }

  /**
   * This reports time as the server sees it, so that if you make changes to a AVObject, then wait a
   * while, and then call save(), the updated time will be the time of the save() call rather than
   * the time the object was changed locally.
   *
   * @return The last time this object was updated on the server.
   */
  public Date getUpdatedAt() {
    return AVUtils.dateFromString(updatedAt);
  }

  /**
   * Whether this object has a particular key. Same as containsKey.
   *
   * @param key The key to check for
   * @return Returns whether this object contains the key
   */
  public boolean has(String key) {
    return (this.get(key) != null);
  }

  /**
   */
  public boolean hasSameId(AVObject other) {
    return other.objectId.equals(this.objectId);
  }

  /**
   * Atomically increments the given key by 1.
   *
   * @param key The key to increment.
   */
  public void increment(String key) {
    this.increment(key, 1);
  }

  private abstract class KeyValueCallback {

    public void execute(String key) {
      this.execute(key, true);
    }

    public void execute(String key, boolean pending) {
      AVOp op = operationQueue.get(key);
      AVOp newOP = createOp();
      if (op == null) {
        op = newOP;
      } else {
        op = op.merge(newOP);
      }
      Object oldValue = instanceData.get(key);
      Object newValue = newOP.apply(oldValue);
      if (pending) {
        operationQueue.put(key, op);
      } else {
        if (null != newValue) {
          serverData.put(key, newValue);
        } else if (serverData.containsKey(key)) {
          serverData.remove(key);
        }
      }
      if (newValue == null) {
        if (alwaysUsePost()) {
          // alwaysUsePost 仅对 AVInstallation 有效，这时候不能直接从 instanceData 中删除属性，而是要设成 {"__op": "Delete"} 才能实际删除。
          // modify by jfeng@2018-01-10
          Map<String, String> deleteValue = new HashMap<>(1);
          deleteValue.put("__op", "Delete");
          instanceData.put(key, deleteValue);
        } else {
          instanceData.remove(key);
        }
      } else {
        instanceData.put(key, newValue);
      }
    }

    public abstract AVOp createOp();

  }

  /**
   * Atomically increments the given key by the given number.
   *
   * @param key    The key to increment.
   * @param amount The amount to increment by.
   */
  public void increment(final String key, final Number amount) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {
        @Override
        public AVOp createOp() {
          return new IncrementOp(key, amount);
        }
      };
      cb.execute(key);
    }
  }

  /**
   * Gets whether the AVObject has been fetched.
   *
   * @return true if the AVObject is new or has been fetched or refreshed. false otherwise.
   */
  public boolean isDataAvailable() {
    return (!AVUtils.isBlankString(objectId) && this.isDataReady);
  }

  /**
   * Returns a set view of the keys contained in this object. This does not include createdAt,
   * updatedAt, authData, or objectId. It does include things like username and ACL.
   */
  public Set<String> keySet() {
    return instanceData.keySet();
  }

  public static final Set<String> INVALID_KEYS = new HashSet<String>();

  static {
    INVALID_KEYS.add(CREATED_AT);
    INVALID_KEYS.add(UPDATED_AT);
    INVALID_KEYS.add(OBJECT_ID);
    INVALID_KEYS.add("ACL");
  }

  private boolean checkKey(String key) {
    if (AVUtils.isBlankString(key)) {
      throw new IllegalArgumentException("Blank key");
    }
    if (key.startsWith("_")) {
      throw new IllegalArgumentException("key should not start with '_'");
    }
    if (INVALID_KEYS.contains(key))
      LogUtil.log.w("Internal key name:`" + key + "`,please use setter/getter for it.");
    return !INVALID_KEYS.contains(key);
  }

  /**
   * Add a key-value pair to this object. It is recommended to name keys in
   * partialCamelCaseLikeThis.
   *
   * @param key   Keys must be alphanumerical plus underscore, and start with a letter.
   * @param value Values may be numerical, String, JSONObject, JSONArray, JSONObject.NULL, or other
   *              AVObjects. value may not be null.
   */
  public void put(final String key, final Object value) {
    this.put(key, value, true);
  }

  protected void put(final String key, final Object value, boolean pending) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {
        @Override
        public AVOp createOp() {
          return new SetOp(key, value);
        }
      };
      cb.execute(key, pending);
    }
  }

  /**
   * Refreshes this object with the data from the server. Call this whenever you want the state of
   * the object to reflect exactly what is on the server.
   *
   * @throws AVException Throws an exception if the server is inaccessible.
   */
  public void refresh() throws AVException {
    this.refresh(null);
  }

  /**
   * Refreshes this object with the data from the server. Call this whenever you want the state of
   * the object to reflect exactly what is on the server.
   *
   * @param includeKeys 以逗号隔开的include字段列表字符串，例如'author,comment'
   * @throws AVException Throws an exception if the server is inaccessible.
   * @since 2.0.2
   */
  public void refresh(String includeKeys) throws AVException {
    refreshInBackground(true, includeKeys, new RefreshCallback<AVObject>() {
      @Override
      public void done(AVObject object, AVException e) {
        if (e != null) {
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
  }

  /**
   * Refreshes this object with the data from the server in a background thread. This is preferable
   * to using refresh(), unless your code is already running from a background thread.
   *
   * @param callback callback.done(object, e) is called when the refresh completes.
   */
  public void refreshInBackground(RefreshCallback<AVObject> callback) {
    refreshInBackground(false, null, callback);
  }

  /**
   * Refreshes this object with the data from the server in a background thread. This is preferable
   * to using refresh(), unless your code is already running from a background thread.
   *
   * @param includeKeys 以逗号隔开的include字段列表字符串，例如'author,comment'
   * @param callback    callback.done(object, e) is called when the refresh completes.
   * @since 2.0.2
   */
  public void refreshInBackground(String includeKeys, RefreshCallback<AVObject> callback) {
    refreshInBackground(false, includeKeys, callback);
  }

  private void refreshInBackground(boolean sync, String includeKeys,
                                   RefreshCallback<AVObject> callback) {
    Map<String, String> params = new HashMap<String, String>();
    if (!AVUtils.isBlankString(includeKeys)) {
      params.put("include", includeKeys);
    }
    getPaasClientInstance().getObject(AVPowerfulUtils.getEndpoint(this),
        new AVRequestParams(params), sync, headerMap(), new FetchObjectCallback(callback));
  }

  /**
   * Removes a key from this object's data if it exists.
   *
   * @param key The key to remove.
   */
  public void remove(String key) {
    removeObjectForKey(key);
  }

  /**
   * Atomically removes all instances of the objects contained in a Collection from the array
   * associated with a given key. To maintain consistency with the Java Collection API, there is no
   * method removing all instances of a single object. Instead, you can call
   * avObject.removeAll(key, Arrays.asList(value)).
   *
   * @param key    The key.
   * @param values The objects to remove.
   */
  public void removeAll(final String key, final Collection<?> values) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {
        @Override
        public AVOp createOp() {
          return new RemoveOp(key, values);
        }
      };
      cb.execute(key);
    }
  }

  /**
   * Saves this object to the server. Typically, you should use
   * {@link #saveInBackground(AVSaveOption, SaveCallback)} instead of this, unless you are managing your
   * own threading.
   *
   * @throws AVException
   */
  public void save() throws AVException {
    saveObject(true, false, new SaveCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
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
  }

  /**
   * Saves this object to the server.
   *
   * @param option save options
   */
  public void save(AVSaveOption option) throws AVException {
    saveObject(option, true, false, new SaveCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
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
  }

  /**
   * Saves each object in the provided list. This is faster than saving each object individually
   * because it batches the requests.
   *
   * @param objects The objects to save.
   * @throws AVException Throws an exception if the server returns an error or is inaccessible.
   */
  public static void saveAll(List<? extends AVObject> objects) throws AVException {
    _saveAll(true, objects, new SaveCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
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
  }

  /**
   * Saves each object in the provided list to the server in a background thread. This is preferable
   * to using saveAll, unless your code is already running from a background thread.
   *
   * @param objects The objects to save.
   */
  public static void saveAllInBackground(List<? extends AVObject> objects) {
    _saveAll(false, objects, null);
  }

  /**
   * Saves each object in the provided list to the server in a background thread. This is preferable
   * to using saveAll, unless your code is already running from a background thread.
   *
   * @param objects  The objects to save.
   * @param callback callback.done(e) is called when the save completes.
   */
  public static void saveAllInBackground(List<? extends AVObject> objects, SaveCallback callback) {
    _saveAll(false, objects, callback);
  }

  private static void _saveAll(final boolean sync, final List<? extends AVObject> objects,
                               final SaveCallback callback) {
    final LinkedList list = new LinkedList();
    List<AVFile> files = new LinkedList<AVFile>();
    for (AVObject o : objects) {
      if (!o.checkCircleReference()) {
        if (callback != null) callback.internalDone(AVErrorUtils.circleException());
        return;
      }
      if (o.processOperationData()) {
        List<AVFile> filesNeedToUpload = o.getFilesToSave();
        if (!AVUtils.isEmptyList(filesNeedToUpload)) {
          files.addAll(filesNeedToUpload);
        }
      } else {
        continue;
      }
    }


    final GenericObjectCallback genericObjectCallback = new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        for (AVObject o : objects) {
          o.copyFromJson(content);
          o.running = false;
          o.onSaveSuccess();
        }
        if (callback != null) {
          callback.done(null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {

        for (AVObject o : objects) {
          o.running = false;
          o.rollbackDataToOperationQueue();
          o.onSaveFailure();
        }
        LogUtil.log.d(content);
        if (callback != null)
          callback.internalDone(null, AVErrorUtils.createException(error, content));
      }
    };
    try {
      if (files != null && files.size() > 0) {
        saveFileBeforeSave(files, sync, new SaveCallback() {

          @Override
          public void done(AVException e) {
            for (AVObject o : objects) {
              o.running = true;
              o.buildParameterForNonSavedObject(list);
            }
            PaasClient.storageInstance().postBatchSave(list, sync, false, null,
                genericObjectCallback, null, null);
          }

        });
      } else {
        for (AVObject o : objects) {
          o.running = true;
          o.buildParameterForNonSavedObject(list);
        }
        PaasClient.storageInstance().postBatchSave(list, sync, false, null, genericObjectCallback,
            null, null);
      }
    } catch (AVException e) {
      if (callback != null) {
        callback.internalDone(e);
      }
    }
  }

  /**
   * 适用于用户并不关心具体保存到服务器的具体时间，或者数据并不需要时常与服务器发生交互时，可以使用本方法 在网络请求遇到异常时，AVOS
   * Cloud会将此类请求保存到本地，等到网络回复正常或者排除故障以后再发送请求 被保存下来的请求会按照初始的发送顺序进行发送
   */
  public void saveEventually() {
    saveEventually(null);
  }

  /**
   * 适用于用户并不关心具体保存到服务器的具体时间，或者数据并不需要时常与服务器发生交互时，可以使用本方法 在网络请求遇到异常时，AVOS
   * Cloud会将此类请求保存到本地，等到网络回复正常或者排除故障以后再发送请求 被保存下来的请求会按照初始的发送顺序进行发送
   * <p/>
   * 由于保存的时间无法确定，回调发生时可能已经超出了原来的运行环境，即便发生也没有意义，所以不鼓励用户saveEventually中传入callback
   *
   * @param callback A callback which will be called if the save completes before the app exits.
   */
  public void saveEventually(SaveCallback callback) {
    PaasClient.registerEventuallyObject(this);
    saveInBackground(callback, true);
  }

  protected void onSaveSuccess() {

  }

  protected void onDataSynchronized() {

  }

  protected void onSaveFailure() {

  }

  protected Map<String, String> headerMap() {
    return getPaasClientInstance().userHeaderMap();
  }

  private void saveObject(final boolean sync, final boolean isEventually,
                          final SaveCallback callback) {
    this.saveObject(null, sync, isEventually, callback);
  }

  private void saveObject(final AVSaveOption option, final boolean sync, final boolean isEventually,
                          final SaveCallback callback) {
    // add request to queue
    if (running) {
      if (callback != null) {
        callback.internalDone(new AVException(AVException.OTHER_CAUSE,
            "already has one request sending"));
      }
      return;
    }
    boolean needToSave = processOperationData();
    if (!needToSave) {
      if (callback != null) {
        callback.internalDone(null);
      }
      return;
    }

    if (option != null && option.matchQuery != null) {
      if (this.getClassName() != null && !this.getClassName().equals(option.matchQuery.getClassName())) {
        callback.internalDone(new AVException(0, "AVObject class inconsistant with AVQuery in AVSaveOption"));
        return;
      }
    }

    running = true;
    try {
      List<AVFile> files = getFilesToSave();
      if (files != null && files.size() > 0) {
        saveFileBeforeSave(files, sync, new SaveCallback() {

          @Override
          public void done(AVException e) {
            _saveObject(option, sync, isEventually, callback);
          }
        });
      } else {
        _saveObject(option, sync, isEventually, callback);
      }
    } catch (AVException e) {
      if (callback != null) {
        callback.internalDone(e);
      }
    }
  }

  private List<AVFile> getFilesToSave() {
    List<AVFile> fileNeedToUpload = new LinkedList<AVFile>();
    for (Map.Entry<String, AVOp> entry : tempDataForServerSaving.entrySet()) {
      final AVOp op = entry.getValue();
      Object o = op.getValues();
      // FIXME: can't make sure subclasses of avobject save
      if (o != null && o instanceof AVObject) {
        List<AVFile> files = ((AVObject) o).getFilesToSave();
        if (files != null && files.size() > 0) fileNeedToUpload.addAll(files);
      } else if (o != null && AVFile.class.isInstance(o)) {
        AVFile file = (AVFile) o;
        if (file.getObjectId() == null) {
          fileNeedToUpload.add(file);
        }
      }
    }
    return fileNeedToUpload;
  }

  // 把新数据平移到缓存中间去
  private boolean processOperationData() {
    for (Map.Entry<String, Object> entry : instanceData.entrySet()) {
      String key = entry.getKey();
      Object o = entry.getValue();
      if (o != null && o instanceof AVObject) {
        if (((AVObject) o).processOperationData()) {
          this.put(key, o);
        }
      }
    }
    if (!operationQueue.isEmpty()) {
      tempDataForServerSaving.putAll(operationQueue);
      operationQueue.clear();
    }
    return !tempDataForServerSaving.isEmpty() || AVUtils.isBlankString(this.objectId);
  }

  private void _saveObject(boolean sync, boolean isEventually, SaveCallback callback) {
    this._saveObject(null, sync, isEventually, callback);
  }

  private void _saveObject(AVSaveOption option, boolean sync, boolean isEventually, SaveCallback callback) {
    LinkedList<Map<String, Object>> pendingRequests = new LinkedList<Map<String, Object>>();
    buildParameterForNonSavedObject(pendingRequests);
    buildMatchQueryParams(option, pendingRequests);
    saveObjectToAVOSCloud(pendingRequests, sync, isEventually, callback);
  }

  private void buildMatchQueryParams(AVSaveOption option, LinkedList<Map<String, Object>> pendingRequests) {
    Map<String, Object> whereOperationMap = null;
    if (option != null && option.matchQuery != null) {
      whereOperationMap = option.matchQuery.conditions.compileWhereOperationMap();
    }
    if (pendingRequests.size() >= 1) {
      Map<String, Object> thisObjectPendingRequest = pendingRequests.get(0);
      Map<String, Object> whereMap = new HashMap<>();
      if ((whereOperationMap != null && !whereOperationMap.isEmpty())) {
        whereMap.put("where", AVUtils.restfulServerData(whereOperationMap));
      }
      if (fetchWhenSave || (option != null && option.fetchWhenSave)) {
        whereMap.put("fetchWhenSave", true);
      }
      if ("PUT".equals(thisObjectPendingRequest.get("method"))) {
        thisObjectPendingRequest.put("params", whereMap);
      }
    }
  }


  // ================================================================================
  // Batch save
  // ================================================================================

  @SuppressWarnings("unchecked")
  private void saveObjectToAVOSCloud(List<Map<String, Object>> requests, final boolean sync,
                                     final boolean isEventually,
                                     final SaveCallback callback) {

    for (Map<String, Object> request : requests) {
      Map<String, Object> body = (Map<String, Object>) request.get("body");
      if (((String) body.get("__internalId")).length() == UUID_LEN) {
        request.put("new", true);
      }
    }


    getPaasClientInstance().postBatchSave(requests, sync, isEventually, headerMap(),
        new GenericObjectCallback() {

          @Override
          public boolean isRequestStatisticNeed() {
            return requestStatistic;
          }

          @Override
          public void onSuccess(String content, AVException e) {
            AVObject.this.running = false;
            copyFromJson(content);
            onSaveSuccess();

            if (callback != null) {
              callback.internalDone(e);
            }

          }

          @Override
          public void onFailure(Throwable error, String content) {
            AVObject.this.running = false;
            rollbackDataToOperationQueue();
            if (callback != null) {
              if (AVObject.this.shouldThrowException(error, content)) {
                callback.internalDone(AVErrorUtils.createException(error, content));
              } else {
                callback.internalDone(null);
              }
            }
            onSaveFailure();
          }
        }, getObjectId(), internalId());

  }

  private void transferDataToServerData() {
    for (Map.Entry<String, AVOp> entry : this.tempDataForServerSaving.entrySet()) {
      Object oldValue = serverData.get(entry.getKey());
      Object newValue = entry.getValue().apply(oldValue);
      if (null != newValue) {
        serverData.put(entry.getKey(), newValue);
      } else if (serverData.containsKey(entry.getKey())) {
        serverData.remove(entry.getKey());
      }
    }
    tempDataForServerSaving.clear();
  }

  private void rollbackDataToOperationQueue() {
    for (Map.Entry<String, AVOp> entry : operationQueue.entrySet()) {
      AVOp newOP = entry.getValue();
      AVOp op = tempDataForServerSaving.get(entry.getKey());
      if (op == null) {
        op = newOP;
      } else {
        op = op.merge(newOP);
      }
      tempDataForServerSaving.put(entry.getKey(), op);
    }
    operationQueue.clear();
    operationQueue.putAll(tempDataForServerSaving);
    tempDataForServerSaving.clear();
  }

  protected void copyFromJson(String jsonStr) {
    try {
      Map map = AVUtils.getFromJSON(jsonStr, Map.class);
      copyFromMap(map);
    } catch (Exception e) {
      LogUtil.log.e("AVObject parse error", e);
    }
  }

  /*
   * copy the item to self Item:
   * "c2": { "objectId": "51625ddd4728a4c7f8254ea8", "createdAt": "2013-04-08T14:04:13.000Z" }
   */
  // TODO need to update to new
  protected void copyFromMap(Map map) {
    transferDataToServerData();
    Object item = map.get(this.uuid);
    if (item != null && item instanceof Map) {
      AVUtils.copyPropertiesFromMapToAVObject((Map<String, Object>) item, this);
    }

    // when put, it may contain value from server side,
    // so update local estimated values too.
    item = map.get(this.getObjectId());
    if (item != null && item instanceof Map) {
      AVUtils.copyPropertiesFromMapToAVObject((Map<String, Object>) item, this);
    }

    for (Object o : instanceData.values()) {
      if (o instanceof AVObject) {
        ((AVObject) o).copyFromMap(map);
      }
    }
  }

  // 这里目前唯一特殊的就是 AVInstallation，AVInstallation 的所有请求都是 post
  // 因为不在同一个模块，这里没有办法直接得到 AVInstallation，所以需要 AVInstallation 重写此函数并返回 true
  protected boolean alwaysUsePost() {
    return false;
  }

  protected String internalId() {
    return AVUtils.isBlankString(getObjectId()) ? getUuid() : getObjectId();
  }

  protected void buildBatchParameterForNonSavedObject(List<AVObject> unSavedChildren,
                                                      List requestQueue) {
    Map<String, Object> body;
    List<Map<String, String>> children;
    if (!alwaysUsePost()) {
      if (!tempDataForServerSaving.isEmpty() || AVUtils.isBlankString(this.objectId)) {
        body = new HashMap<String, Object>();
        children = new ArrayList<Map<String, String>>();
        for (Map.Entry<String, AVOp> entry : tempDataForServerSaving.entrySet()) {
          String key = entry.getKey();
          AVOp op = entry.getValue();
          parseOperation(body, key, op, children, unSavedChildren, requestQueue);
        }
        this.mergeRequestQueue(wrapperRequestBody(body, children, false), requestQueue);
      }
    } else if (!instanceData.isEmpty()) {
      body = new HashMap<String, Object>();
      children = new ArrayList<Map<String, String>>();
      for (Map.Entry<String, Object> entry : instanceData.entrySet()) {
        Object o = entry.getValue();
        String key = entry.getKey();
        parseObjectValue(unSavedChildren, body, children, o, key);
      }
      this.mergeRequestQueue(wrapperRequestBody(body, children, false), requestQueue);
    }
  }

  private void parseObjectValue(List<AVObject> unSavedChildren, Map<String, Object> body,
                                List<Map<String, String>> children, Object o, String key) {
    if (o instanceof AVObject) {
      AVObject oo = (AVObject) o;
      Map<String, String> child = AVUtils.mapFromChildObject(oo, key);
      children.add(child);
      if (oo.processOperationData()) {
        unSavedChildren.add(oo);
      }
    } else if (o instanceof AVGeoPoint) {
      body.put(key, AVUtils.mapFromGeoPoint((AVGeoPoint) o));
    } else if (o instanceof Date) {
      body.put(key, AVUtils.mapFromDate((Date) o));
    } else if (o instanceof byte[]) {
      body.put(key, AVUtils.mapFromByteArray((byte[]) o));
    } else if (o instanceof AVFile) {
      body.put(key, AVUtils.mapFromFile((AVFile) o));
    } else {
      body.put(key, AVUtils.getParsedObject(o));
    }
  }

  // 解析单个Operation
  private Map<String, Object> parseOperation(Map<String, Object> body, String key, AVOp op,
                                             List children, List unSavedChildren, List requestQueue) {
    Object o = op.getValues();
    // If the key's value is not a relation data,we store it.
    if (!(op instanceof CollectionOp || op instanceof IncrementOp || op instanceof DeleteOp)) {
      parseObjectValue(unSavedChildren, body, children, o, key);
    } else if (op instanceof IncrementOp || op instanceof AddOp || op instanceof RemoveOp
        || op instanceof AddRelationOp || op instanceof RemoveRelationOp
        || op instanceof AddUniqueOp || op instanceof DeleteOp) {
      body.putAll(op.encodeOp());
    } else if (op instanceof CompoundOp) {
      // 第一个Op还是跟着主的requestBody走，这样不会出现空的无实际意义的requestBody
      List<AVOp> compoundOps = ((CompoundOp) op).getValues();
      if (!AVUtils.isEmptyList(compoundOps)) {
        AVOp firstOp = compoundOps.get(0);
        parseOperation(body, key, firstOp, children, unSavedChildren, requestQueue);
      }

      for (int index = 1; index < compoundOps.size(); index++) {
        AVOp avOp = compoundOps.get(index);
        Map<String, Object> compoundChildBody = new HashMap<String, Object>();
        List<Map<String, String>> compoundChildrenObjects =
            new ArrayList<Map<String, String>>();
        this.parseOperation(compoundChildBody, key, avOp, compoundChildrenObjects,
            unSavedChildren,
            requestQueue);
        mergeRequestQueue(wrapperRequestBody(compoundChildBody, compoundChildrenObjects, true),
            requestQueue);
      }
    }
    return body;
  }

  // 合并request进去整个requests数组
  private void mergeRequestQueue(Map<String, Object> requestBody, List requestQueue) {
    if (!requestBody.isEmpty()) {
      requestQueue.add(0, requestBody);
    }
  }

  // 封装一些request公用的数据结构
  private Map<String, Object> wrapperRequestBody(Map<String, Object> requestBody, List children,
                                                 boolean compoundRequest) {
    requestBody.put("__children", children);
    if (acl != null) {
      requestBody.putAll(AVUtils.getParsedMap(acl.getACLMap()));
    }

    requestBody.put("__internalId", internalId());
    String method = "PUT";
    boolean post = (AVUtils.isBlankString(getObjectId()) || alwaysUsePost()) && !compoundRequest;
    if (post) {
      method = "POST";
    }
    String path = AVPowerfulUtils.getBatchEndpoint(getPaasClientInstance().getApiVersion(), this, post);
    return getPaasClientInstance().batchItemMap(method, path, requestBody, getBatchParams());
  }

  private Map getBatchParams() {
    if (this.fetchWhenSave) {
      HashMap<Object, Object> hashMap = new HashMap();
      hashMap.put("new", fetchWhenSave);
      return hashMap;
    }
    return null;
  }

  private void buildParameterForNonSavedObject(List list) {
    List<AVObject> unSavedChildren = new LinkedList<AVObject>();
    buildBatchParameterForNonSavedObject(unSavedChildren, list);
    for (AVObject o : unSavedChildren) {
      o.buildParameterForNonSavedObject(list);
    }
  }

  // ================================================================================
  // Check and avoid circle during batch save
  // ================================================================================

  private boolean checkCircleReference() {
    return checkCircleReference(new HashMap<AVObject, Boolean>());
  }

  /*
   * null: node never accessed false: during access true: all the children have accessed
   */
  private boolean checkCircleReference(Map<AVObject, Boolean> status) {
    boolean result = true;

    if (status.get(this) == null) {
      status.put(this, false);
    } else if (status.get(this) == false) {
      LogUtil.log.e("Found a circular dependency while saving");
      return false;
    } else {
      return true;
    }

    for (Object o : instanceData.values()) {
      if (o instanceof AVObject) {
        result = result && ((AVObject) o).checkCircleReference(status);
      }
    }

    status.put(this, true);

    return result;
  }

  /**
   * Saves this object to the server in a background thread. Use this when you do not have code to
   * run on completion of the push.
   */
  public void saveInBackground() {
    saveInBackground(null, null);
  }

  /**
   * Saves this object to the server in a background thread.Use this when you do not have code to
   * run on completion of the push.
   *
   * @param option save options
   */

  public void saveInBackground(AVSaveOption option) {
    saveInBackground(option, null);
  }

  /**
   * Saves this object to the server in a background thread. This is preferable to using save(),
   * unless your code is already running from a background thread.
   *
   * @param callback callback.done(e) is called when the save completes.
   */
  public void saveInBackground(SaveCallback callback) {
    saveInBackground(callback, false);
  }

  /**
   * Saves this object to the server in a background thread.
   *
   * @param option   save options
   * @param callback
   */

  public void saveInBackground(AVSaveOption option, SaveCallback callback) {
    saveObject(option, false, false, callback);
  }

  private void saveInBackground(SaveCallback callback, boolean isEventually) {
    saveObject(false, isEventually, callback);
  }

  /**
   * Set the AVACL governing this object
   *
   * @param acl
   */
  public void setACL(AVACL acl) {
    this.acl = acl;
  }

  /**
   * Setter for the object id. In general you do not need to use this. However, in some cases this
   * can be convenient. For example, if you are serializing a AVObject yourself and wish to recreate
   * it, you can use this to recreate the AVObject exactly.
   */
  public void setObjectId(String newObjectId) {
    objectId = newObjectId;
  }

  private List findArray(Map<String, Object> parent, final String key, boolean create) {
    List array = null;
    try {
      array = (List) parent.get(key);
      if (array != null || !create) {
        return array;
      }
      array = new ArrayList();
      parent.put(key, array);
      return array;
    } catch (Exception exception) {
      LogUtil.log.e(LOGTAG, "find array failed.", exception);
    }
    return array;
  }

  protected String internalClassName() {
    return this.getClassName();
  }

  protected boolean shouldThrowException(Throwable error, String content) {
    return true;
  }

  void addRelationFromServer(final String key, final String className, boolean submit) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {

        @Override
        public AVOp createOp() {
          return new AddRelationOp(key);
        }
      };
      cb.execute(key, submit);
    }
  }

  void addRelation(final AVObject object, final String key, boolean submit) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {

        @Override
        public AVOp createOp() {
          return new AddRelationOp(key, object);
        }
      };
      cb.execute(key, submit);
    }
  }

  void removeRelation(final AVObject object, final String key, boolean submit) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {

        @Override
        public AVOp createOp() {
          return new RemoveRelationOp(key, object);
        }
      };
      cb.execute(key, submit);
    }
  }

  private void addObjectToArray(final String key, final Object value, final boolean unique) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {

        @Override
        public AVOp createOp() {
          if (unique) {
            return new AddUniqueOp(key, value);
          } else {
            return new AddOp(key, value);
          }
        }
      };
      cb.execute(key);
    }

  }

  private void removeObjectForKey(final String key) {
    if (checkKey(key)) {
      KeyValueCallback cb = new KeyValueCallback() {
        @Override
        public AVOp createOp() {
          return new DeleteOp(key);
        }
      };
      cb.execute(key);
    }
  }

  public static void saveFileBeforeSave(List<AVFile> files, final boolean sync,
                                        final SaveCallback callback) throws AVException {
    if (sync) {
      for (AVFile file : files) {
        if (file != null) {
          file.save();
        }
      }
      callback.done(null);
    } else {
      final AtomicInteger lock = new AtomicInteger(AVUtils.collectionNonNullCount(files));
      final AtomicBoolean failureLock = new AtomicBoolean(false);
      for (AVFile file : files) {
        if (file != null) {
          file.saveInBackground(new SaveCallback() {

            @Override
            public void done(AVException e) {
              if (e != null && failureLock.compareAndSet(false, true)) {
                callback.done(e);
              } else if (e != null) {
                return;
              } else if (lock.decrementAndGet() == 0) {
                callback.done(null);
              }
            }
          });
        }
      }
    }
  }


  @Override
  public int hashCode() {
    if (AVUtils.isBlankString(this.objectId)) {
      return super.hashCode();
    }
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getClassName() == null) ? 0 : getClassName().hashCode());
    result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (AVUtils.isBlankString(this.objectId)) {
      return false;
    }
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AVObject other = (AVObject) obj;
    if (getClassName() == null) {
      if (other.getClassName() != null) return false;
    } else if (!getClassName().equals(other.getClassName())) return false;
    if (objectId == null) {
      if (other.objectId != null) return false;
    } else if (!objectId.equals(other.objectId)) {
      return false;
    }
    return true;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int i) {
    out.writeString(this.className);
    out.writeString(this.createdAt);
    out.writeString(this.updatedAt);
    out.writeString(this.objectId);
    out.writeString(JSON.toJSONString(serverData, new ObjectValueFilter(),
        SerializerFeature.NotWriteRootClassName, SerializerFeature.WriteClassName));
    out.writeString(JSON.toJSONString(operationQueue, SerializerFeature.WriteClassName,
        SerializerFeature.NotWriteRootClassName));
  }

  public AVObject(Parcel in) {
    this();
    this.className = in.readString();
    this.createdAt = in.readString();
    this.updatedAt = in.readString();
    this.objectId = in.readString();
    String serverDataStr = in.readString();
    Map<String, Object> serverDataMap = (Map<String, Object>) JSON.parse(serverDataStr);
    if (serverDataMap != null && !serverDataMap.isEmpty()) {
      this.serverData.putAll(serverDataMap);
    }
    Map<String, AVOp> operationQueueMap = (Map<String, AVOp>) JSON.parse(in.readString());
    if (operationQueueMap != null && !operationQueueMap.isEmpty()) {
      this.operationQueue.putAll(operationQueueMap);
    }

    this.rebuildInstanceData();
  }

  protected void rebuildInstanceData() {
    this.instanceData.putAll(serverData);
    for (Map.Entry<String, AVOp> opEntry : operationQueue.entrySet()) {
      String key = opEntry.getKey();
      AVOp op = opEntry.getValue();
      Object oldValue = instanceData.get(key);
      Object newValue = op.apply(oldValue);

      if (newValue == null) {
        instanceData.remove(key);
      } else {
        instanceData.put(key, newValue);
      }
    }
  }

  protected PaasClient getPaasClientInstance() {
    return PaasClient.storageInstance();
  }

  public static transient final Creator CREATOR = AVObjectCreator.instance;

  public static class AVObjectCreator implements Creator {
    public static AVObjectCreator instance = new AVObjectCreator();

    private AVObjectCreator() {

    }

    @Override
    public AVObject createFromParcel(Parcel parcel) {
      AVObject avobject = new AVObject(parcel);
      Class<? extends AVObject> subClass = AVUtils.getAVObjectClassByClassName(avobject.className);
      if (subClass != null) {
        try {
          AVObject returnValue = AVObject.cast(avobject, subClass);
          return returnValue;
        } catch (Exception e) {
        }
      }
      return avobject;
    }

    @Override
    public AVObject[] newArray(int i) {
      return new AVObject[i];
    }
  }

  protected static <T extends AVObject> T cast(AVObject object, Class<T> clazz) throws Exception {
    if (clazz.getClass().isAssignableFrom(object.getClass())) {
      return (T) object;
    } else {
      T newItem = clazz.newInstance();
      newItem.operationQueue.putAll(object.operationQueue);
      newItem.serverData.putAll(object.serverData);
      newItem.createdAt = object.createdAt;
      newItem.updatedAt = object.updatedAt;
      newItem.objectId = object.objectId;
      newItem.rebuildInstanceData();
      return newItem;
    }
  }
}
