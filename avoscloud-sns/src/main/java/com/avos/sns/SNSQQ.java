package com.avos.sns;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.avos.avoscloud.AVUtils;
import com.tencent.open.HttpStatusException;
import com.tencent.open.NetworkUnavailableException;
import com.tencent.tauth.Constants;
import com.tencent.tauth.IRequestListener;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Iterator;


/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 10/31/13 Time: 12:23 PM To change this template
 * use File | Settings | File Templates.
 */
public class SNSQQ extends SNSBase {
  private Tencent tencent;
  private QQListener listener;
  public static final String TAG = SNSQQ.class.getSimpleName();

  public SNSQQ(final String key, final String sec, final String url) {
    super();
    appKey = key;
    appSec = sec;
    redirectUrl = url;
  }

  public SNSQQ(final String authorizeUrl) {
    super();
    this.authorizeUrl = authorizeUrl;
  }

  @Override
  public void logIn(Activity activity, SNSCallback cb) {
    callback = cb;
    applicationContext = activity.getApplicationContext();

    // check session.
    if (hasValidSessionToken(activity, type())) {
      if (callback != null) {
        callback.internalDone(this, null);
      }
      return;
    }

    // check if we have mobile qq installed.
    if (AVUtils.isBlankString(appKey) && AVUtils.isBlankString(authorizeUrl)) {
      // navigateToWebAuthentication(activity, type());
      if (callback != null) {
        callback.internalDone(this, SNSException.noAppKeyException());
      }
      return;
    }


    if (!AVUtils.isBlankString(authorizeUrl)) {
      Intent i = new Intent(activity, SNSWebActivity.class);
      i.putExtra(SNSBase.urlTag, authorizeUrl);
      activity.startActivityForResult(i, REQUEST_CODE_FOR_QQ_AUTHORIZE);
      return;
    }


    tencent = Tencent.createInstance(appKey, applicationContext);
    listener = new QQListener();

    // specify scope later.
    // private static final String SCOPE =
    // "get_user_info,get_simple_userinfo,get_user_profile,get_app_friends,"
    // +
    // "add_share,add_topic,list_album,upload_pic,add_album,set_user_face,get_vip_info,get_vip_rich_info,get_intimate_friends_weibo,match_nick_tips_weibo";
    tencent.login(activity, "", listener);
  }

  @Override
  public void logOut(Activity activity) {
    if (tencent != null) {
      tencent.logout(activity);
    }
    removeArchive(activity, type());
  }

  class QQListener implements IUiListener {
    @Override
    public void onComplete(final org.json.JSONObject jsonObject) {
      try {
        // usually it's 3 monthes.
        String expiresIn = jsonObject.getString(SNS.expiresInTag);
        Long seconds = Long.parseLong(expiresIn);
        Date expiresAt = new Date(System.currentTimeMillis() + seconds * 1000);
        SNSQQ.this.expiresAt = AVUtils.stringFromDate(expiresAt);
        SNSQQ.this.accessToken = jsonObject.getString(SNS.accessTokenTag);
        SNSQQ.this.userId = jsonObject.getString("openid");

        if (tencent != null) {
          tencent.requestAsync(Constants.GRAPH_SIMPLE_USER_INFO, null, Constants.HTTP_GET,
              new IRequestListener() {
                @Override
                public void onComplete(JSONObject userInfoObject, Object o) {
                  try {
                    JSONObject authData = new JSONObject();
                    Iterator<String> keyIterator = userInfoObject.keys();
                    while (keyIterator.hasNext()) {
                      String key = keyIterator.next();
                      authData.put(key, userInfoObject.get(key));
                    }

                    keyIterator = jsonObject.keys();
                    while (keyIterator.hasNext()) {
                      String key = keyIterator.next();
                      authData.put(key, jsonObject.get(key));
                    }
                    SNSQQ.this.setAuthorizedData(authData);
                  } catch (JSONException e) {
                    SNSQQ.this.setAuthorizedData(jsonObject);
                  } finally {
                    done(null, null, jsonObject);
                  }
                }

                @Override
                public void onIOException(IOException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onMalformedURLException(MalformedURLException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onJSONException(JSONException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onConnectTimeoutException(ConnectTimeoutException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onSocketTimeoutException(SocketTimeoutException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onNetworkUnavailableException(NetworkUnavailableException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onHttpStatusException(HttpStatusException e, Object o) {
                  done(null, e, jsonObject);
                }

                @Override
                public void onUnknowException(Exception e, Object o) {
                  done(null, e, jsonObject);
                }
              }, null);
        } else {
          SNSQQ.this.setAuthorizedData(jsonObject);
        }

      } catch (Exception e) {

      } finally {
        SNSQQ.this.save(SNSQQ.this.applicationContext, type());
        done(null, null, jsonObject);
      }
    }

    private void done(SNSException authException, Exception userInfoException,
        JSONObject jsonObject) {
      if (userInfoException != null) {
        SNSQQ.this.setAuthorizedData(jsonObject);
      }
      if (authException != null && SNSQQ.this.callback != null) {
        SNSQQ.this.callback.internalDone(SNSQQ.this, authException);
        return;
      }
      if (SNSQQ.this.callback != null) {
        SNSQQ.this.callback.internalDone(SNSQQ.this, null);
      }
    }

    @Override
    public void onError(com.tencent.tauth.UiError uiError) {
      Log.e(TAG, "Error: " + uiError.errorMessage);
      done(new SNSException(uiError.errorCode,
          uiError.errorMessage), null, null);

    }

    @Override
    public void onCancel() {
      Log.w(TAG, "User cancelled");
      done(new SNSException(SNSException.USER_CANCEL, ""), null, null);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (tencent != null) {
      tencent.onActivityResult(requestCode, resultCode, data);
    } else if (callback != null && requestCode == REQUEST_CODE_FOR_QQ_AUTHORIZE) {
      if (resultCode == Activity.RESULT_OK) {
        String authorizeData = data.getStringExtra(AUTHORIZE_RESULT);
        JSONObject result;
        try {
          result = new JSONObject(authorizeData);
          if (result.has(SNS.accessTokenTag)) {
            Long seconds = result.getLong(SNS.expiresInTag);
            Date expiresAt = new Date(System.currentTimeMillis() + seconds * 1000);
            SNSQQ.this.expiresAt = AVUtils.stringFromDate(expiresAt);
            SNSQQ.this.accessToken = result.getString(SNS.accessTokenTag);
            SNSQQ.this.userId = result.getString("openid");
            SNSQQ.this.save(SNSQQ.this.applicationContext, type());
            SNSQQ.this.setAuthorizedData(result);
            callback.internalDone(SNSQQ.this, null);
          } else {
            String errorString = result.getString(SNS.errorTag);
            callback.internalDone(SNSQQ.this, new SNSException(SNSException.OTHER_CAUSE, errorString));
          }
        } catch (JSONException e) {
          e.printStackTrace();
          callback.internalDone(SNSQQ.this, new SNSException(e));
        }

      } else if (resultCode == Activity.RESULT_CANCELED) {
        SNSQQ.this.callback.internalDone(SNSQQ.this, new SNSException(SNSException.USER_CANCEL, ""));
      }
    }
  }

  @Override
  public SNSType type() {
    return SNSType.AVOSCloudSNSQQ;
  }

}
