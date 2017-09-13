package com.avos.avoscloud;

import java.util.List;

/**
 * Created by nsun on 5/15/14.
 */
public interface SignatureFactory {
  public static final int SIGNATURE_FAILED_LOGIN = 4102;

  /**
   * 实现一个基础签名方法 其中的签名算法会在SessionManager和AVIMClient(V2)中被使用
   * 
   * @param peerId
   * @param watchIds
   * @return
   * @throws SignatureException 如果签名计算中间发生任何问题请抛出本异常
   */
  public Signature createSignature(String peerId, List<String> watchIds) throws SignatureException;

  /**
   * 实现AVGroup相关的签名计算
   * 
   * @param groupId
   * @param peerId
   * @param targetPeerIds
   * @param action
   * @return
   * @throws SignatureException 如果签名计算中间发生任何问题请抛出本异常
   */
  @Deprecated
  public Signature createGroupSignature(String groupId, String peerId, List<String> targetPeerIds,
      String action) throws SignatureException;

  /**
   * 实现AVIMConversation相关的签名计算
   * 
   * @param conversationId
   * @param clientId
   * @param targetIds 操作所对应的数据
   * @param action - 此次行为的动作，行为分别对应常量 invite（加群和邀请）和 kick（踢出群）
   * @return
   * @throws SignatureException 如果签名计算中间发生任何问题请抛出本异常
   */
  public Signature createConversationSignature(String conversationId, String clientId,
      List<String> targetIds, String action) throws SignatureException;

  public static class SignatureException extends AVException {

    public SignatureException(int theCode, String theMessage) {
      super(theCode, theMessage);
    }

  }
}
