package com.avos.sns;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.avos.avoscloud.AVPersistenceUtils;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.PaasClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.Object;
import java.lang.String;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 4/17/13 Time: 10:40 AM To change this template
 * use File | Settings | File Templates.
 */
public abstract class SNSBase {

  public static final String TAG = SNSBase.class.getSimpleName();
  public static final int REQUEST_CODE_FOR_SINAWEIBO_AUTHORIZE = 10000;
  public static final int REQUEST_CODE_FOR_QQ_AUTHORIZE = 1111;
  public String appKey;
  public String appSec;
  public String redirectUrl;
  String authorizeUrl;
  public String accessToken;
  public String userId;
  public String userName;
  public String expiresAt;
  public Context applicationContext;

  public SNSCallback callback;
  public static final String urlTag = "url";
  public static final String AUTHORIZE_RESULT = "result";

  private JSONObject authorizedData;

  public static final String encodingTag = "UTF-8";

  public abstract void logIn(Activity activity, SNSCallback callback);

  public abstract void logOut(Activity activity);

  public abstract void onActivityResult(int requestCode, int resultCode, Intent data);

  public abstract SNSType type();


  private final Map<String, Object> userInfo = new ConcurrentHashMap<String, Object>();

  static public boolean isPkgInstalled(Activity activity, String pkgName) {
    PackageInfo packageInfo = null;
    try {
      packageInfo = activity.getPackageManager().getPackageInfo(pkgName, 0);
      return packageInfo != null;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  static public String urlForType(SNSType type) {
    switch (type) {
      case AVOSCloudSNSSinaWeibo:
        return String.format("%s/%s", PaasClient.storageInstance().getBaseUrl(),
            "1/oauth2/goto/weibo?mobile_sns=true");
      case AVOSCloudSNSQQ:
        return String.format("%s/%s", PaasClient.storageInstance().getBaseUrl(),
            "1/oauth2/goto/qq?mobile_sns=true");
      case AVOSCloudSNS:
        return String.format("%s/%s", PaasClient.storageInstance().getBaseUrl(), "sns.html");
      default:
        return String.format("%s/%s", PaasClient.storageInstance().getBaseUrl(), "sns.html");
    }
  }

  static public String typeName(SNSType type) {
    switch (type) {
      case AVOSCloudSNSSinaWeibo:
        return "weibo";
      case AVOSCloudSNSQQ:
        return "qq";
      default:
        throw new IllegalStateException("Unknown SNSType:" + type);
    }
  }

  static public String platformUserIdTag(SNSType type) {
    switch (type) {
      case AVOSCloudSNSSinaWeibo:
        return "uid";
      case AVOSCloudSNSQQ:
        return "openid";
      default:
        throw new IllegalStateException("Unknown SNSType:" + type);
    }
  }

  static private File archivePath(Context context, SNSType type) {
    File dir = context.getDir("AVOSCloudSNS", Context.MODE_PRIVATE);
    return new File(dir, typeName(type));
  }

  static public void navigateToWebAuthentication(Activity activity, SNSType type) {
    Intent intent = new Intent(activity, SNSWebActivity.class);
    intent.putExtra(urlTag, urlForType(type));
    activity.startActivity(intent);
  }

  static public boolean expired(final String value) {
    Date date = AVUtils.dateFromString(value);
    Date now = new Date();
    return date.before(now);
  }

  protected void removeArchive(Context context, SNSType type) {
    File file = archivePath(context, type);
    file.delete();
  }

  protected void save(Context context, SNSType type) {
    JSONObject object = new JSONObject();
    try {
      object.put(SNS.accessTokenTag, accessToken);
      object.put(SNS.expiresAtTag, expiresAt);
      object.put(SNS.userIdTag, userId);
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
    }
    String jsonString = object.toString();
    if (!AVUtils.isBlankString(jsonString)) {
      AVPersistenceUtils.saveContentToFile(jsonString, archivePath(context, type));
    }
  }

  protected void load(Context context, SNSType type) {
    String jsonString = AVPersistenceUtils.readContentFromFile(archivePath(context, type));
    if (AVUtils.isBlankString(jsonString)) {
      return;
    }
    try {
      JSONObject object = new JSONObject(jsonString);
      accessToken = object.getString(SNS.accessTokenTag);
      expiresAt = object.getString(SNS.expiresAtTag);
      userId = object.getString(SNS.userIdTag);
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
    }
  }

  public Map<String, Object> userInfo() {
    userInfo.clear();
    if (!AVUtils.isBlankString(userName)) {
      userInfo.put(SNS.userNameTag, userName);
    }
    if (!AVUtils.isBlankString(accessToken)) {
      userInfo.put(SNS.accessTokenTag, accessToken);
    }
    if (!AVUtils.isBlankString(userId)) {
      userInfo.put(SNS.userIdTag, userId);
    }
    if (!AVUtils.isBlankString(expiresAt)) {
      userInfo.put(SNS.expiresAtTag, expiresAt);
    }
    userInfo.put(SNS.snsTypeTag, this.type());
    return userInfo;
  }

  /**
   * 获取授权的全部数据
   * 
   * @return
   */
  public JSONObject authorizedData() {
    return this.authorizedData;
  }

  protected void setAuthorizedData(JSONObject data) {
    this.authorizedData = data;
  }

  public boolean hasValidSessionToken(Activity activity, SNSType type) {
    load(activity, type);
    if (AVUtils.isBlankString(accessToken) || AVUtils.isBlankString(expiresAt)) {
      return false;
    }
    return !expired(expiresAt);
  }

  public boolean doesUserExpireOfPlatform() {
    if (AVUtils.isBlankString(accessToken) || AVUtils.isBlankString(expiresAt)) {
      return true;
    }
    return expired(expiresAt);
  }


}
