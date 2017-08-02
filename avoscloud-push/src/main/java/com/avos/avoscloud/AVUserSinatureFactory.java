package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wli on 2017/7/28.
 */
public class AVUserSinatureFactory {

  private static final String RTM_SIGN_ENDPOINT = "rtm/sign";

  private String sessionToken;

  public AVUserSinatureFactory(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  public Signature getOpenSignature() throws AVException {
    final StringBuffer stringBuffer = new StringBuffer();
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("session_token", sessionToken);
    PaasClient.storageInstance().postObject(RTM_SIGN_ENDPOINT, JSON.toJSONString(data), true, false,
      new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          if (e != null) {
            AVExceptionHolder.add(e);
          } else {
            stringBuffer.append(content);
          }
        }

        @Override
        public void onFailure(Throwable error, String content) {
          AVExceptionHolder.add(AVErrorUtils.createException(error, content));
        }
      }, null, null);

    if (AVExceptionHolder.exists()) {
      throw  AVExceptionHolder.remove();
    }

    return parseSiparseSignaturegnature(stringBuffer.toString());
  }

  private Signature parseSiparseSignaturegnature(String content) throws AVException {
    if (AVUtils.isBlankString(content)) {
      throw new AVException(new Throwable("singnature is empty"));
    }
    Signature signature = new Signature();
    try {
      JSONObject jsonObject = JSON.parseObject(content);
      signature.setNonce(jsonObject.getString("nonce"));
      signature.setSignature(jsonObject.getString("signature"));
      signature.setTimestamp(jsonObject.getLong("timestamp"));
    } catch (Exception e) {
      throw new AVException(new Throwable("singnature content parse error," + content));
    }
    return signature;
  }
}
