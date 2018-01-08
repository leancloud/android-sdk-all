package com.avos.avoscloud;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.utils.StringUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
public class AVUtils {
  private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String classNameTag = "className";
  public static final String typeTag = "__type";
  public static final String objectIdTag = "objectId";

  public static Map<String, Object> createArrayOpMap(String key, String op, Collection<?> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Object> array = new ArrayList<Object>();
    for (Object obj : objects) {
      array.add(getParsedObject(obj));
    }
    map.put("objects", array);
    Map<String, Object> ops = new HashMap<String, Object>();
    ops.put(key, map);
    return ops;
  }

  private static Map<Class<?>, Field[]> fieldsMap = Collections
      .synchronizedMap(new WeakHashMap<Class<?>, Field[]>());

  public static Field[] getAllFiels(Class<?> clazz) {
    if (clazz == null || clazz == Object.class) {
      return new Field[0];
    }
    Field[] theResult = fieldsMap.get(clazz);
    if (theResult != null) {
      return theResult;
    }
    List<Field[]> fields = new ArrayList<Field[]>();
    int length = 0;
    while (clazz != null && clazz != Object.class) {
      Field[] declaredFields = clazz.getDeclaredFields();
      length += declaredFields != null ? declaredFields.length : 0;
      fields.add(declaredFields);
      clazz = clazz.getSuperclass();
    }
    theResult = new Field[length];
    int i = 0;
    for (Field[] someFields : fields) {
      if (someFields != null) {
        for (Field field : someFields) {
          field.setAccessible(true);
        }
        System.arraycopy(someFields, 0, theResult, i, someFields.length);
        i += someFields.length;
      }
    }
    fieldsMap.put(clazz, theResult);
    return theResult;
  }

  static Pattern pattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  static Pattern emailPattern = Pattern.compile("^\\w+?@\\w+?[.]\\w+");
  static Pattern phoneNumPattern = Pattern.compile("1\\d{10}");
  static Pattern verifyCodePattern = Pattern.compile("\\d{6}");

  public static boolean checkEmailAddress(String email) {
    return emailPattern.matcher(email).find();
  }

  public static boolean checkMobilePhoneNumber(String phoneNumber) {
    return phoneNumPattern.matcher(phoneNumber).find();
  }

  public static boolean checkMobileVerifyCode(String verifyCode) {
    return verifyCodePattern.matcher(verifyCode).find();
  }

  public static void checkClassName(String className) {
    if (isBlankString(className)) throw new IllegalArgumentException("Blank class name");
    if (!pattern.matcher(className).matches())
      throw new IllegalArgumentException("Invalid class name");
  }

  public static boolean isBlankString(String str) {
    return StringUtils.isBlankString(str);
  }

  public static boolean isBlankContent(String content) {
    return StringUtils.isBlankJsonContent(content);
  }

  public static boolean contains(Map<String, Object> map, String key) {
    return map.containsKey(key);
  }

