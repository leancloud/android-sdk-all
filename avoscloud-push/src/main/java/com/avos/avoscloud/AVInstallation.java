package com.avos.avoscloud;

import android.content.Context;
import android.os.Parcel;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;


/**
 *
 */
@AVClassName("_Installation")
public final class AVInstallation extends AVObject {
  private static final String LOGTAG = AVInstallation.class.getName();
  private static final String INSTALLATION = "installation";
  private static final String DEVICETYPETAG = "deviceType";
  private static final String CHANNELSTAG = "channel";
  private static final String INSTALLATIONIDTAG = "installationId";
  private static final String INSTALLATION_AVNAME = "_Installation";
  public static final String REGISTRATION_ID = "registrationId";
  public static final String VENDOR = "vendor";
  private static volatile AVInstallation currentInstallation;
  private volatile String installationId = null;

  public static final String AVINSTALLATION_ENDPOINT = "installations";

  static {
    AVPowerfulUtils.createSettings(AVInstallation.class.getSimpleName(), AVINSTALLATION_ENDPOINT,
      "_Installation");
    AVPowerfulUtils.createSettings("_Installation", AVINSTALLATION_ENDPOINT, "_Installation");
    AVObject.registerSubclass(AVInstallation.class);
  }


  // setter for fastjson.
  void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  /**
   * FeedbackThread.java 会反射调用此函数
   * @return
   */
  public static AVInstallation getCurrentInstallation() {
    return getCurrentInstallation(null);
  }

  public static AVInstallation getCurrentInstallation(Context ctx) {
    Context usingCtx = (null == ctx)? AVOSCloud.applicationContext : ctx;

    if (currentInstallation == null) {
      synchronized (AVInstallation.class) {
        if (currentInstallation == null && readInstallationFile(usingCtx) == null) {
          createNewInstallation(usingCtx);
        }
      }
    }
    if (currentInstallation != null) {
      currentInstallation.initialize();
    }
    return currentInstallation;
  }

  private static void createNewInstallation(Context ctx) {
    String id = genInstallationId();
    currentInstallation = new AVInstallation();
    currentInstallation.setInstallationId(id);
    currentInstallation.put(INSTALLATIONIDTAG, id);
    saveCurrentInstalationToLocal(ctx);
  }

  /**
   * 获取设备唯一 id，每次重新安装都会生成新的 installationId
   *
   * @return
   */
  private static String genInstallationId() {
    // app的包名
    String packageName = AVOSCloud.applicationContext.getPackageName();
    String additionalStr = UUID.randomUUID().toString();
    return AVUtils.md5(packageName + additionalStr);
  }

  private static void saveCurrentInstalationToLocal(Context ctx) {
    try {
      writeInstallationFile(ctx, currentInstallation);
    } catch (Exception e) {
      LogUtil.log.e(LOGTAG, e);
    }
  }

  public AVInstallation() {
    super(INSTALLATION_AVNAME);
    requestStatistic = false;
    initialize();
  }

  public AVInstallation(Parcel in) {
    super(in);
  }

  private void initialize() {
    try {
      if (!AVUtils.isBlankString(getInstallationId())) {
        put(INSTALLATIONIDTAG, getInstallationId(), false);
      }
      if (currentInstallation != null) {
        put(INSTALLATIONIDTAG, currentInstallation.getInstallationId(), false);
      }
      this.put(DEVICETYPETAG, deviceType(), false);
      this.put("timeZone", timezone(), false);
    } catch (IllegalArgumentException exception) {
      // TODO: need to find out why this exception happen
      exception.printStackTrace();
    }
  }

  private static String timezone() {
    TimeZone defaultTimezone = TimeZone.getDefault();
    return defaultTimezone != null ? defaultTimezone.getID() : "unknown";
  }

  // Zeng: finally, we decide to use multi service. The reasons are
  // 1. easy to develop and maintain.
  // 2. use unique id for different apps with avoscloud backend.
  // problems with single service are
  // 1. has to use unique mac address as the installation id.
  // 2. has to communicate with the single service. The service has to
  // serialize package name and class name.
  // 3. The single service has to maintain receiver list.

  /**
   * Returns the unique ID of this installation.
   *
   * @return A UUID that represents this device.
   */
  public String getInstallationId() {
    return installationId;
  }


  @Override
  protected void onSaveSuccess() {
    super.onSaveSuccess();
    try {
      writeInstallationFile(AVOSCloud.applicationContext, this);
    } catch (Exception e) {
      LogUtil.log.e(LOGTAG, e);
    }
  }

