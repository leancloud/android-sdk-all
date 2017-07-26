package com.avos.avoscloud;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wli on 2017/5/11.
 */

public class AVSMS {

  /**
   *  request sms code
   * @param phone the phone need to receive message
   * @param smsOption the option value for this operation
   * @throws AVException
   */
  public static void requestSMSCode(String phone, AVSMSOption smsOption) throws AVException {
    requestSMSCodeInBackground(phone, smsOption, true,
      new RequestMobileCodeCallback() {
        @Override
        public void done(AVException e) {
          if (e != null) {
            AVExceptionHolder.add(e);
          }
        }

        @Override
        public boolean mustRunOnUIThread() {
          return false;
        }
      });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  /**
   * request sms code
   * @param phone the phone need to receive message
   * @param option the option value for this operation
   * @param callback the callback to execute when this operation is complete.
   */
  public static void requestSMSCodeInBackground(String phone, AVSMSOption option, RequestMobileCodeCallback callback) {
    requestSMSCodeInBackground(phone, option, false, callback);
  }

  private static void requestSMSCodeInBackground(String phone, AVSMSOption option, boolean sync, RequestMobileCodeCallback callback) {
    final RequestMobileCodeCallback internalCallback = callback;
    if (AVUtils.isBlankString(phone) || !AVUtils.checkMobilePhoneNumber(phone)) {
      callback.internalDone(new AVException(AVException.INVALID_PHONE_NUMBER,
        "Invalid Phone Number"));
    }
    Map<String, Object> map = null;
    if (null == option) {
      map = new HashMap<>();
    } else {
      map = option.getOptionMaps();
    }

    map.put("mobilePhoneNumber", phone);
    String object = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("requestSmsCode", object, sync, false,
      new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, null);
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
          }
        }
      }, null, null);
  }

  /**
   * 验证验证码
   *
   * @param code              验证码
   * @param mobilePhoneNumber 手机号码
   * @throws AVException
   */
  public static void verifySMSCode(String code, String mobilePhoneNumber) throws AVException {
    verifySMSCodeInBackground(code, mobilePhoneNumber, true, new AVMobilePhoneVerifyCallback() {
      @Override
      public void done(AVException e) {
        if (e != null) {
          AVExceptionHolder.add(e);
        }
      }

      @Override
      public boolean mustRunOnUIThread() {
        return false;
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
  }

  /**
   * 验证验证码
   *
   * @param code              验证码
   * @param phoneNumber 手机号
   * @param callback
   */
  public static void verifySMSCodeInBackground(String code, String phoneNumber,
                                               AVMobilePhoneVerifyCallback callback) {
    verifySMSCodeInBackground(code, phoneNumber, false, callback);
  }

  private static void verifySMSCodeInBackground(String code, String phoneNumber,
                                                boolean sync,
                                                AVMobilePhoneVerifyCallback callback) {
    final AVMobilePhoneVerifyCallback internalCallback = callback;

    if (AVUtils.isBlankString(code) || !AVUtils.checkMobileVerifyCode(code)) {
      callback
        .internalDone(new AVException(AVException.INVALID_PHONE_NUMBER, "Invalid Verify Code"));
    }
    String endpointer = String.format("verifySmsCode/%s", code);
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("mobilePhoneNumber", phoneNumber);
    PaasClient.storageInstance().postObject(endpointer, AVUtils.restfulServerData(params), sync,
      false, new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, null);
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          if (internalCallback != null) {
            internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
          }
        }
      }, null, null);
  }

}
