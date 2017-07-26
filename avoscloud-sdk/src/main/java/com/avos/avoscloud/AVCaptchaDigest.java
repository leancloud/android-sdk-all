package com.avos.avoscloud;

/**
 * Created by wli on 2017/5/10.
 */
public class AVCaptchaDigest {

  /**
   * vaptcha token
   */
  private String nonce;

  /**
   * captcha image url
   */
  private String url;

  String getNonce() {
    return nonce;
  }

  void setNonce(String nonce) {
    this.nonce = nonce;
  }

  /**
   * get the captcha image url
   * @return image url
   */
  public String getUrl() {
    return url;
  }

  void setUrl(String url) {
    this.url = url;
  }
}
