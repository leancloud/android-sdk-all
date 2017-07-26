package com.avos.avoscloud;

import java.io.File;

/**
 * User: summer Date: 13-4-11 Time: PM2:38
 */
public class AVCacheManager {
  private static AVCacheManager instance = null;

  private static File keyValueCacheDir() {
    File dir = new File(AVPersistenceUtils.getCacheDir(), "PaasKeyValueCache");
    dir.mkdirs();
    return dir;
  }

  private static File getCacheFile(String fileName) {
    return new File(keyValueCacheDir(), fileName);
  }

  private AVCacheManager() {
  }

  public static synchronized AVCacheManager sharedInstance() {
    if (instance == null) {
      instance = new AVCacheManager();
    }
    return instance;
  }

  public String fileCacheKey(final String key, String ts) {
    if (!AVUtils.isBlankString(ts)) {
      return AVUtils.md5(key + ts);
    }
    return AVUtils.md5(key);
  }

  public boolean hasCache(String key) {
    return hasCache(key, null);
  }

  public boolean hasCache(String key, String ts) {
    File file = getCacheFile(key, ts);
    return file.exists();
  }

  /**
   * 是否有有效的 cache，本地有相应缓存并且没有超过缓存限制及认为有效
   *
   * @param key
   * @param ts
   * @param maxAgeInMilliseconds
   * @return
   */
  public boolean hasValidCache(String key, String ts, long maxAgeInMilliseconds) {
    File file = getCacheFile(key, ts);
    return file.exists() && (maxAgeInMilliseconds <= 0 || (System.currentTimeMillis() - file.lastModified() < maxAgeInMilliseconds));
  }

  private File getCacheFile(String key, String ts) {
    File file = getCacheFile(fileCacheKey(key, ts));
    return file;
  }

  public void get(String key, long maxAgeInMilliseconds, String ts,
                  GenericObjectCallback getCallback) {
    File file = getCacheFile(key, ts);
    if (!file.exists()
        || ((maxAgeInMilliseconds > 0) && (System.currentTimeMillis() - file.lastModified() > maxAgeInMilliseconds))) {
      getCallback
          .onFailure(AVErrorUtils.createException(AVException.CACHE_MISS,
              AVException.cacheMissingErrorString), null);
    } else {
      String content = AVPersistenceUtils.readContentFromFile(file);
      getCallback.onSuccess(content, null);
    }
  }

  public void delete(String key) {
    File file = getCacheFile(AVUtils.md5(key));
    String absolutePath = file.getAbsolutePath();
    if (file.exists()) {
      if (!file.delete()) {
        // If we can't delete file,write empty content to the file.
        AVPersistenceUtils.saveContentToFile("{}", file);
      } else {
        AVPersistenceUtils.removeLock(absolutePath);
      }
    }
  }

  public boolean save(String key, String content, String lastModifyTs) {
    File cacheFile = getCacheFile(key, lastModifyTs);
    return AVPersistenceUtils.saveContentToFile(content, cacheFile);
  }

  public void remove(String key, String ts) {
    File cacheFile = getCacheFile(key, ts);
    String absolutePath = cacheFile.getAbsolutePath();
    if (cacheFile.exists()) {
      if (!cacheFile.delete()) {
        // If we can't delete file,write empty content to the file.
        AVPersistenceUtils.saveContentToFile("{}", cacheFile);
      } else {
        AVPersistenceUtils.removeLock(absolutePath);
      }
    }
  }

  public boolean haveCache(String key) {
    return getCacheFile(AVUtils.md5(key)).exists();
  }

  public static boolean clearAllCache() {
    return clearCacheMoreThanDays(-1);
  }

  public static boolean clearCacheMoreThanOneDay() {
    return clearCacheMoreThanDays(1);
  }

  public static boolean clearCacheMoreThanDays(int numberOfDays) {
    File keyValueCacheDir = keyValueCacheDir();
    if (keyValueCacheDir != null && keyValueCacheDir.listFiles() != null) {
      for (File file : keyValueCacheDir.listFiles()) {
        if (System.currentTimeMillis() - file.lastModified() > numberOfDays * 24L * 3600L * 1000L) {
          if (file.exists()) {
            String path = file.getAbsolutePath();
            if (!file.delete()) {
              return false;
            } else {
              AVPersistenceUtils.removeLock(path);
            }
          }
        }
      }
    } else {
      LogUtil.avlog.d("Cache Directory Failure");
    }
    return true;
  }

  public static boolean clearFileCacheMoreThanDays(int numberOfDays) {
    if (AVOSCloud.applicationContext == null) {
      LogUtil.log.e("applicationContext is null, Please call AVOSCloud.initialize first");
      return false;
    }
    File keyValueCacheDir = AVOSCloud.applicationContext.getFilesDir();
    if (keyValueCacheDir != null && keyValueCacheDir.listFiles() != null) {
      for (File file : keyValueCacheDir.listFiles()) {
        if (System.currentTimeMillis() - file.lastModified() > numberOfDays * 24L * 3600L * 1000L) {
          if (file.exists() && file.isFile()) {
            String path = file.getAbsolutePath();
            if (!file.delete()) {
              return false;
            } else {
              AVPersistenceUtils.removeLock(path);
            }
          }
        }
      }
    } else {
      LogUtil.avlog.d("File Cache Directory Failure");
    }
    return true;
  }
}