  @Override
  protected void onDataSynchronized() {
    super.onDataSynchronized();
    this.onSaveSuccess();
  }

  /*
   * 如果保存发生了异常或者失败，内存中的AVInstallation进行一次回滚
   */
  @Override
  protected void onSaveFailure() {
    LogUtil.avlog.d("roll back installationId since error there");
    synchronized (AVInstallation.class) {
      if (readInstallationFile(AVOSCloud.applicationContext) == null) {
        createNewInstallation(AVOSCloud.applicationContext);
      }
    }
  }

  protected static AVInstallation readInstallationFile(Context usingCtx) {
    if (null == usingCtx) {
      LogUtil.log.e(LOGTAG,"Please call AVOSCloud.initialize at first in Application");
      return null;
    }
    String json = "";
    try {
      File installationFile = new File(usingCtx.getFilesDir(), INSTALLATION);

      if (installationFile.exists()) {
        json = AVPersistenceUtils.readContentFromFile(installationFile);
        if (json.indexOf("{") >= 0) {
          currentInstallation = (AVInstallation) JSON.parse(json);
        } else {
          if (json.length() == UUID_LEN) {
            // old sdk verson.
            currentInstallation = new AVInstallation();
            currentInstallation.setInstallationId(json);
            // update it
            saveCurrentInstalationToLocal(usingCtx);
          }
        }
        return currentInstallation;
      }
    } catch (Exception e) {
      // try to instance a new installation
      LogUtil.log.e(LOGTAG, json, e);
    }
    return null;
  }

  private static void writeInstallationFile(Context ctx, AVInstallation installation) throws IOException {
    if (installation != null) {
      installation.initialize();
      File installationFile = new File(ctx.getFilesDir(), INSTALLATION);
      String jsonString =
        JSON.toJSONString(installation, ObjectValueFilter.instance,
          SerializerFeature.WriteClassName,
          SerializerFeature.DisableCircularReferenceDetect);

      AVPersistenceUtils.saveContentToFile(jsonString, installationFile);
    }
  }

  public static AVQuery<AVInstallation> getQuery() {
    AVQuery<AVInstallation> query = new AVQuery<AVInstallation>(INSTALLATION_AVNAME);
    return query;
  }

  /**
   * Add a key-value pair to this object. It is recommended to name keys in
   * partialCamelCaseLikeThis.
   *
   * @param key   Keys must be alphanumerical plus underscore, and start with a letter.
   * @param value Values may be numerical, String, JSONObject, JSONArray, JSONObject.NULL, or other
   *              AVObjects. value may not be null.
   */
  @Override
  public void put(String key, Object value) {
    super.put(key, value);
  }

  /**
   * Removes a key from this object's data if it exists.
   *
   * @param key The key to remove.
   */
  @Override
  public void remove(String key) {
    super.remove(key);
  }

  static private String deviceType() {
    return "android";
  }

  @Override
  protected boolean alwaysUsePost() {
    return true;
  }

  protected static void updateCurrentInstallation() {
    try {

      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("try to update installation to fix date type data");
      }
      AVInstallation currentInstallation = AVInstallation.readInstallationFile(AVOSCloud.applicationContext);
      if (currentInstallation != null && !AVUtils.isBlankString(currentInstallation.getObjectId())) {
        currentInstallation.fetchInBackground(new GetCallback<AVObject>() {

          @Override
          public void done(AVObject object, AVException e) {
            AVInstallation updatedInstallation = (AVInstallation) object;
            try {
              AVInstallation.writeInstallationFile(AVOSCloud.applicationContext, updatedInstallation);
            } catch (IOException e1) {
              if (AVOSCloud.showInternalDebugLog()) {
                e1.printStackTrace();
              }
            }
          }
        });
      }
    } catch (Exception e) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.log.e("failed to update installation", e);
      }
    }
  }

  protected boolean isDirty() {
    return (AVUtils.isBlankString(objectId) || !this.operationQueue.isEmpty()
      || this.getUpdatedAt() == null || (System.currentTimeMillis() - this.getUpdatedAt()
      .getTime()) > 86400000);
  }

  @Override
  protected void rebuildInstanceData() {
    super.rebuildInstanceData();
    this.installationId = this.getString("installationId");
  }


  @Override
  protected PaasClient getPaasClientInstance() {
    return PaasClient.pushInstance();
  }

  public static transient final Creator CREATOR = AVObjectCreator.instance;
}
