package com.avos.avoscloud;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.im.v2.AVIMClient;

class AVSessionCacheHelper {

  private static final String SESSION_KEY = "sessionids";
  private static SessionTagCache tagCacheInstance;

  static {
    // 删除旧的 SharedPreferences
    AVPersistenceUtils.sharedInstance().removePersistentSettingString("com.avos.avoscloud.session.version", SESSION_KEY);
    AVPersistenceUtils.sharedInstance().removePersistentSettingString("com.avos.avoscloud.session", SESSION_KEY);
    AVPersistenceUtils.sharedInstance().removePersistentSettingString("com.avos.avoscloud.session.tag", SESSION_KEY);
  }

  static synchronized SessionTagCache getTagCacheInstance() {
    if (null == tagCacheInstance) {
      tagCacheInstance = new SessionTagCache();
    }
    return tagCacheInstance;
  }

  static class SessionTagCache {

    private final String SESSION_TAG_CACHE_KEY = "session_tag_cache_key";
    Map<String, String> cachedTagMap = Collections.synchronizedMap(new HashMap<String, String>());

    private SessionTagCache() {
      syncLocalToMemory(cachedTagMap);
    }

    private synchronized void syncTagsToLocal(Map<String, String> map) {
      if (null != map) {
        AVPersistenceUtils.sharedInstance().savePersistentSettingString(SESSION_KEY,
          SESSION_TAG_CACHE_KEY, JSON.toJSONString(map));
      }
    }

    private void syncLocalToMemory(Map<String, String> map) {
      String sessionIdsString =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(SESSION_KEY,
          SESSION_TAG_CACHE_KEY, "{}");
      Map<String, String> sessionIds = JSON.parseObject(sessionIdsString, HashMap.class);
      if (sessionIds != null && !sessionIds.isEmpty()) {
        map.clear();
        map.putAll(sessionIds);
      }
    }

    void addSession(String clientId, String tag) {
      cachedTagMap.put(clientId, tag);
      if (AVIMClient.isAutoOpen()) {
        syncTagsToLocal(cachedTagMap);
      }
    }

    void removeSession(String clientId) {
      if (cachedTagMap.containsKey(clientId)) {
        cachedTagMap.remove(clientId);
        if (AVIMClient.isAutoOpen()) {
          syncTagsToLocal(cachedTagMap);
        }
      }
    }

    Map<String, String> getAllSession() {
      HashMap<String, String> sessionMap = new HashMap<>();
      sessionMap.putAll(cachedTagMap);
      return sessionMap;
    }
  }

  static class SignatureCache {

    private static final String SESSION_SIGNATURE_KEY = "com.avos.avoscloud.session.signature";

    /**
     * 缓存 Signature 到本地
     * 注：因为已经有了 imToken 去限制自动登录，所以关于 Signature 缓存的代码目前为止是无效的，
     * 所以这里并没有对 AVIMClient.isAutoOpen() 做判断。 2016-08-12
     *
     * @param clientId
     * @param signature
     */
    static void addSessionSignature(String clientId, Signature signature) {
      Map<String, Signature> signatureMap = getSessionSignatures();
      signatureMap.put(clientId, signature);
      AVPersistenceUtils.sharedInstance().savePersistentSettingString(SESSION_SIGNATURE_KEY,
        SESSION_KEY,
        JSON.toJSONString(signatureMap, SerializerFeature.WriteClassName));
    }

    static Signature getSessionSignature(String clientId) {
      Map<String, Signature> signatureMap = getSessionSignatures();
      return signatureMap.get(clientId);
    }

    private static Map<String, Signature> getSessionSignatures() {
      String sessionSignatureString =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(SESSION_SIGNATURE_KEY,
          SESSION_KEY, "{}");
      Map<String, Signature> signatureMap = JSON.parseObject(sessionSignatureString, Map.class);
      return signatureMap;
    }
  }

  static class IMSessionTokenCache {
    private static final String SESSION_TOKEN_KEY = "com.avos.avoscloud.session.token";

    /**
     * 用来缓存 sessionToken，sessionToken 用来做自动登录使用
     */
    private static Map<String, String> imSessionTokenMap = new HashMap<>();

    static String getIMSessionToken(String clientId) {
      if (AVIMClient.isAutoOpen()) {
        String token =
          AVPersistenceUtils.sharedInstance().getPersistentSettingString(SESSION_TOKEN_KEY, clientId,
            null);
        String expiredAt =
          AVPersistenceUtils.sharedInstance().getPersistentSettingString(SESSION_TOKEN_KEY,
            getSessionTokenExpiredAtKey(clientId), null);
        if (!AVUtils.isBlankString(token) && !AVUtils.isBlankString(expiredAt)) {
          try {
            long expiredAtInLong = Long.parseLong(expiredAt);
            if (expiredAtInLong > System.currentTimeMillis()) {
              return token;
            }
          } catch (Exception e) {

          }
        }
      } else {
        if (imSessionTokenMap.containsKey(clientId)) {
          return imSessionTokenMap.get(clientId);
        }
      }
      return null;
    }

    /**
     * 将 sessionToken 写入缓存
     * 如果自动登录为 true，则写入本地缓存，否则只写入内存，默认写入内存的有效期为当前 app 的生命周期内
     *
     * @param clientId
     * @param sessionToken
     * @param expireInSec
     */
    static void addIMSessionToken(String clientId, String sessionToken, int expireInSec) {
      if (AVIMClient.isAutoOpen()) {
        AVPersistenceUtils.sharedInstance().savePersistentSettingString(SESSION_TOKEN_KEY, clientId,
          sessionToken);
        AVPersistenceUtils.sharedInstance().savePersistentSettingString(SESSION_TOKEN_KEY,
          getSessionTokenExpiredAtKey(clientId),
          String.valueOf(System.currentTimeMillis() + expireInSec * 1000));
      } else {
        imSessionTokenMap.put(clientId, sessionToken);
      }
    }

    /**
     * 删除 client 对应的 sessionToken
     *
     * @param clientId
     */
    static void removeIMSessionToken(String clientId) {
      if (AVIMClient.isAutoOpen()) {
        AVPersistenceUtils.sharedInstance().removePersistentSettingString(SESSION_TOKEN_KEY, clientId);
        AVPersistenceUtils.sharedInstance().removePersistentSettingString(SESSION_TOKEN_KEY,
          getSessionTokenExpiredAtKey(clientId));
      } else {
        imSessionTokenMap.remove(clientId);
      }
    }

    private static String getSessionTokenExpiredAtKey(String clientId) {
      return clientId + ".expiredAt";
    }
  }
}
