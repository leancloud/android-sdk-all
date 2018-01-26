package com.avos.avoscloud;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


/**
 * <p>
 * The AVCloud class defines provides methods for interacting with AVOSCloud Cloud Functions. A
 * Cloud Function can be called with AVCloud.callFunctionInBackground(String, Map, FunctionCallback)
 * using a FunctionCallback. For example, this sample code calls the "validateGame" Cloud Function
 * and calls processResponse if the call succeeded and handleError if it failed.
 * </p>
 * 
 * <pre>
 *   AVCloud.callFunctionInBackground("validateGame", parameters, new FunctionCallback() {
 *       public void done(Object object, AVException e) {
 *           if (e == null) {
 *               processResponse(object);
 *           } else {
 *               handleError();
 *           }
 *       }
 *   }
 * </pre>
 * <p>
 * Using the callback methods is usually preferred because the network operation will not block the
 * calling thread. However, in some cases it may be easier to use the * AVCloud.callFunction(String,
 * Map) call which do block the calling thread. For example, if your application has already spawned
 * a background task to perform work, that background task could use the blocking calls and avoid
 * the code complexity of callbacks.
 * </p>
 */
public class AVCloud {

  private static boolean isProduction = true;
  /**
   * 设置调用云代码函数的测试环境或者生产环境，默认为true，也就是生产环境。
   * 
   * @param productionMode
   */
  public static void setProductionMode(boolean productionMode) {
    isProduction = productionMode;
  }
  public static boolean isProductionMode() {return isProduction;}

  /**
   * Calls a cloud function.
   * 
   * @param name The cloud function to call
   * @param params The parameters to send to the cloud function. This map can contain anything that
   *          could be placed in a AVObject except for AVObjects themselves.
   * @return The result of the cloud call. Result may be a @{link Map}< String, ?>, AVObject,
   *         List<?>, or any type that can be set as a field in a AVObject.
   * @throws AVException
   */
  public static <T> T callFunction(String name, Map<String, ?> params) throws AVException {
    final AtomicReference<T> reference = new AtomicReference<T>();
    PaasClient.cloudInstance().postObject("functions/" + name, AVUtils.restfulServerData(params),
      getProductionHeader(), true, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            reference.set((T) convertCloudResponse(content));
          }

          @Override
          public void onFailure(Throwable error, String content) {
            LogUtil.log.d(content + error);
            AVExceptionHolder.add(AVErrorUtils.createException(error, content));
          }
        });
    if (AVExceptionHolder.exists()) throw AVExceptionHolder.remove();
    return reference.get();
  }

  /**
   * Calls a cloud function in the background.
   * 
   * @param name The cloud function to call
   * @param params The parameters to send to the cloud function. This map can contain anything that
   *          could be placed in a AVObject except for AVObjects themselves.
   * @param callback The callback that will be called when the cloud function has returned.
   */
  public static <T> void callFunctionInBackground(String name, Map<String, ?> params,
      final FunctionCallback<T> callback) {
    // for test
    PaasClient.cloudInstance().postObject("functions/" + name, AVUtils.restfulServerData(params),
      getProductionHeader(), false, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (callback != null) {
              callback.internalDone((T) convertCloudResponse(content), e);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            if (callback != null) {
              callback.internalDone(null, AVErrorUtils.createException(error, content));
            }
          }
        });
  }

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
  // TODO: should be private
  public static Object convertCloudResponse(String response) {
    Object newResultValue = null;
    try {
      Map<String, ?> resultMap = AVUtils.getFromJSON(response, Map.class);
      Object resultValue = resultMap.get("result");

      if (resultValue instanceof Collection) {
        newResultValue = AVUtils.getObjectFrom((Collection) resultValue);
      } else if (resultValue instanceof Map) {
        newResultValue = AVUtils.getObjectFrom((Map<String, Object>) resultValue);
      } else {
        // String or somethings
        newResultValue = resultValue;
      }
    } catch (Exception e) {
      LogUtil.log.e("Error during response parse", e);
    }

    return newResultValue;
  }

  public static <T> void rpcFunctionInBackground(String name, Object params,
      final FunctionCallback<T> callback) {
    rpcFunctionInBackground(name, params, false, callback);
  }

  public static <T> T rpcFunction(String name, Object params) throws AVException {
    final AtomicReference<T> reference = new AtomicReference<T>();
    rpcFunctionInBackground(name, params, true, new FunctionCallback<T>() {
      @Override
      public void done(T object, AVException e) {
        if (e == null) {
          reference.set(object);
        } else {
          AVExceptionHolder.add(e);
        }
      }

      @Override
      protected boolean mustRunOnUIThread() {
        return false;
      }
    });
    if (AVExceptionHolder.exists()) throw AVExceptionHolder.remove();
    return reference.get();
  }

  private static Map<String, String> getProductionHeader() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-LC-Prod", isProduction ? "1" : "0");
    return headers;
  }

  private static <T> void rpcFunctionInBackground(String name, Object params, final boolean sync,
      final FunctionCallback<T> callback) {
    String paramString = AVUtils.restfulCloudData(params);
    PaasClient.cloudInstance().postObject("call/" + name, paramString, getProductionHeader(),
        sync, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            callback.internalDone((T) convertCloudResponse(content), e);
          }

          @Override
          public void onFailure(Throwable error, String content) {
            LogUtil.log.d(content + error);
            callback.internalDone(null, AVErrorUtils.createException(error, content));
          }
        });
  }
}
