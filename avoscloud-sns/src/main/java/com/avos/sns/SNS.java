package com.avos.sns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.lang.Object;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.*;


/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 4/17/13 Time: 10:40 AM To change this template
 * use File | Settings | File Templates.
 */
public class SNS {
  private static final String TAG = "SNS";

  // define universal platform independency tag.
  public static final String accessTokenTag = "access_token";
  public static final String expiresInTag = "expires_in";
  public static final String expiresAtTag = "expires_at";
  public static final String userNameTag = "userName";
  public static final String snsTypeTag = "snsType";
  public static final String userIdTag = "userId";
  public static final String authDataTag = "authData";
  public static final String errorTag = "error";

  // store
  static private ConcurrentHashMap<String, SNSBase> components =
      new ConcurrentHashMap<String, SNSBase>();

  /**
   * 设置平台所需要的参数
   * 
   * @warning 如果不进行设置, 则用`AVOSCloud`进行登录认证. 目前此设置只用来进行SSO登录, Web方式认证统一为`AVOSCloud`
   * 
   * @param type 平台类型
   * @param appKey 该平台分配的AppKey
   * @param appSec 该平台分配的AppSecret
   * @param redirectUrl 该平台上设置的回调地址
   * @since 1.4.4
   */

  public static void setupPlatform(Context context, SNSType type, final String appKey,
      final String appSec, final String redirectUrl) throws AVException {
    SNSBase base = null;
    switch (type) {
      case AVOSCloudSNSSinaWeibo:
        base = new SNSSinaWeibo(context, appKey, appSec, redirectUrl);
        break;
      case AVOSCloudSNSQQ:
        base = new SNSQQ(appKey, appSec, redirectUrl);
        break;
      default:
        throw AVErrorUtils.createException(AVException.OTHER_CAUSE, "Not supported type");
    }
    if (base != null) {
      components.put(SNSBase.typeName(type), base);
    }
  }

  /**
   * 采用LeanCloud 统一的WebView进行授权
   * 
   * @param type
   * @param authorizeUrl 请填写LeanCloud网页管理界面获取的登录URL
   * @return
   * @throws AVException
   */

  public static SNSBase setupPlatform(SNSType type, final String authorizeUrl) throws AVException {
    SNSBase base = null;
    switch (type) {
      case AVOSCloudSNSSinaWeibo:
        base = new SNSSinaWeibo(authorizeUrl);
        break;
      case AVOSCloudSNSQQ:
        base = new SNSQQ(authorizeUrl);
        break;
      default:
        throw AVErrorUtils.createException(AVException.OTHER_CAUSE, "Not supported type");
    }
    if (base != null) {
      components.put(SNSBase.typeName(type), base);
    }
    return base;
  }


  /**
   * 用社交平台登录, 优先使用sso方式进行登录，如果客户端未安装相应的应用，将会使用web方式进行认证。 如果未指定任何app key，AVOSCloud进行验证
   * 
   * @param activity 调用方的activity
   * @param type 使用何种社交平台登录
   * @param callback 登录结果回调
   * @since 1.4.4
   * @return void.
   */
  static public void loginWithCallback(Activity activity, SNSType type, SNSCallback callback) {
    SNSBase base = component(type);
    if (base == null) {
      SNSBase.navigateToWebAuthentication(activity, type);
      return;
    }
    base.logIn(activity, callback);
  }

  /**
   * SSO回调，在您的activity中，重载此函数，以便您所注册的callback可以得到调用。
   * 
   * @param requestCode onActivityResult的返回值
   * @param resultCode onActivityResult的返回值
   * @param data onActivityResult的返回值
   * @param type 指定SNS平台类型，目前支持微博和QQ。
   * @since 1.4.4
   */
  static public void onActivityResult(int requestCode, int resultCode, Intent data, SNSType type) {
    SNSBase base = component(type);
    if (base != null) {
      base.onActivityResult(requestCode, resultCode, data);
    }
  }

  static private SNSBase component(SNSType type) {
    return components.get(SNSBase.typeName(type));
  }

