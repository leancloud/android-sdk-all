package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wli on 2017/5/9.
 */
public class AVCaptcha {

  private static final String CAPTCHA_CODE = "captcha_code";
  private static final String CAPTCHA_TOKEN = "captcha_token";
  private static final String CAPTCHA_URL = "captcha_url";
  private static final String VALIDATE_TOKEN = "validate_token";

  /**
   * request captcha digest from server
   * @param option the option value for this operation
   * @param callback the captcha digest will be passed back through the callback
   */
  public static void requestCaptchaInBackground(AVCaptchaOption option, final AVCallback<AVCaptchaDigest> callback) {
    if (null == callback) {
      return;
    }

    PaasClient.storageInstance().getObject("requestCaptcha", getOptionParams(option), false, null,
      new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          AVCaptchaDigest digest = new AVCaptchaDigest();
          if (!AVUtils.isBlankString(content)) {
            Map<String, String> map = JSON.parseObject(content, HashMap.class);
            if (null != map) {
              if (map.containsKey(CAPTCHA_TOKEN)) {
                digest.setNonce(map.get(CAPTCHA_TOKEN));
              }
              if (map.containsKey(CAPTCHA_URL)) {
                digest.setUrl(map.get(CAPTCHA_URL));
              }
            }
          }
          callback.internalDone(digest, null);
        }

        @Override
        public void onFailure(Throwable error, String content) {
          callback.internalDone(null, AVErrorUtils.createException(error, content));
        }
      });
  }

  /**
   * verify captcha code
   * @param captchaCode the code parsed form the image obtained by {@link AVCaptchaDigest#getUrl()}
   * @param digest the captcha digest return form {@link #requestCaptchaInBackground}
   * @param callback the validate token will be passed back through the callback
   */
  public static void verifyCaptchaCodeInBackground(String captchaCode, AVCaptchaDigest digest, final AVCallback<String> callback) {
    if (null == callback) {
      return;
    }
    if (AVUtils.isBlankString(captchaCode)) {
      throw new IllegalArgumentException("captchaCode cannot be null.");
    }

    if (null == digest) {
      throw new IllegalArgumentException("digest cannot be null.");
    }

    if (AVUtils.isBlankString(digest.getNonce())) {
      throw new IllegalArgumentException("nonce in digest cannot be null.");
    }

    Map<String, Object> map = new HashMap<>();
    map.put(CAPTCHA_CODE, captchaCode);
    map.put(CAPTCHA_TOKEN, digest.getNonce());
    String jsonString = AVUtils.jsonStringFromMapWithNull(map);
    PaasClient.storageInstance().postObject("verifyCaptcha", jsonString, false, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        if (!AVUtils.isBlankString(content)) {
          Map<String, String> map = JSON.parseObject(content, HashMap.class);
          if (null != map && map.containsKey(VALIDATE_TOKEN)) {
            callback.internalDone(map.get(VALIDATE_TOKEN), null);
            return;
          }
        }
        callback.internalDone(null, null);
      }

      @Override
      public void onFailure(Throwable error, String content) {
        callback.internalDone(AVErrorUtils.createException(error, content));
      }
    });
  }

  private static AVRequestParams getOptionParams(AVCaptchaOption option) {
    AVRequestParams params = new AVRequestParams();
    if (null != option) {
      if (option.getHeight() > 0) {
        params.put("height", option.getHeight());
      }

      if (option.getWidth() > 0) {
        params.put("width", option.getWidth());
      }
    }
    return params;
  }
}
