package com.avos.avoscloud;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.signature.Base64Decoder;
import com.avos.avoscloud.signature.Base64Encoder;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by lbt05 on 6/18/15.
 */
class NotifyUtil {
  protected static HandlerThread thread = new HandlerThread("com.avos.avoscloud.notify");
  static final int SERVICE_RESTART = 1024;
  static final String SERVICE_RESTART_ACTION = "com.avos.avoscloud.notify.action";
  static {
    thread.start();
  }

  static Handler notifyHandler = new Handler(thread.getLooper()) {
    @Override
    public void handleMessage(Message m) {
      if (m.what == SERVICE_RESTART && AVOSCloud.applicationContext != null) {
        this.removeMessages(SERVICE_RESTART);
        try {
          Set<String> registeredApps = getRegisteredApps();
          for (String encodedAppPackage : registeredApps) {
            String appPackage = Base64Decoder.decode(encodedAppPackage);
            if (!AVOSCloud.applicationContext.getPackageName().equals(appPackage)) {
              Intent intent = new Intent();
              intent.setClassName(appPackage,
                  "com.avos.avoscloud.PushService");
              intent.setAction(SERVICE_RESTART_ACTION);
              if (AVOSCloud.showInternalDebugLog()) {
                LogUtil.avlog.d("try to start:" + appPackage + " from:"
                    + AVOSCloud.applicationContext.getPackageName());
              }
              try {
                AVOSCloud.applicationContext.startService(intent);
              } catch (Exception ex) {
                LogUtil.avlog.e("failed to startService. cause: " + ex.getMessage());
              }
            }
          }
        } catch (Exception e) {

        }
        registerApp();
      }
    }
  };

  private static void registerApp() {
    Set<String> appSet = getRegisteredApps();
    if (appSet != null) {
      appSet.add(Base64Encoder.encode(AVOSCloud.applicationContext.getPackageName()));
      AVPersistenceUtils.sharedInstance().saveContentToFile(JSON.toJSONString(appSet),
          getRegisterAppsFile());
    }
  }

  private static Set<String> getRegisteredApps() {
    if (AVOSCloud.applicationContext == null) {
      return null;
    }
    File registerFile = getRegisterAppsFile();
    Set<String> appSet = new HashSet<String>();
    if (registerFile.exists()) {
      String registerApps = AVPersistenceUtils.sharedInstance().readContentFromFile(registerFile);
      if (!AVUtils.isBlankString(registerApps)) {
        // catch parse Exception
        try {
          appSet.addAll(JSON.parseObject(registerApps, Set.class));
        } catch (Exception e) {
          if (AVOSCloud.showInternalDebugLog()) {
            LogUtil.log.e("NotifyUtil", "getRegisteredApps", e);
          }
        }
        return appSet;
      }
    }
    return appSet;
  }

  private static File getRegisterAppsFile() {
    File file =
        new File(Environment.getExternalStorageDirectory() + "/Android/data/leancloud/",
            "dontpanic.cp");
    if (file.exists()) {
      return file;
    } else {
      File folder =
          new File(Environment.getExternalStorageDirectory() + "/Android/data/leancloud/");
      folder.mkdirs();
      return file;
    }
  }
}
