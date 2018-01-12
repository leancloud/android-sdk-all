package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wli on 2017/7/28.
 */
public class AVUserSignatureFactory implements SignatureFactory {

  private static final String RTM_SIGN_ENDPOINT = "rtm/sign";

  private String sessionToken;

  public AVUserSignatureFactory(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  public Signature createSignature(String peerId, List<String> watchIds) throws SignatureException {
    final StringBuffer stringBuffer = new StringBuffer();
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("session_token", sessionToken);
    PaasClient.storageInstance().postObject(RTM_SIGN_ENDPOINT, JSON.toJSONString(data), true, false,
        new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (e != null) {
              AVExceptionHolder.add(new SignatureException(SignatureFactory.SIGNATURE_FAILED_LOGIN,
                  "failed to create signature. cause:" + e.getMessage()));
            } else {
              stringBuffer.append(content);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            AVExceptionHolder.add(new SignatureException(SignatureFactory.SIGNATURE_FAILED_LOGIN, content));
          }
        }, null, null);

    if (AVExceptionHolder.exists()) {
      throw (SignatureException)AVExceptionHolder.remove();
    }

    return parseSiparseSignaturegnature(stringBuffer.toString());
  }

  public Signature createGroupSignature(String groupId, String peerId, List<String> targetPeerIds,
                                        String action) throws SignatureException {
    return null;
  }

  public Signature createConversationSignature(String conversationId, String clientId,
                                               List<String> targetIds, String action) throws SignatureException {
    return null;
  }

  public Signature createBlacklistSignature(String clientId, String conversationId, List<String> memberIds,
                                            String action) throws SignatureException {
    return null;
  }

  private Signature parseSiparseSignaturegnature(String content) throws SignatureException {
    if (AVUtils.isBlankString(content)) {
      throw new SignatureException(SignatureFactory.SIGNATURE_FAILED_LOGIN, "singnature is empty");
    }
    Signature signature = new Signature();
    try {
      JSONObject jsonObject = JSON.parseObject(content);
      signature.setNonce(jsonObject.getString("nonce"));
      signature.setSignature(jsonObject.getString("signature"));
      signature.setTimestamp(jsonObject.getLong("timestamp"));
    } catch (Exception e) {
      throw new SignatureException(SignatureFactory.SIGNATURE_FAILED_LOGIN, "singnature content parse error: " + content);
    }
    return signature;
  }
}
