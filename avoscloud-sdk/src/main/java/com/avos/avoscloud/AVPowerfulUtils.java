package com.avos.avoscloud;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: tangxiaomin Date: 4/18/13 Time: 3:19 PM
 */
public class AVPowerfulUtils {

  /**
   * 系统内置的一些表
   */
  private static Map<String/* javaClassName */, Map<String, String>> powerfulTable =
      new HashMap<String, Map<String, String>>();
  private static final String ENDPOINT = "endpoint";
  private static final String AV_CLASSNAME = "dbClassName";
  static {
    createTable();
  }

  static void createSettings(String javaClassName, String endpoint, String avClassName) {
    Map<String, String> settings = new HashMap<String, String>();
    settings.put(ENDPOINT, endpoint);
    settings.put(AV_CLASSNAME, avClassName);
    powerfulTable.put(javaClassName, settings);
  }

  private static void createTable() {
    // Add both java class name and av class name.
    // init AVUser
    createSettings(AVUser.class.getSimpleName(), AVUser.AVUSER_ENDPOINT, "_User");
    createSettings("_User", AVUser.AVUSER_ENDPOINT, "_User");

    // init AVRole
    createSettings(AVRole.class.getSimpleName(), AVRole.AVROLE_ENDPOINT, "_Role");
    createSettings("_Role", AVRole.AVROLE_ENDPOINT, "_Role");

    // init AVFile
    createSettings(AVFile.class.getSimpleName(), AVFile.AVFILE_ENDPOINT, "_File");
    createSettings("_File", AVFile.AVFILE_ENDPOINT, "_File");

  }

  private static String get(String javaClassName, String key) {
    String res = "";
    if (powerfulTable.containsKey(javaClassName)) {
      res = powerfulTable.get(javaClassName).get(key);
      if (res == null) {
        res = "";
      }
    }
    return res;
  }

  private static String getAVClassEndpoint(String javaClassName, String avClassName, String objectId) {
    String endpoint = get(javaClassName, ENDPOINT);
    if (AVUtils.isBlankString(endpoint)) {
      if (AVUtils.isBlankString(objectId)) {
        endpoint = String.format("classes/%s", avClassName);
      } else {
        endpoint = String.format("classes/%s/%s", avClassName, objectId);
      }
    } else {
      if (!AVUtils.isBlankString(objectId)) {
        endpoint = String.format("%s/%s", endpoint, objectId);
      } else {
        // 如果 objectId 为空的话，直接返回 endpoint
      }
    }
    return endpoint;
  }

  private static String getAVUserEndpoint(AVUser object) {
    String endpoint = get(AVUser.class.getSimpleName(), ENDPOINT);
    if (!AVUtils.isBlankString(object.getObjectId())) {
      endpoint = String.format("%s/%s", endpoint, object.getObjectId());
    }
    return endpoint;
  }

  private static String getAVRoleEndpoint(AVRole object) {
    String endpoint = get(AVRole.class.getSimpleName(), ENDPOINT);
    if (!AVUtils.isBlankString(object.getObjectId())) {
      endpoint = String.format("%s/%s", endpoint, object.getObjectId());
    }
    return endpoint;
  }

  // Endpoint handler. Try java classname at first, fallback to class name if not found.
  public static String getEndpoint(String className) {
    String endpoint = get(className, ENDPOINT);
    if (AVUtils.isBlankString(endpoint)) {
      if (!AVUtils.isBlankString(className)) {
        endpoint = String.format("classes/%s", className);
      } else {
        throw new AVRuntimeException("Blank class name");
      }
    }
    return endpoint;
  }

  public static String getEndpoint(Object object) {
    return getEndpoint(object, false);
  }

  public static String getEndpoint(Object object, boolean isPost) {
    if (object instanceof AVUser) {
      AVUser avUser = (AVUser) object;
      return getAVUserEndpoint(avUser);
    } else if (object instanceof AVRole) {
      AVRole role = (AVRole) object;
      return getAVRoleEndpoint(role);
    } else if (object instanceof AVObject) {
      AVObject avObject = (AVObject) object;
      Class<? extends AVObject> clazz = avObject.getClass();
      String javaClassName = clazz.getSimpleName();
      String subClassName = AVObject.getSubClassName(clazz);
      if (subClassName != null) {
        // 对于 post 请求，endpoint 中不应该包含 objectId
        return getAVClassEndpoint(javaClassName, subClassName, isPost ? "" : avObject.getObjectId());
      } else {
        return getAVClassEndpoint(javaClassName, avObject.getClassName(), isPost ? "" : avObject.getObjectId());
      }
    } else {
      return getEndpoint(object.getClass().getSimpleName());
    }
  }

  public static String getBatchEndpoint(String version, AVObject object, boolean isPost) {
    return String.format("/%s/%s", version, getEndpoint(object, isPost));
  }

  public static String getEndpointByAVClassName(String className, String objectId) {
    String rootUrl = getEndpoint(className);
    if (AVUtils.isBlankString(rootUrl)) {
      return rootUrl;
    }
    return String.format("%s/%s", rootUrl, objectId);
  }

  public static String getAVClassName(String className) {
    return get(className, AV_CLASSNAME);
  }

  // followee follows follower.
  public static String getFollowEndPoint(String followee, String follower) {
    return String.format("users/%s/friendship/%s", followee, follower);
  }

  public static String getFollowersEndPoint(String userId) {
    return String.format("users/%s/followers", userId);
  }

  public static String getFolloweesEndPoint(String userId) {
    return String.format("users/%s/followees", userId);
  }

  public static String getFollowersAndFollowees(String userId) {
    return String.format("users/%s/followersAndFollowees", userId);
  }

  public static String getInternalIdFromRequestBody(Map request) {
    if (request.get("body") != null) {
      Map body = (Map) request.get("body");
      return (String) body.get("__internalId");
    }
    return null;
  }

}
