package com.avos.avospush.session;

import java.util.List;
import java.util.Map;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.Messages;
import com.avos.avoscloud.Signature;
import com.avos.avoscloud.im.v2.AVIMClient;

public class ConversationControlPacket extends PeerBasedCommandPacket {
  public static final String CONVERSATION_CMD = "conv";

  public static class ConversationControlOp {
    // 客户端发出的op
    public static final String START = "start";
    public static final String ADD = "add";
    public static final String REMOVE = "remove";
    public static final String QUERY = "query";
    public static final String UPDATE = "update";
    public static final String MUTE = "mute";
    public static final String UNMUTE = "unmute";
    public static final String COUNT = "count";
    public static final String MAX_READ = "max-read";

    // 服务器端会响应的op
    public static final String STARTED = "started";
    public static final String JOINED = "joined";
    public static final String MEMBER_JOINED = "members_joined";
    public static final String MEMBER_LEFTED = "members_left";
    public static final String ADDED = "added";
    public static final String REMOVED = "removed";
    public static final String LEFT = "left";
    public static final String QUERY_RESULT = "results";
    public static final String MEMBER_COUNT_QUERY_RESULT = "result";
    public static final String UPDATED = "updated";
  }

  private List<String> members;
  private String signature;

  private long timestamp;

  private String nonce;

  private String conversationId;

  private String op;

  private Map<String, Object> attributes;

  private boolean isTransient;

  /**
   * 原子创建单聊会话，如果为 true，则先查询是否有符合条件的 conversation，有则返回已存在的，否则创建新的
   * 详见 https://github.com/leancloud/avoscloud-push/issues/293
   */
  private boolean isUnique;

  private boolean isTemporary = false;

  private int tempTTL = 0;

  public ConversationControlPacket() {
    this.setCmd(CONVERSATION_CMD);
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public List<String> getMembers() {
    return members;
  }

  public void setMembers(List<String> members) {
    this.members = members;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public void setTransient(boolean isTransient) {
    this.isTransient = isTransient;
  }

  public boolean isUnique() {
    return isUnique;
  }

  public void setUnique(boolean isUnique) {
    this.isUnique = isUnique;
  }

  public boolean isTemporary() {return isTemporary;}

  public void setTemporary(boolean val) {
    this.isTemporary = val;
  }

  public int getTempTTL() {
    return tempTTL;
  }

  public void setTempTTL(int tempTTL) {
    this.tempTTL = tempTTL;
  }


  @Override
  protected Messages.GenericCommand.Builder getGenericCommandBuilder() {
    Messages.GenericCommand.Builder builder = super.getGenericCommandBuilder();
    builder.setOp(Messages.OpType.valueOf(op));
    builder.setConvMessage(getConvCommand());
    return builder;
  }

  private Messages.ConvCommand getConvCommand() {
    Messages.ConvCommand.Builder builder = Messages.ConvCommand.newBuilder();

    if (attributes != null && !attributes.isEmpty()) {
      Messages.JsonObjectMessage.Builder attrBuilder = Messages.JsonObjectMessage.newBuilder();

      attrBuilder.setData(attributes.toString());
      builder.setAttr(attrBuilder);
    }

    if (!AVUtils.isEmptyList(members)) {
      builder.addAllM(members);
    }

    if (getSignature() != null) {
      builder.setS(getSignature());
      builder.setT(getTimestamp());
      builder.setN(getNonce());
    }

    if (!AVUtils.isBlankString(conversationId)) {
      builder.setCid(conversationId);
    }
    if (isTransient) {
      builder.setTransient(isTransient);
    }
    if (isUnique) {
      builder.setUnique(isUnique);
    }
    if (isTemporary) {
      builder.setTempConv(isTemporary);
      builder.setTempConvTTL(tempTTL);
    }

    return builder.build();
  }

  public static ConversationControlPacket genConversationCommand(String selfId,
      String conversationId, List<String> peers, String op, Map<String, Object> attributes,
      Signature signature, boolean isTransient, boolean isUnique, boolean isTemporary, int tempTTL,
      boolean isSystem, int requestId) {
    ConversationControlPacket ccp = new ConversationControlPacket();
    if (AVIMClient.getClientsCount() > 1) {
      // selfId is necessary only when more than 1 clients logined.
      ccp.setPeerId(selfId);
    }
    ccp.setConversationId(conversationId);
    ccp.setRequestId(requestId);
    ccp.setTransient(isTransient);
    ccp.setUnique(isUnique);
    ccp.setTemporary(isTemporary);
    if (isTemporary) {
      ccp.setTempTTL(tempTTL);
    }

    if (!AVUtils.isEmptyList(peers)) {
      ccp.setMembers(peers);
    }
    ccp.setOp(op);

    if (signature != null) {
      if (op.equals(ConversationControlOp.ADD) || op.equals(ConversationControlOp.REMOVE)
          || op.equals(ConversationControlOp.START)) {
        ccp.setSignature(signature.getSignature());
        ccp.setNonce(signature.getNonce());
        ccp.setTimestamp(signature.getTimestamp());
      }
    }
    ccp.setRequestId(requestId);
    ccp.setAttributes(attributes);

    return ccp;
  }

  public static ConversationControlPacket genConversationCommand(String selfId,
      String conversationId, List<String> peers, String op, Map<String, Object> attributes,
      Signature signature, boolean isTransient, int requestId) {
    return genConversationCommand(selfId, conversationId, peers, op, attributes, signature, isTransient,
        false, false, 0, false, requestId);
  }

  public static ConversationControlPacket genConversationCommand(String selfId,
      String conversationId, List<String> peers, String op, Map<String, Object> attributes,
      Signature signature, int requestId) {
    return genConversationCommand(selfId, conversationId, peers, op, attributes, signature, false,
        requestId);
  }
}