  public static Map<String, Object> createDeleteOpMap(String key) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", "Delete");
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(key, map);
    return result;
  }

  public static Map<String, Object> createPointerArrayOpMap(String key, String op,
      Collection<AVObject> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Map<String, ?>> list = new ArrayList<Map<String, ?>>();
    for (AVObject obj : objects) {
      list.add(AVUtils.mapFromPointerObject(obj));
    }
    map.put("objects", list);
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(key, map);
    return result;
  }

  public static Map<String, Object> createStringObjectMap(String key, Object value) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(key, value);
    return map;
  }

  public static Map<String, Object> mapFromPointerObject(AVObject object) {
    return mapFromAVObject(object, false);
  }

  public static Map<String, Object> mapFromUserObjectId(final String userObjectId) {
    if (isBlankString(userObjectId)) {
      return null;
    }
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "Pointer");
    result.put("className", "_User");
    result.put("objectId", userObjectId);
    return result;
  }

  public static Map<String, String> mapFromChildObject(AVObject object, String key) {
    String cid = object.internalId();
    Map<String, String> child = new HashMap(3);
    child.put("cid", cid);
    child.put("className", object.getClassName());
    child.put("key", key);
    return child;
  }

  private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_DATE_FORMAT =
      new ThreadLocal<SimpleDateFormat>();

  public static boolean isDigitString(String s) {
    return StringUtils.isDigitString(s);
  }

  public static Date dateFromString(String content) {
    if (isBlankString(content)) return null;
    if (isDigitString(content)) {
      return new Date(Long.parseLong(content));
    }
    Date date = null;
    SimpleDateFormat format = THREAD_LOCAL_DATE_FORMAT.get();
    // reuse date format.
    if (format == null) {
      format = new SimpleDateFormat(dateFormat);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      THREAD_LOCAL_DATE_FORMAT.set(format);
    }
    try {
      date = format.parse(content);
    } catch (Exception exception) {
      LogUtil.log.e(exception.toString());
    }
    return date;
  }

  public static String stringFromDate(Date date) {
    if (null == date) {
      return null;
    }

    SimpleDateFormat df = new SimpleDateFormat(dateFormat);
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    String isoDate = df.format(date);
    return isoDate;
  }

  public static Map<String, Object> mapFromDate(Date date) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Date");
    result.put("iso", stringFromDate(date));
    return result;
  }

  public static Date dateFromMap(Map<String, Object> map) {
    String value = (String) map.get("iso");
    return dateFromString(value);
  }

  public static Map<String, Object> mapFromGeoPoint(AVGeoPoint point) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "GeoPoint");
    result.put("latitude", point.getLatitude());
    result.put("longitude", point.getLongitude());
    return result;
  }

  public static AVGeoPoint geoPointFromMap(Map<String, Object> map) {
    double la = ((Number) map.get("latitude")).doubleValue();
    double lo = ((Number) map.get("longitude")).doubleValue();
    AVGeoPoint point = new AVGeoPoint(la, lo);
    return point;
  }

  // create cooresponding object from class name.
  public static AVObject objectFromRelationMap(Map<String, Object> map) {
    String className = (String) map.get(classNameTag);
    AVObject object = objectFromClassName(className);
    return object;
  }

  public static Map<String, Object> mapFromByteArray(byte[] data) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Bytes");
    result.put("base64", Base64.encodeToString(data, Base64.NO_WRAP));
    return result;
  }

  public static byte[] dataFromMap(Map<String, Object> map) {
    String value = (String) map.get("base64");
    return Base64.decode(value, Base64.NO_WRAP);
  }

  public static String jsonStringFromMapWithNull(Object map) {

    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero,
          SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero);
    }
  }

  public static String jsonStringFromObjectWithNull(Object map) {

    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero,
          SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,
          SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullNumberAsZero);
    }
  }

  public static Map<String, Object> mapFromFile(AVFile file) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", AVFile.className());
    result.put("metaData", file.getMetaData());
    result.put("id", file.getName());
    return result;
  }

  public static AVFile fileFromMap(Map<String, Object> map) {
    AVFile file = new AVFile("", "");
    AVUtils.copyPropertiesFromMapToObject(map, file);
    Object metadata = map.get("metaData");
    if (metadata != null && metadata instanceof Map) file.getMetaData().putAll((Map) metadata);
    if (AVUtils.isBlankString((String) file.getMetaData(AVFile.FILE_NAME_KEY))) {
      file.getMetaData().put(AVFile.FILE_NAME_KEY, file.getName());
    }
    file.setName((String) map.get("objectId"));
    return file;
  }

  public static AVObject parseObjectFromMap(Map<String, Object> map) {
    AVObject object = newAVObjectByClassName((String) map.get(classNameTag));
    object.setObjectId((String) map.get("objectId"));
    AVUtils.copyPropertiesFromMapToAVObject(map, object);
    return object;
  }

  public static String restfulServerData(Map<String, ?> data) {
    if (data == null) return "{}";

    Map<String, Object> map = getParsedMap((Map<String, Object>) data);
    return jsonStringFromMapWithNull(map);
  }

  public static String restfulCloudData(Object object) {
    if (object == null) return "{}";
    if (object instanceof Map) {
      return jsonStringFromMapWithNull(getParsedMap((Map<String, Object>) object, true));
    } else if (object instanceof Collection) {
      return jsonStringFromObjectWithNull(getParsedList((Collection) object, true));
    } else if (object instanceof AVObject) {
      return jsonStringFromMapWithNull(mapFromAVObject((AVObject) object, true));
    } else if (object instanceof AVGeoPoint) {
      return jsonStringFromMapWithNull(mapFromGeoPoint((AVGeoPoint) object));
    } else if (object instanceof Date) {
      return jsonStringFromObjectWithNull(mapFromDate((Date) object));
    } else if (object instanceof byte[]) {
      return jsonStringFromMapWithNull(mapFromByteArray((byte[]) object));
    } else if (object instanceof AVFile) {
      return jsonStringFromMapWithNull(mapFromFile((AVFile) object));
    } else if (object instanceof org.json.JSONObject) {
      return jsonStringFromObjectWithNull(JSON.parse(object.toString()));
    } else if (object instanceof org.json.JSONArray) {
      return jsonStringFromObjectWithNull(JSON.parse(object.toString()));
    } else {
      return jsonStringFromObjectWithNull(object);
    }
  }

  private static Map<String, Object> mapFromAVObject(AVObject object, boolean topObject) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("className", object.internalClassName());

    if (!isBlankString(object.getObjectId())) {
      result.put("objectId", object.getObjectId());
    }
    if (!topObject) {
      result.put("__type", "Pointer");
    } else {
      result.put("__type", "Object");

      Map<String, Object> serverData = getParsedMap(object.serverData, false);
      if (serverData != null && !serverData.isEmpty()) {
        result.putAll(serverData);
      }
    }
    return result;
  }

  private static List getParsedList(Collection object, boolean topObject) {
    if (!topObject) {
      return getParsedList(object);
    } else {
      List newList = new ArrayList(object.size());

      for (Object o : object) {
        newList.add(getParsedObject(o, true));
      }

      return newList;
    }
  }

  private static Map<String, Object> getParsedMap(Map<String, Object> object, boolean topObject) {
    Map newMap = new HashMap<String, Object>(object.size());

    for (Map.Entry<String, Object> entry : object.entrySet()) {
      final String key = entry.getKey();
      Object o = entry.getValue();
      newMap.put(key, getParsedObject(o, topObject));
    }

    return newMap;
  }

  public static boolean hasProperty(Class<?> clazz, String property) {
    Field fields[] = getAllFiels(clazz);
    for (Field f : fields) {
      if (f.getName().equals(property)) {
        return true;
      }
    }
    return false;
  }

  public static boolean checkAndSetValue(Class<?> clazz, Object parent, String property,
      Object value) {
    if (clazz == null) {
      return false;
    }
    try {
      Field fields[] = getAllFiels(clazz);
      for (Field f : fields) {
        if (f.getName().equals(property) && (f.getType().isInstance(value) || value == null)) {
          f.set(parent, value);
          return true;
        }
      }
      return false;
    } catch (Exception exception) {
      // TODO throw exception?
      // exception.printStackTrace();
    }
    return false;
  }

  public static void updatePropertyFromMap(AVObject parent, String key, Map<String, Object> map) {
    if (isACL(key)) {
      parent.setACL(new AVACL(map));
      return;
    }

    String objectId = (String) map.get(objectIdTag);
    String type = (String) map.get(typeTag);
    if (type == null && objectId == null) {
      parent.put(key, map, false);
      return;
    }

    if (isGeoPoint(type)) {
      AVGeoPoint point = geoPointFromMap(map);
      parent.put(key, point, false);
    } else if (isDate(type)) {
      Date date = dateFromMap(map);
      parent.put(key, date, false);
    } else if (isData(type)) {
      byte[] data = dataFromMap(map);
      parent.put(key, data, false);
    } else if (isFile(type)) {
      AVFile file = AVUtils.fileFromMap(map);
      parent.put(key, file, false);
    } else if (isFileFromUrulu(map)) {
      AVFile file = AVUtils.fileFromMap(map);
      parent.put(key, file, false);
    } else if (isRelation(type)) {
      parent.addRelationFromServer(key, (String) map.get(classNameTag), false);
    } else if (isPointer(type) || (!isBlankString(objectId) && type != null)) {
      AVObject object = AVUtils.parseObjectFromMap(map);
      parent.put(key, object, false);
    } else {
      parent.put(key, map, false);
    }
  }

  public static void updatePropertyFromList(AVObject object, String key, Collection<Object> list) {
    List data = getObjectFrom(list);
    object.put(key, data, false);
  }

  public static void copyPropertiesFromJsonStringToAVObject(String content, AVObject object) {
    if (isBlankString(content)) return;
    try {
      Map<String, Object> map = JSONHelper.mapFromString(content);
      copyPropertiesFromMapToAVObject(map, object);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  public static void copyPropertiesFromMapToAVObject(Map<String, Object> map, AVObject object) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      if (key != null && key.startsWith("_")) {
        continue;
      }
      Object valueObject = entry.getValue();
      if (checkAndSetValue(object.getClass(), object, key, valueObject)) {
        // also put it into keyValues map.
        if (!key.startsWith("_") && !AVObject.INVALID_KEYS.contains(key)) {
          object.put(key, valueObject, false);
        }
        continue;
      } else if (valueObject instanceof Collection) {
        updatePropertyFromList(object, key, (Collection) valueObject);
      } else if (valueObject instanceof Map) {
        updatePropertyFromMap(object, key, (Map<String, Object>) valueObject);
      } else {
        if (!key.startsWith("_")) {
          object.put(key, valueObject, false);
        }
      }
    }
  }

  public static void copyPropertiesFromMapToObject(Map<String, Object> map, Object object) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      Object valueObject = entry.getValue();
      if (checkAndSetValue(object.getClass(), object, key, valueObject)) {
        continue;
      }
    }
  }

  public static Class getClass(Map<String, ?> map) {
    Object type = map.get("__type");

    if (type == null || !(type instanceof String)) {
      return Map.class;
    } else if (type.equals("Pointer")) {
      return AVObject.class;
    } else if (type.equals("GeoPoint")) {
      return AVGeoPoint.class;
    } else if (type.equals("Bytes")) {
      return byte[].class;
    } else if (type.equals("Date")) {
      return Date.class;
    }

    return Map.class;
  }

  public static boolean isRelation(String type) {
    return (type != null && type.equals("Relation"));
  }

  public static boolean isPointer(String type) {
    return (type != null && type.equals("Pointer"));
  }

  public static boolean isGeoPoint(String type) {
    return (type != null && type.equals("GeoPoint"));
  }

  public static boolean isACL(String type) {
    return (type != null && type.equals("ACL"));
  }

  public static boolean isDate(String type) {
    return (type != null && type.equals("Date"));
  }

  public static boolean isData(String type) {
    return (type != null && type.equals("Bytes"));
  }

  public static boolean isFile(String type) {
    return (type != null && type.equals("File"));
  }

  public static boolean isFileFromUrulu(Map<String, Object> map) {
    // ugly way to check dict whether is avfile
    boolean result = true;
    result &= map.get("mime_type") != null;
    return result;
  }

  public static AVObject objectFromClassName(String className) {
    if (className.equals(AVPowerfulUtils.getAVClassName(AVUser.class.getSimpleName()))) {
      return AVUser.newAVUser();
    }
    AVObject object = newAVObjectByClassName(className);
    return object;
  }

  public static AVObject newAVObjectByClassName(String name) {
    if (name.equals(AVRole.className)) {
      return new AVRole();
    } else if (name.equals(AVUser.userClassName())) {
      return AVUser.newAVUser();
    } else {
      // maybe it's AVObject's subclass
      Class<? extends AVObject> subClazz = AVObject.getSubClass(name);
      if (subClazz != null) {
        try {
          return subClazz.newInstance();
        } catch (Exception e) {
          throw new AVRuntimeException("New subclass instance failed.", e);
        }
      } else {
        // just new a AVObject
        return new AVObject(name);
      }
    }
  }

  public static Class<? extends AVObject> getAVObjectClassByClassName(String name) {
    if (name.equals(AVRole.className)) {
      return AVRole.class;
    } else if (name.equals(AVUser.userClassName())) {
      return AVUser.class;
    } else {
      // maybe it's AVObject's subclass
      Class<? extends AVObject> subClazz = AVObject.getSubClass(name);
      return subClazz;
    }
  }

  public static AVObject newAVObjectByClassName(String className, String defaultClassName) {
    String objectClassName = AVUtils.isBlankString(className) ? defaultClassName : className;
    return newAVObjectByClassName(objectClassName);
  }

  // ================================================================================
  // Handle JSON and Object
  // ================================================================================

  public static final <T> T getFromJSON(String json, Class<T> clazz) {
    return JSON.parseObject(json, clazz);
  }

  public static final <T> String toJSON(T clazz) {
    if (AVOSCloud.showInternalDebugLog()) {
      return JSON.toJSONString(clazz, SerializerFeature.PrettyFormat);
    } else {
      return JSON.toJSONString(clazz);
    }
  }

  // ================================================================================
  // Data for server
  // ================================================================================

  public static Map<String, Object> getParsedMap(Map<String, Object> map) {
    return getParsedMap(map, false);
  }

  static List getParsedList(Collection list) {
    List newList = new ArrayList(list.size());

    for (Object o : list) {
      newList.add(getParsedObject(o));
    }

    return newList;
  }

  public static Object getParsedObject(Object object) {
    return getParsedObject(object, false);
  }

  public static Object getParsedObject(Object object, boolean topObject) {
    if (object == null) {
      return null;
    } else if (object instanceof Map) {
      return getParsedMap((Map<String, Object>) object, topObject);
    } else if (object instanceof Collection) {
      return getParsedList((Collection) object, topObject);
    } else if (object instanceof AVObject) {
      if (!topObject) {
        return mapFromPointerObject((AVObject) object);
      } else {
        return mapFromAVObject((AVObject) object, true);
      }
    } else if (object instanceof AVGeoPoint) {
      return mapFromGeoPoint((AVGeoPoint) object);
    } else if (object instanceof Date) {
      return mapFromDate((Date) object);
    } else if (object instanceof byte[]) {
      return mapFromByteArray((byte[]) object);
    } else if (object instanceof AVFile) {
      return mapFromFile((AVFile) object);
    } else if (object instanceof org.json.JSONObject) {
      return JSON.parse(object.toString());
    } else if (object instanceof org.json.JSONArray) {
      return JSON.parse(object.toString());
    } else {
      return object;
    }
  }

  // ================================================================================
  // Data from server
  // ================================================================================
  /*
   * response like this:
   * {"result":"Hello world!"}
   * { "result": { "__type": "Object", "className": "Armor", "createdAt":
   * "2013-04-02T06:15:27.211Z", "displayName": "Wooden Shield", "fireproof": false, "objectId":
   * "2iGGg18C7H", "rupees": 50, "updatedAt": "2013-04-02T06:15:27.211Z" } }
   * { "result": [ { "__type": "Object", "cheatMode": false, "className": "Armor", "createdAt":
   * "2013-04-20T07:45:54.962Z", "objectId": "8o2ncpWitt", "otherArmor": { "__type": "Pointer",
   * "className": "Armor", "objectId": "dEvrhyRGcr" }, "playerName": "Sean Plott", "score": 1337,
   * "testBytes": { "__type": "Bytes", "base64": "VGhpcyBpcyBhbiBlbmNvZGVkIHN0cmluZw==" },
   * "testDate": { "__type": "Date", "iso": "2011-08-21T18:02:52.249Z" }, "testGeoPoint": {
   * "__type": "GeoPoint", "latitude": 40, "longitude": -30 }, "testRelation": { "__type":
   * "Relation", "className": "GameScore" }, "updatedAt": "2013-04-20T07:45:54.962Z" } ] }
   */
  static List getObjectFrom(Collection list) {
    List newList = new ArrayList();

    for (Object obj : list) {
      newList.add(getObjectFrom(obj));
    }

    return newList;
  }

  static Object getObjectFrom(Map<String, Object> map) {
    Object type = map.get("__type");
    if (type == null || !(type instanceof String)) {
      Map<String, Object> newMap = new HashMap<String, Object>(map.size());

      for (Map.Entry<String, Object> entry : map.entrySet()) {
        final String key = entry.getKey();
        Object o = entry.getValue();
        newMap.put(key, getObjectFrom(o));
      }

      return newMap;
    } else if (type.equals("Pointer") || type.equals("Object")) {
      AVObject avObject = objectFromClassName((String) map.get("className"));
      map.remove("__type");
      AVUtils.copyPropertiesFromMapToAVObject(map, avObject);
      return avObject;
    } else if (type.equals("GeoPoint")) {
      return AVUtils.geoPointFromMap(map);
    } else if (type.equals("Bytes")) {
      return AVUtils.dataFromMap(map);
    } else if (type.equals("Date")) {
      return AVUtils.dateFromMap(map);
    } else if (type.equals("Relation")) {
      return AVUtils.objectFromRelationMap(map);
    } else if (type.equals("File")) {
      return AVUtils.fileFromMap(map);
    }
    return map;
  }

  static Object getObjectFrom(Object obj) {
    if (obj instanceof Collection) {
      return getObjectFrom((Collection) obj);
    } else if (obj instanceof Map) {
      return getObjectFrom((Map<String, Object>) obj);
    }

    return obj;
  }

  // ================================================================================
  // String Utils
  // ================================================================================
  public static String md5(String string) {
    return StringUtils.md5(string);
  }

  static Random random = new Random(System.currentTimeMillis());

  public static String getRandomString(int length) {
    return StringUtils.getRandomString(length);
  }

  static AtomicInteger acu = new AtomicInteger(-65536);

  public static int getNextIMRequestId() {
    int val = acu.incrementAndGet();
    if (val > 65535) {
      while (val > 65535 && !acu.compareAndSet(val, -65536)) {
        val = acu.get();
      }
      return val;
    } else {
      return val;
    }
  }

  // ================================================================================
  // NetworkUtil
  // ================================================================================
  public static boolean isWifi(Context context) {
    ConnectivityManager connectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
    if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
      return true;
    }
    return false;
  }

  public static boolean isConnected(Context context) {
    try {
      ConnectivityManager conMgr =
          (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

      if (activeNetwork != null && activeNetwork.isConnected()) {
        return true;
      }
    } catch (SecurityException e) {
      LogUtil.log.e("Please add ACCESS_NETWORK_STATE permission in your manifest", e);
      return true;
    } catch (Exception e) {
      LogUtil.log.e("Exception: ", e);
    }
    return false;
  }

  public static long getCurrentTimestamp() {
    return System.currentTimeMillis();
  }

  public static String joinCollection(Collection<String> collection, String separator) {
    return StringUtils.join(separator, collection);
  }

  public static String stringFromBytes(byte[] bytes) {
    return StringUtils.stringFromBytes(bytes);
  }

  // ==================
  // added by xtang
  public static boolean checkPermission(Context context, String permission) {
    if (context == null) {
      LogUtil.log.e("context is null");
      return false;
    }

    return PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(permission);
  }

  public static boolean isPushServiceAvailable(Context context, final java.lang.Class cls) {
    final PackageManager packageManager = context.getPackageManager();
    final Intent intent = new Intent(context, cls);
    List resolveInfo =
        packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
    if (resolveInfo.size() > 0) {
      return true;
    }
    return false;
  }

  public static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  public static String fileMd5(String fileName) throws IOException {
    return computeMD5(readFile(fileName));
  }

  public static byte[] readFile(String file) throws IOException {
    return readFile(new File(file));
  }

  public static byte[] readFile(File file) throws IOException {
    // Open file
    RandomAccessFile f = new RandomAccessFile(file, "r");

    try {
      // Get and check length
      long longlength = f.length();
      int length = (int) longlength;
      if (length != longlength) throw new IOException("File size >= 2 GB");

      // Read file and return data
      byte[] data = new byte[length];
      f.readFully(data);
      return data;
    } finally {
      AVPersistenceUtils.closeQuietly(f);
    }
  }

  /**
   * get a remote file's mime type
   * @param url
   * @return the mime type for the given file or empty string if there is none.
   */
  public static String getMimeTypeFromUrl(String url) {
    if (!AVUtils.isBlankContent(url)) {
      String extension = MimeTypeMap.getFileExtensionFromUrl(url);
      if (!AVUtils.isBlankContent(extension)) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(extension);
      }
    }
    return "";
  }

  /**
   * get a local file's mime type
   * @param localPath
   * @return the mime type for the given file or empty string if there is none.
   */
  public static String getMimeTypeFromLocalFile(String localPath) {
    if (!AVUtils.isBlankContent(localPath) && localPath.contains(".")) {
      String extension = localPath.substring(localPath.lastIndexOf('.') + 1);
      if (!AVUtils.isBlankContent(extension)) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(extension);
      }
    }
    return "";
  }

  public static String hexEncodeBytes(byte[] md5bytes) {
    return StringUtils.hexEncodeBytes(md5bytes);
  }

  public static String computeMD5(byte[] input) {
    return StringUtils.computeMD5(input);
  }

  static String getJSONString(com.alibaba.fastjson.JSONObject object, final String key,
      final String defaultValue) {
    if (object.containsKey(key)) {
      return object.getString(key);
    }
    return defaultValue;
  }

  static long getJSONInteger(com.alibaba.fastjson.JSONObject object, final String key,
      long defaultValue) {
    if (object.containsKey(key)) {
      return object.getInteger(key);
    }
    return defaultValue;
  }

  public static final int TYPE_WIFI = 1;
  public static final int TYPE_MOBILE = 2;
  public static final int TYPE_NOT_CONNECTED = 0;

  public static int getConnectivityStatus(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    if (null != activeNetwork) {
      if (!activeNetwork.isConnected()) return TYPE_NOT_CONNECTED;

      if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) return TYPE_WIFI;

      if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) return TYPE_MOBILE;
    }
    return TYPE_NOT_CONNECTED;
  }

  public static String getConnectivityStatusString(Context context) {
    int conn = getConnectivityStatus(context);
    String status = null;
    if (conn == TYPE_WIFI) {
      status = "Wifi enabled";
    } else if (conn == TYPE_MOBILE) {
      status = "Mobile data enabled";
    } else if (conn == TYPE_NOT_CONNECTED) {
      status = "Not connected to Internet";
    }
    return status;
  }

  // TODO return file name for different eventually request
  public static String getArchiveRequestFileName(String objectId, String _internalId,
      String method, String relativePath, String paramString) {
    // 当数据是更新时，说明已经有了ObjectId，那么paramString其实是增量数据，所以需要分开文件
    if (method.equalsIgnoreCase("put")) {
      return AVUtils.md5(relativePath + paramString);
    }
    // 当对象尚未在服务器创建时，每一次请求都是全数据的，所以只需要最新的那份数据即可
    else if (method.equalsIgnoreCase("post")) {
      return _internalId;
    } else if (method.equalsIgnoreCase("delete")) {
      // 倘若都没有ObjectId就已经出现代码删除，则直接覆盖
      return AVUtils.isBlankString(objectId) ? _internalId : AVUtils
          .md5(relativePath + paramString);
    }
    return AVUtils.md5(relativePath + paramString);
  }

  public static int collectionNonNullCount(Collection collection) {
    int count = 0;
    Iterator iterator = collection.iterator();
    while (iterator.hasNext()) {
      if (iterator.next() != null) {
        count++;
      }
    }
    return count;
  }

  public static String urlCleanLastSlash(String url) {
    if (!AVUtils.isBlankString(url) && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    } else {
      return url;
    }
  }

  public static String getSessionKey(String selfId) {
    StringBuilder sb = new StringBuilder(AVOSCloud.applicationId);
    sb.append(selfId);
    return sb.toString();
  }

  public static boolean isEmptyList(List e) {
    return e == null || e.isEmpty();
  }

  public static void ensureElementsNotNull(List<String> e, String errorLog) {
    for (String i : e) {
      if (i == null) {
        throw new NullPointerException(errorLog);
      }
    }
  }

  /*
   * true when firstNumber is bigger
   * false when firstNumber is smaller
   */
  public static boolean compareNumberString(String firstNumber, String secondNumber) {
    return (Double.compare(Double.parseDouble(firstNumber), Double.parseDouble(secondNumber)) == 1);
  }

  public static String base64Encode(String data) {
    if (null == data) {
      return "";
    }
    return base64Encode(data.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
  }

  private static String base64Encode(byte[] val, int flags) {
    if (null == val) {
      return "";
    }
    return android.util.Base64.encodeToString(val, flags);
  }
  public static String base64Encode(byte[] data) {
    return base64Encode(data, Base64.NO_WRAP);
  }

  public static byte[] base64Decode(String data) {
    if (null == data) {
      return null;
    }
    return android.util.Base64.decode(data.getBytes(), android.util.Base64.NO_WRAP);
  }

  public static Handler getUIThreadHandler() {
    return AVOSCloud.handler;
  }

  public static Map<String, Object> createMap(String cmp, Object value) {
    Map<String, Object> dict = new HashMap<String, Object>();
    dict.put(cmp, value);
    return dict;
  }

  public static boolean checkResponseType(int statusCode, String content, String contentType,
      GenericObjectCallback callback) {
    if (statusCode > 0 && !PaasClient.isJSONResponse(contentType)) {
      if (callback != null) {
        callback
            .onFailure(
                statusCode, new AVException(AVException.INVALID_JSON,
                    "Wrong response content type:"
                        + contentType),
                content);
      }
      return true;
    }
    return false;
  }

  public static boolean checkDNSException(int statusCode, Throwable throwable) {
    return AVUtils.isConnected(AVOSCloud.applicationContext) && statusCode == 0
        && throwable instanceof IOException
        && throwable.getMessage() != null
        && throwable.getMessage().toLowerCase().contains("unknownhostexception");
  }

  public static String getHostName(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  public static String getPath(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return uri.getPath();
  }

  public static String getAVObjectClassName(Class<? extends AVObject> clazz) {
    return AVObject.getSubClassName(clazz);
  }

  public static String getAVObjectCreatedAt(AVObject object) {
    return object.createdAt;
  }

  public static String getAVObjectUpdatedAt(AVObject object) {
    return object.updatedAt;
  }

  public static String getEncodeUrl(String url, Map<String, String> params) {
    return new AVRequestParams(params).getWholeUrl(url);
  }

  @SuppressWarnings("unchecked")
  public static String getJSONValue(String msg, String key) {
    Map<String, Object> jsonMap = JSON.parseObject(msg, HashMap.class);
    if (jsonMap == null || jsonMap.isEmpty()) return null;

    Object action = jsonMap.get(key);
    return action != null ? action.toString() : null;
  }

  public static String addQueryParams(String path, Map<String, Object> params) {
    LinkedList<NameValuePair> pairs = new LinkedList<>();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      pairs.add(new BasicNameValuePair(entry.getKey(), JSON.toJSONString(entry.getValue())));
    }
    return String.format("%s?%s", path, URLEncodedUtils.format(pairs, "UTF-8"));
  }

  protected static final int defaultFileKeyLength = 40;

  public static String parseFileKey(String fileName) {
    String key = AVUtils.getRandomString(defaultFileKeyLength);
    int idx = 0;
    if (!AVUtils.isBlankString(fileName)) {
      idx = fileName.lastIndexOf(".");
    }
    // try to add post fix.
    if (idx > 0) {
      String postFix = fileName.substring(idx);
      key += postFix;
    }
    return key;
  }

  private static String convertToHex(byte[] data) {
    StringBuilder buf = new StringBuilder();
    for (byte b : data) {
      int halfbyte = (b >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        buf.append((0 <= halfbyte) && (halfbyte <= 9)
            ? (char) ('0' + halfbyte)
            : (char) ('a' + (halfbyte - 10)));
        halfbyte = b & 0x0F;
      } while (two_halfs++ < 1);
    }
    return buf.toString();
  }

  public static String SHA1(byte[] data) throws NoSuchAlgorithmException,
      UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(data, 0, data.length);
    byte[] sha1hash = md.digest();
    return convertToHex(sha1hash);
  }
}
