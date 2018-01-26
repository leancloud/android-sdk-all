package com.avos.avoscloud;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PaasClient {
  private String baseUrl;
  private final String apiVersion;

  private static String applicationIdField;

  private static String apiKeyField;

  static String sessionTokenField;

  private static final String DEFAULT_ENCODING = "UTF-8";
  static final String DEFAULT_CONTENT_TYPE = "application/json";
  public static final String DEFAULT_FAIL_STRING = "request failed!!!";

  public static final String sdkVersion = "v4.6.2";

  private static final String userAgent = "AVOS Cloud android-" + sdkVersion + " SDK";
  private AVUser currentUser = null;
  private AVACL defaultACL;

  private static boolean lastModifyEnabled = false;
  static String REQUEST_STATIS_HEADER = "X-Android-RS";

  private static HashMap<String, PaasClient> serviceClientMap = new HashMap<String, PaasClient>();
  private static Map<String, AVObjectReferenceCount> internalObjectsForEventuallySave = Collections
      .synchronizedMap(new HashMap<String, AVObjectReferenceCount>());

  private static Map<String, String> lastModify = Collections
      .synchronizedMap(new WeakHashMap<String, String>());

  /**
   * 针对不同类型的 AVOSServices（存储、统计、云函数）返回不同的 PaasClient 实例
   * @return
   */
  protected static PaasClient sharedInstance(String host) {
    PaasClient instance = serviceClientMap.get(host);
    if (instance == null) {
      instance = new PaasClient(host);
      serviceClientMap.put(host, instance);
    }
    return instance;
  }

  public static PaasClient storageInstance() {
    return sharedInstance(AppRouterManager.getInstance().getStorageServer());
  }

  public static PaasClient pushInstance() {
    return sharedInstance(AppRouterManager.getInstance().getPushServer());
  }

  public static PaasClient cloudInstance() {
    return sharedInstance(AppRouterManager.getInstance().getEngineServer());
  }

  public static PaasClient statistisInstance() {
    return sharedInstance(AppRouterManager.getInstance().getStatsServer());
  }

  AVACL getDefaultACL() {
    return defaultACL;
  }

  void setDefaultACL(AVACL acl) {
    defaultACL = acl;
  }

  AVUser getCurrentUser() {
    return currentUser;
  }

  public Map<String, String> userHeaderMap() {
    AVUser user = AVUser.getCurrentUser();
    if (user != null) {
      return user.headerMap();
    }
    return null;
  }

  void setCurrentUser(AVUser user) {
    currentUser = user;
  }


  private PaasClient(String url) {
    apiVersion = "1.1";
    baseUrl = url;
    useUruluServer();
  }

  private String signRequest() {
    StringBuilder builder = new StringBuilder();
    long ts = AVUtils.getCurrentTimestamp();
    StringBuilder result = new StringBuilder();
    result.append(AVUtils.md5(builder.append(ts).append(AVOSCloud.clientKey).toString())
        .toLowerCase());
    return result.append(',').append(ts).toString();
  }

  protected void updateHeaders(Request.Builder builder, Map<String, String> header,
                               boolean needRequestStatistic) {
    // if the field isnt exist, the server will assume it's true
    builder.header("X-LC-Prod", AVCloud.isProductionMode() ? "1" : "0");

    AVUser currAVUser = AVUser.getCurrentUser();
    builder.header(sessionTokenField,
        (currAVUser != null && currAVUser.getSessionToken() != null)
            ? currAVUser.getSessionToken()
            : "");
    builder.header(applicationIdField, AVOSCloud.applicationId);
    builder.header("Accept", DEFAULT_CONTENT_TYPE);
    builder.header("Content-Type", DEFAULT_CONTENT_TYPE);
    builder.header("User-Agent", userAgent);
    builder.header("X-LC-Sign", signRequest());


    if (header != null) {
      for (Map.Entry<String, String> entry : header.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }

    if (needRequestStatistic) {
      builder.header(REQUEST_STATIS_HEADER, "1");
    }
  }

  public void useUruluServer() {
    if (AVOSCloud.isCN()) {
      useAVCloudCN();
    } else {
      useAVCloudUS();
    }
  }

  public static void useAVCloudUS() {
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
  }

  public static void useAVCloudCN() {
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
  }

  public static void useLocalStg() {
    applicationIdField = "X-LC-Id";
    apiKeyField = "X-LC-Key";
    sessionTokenField = "X-LC-Session";
  }

  public String buildUrl(final String path) {
    return String.format("%s/%s/%s", baseUrl, apiVersion, path);
  }

  public String buildUrl(final String path, AVRequestParams params) {
    String endPoint = buildUrl(path);
    if (params == null || params.isEmpty()) {
      return endPoint;
    } else {
      return params.getWholeUrl(endPoint);
    }

  }

  private String batchUrl() {
    return String.format("%s/%s/batch", baseUrl, apiVersion);
  }

  private String batchSaveRelativeUrl() {
    return "batch/save";
  }

  private AsyncHttpResponseHandler createGetHandler(GenericObjectCallback callback,
                                                    AVQuery.CachePolicy policy, String absoluteURLString) {
    AsyncHttpResponseHandler handler =
        new GetHttpResponseHandler(callback, policy, absoluteURLString);
    return handler;
  }

  private AsyncHttpResponseHandler createPostHandler(GenericObjectCallback callback) {
    AsyncHttpResponseHandler handler = new PostHttpResponseHandler(callback);
    return handler;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  private void setBaseUrl(final String url) {
    baseUrl = url;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getObject(final String relativePath, final AVRequestParams parameters,
                          final boolean sync, final Map<String, String> header, final GenericObjectCallback callback,
                          final AVQuery.CachePolicy policy, final long maxAgeInMilliseconds) {
    final String absoluteURLString = buildUrl(relativePath, parameters);


    final String absolutURLString = generateQueryPath(relativePath, parameters);
    final String lastModifyTime = getLastModify(absolutURLString);

    switch (policy) {
      default:
      case IGNORE_CACHE:
        getObject(relativePath, parameters, sync, header, callback, policy);
        break;
      case CACHE_ONLY:
        AVCacheManager.sharedInstance().get(absolutURLString, maxAgeInMilliseconds, lastModifyTime,
            callback);
        break;
      case NETWORK_ONLY:
        getObject(relativePath, parameters, sync, header, callback, policy);
        break;
      case CACHE_ELSE_NETWORK:
        AVCacheManager.sharedInstance().get(absolutURLString, maxAgeInMilliseconds, lastModifyTime,
            new GenericObjectCallback() {
              @Override
              public void onSuccess(String content, AVException e) {
                callback.onSuccess(content, e);
              }

              @Override
              public void onFailure(Throwable error, String content) {
                getObject(relativePath, parameters, sync, header, callback, policy);
              }
            });
        break;
      case NETWORK_ELSE_CACHE:
        getObject(relativePath, parameters, sync, header, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            callback.onSuccess(content, e);
          }

          @Override
          public void onFailure(Throwable error, String content) {
            AVCacheManager cacheManager = AVCacheManager.sharedInstance();
            if (cacheManager.hasValidCache(absolutURLString, lastModifyTime, maxAgeInMilliseconds)) {
              cacheManager.get(absolutURLString, maxAgeInMilliseconds, lastModifyTime, callback);
            } else {
              callback.onFailure(error, content);
            }
          }
        }, policy);
        break;
      case CACHE_THEN_NETWORK:
        AVCacheManager.sharedInstance().get(absolutURLString, maxAgeInMilliseconds, lastModifyTime,
            new GenericObjectCallback() {
              @Override
              public void onSuccess(String content, AVException e) {
                callback.onSuccess(content, e);
                getObject(relativePath, parameters, sync, header, callback, policy);
              }

              @Override
              public void onFailure(Throwable error, String content) {
                callback.onFailure(error, content);
                getObject(relativePath, parameters, sync, header, callback, policy);
              }
            });
        break;
    }
    return absoluteURLString;
  }

  String generateQueryPath(final String relativePath, final AVRequestParams parameters) {
    return buildUrl(relativePath, parameters);
  }

  public void getObject(final String relativePath, final AVRequestParams parameters,
                        final boolean sync,
                        final Map<String, String> inputHeader, GenericObjectCallback callback,
                        final AVQuery.CachePolicy policy) {
    Map<String, String> myHeader = inputHeader;
    if (inputHeader == null) {
      myHeader = new HashMap<String, String>();
    }
    updateHeaderForPath(relativePath, parameters, myHeader);

    String url = buildUrl(relativePath, parameters);
    AsyncHttpResponseHandler handler =
        createGetHandler(callback, policy, url);
    if (AVOSCloud.isDebugLogEnabled()) {
      dumpHttpGetRequest(buildUrl(relativePath),
          parameters == null ? null : parameters.getDumpQueryString(), myHeader);
    }
    AVHttpClient client = AVHttpClient.clientInstance();
    Request.Builder builder = new Request.Builder();
    builder.url(url).get();
    updateHeaders(builder, myHeader, callback != null && callback.isRequestStatisticNeed());
    client.execute(builder.build(), sync, handler);
  }

  public void getObject(final String relativePath, AVRequestParams parameters, boolean sync,
                        Map<String, String> header, GenericObjectCallback callback) {
    getObject(relativePath, parameters, sync, header, callback, AVQuery.CachePolicy.IGNORE_CACHE);
  }

  public void putObject(final String relativePath, String object, boolean sync,
                        Map<String, String> header, GenericObjectCallback callback, String objectId,
                        String _internalId) {
    putObject(relativePath, object, sync, false, header, callback, objectId, _internalId);
  }


  public void putObject(final String relativePath, String object, boolean sync,
                        boolean isEventually, Map<String, String> header, GenericObjectCallback callback,
                        String objectId, String _internalId) {
    try {
      if (isEventually) {
        File archivedFile = archiveRequest("put", relativePath, object, objectId, _internalId);
        handleArchivedRequest(archivedFile, sync, callback);
      } else {
        String url = buildUrl(relativePath);
        AsyncHttpResponseHandler handler = createPostHandler(callback);
        if (AVOSCloud.isDebugLogEnabled()) {
          dumpHttpPutRequest(header, url, object);
        }
        AVHttpClient client = AVHttpClient.clientInstance();
        Request.Builder builder = new Request.Builder();
        builder.url(url).put(RequestBody.create(AVHttpClient.JSON, object));
        updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());
        client.execute(builder.build(), sync, handler);
      }
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  private void processException(Exception e, GenericObjectCallback cb) {
    if (cb != null) {
      cb.onFailure(e, null);
    }
  }

  // path=/1/classes/Parent/a1QCssTp7r
  Map<String, Object> batchItemMap(String method, String path, Object body, Map params) {
    // String myPath = String.format("/%s/%s",
    // PaasClient.sharedInstance().apiVersion, path);
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("method", method);
    result.put("path", path);
    result.put("body", body);
    if (params != null) {
      result.put("params", params);
    }
    return result;
  }

  Map<String, Object> batchItemMap(String method, String path, Object body) {
    return this.batchItemMap(method, path, body, null);
  }

  @Deprecated
  List<Object> assembleBatchOpsList(List<Object> itemList, String path) {
    List<Object> list = new ArrayList<Object>();
    for (Object object : itemList) {
      Map<String, Object> opDict = batchItemMap("PUT", path, object);
      list.add(opDict);
    }
    return list;
  }

  private Map<String, Object> batchRequest(List<Object> list) {
    Map<String, Object> requests = new HashMap<String, Object>();
    requests.put("requests", list);
    return requests;
  }

  // only called @sendPendingOps
  public void postBatchObject(List<Object> parameters, boolean sync, Map<String, String> header,
                              GenericObjectCallback callback) {
    try {
      String url = batchUrl();
      Map<String, Object> requests = batchRequest(parameters);
      String json = JSON.toJSONString(requests);
      ByteArrayEntity entity = new ByteArrayEntity(json.getBytes(DEFAULT_ENCODING));
      if (AVOSCloud.isDebugLogEnabled()) {
        dumpHttpPostRequest(header, url, json);
      }
      AsyncHttpResponseHandler handler = createPostHandler(callback);
      AVHttpClient client = AVHttpClient.clientInstance();
      Request.Builder builder = new Request.Builder();
      builder.url(url).post(RequestBody.create(AVHttpClient.JSON, json));
      updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());

      client.execute(builder.build(), sync, handler);
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }


  public void postBatchSave(final List list, final boolean sync, final boolean isEventually,
                            final Map<String, String> header, final GenericObjectCallback callback,
                            final String objectId,
                            final String _internalId) {
    try {
      Map params = new HashMap();
      params.put("requests", list);
      String paramString = AVUtils.jsonStringFromMapWithNull(params);
      if (isEventually) {
        File archivedFile =
            archiveRequest("post", batchSaveRelativeUrl(), paramString, objectId, _internalId);
        handleArchivedRequest(archivedFile, sync, callback);
      } else {
        String url = buildUrl(batchSaveRelativeUrl());
        if (AVOSCloud.isDebugLogEnabled()) {
          dumpHttpPostRequest(header, url, paramString);
        }
        AsyncHttpResponseHandler handler = createPostHandler(callback);
        AVHttpClient client = AVHttpClient.clientInstance();
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(RequestBody.create(AVHttpClient.JSON, paramString));
        updateHeaders(builder, header, callback != null && callback.isRequestStatisticNeed());
        client.execute(builder.build(), sync, handler);
      }
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  public void postObject(final String relativePath, String object, boolean sync,
                         GenericObjectCallback callback) {
    postObject(relativePath, object, sync, false, callback, null, null);
  }

  public void postObject(final String relativePath, String object, final Map<String, String> headers,
                         boolean sync, GenericObjectCallback callback) {
    postObject(relativePath, object, headers, sync, false, callback, null, null);
  }

  public void postObject(final String relativePath, String object, boolean sync,
                         boolean isEventually, GenericObjectCallback callback, String objectId, String _internalId) {
    postObject(relativePath, object, null, sync, isEventually, callback, objectId, _internalId);
  }

  public void postObject(final String relativePath, String object, final Map<String, String> headers,
                         boolean sync, boolean isEventually,
                         GenericObjectCallback callback, String objectId, String _internalId) {
    try {
      if (isEventually) {
        File archivedFile = archiveRequest("post", relativePath, object, objectId, _internalId);
        handleArchivedRequest(archivedFile, sync, callback);
      } else {
        String url = buildUrl(relativePath);
        if (AVOSCloud.isDebugLogEnabled()) {
          dumpHttpPostRequest(null, url, object);
        }
        AsyncHttpResponseHandler handler = createPostHandler(callback);
        AVHttpClient client = AVHttpClient.clientInstance();
        Request.Builder builder = new Request.Builder();
        updateHeaders(builder, headers, callback != null && callback.isRequestStatisticNeed());
        builder.url(url).post(RequestBody.create(AVHttpClient.JSON, object));
        client.execute(builder.build(), sync, handler);
      }
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }

  public void deleteObject(final String relativePath, boolean sync, GenericObjectCallback callback,
                           String objectId, String _internalId) {
    deleteObject(relativePath, sync, false, callback, objectId, _internalId);
  }

  public void deleteObject(final String relativePath, boolean sync, boolean isEventually,
                           GenericObjectCallback callback, String objectId, String _internalId) {
    try {
      if (isEventually) {
        File archivedFile = archiveRequest("delete", relativePath, null, objectId, _internalId);
        handleArchivedRequest(archivedFile, sync, callback);
      } else {
        String url = buildUrl(relativePath);
        if (AVOSCloud.isDebugLogEnabled()) {
          dumpHttpDeleteRequest(null, url, null);
        }
        AsyncHttpResponseHandler handler = createPostHandler(callback);
        AVHttpClient client = AVHttpClient.clientInstance();
        Request.Builder builder = new Request.Builder();
        updateHeaders(builder, null, callback != null && callback.isRequestStatisticNeed());
        builder.url(url).delete();

        client.execute(builder.build(), sync, handler);
      }
    } catch (Exception exception) {
      processException(exception, callback);
    }
  }


  // ================================================================================
  // Archive and handle request
  // ================================================================================

  /*
   * type for archive: 1. post 2. delete
   */
  private File archiveRequest(String method, String relativePath, String paramString,
                              String objectId, String _internalId) {
    File theArchivedFile =
        new File(AVPersistenceUtils.getCommandCacheDir(), AVUtils.getArchiveRequestFileName(
            objectId, _internalId, method, relativePath, paramString));

    Map<String, String> fileMap = new HashMap<String, String>(3);
    fileMap.put("method", method);
    fileMap.put("relativePath", relativePath);
    fileMap.put("paramString", paramString);
    fileMap.put("objectId", objectId);
    fileMap.put("_internalId", _internalId);

    AVPersistenceUtils.saveContentToFile(AVUtils.toJSON(fileMap), theArchivedFile);

    if (AVOSCloud.showInternalDebugLog()) {
      LogUtil.log.d(AVUtils.restfulServerData(fileMap) + "\n" + "did save to "
          + theArchivedFile.getAbsolutePath());
    }
    return theArchivedFile;
  }

  private void handleArchivedRequest(File archivedFile, boolean sync) {
    handleArchivedRequest(archivedFile, sync, null);
  }

  private void handleArchivedRequest(final File archivedFile, boolean sync,
                                     final GenericObjectCallback callback) {
    try {
      String archivedFileContent = AVPersistenceUtils.readContentFromFile(archivedFile);
      Map<String, String> fileMap = null;

      fileMap = AVUtils.getFromJSON(archivedFileContent, Map.class);
      if (fileMap != null && !fileMap.isEmpty()) {
        String method = fileMap.get("method");
        String relativePath = fileMap.get("relativePath");
        String paramString = fileMap.get("paramString");
        String objectId = fileMap.get("objectId");
        String _internalId = fileMap.get("_internalId");
        GenericObjectCallback newCallback = new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (callback != null) {
              callback.onSuccess(content, e);
            }
            try {
              Map<String, String> objectMap = AVUtils.getFromJSON(content, Map.class);
              for (String _internalId : objectMap.keySet()) {
                if (internalObjectsForEventuallySave.get(_internalId) != null) {
                  internalObjectsForEventuallySave.get(_internalId).getValue()
                      .copyFromMap(objectMap);
                  unregisterEvtuallyObject(internalObjectsForEventuallySave.get(_internalId)
                      .getValue());
                }
              }
            } catch (Exception e1) {
              LogUtil.avlog.e("parse exception during archive request" + e.getMessage());
            }
            archivedFile.delete();
            AVPersistenceUtils.removeLock(archivedFile.getAbsolutePath());
          }

          @Override
          public void onFailure(Throwable error, String content) {
            // handle retry
            if (callback != null) callback.onFailure(error, content);
            AVPersistenceUtils.removeLock(archivedFile.getAbsolutePath());
          }
        };
        if (method == null) {
          newCallback.onFailure(new AVRuntimeException("Null method."), null);
        }
        if ("post".equalsIgnoreCase(method)) {
          postObject(relativePath, paramString, sync, newCallback);
        } else if ("put".equalsIgnoreCase(method)) {
          putObject(relativePath, paramString, sync, null, newCallback, objectId, _internalId);
        } else if ("delete".equalsIgnoreCase(method)) {
          deleteObject(relativePath, sync, newCallback, objectId, _internalId);
        }
      }
    } catch (Exception e) {
      return;
    }
  }

  public void handleAllArchivedRequest() {
    handleAllArchivedRequest(false);
  }

  protected void handleAllArchivedRequest(boolean sync) {
    File commandCacheDir = AVPersistenceUtils.getCommandCacheDir();
    File[] archivedRequests = commandCacheDir.listFiles();
    if (archivedRequests != null && archivedRequests.length > 0) {
      Arrays.sort(archivedRequests, fileModifiedDateComparator);
      for (File file : archivedRequests) {
        if (file.isFile()) {
          handleArchivedRequest(file, sync);
        } else if (AVOSCloud.showInternalDebugLog()) {
          LogUtil.avlog.e(file.getAbsolutePath() + " is a dir");
        }
      }
    }
  }

  // ================================================================================
  // For Debug
  // ================================================================================

  public void dumpHttpGetRequest(String path, String parameters, Map<String, String> header) {
    String dumpString = "";
    StringBuilder additionalHeader = new StringBuilder();
    if (null != header) {
      for (Map.Entry<String, String> entry : header.entrySet()) {
        additionalHeader.append(entry.getKey() + ": " + entry.getValue());
      }
    }
    if (parameters != null) {
      dumpString =
          String.format("curl -X GET -H \"%s: %s\" -H \"%s: %s\" -H \"%s\" -G --data-urlencode \'%s\' %s",
              applicationIdField, AVOSCloud.applicationId, apiKeyField, getDebugClientKey(),
              additionalHeader.toString(), parameters, path);
    } else {
      dumpString =
          String.format("curl -X GET -H \"%s: %s\" -H \"%s: %s\" -H \"%s\" %s", applicationIdField,
              AVOSCloud.applicationId, apiKeyField, getDebugClientKey(),additionalHeader.toString(), path);
    }
    LogUtil.avlog.d(dumpString);
  }

  private String getDebugClientKey() {
    if (AVOSCloud.showInternalDebugLog()) {
      return AVOSCloud.clientKey;
    } else {
      return "YourAppKey";
    }
  }

  private String headerString(Map<String, String> header) {
    String string =
        String.format(" -H \"%s: %s\" -H \"%s: %s\" ", applicationIdField, AVOSCloud.applicationId,
            apiKeyField, getDebugClientKey());
    StringBuilder sb = new StringBuilder(string);
    if (header != null) {
      for (Map.Entry<String, String> entry : header.entrySet()) {
        String item = String.format(" -H \"%s: %s\" ", entry.getKey(), entry.getValue());
        sb.append(item);
      }
    }
    sb.append(" -H \"Content-Type: application/json\" ");
    return sb.toString();
  }

  public void dumpHttpPutRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X PUT %s  -d \' %s \' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public void dumpHttpPostRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X POST %s  -d \'%s\' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public void dumpHttpDeleteRequest(Map<String, String> header, String path, String object) {
    String string =
        String.format("curl -X DELETE %s  -d \'%s\' %s", headerString(header), object, path);
    LogUtil.avlog.d(string);
  }

  public void updateHeaderForPath(final String relativePath, AVRequestParams parameters,
                                  final Map<String, String> header) {
    // if disabled, don't add modify to header so server side will
    // return raw data instead of flag only.
    if (PaasClient.isLastModifyEnabled() && null != header && !TextUtils.isEmpty(relativePath)) {
      final String absoluteURLString = generateQueryPath(relativePath, parameters);
      final String modify = getLastModify(absoluteURLString);
      // double check local cache
      boolean exist = AVCacheManager.sharedInstance().hasCache(absoluteURLString, modify);
      if (modify != null && exist) {
        header.put("If-Modified-Since", modify);
      }
    }
  }

  public static String getLastModify(final String absolutURLString) {
    if (!PaasClient.isLastModifyEnabled()) {
      return null;
    }
    return lastModify.get(absolutURLString);
  }

  public static boolean isLastModifyEnabled() {
    return lastModifyEnabled;
  }

  public static void setLastModifyEnabled(boolean e) {
    lastModifyEnabled = e;
  }

  public static void clearLastModifyCache() {
    // also clear cache files
    Iterator it = lastModify.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pairs = (Map.Entry) it.next();
      AVCacheManager.sharedInstance().remove((String) pairs.getKey(), (String) pairs.getValue());
    }
    lastModify.clear();
  }

  static public String lastModifyFromHeaders(Header[] headers) {
    for (Header h : headers) {
      if (h.getName().equalsIgnoreCase("Last-Modified")) {
        return h.getValue();
      }
    }
    return null;
  }

  @SuppressLint("DefaultLocale")
  public static boolean isJSONResponse(String contentType) {
    if (!AVUtils.isBlankString(contentType)) {
      return contentType.toLowerCase().contains("application/json");
    }
    return false;
  }

  protected static String extractContentType(Header[] headers) {
    if (headers != null) {
      for (Header h : headers) {
        if (h.getName().equalsIgnoreCase("Content-Type")) {
          return h.getValue();
        }
      }
    }
    return null;
  }

  public static boolean updateLastModify(final String absolutURLString, final String ts) {
    if (!isLastModifyEnabled()) {
      return false;
    }

    if (!AVUtils.isBlankString(ts)) {
      lastModify.put(absolutURLString, ts);
      return true;
    }
    return false;
  }

  public static void removeLastModifyForUrl(final String absolutURLString) {
    lastModify.remove(absolutURLString);
  }

  protected static void registerEventuallyObject(AVObject object) {
    if (object != null) {
      synchronized (object) {
        AVObjectReferenceCount counter = internalObjectsForEventuallySave.get(object.internalId());
        if (counter != null) {
          counter.increment();
        } else {
          counter = new AVObjectReferenceCount(object);
          internalObjectsForEventuallySave.put(object.internalId(), counter);
        }
      }
    }
  }

  protected static void unregisterEvtuallyObject(AVObject object) {
    if (object != null) {
      synchronized (object) {
        AVObjectReferenceCount counter =
            internalObjectsForEventuallySave.get(object.internalId()) == null
                ? internalObjectsForEventuallySave.get(object.internalId())
                : internalObjectsForEventuallySave.get(object.getUuid());
        if (counter != null) {
          if (counter.desc() <= 0) {
            internalObjectsForEventuallySave.remove(object.internalId());
            internalObjectsForEventuallySave.remove(object.getUuid());
          }
        }
      }
    }
  }

  private static Comparator<File> fileModifiedDateComparator = new Comparator<File>() {
    @Override
    public int compare(File f, File s) {
      return (int) (f.lastModified() - s.lastModified());
    }
  };
}