  /**
   * 生成一个新的AarseUser，并且将AVUser与SNS平台获取的userInfo关联。
   * 
   * @param userInfo 在SNS登录成功后，返回的userInfo信息。
   * @param callback 关联完成后，调用的回调函数。
   */
  static public void loginWithAuthData(Map<String, Object> userInfo,
      final LogInCallback<AVUser> callback) {
    loginWithAuthData(AVUser.class, userInfo, callback);
  }

  /**
   * 生成一个新的AVUser子类化对象，并且将该对象与SNS平台获取的userInfo关联。
   * 
   * @param clazz 子类化的AVUer的class对象
   * @param userInfo 在SNS登录成功后，返回的userInfo信息。
   * @param callback 关联完成后，调用的回调函数。
   * @since 1.4.4
   */
  static public <T extends AVUser> void loginWithAuthData(final Class<T> clazz,
      Map<String, Object> userInfo, final LogInCallback<T> callback) {

    AVUser.AVThirdPartyUserAuth userAuth =
        new AVUser.AVThirdPartyUserAuth((String) userInfo.get(accessTokenTag),
            (String) userInfo.get(expiresAtTag), SNSBase.typeName((SNSType) userInfo
                .get(snsTypeTag)), (String) userInfo.get(userIdTag));
    AVUser.loginWithAuthData(clazz, userAuth, callback);
  }

  /**
   * 将现存的AVUser与从SNS平台获取的userInfo关联起来。
   * 
   * @param user AVUser 对象。
   * @param userInfo 在SNS登录成功后，返回的userInfo信息。
   * @param callback 关联完成后，调用的回调函数。
   * @since 1.4.4
   */
  static public void associateWithAuthData(AVUser user, Map<String, Object> userInfo,
      final SaveCallback callback) {
    AVUser.AVThirdPartyUserAuth userAuth =
        new AVUser.AVThirdPartyUserAuth((String) userInfo.get(accessTokenTag),
            (String) userInfo.get(expiresAtTag), SNSBase.typeName((SNSType) userInfo
                .get(snsTypeTag)), (String) userInfo.get(userIdTag));
    AVUser.associateWithAuthData(user, userAuth, callback);
  }

  /**
   * 解除AVUser与SNS平台的绑定。
   * 
   * @param user 现有的AVUser 对象。
   * @param type 指定SNS平台类型，目前支持微博和QQ。
   * @param callback 关联完成后，调用的回调函数。
   * @since 1.4.4
   */
  static public void logout(AVUser user, SNSType type, final SaveCallback callback) {
    AVUser.dissociateAuthData(user, SNSBase.typeName(type), callback);
  }

  /**
   * @deprecated Not yet ready. 直接使用AVOSCloud
   *             提供的web平台进行登录，不使用任何sso方式登录。您的APP不需要预先申请任何app，而是使用AVOSCloud APP进行登录。
   * @param activity 登录调用方的activity
   * @param callback 登录结果回调
   * 
   * @return void.
   */
  @Deprecated
  static private void webLoginWithCallback(Activity activity, SNSCallback callback) {
    SNSBase.navigateToWebAuthentication(activity, SNSType.AVOSCloudSNS);
  }

  /**
   * 注销指定的社交平台绑定的账号
   * 
   * @param type 指定SNS平台类型，目前支持微博和QQ。
   * 
   */
  static public void logout(Activity activity, SNSType type) {
    SNSBase base = component(type);
    if (base != null) {
      base.logOut(activity);
    }
    components.remove(SNSBase.typeName(type));
  }

  /**
   * 获取指定的社交平台已经缓存的用户信息
   * 
   * @param type 指定SNS平台类型，目前支持微博和QQ。
   * @return 包含用户信息的字典, 如果返回nil则没有绑定的用户. 包括常用字段, "accessTokenTag", "expires_in", "expiresAt"
   *         "userName"和"userId"等。
   * 
   * 
   */
  static public Map<String, Object> userInfo(SNSType type) {
    SNSBase base = component(type);
    if (base != null) {
      return base.userInfo();
    }
    return null;
  }

  /**
   * 判断指定的社交平台已经缓存的用户信息是否过期
   * 
   * @param type 指定SNS平台类型，目前支持微博和QQ。
   * @return 是否过期
   */
  static public boolean doesUserExpireOfPlatform(SNSType type) {
    SNSBase base = component(type);
    if (base != null) {
      return base.doesUserExpireOfPlatform();
    }
    return true;
  }
}
