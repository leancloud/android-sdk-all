package com.avos.avoscloud;

import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 4/10/13 Time: 9:54 AM To change this template use
 * File | Settings | File Templates.
 */
public class AVErrorUtils {
  public static final int CIRCLE_REFERENCE = 100001;
  public static final int MISSING_OBJECTID = 104;

  // {"error":"The user cannot be altered by a client without the session.","code":206}
  public static AVException createException(String content) {
    try {
      JSONObject object = new JSONObject(content);
      String errorMessage = object.getString("error");
      int code = object.getInt("code");
      return new AVException(code, errorMessage);
    } catch (Exception exception) {
      return new AVException(AVException.UNKNOWN, content);
    }
  }

  public static AVException createException(Throwable t, String content) {
    if (content != null) {
      return createException(content);
    } else if (t != null) {
      if (t instanceof AVException) {
        return (AVException) t;
      }
      return new AVException(t);
    } else {
      return new AVException(AVException.UNKNOWN, "unknown reason");
    }

  }

  static int errorCode(String content) {
    int code = 0;
    try {
      JSONObject object = new JSONObject(content);
      code = object.getInt("code");
      return code;
    } catch (Exception exception) {}
    return code;
  }

  static public AVException createException(int code, String content) {
    return new AVException(code, content);
  }

  static public AVException invalidObjectIdException() {
    return AVErrorUtils.createException(AVException.MISSING_OBJECT_ID, "Invalid object id.");
  }

  static public AVException invalidQueryException() {
    return AVErrorUtils.createException(AVException.INVALID_QUERY, "Invalid query.");
  }

  static public AVException sessionMissingException() {
    return AVErrorUtils.createException(AVException.SESSION_MISSING,
        "No valid session token, make sure signUp or login has been called.");
  }

  static public AVException fileDownloadInConsistentFailureException() {
    return AVErrorUtils.createException(AVException.FILE_DOWNLOAD_INCONSISTENT_FAILURE,
        "Downloaded file is inconsistent with original file");
  }


  // ================================================================================
  // Some Special Exception
  // ================================================================================

  static AVException circleException() {
    return new AVException(CIRCLE_REFERENCE, "Found a circular dependency when saving.");
  }

}
