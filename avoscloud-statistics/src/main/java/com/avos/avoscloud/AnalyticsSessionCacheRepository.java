package com.avos.avoscloud;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created by lbt05 on 2/11/15.
 */
class AnalyticsSessionCacheRepository {
  private static final int CACHE_REQUEST = 1;
  private static final String SESSION_KEY = "session.key";
  private static final String SESSION_CACHE_FILENAME = "avoscloud-analysis";

  Handler sessionCacheHandler;
  HandlerThread handlerThread;

  private AnalyticsSessionCacheRepository() {
    handlerThread = new HandlerThread("com.avos.avoscloud.AnalyticsCacheHandlerThread");
    handlerThread.start();
    sessionCacheHandler = new Handler(handlerThread.getLooper()) {

      @Override
      public void handleMessage(Message message) {
        Bundle bundle = message.getData();
        String sessionId = bundle.getString(SESSION_KEY);
        try {
          if (!AVUtils.isBlankString(sessionId) && message.what == CACHE_REQUEST) {
            byte[] sessionData =
                message.obj == null ? null : marshall((Parcelable) message.obj);
            File cacheFile = getSessionCacheFile();
            if (sessionData != null && sessionData.length > 0) {
              // store data
              AVPersistenceUtils.saveContentToFile(sessionData, cacheFile);
            } else {
              // clean data
              cacheFile.delete();
            }
          }
        } catch (Exception e) {

        }
      }
    };
  }

  static AnalyticsSessionCacheRepository instance = null;

  public static AnalyticsSessionCacheRepository getInstance() {
    if (instance == null) {
      instance = new AnalyticsSessionCacheRepository();
    }
    return instance;
  }

  void cacheSession(AnalyticsSession session) {
    sessionCacheHandler.sendMessage(getCacheRequestMessage(CACHE_REQUEST, session.getSessionId(),
        session));
  }

  AnalyticsSession getCachedSession() {
    byte[] data = AVPersistenceUtils.readContentBytesFromFile(getSessionCacheFile());
    if (data != null && data.length > 0) {
      AnalyticsSession lastSession = new AnalyticsSession(unMarshall(data));
      lastSession.endSession();
      return lastSession;
    } else {
      return null;
    }
  }

  static Message getCacheRequestMessage(int code, String sessionId, AnalyticsSession data) {
    Message message = new Message();
    message.what = code;
    Bundle bundle = new Bundle();
    bundle.putString(SESSION_KEY, sessionId);
    if (data != null) {
      message.obj = data;
    }
    message.setData(bundle);

    return message;
  }

  private static byte[] marshall(Parcelable parcelable) {
    Parcel outer = Parcel.obtain();
    parcelable.writeToParcel(outer, 0);
    byte[] data = outer.marshall();
    return data;
  }

  private static Parcel unMarshall(byte[] data) {
    Parcel parcel = Parcel.obtain();
    parcel.unmarshall(data, 0, data.length);
    parcel.setDataPosition(0); // this is extremely important!
    return parcel;
  }

  private static File getSessionCacheFile() {
    return new File(AVPersistenceUtils.getAnalysisCacheDir(), SESSION_CACHE_FILENAME);
  }
}
