package com.avos.sns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.avos.avoscloud.AVUtils;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

import java.lang.String;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 4/17/13 Time: 10:40 AM To change this template
 * use File | Settings | File Templates.
 */
public class SNSSinaWeibo extends SNSBase {

  public static final String TAG = SNSSinaWeibo.class.getSimpleName();
  private Oauth2AccessToken oauth2AccessToken;
  private final AuthInfo weiboImpl;
  private SsoHandler ssoHandler;

  public SNSSinaWeibo(Context context, final String key, final String sec, final String url) {
    super();
    appKey = key;
    appSec = sec;
    redirectUrl = url;
    weiboImpl = new AuthInfo(context, appKey, redirectUrl, "");
  }

  public SNSSinaWeibo(final String authorizeUrl) {
    super();
    this.authorizeUrl = authorizeUrl;
    weiboImpl = null;
  }

  private void logInImpl(Activity activity, SNSCallback cb) {
    callback = cb;


    // check session.
    if (hasValidSessionToken(activity, type())) {
      if (callback != null) {
        callback.internalDone(this, null);
      }
      return;
    }

    // check if we have weibo installed.
    if (AVUtils.isBlankString(appKey) && AVUtils.isBlankString(authorizeUrl)) {
      callback.internalDone(this, SNSException.noAppKeyException());
      // navigateToWebAuthentication(activity, type());
      return;
    }

    if (!AVUtils.isBlankString(authorizeUrl)) {
      Intent i = new Intent(activity, SNSWebActivity.class);
      i.putExtra(SNSBase.urlTag, authorizeUrl);
      activity.startActivityForResult(i, REQUEST_CODE_FOR_SINAWEIBO_AUTHORIZE);
      return;
    }

    ssoHandler = new SsoHandler(activity, weiboImpl);
    ssoHandler.authorize(new AuthDialogListener());
  }

  @Override
  public void logIn(Activity activity, SNSCallback callback) {
    applicationContext = activity.getApplicationContext();
    logInImpl(activity, callback);
  }

  @Override
  public void logOut(Activity activity) {
    // remove local session token.
    removeArchive(activity, type());
  }

  class AuthDialogListener implements WeiboAuthListener {
    @Override
    public void onComplete(Bundle values) {

      String token = values.getString(SNS.accessTokenTag);
      String expiresIn = values.getString(SNS.expiresInTag);
      SNSSinaWeibo.this.oauth2AccessToken = new Oauth2AccessToken(token, expiresIn);
      Long seconds = Long.parseLong(expiresIn);
      Date expires = new Date(System.currentTimeMillis() + seconds * 1000);
      SNSSinaWeibo.this.expiresAt = AVUtils.stringFromDate(expires);

      SNSSinaWeibo.this.accessToken = token;
      SNSSinaWeibo.this.userId = values.getString("uid");
      SNSSinaWeibo.this.userName = values.getString(SNS.userNameTag);
      if (SNSSinaWeibo.this.oauth2AccessToken.isSessionValid()) {
        SNSSinaWeibo.this.save(SNSSinaWeibo.this.applicationContext, type());
      }
      JSONObject object = new JSONObject();
      for (String key : values.keySet()) {
        try {
          object.put(key, values.get(key));
        } catch (JSONException e) {
          // ignore exception e.printStackTrace();
        }
      }
      SNSSinaWeibo.this.setAuthorizedData(object);
      if (SNSSinaWeibo.this.callback != null) {
        SNSSinaWeibo.this.callback.internalDone(SNSSinaWeibo.this, null);
      }
    }

    @Override
    public void onCancel() {
      Log.w(TAG, "User Cancelled");
      if (SNSSinaWeibo.this.callback != null) {
        SNSSinaWeibo.this.callback.internalDone(SNSSinaWeibo.this, new SNSException(
            SNSException.USER_CANCEL, ""));
      }
    }

    @Override
    public void onWeiboException(WeiboException e) {
      Log.e(TAG, "Exception: " + e.getMessage());
      if (SNSSinaWeibo.this.callback != null) {
        SNSSinaWeibo.this.callback.internalDone(SNSSinaWeibo.this,
            new SNSException(e.getMessage(), e.getCause()));
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (ssoHandler != null) {
      ssoHandler.authorizeCallBack(requestCode, resultCode, data);
    } else if (callback != null && requestCode == REQUEST_CODE_FOR_SINAWEIBO_AUTHORIZE) {
      if (resultCode == Activity.RESULT_OK) {
        String authorizeData = data.getStringExtra(AUTHORIZE_RESULT);
        JSONObject result;
        try {
          result = new JSONObject(authorizeData);
          if (result.has(SNS.accessTokenTag)) {
            String token = result.getString(SNS.accessTokenTag);
            Long expiresIn = result.getLong(SNS.expiresInTag);
            SNSSinaWeibo.this.oauth2AccessToken =
                new Oauth2AccessToken(token, expiresIn.toString());
            Date expires = new Date(System.currentTimeMillis() + expiresIn * 1000);
            SNSSinaWeibo.this.expiresAt = AVUtils.stringFromDate(expires);

            SNSSinaWeibo.this.accessToken = token;
            SNSSinaWeibo.this.userId = result.getString("uid");
            SNSSinaWeibo.this.userName = result.getJSONObject("user_info").getString("name");
            if (SNSSinaWeibo.this.oauth2AccessToken.isSessionValid()) {
              SNSSinaWeibo.this.save(SNSSinaWeibo.this.applicationContext, type());
            }
            SNSSinaWeibo.this.setAuthorizedData(result);
            callback.internalDone(SNSSinaWeibo.this, null);
          } else {
            String errorString = result.getString(SNS.errorTag);
            callback.internalDone(SNSSinaWeibo.this,
                new SNSException(SNSException.OTHER_CAUSE, errorString));
          }
        } catch (JSONException e) {
          e.printStackTrace();
          callback.internalDone(SNSSinaWeibo.this, new SNSException(e));
        }
      } else if (resultCode == Activity.RESULT_CANCELED) {
        callback.internalDone(SNSSinaWeibo.this, new SNSException(SNSException.USER_CANCEL, ""));
      }
    }
  }

  @Override
  public SNSType type() {
    return SNSType.AVOSCloudSNSSinaWeibo;
  }
}
