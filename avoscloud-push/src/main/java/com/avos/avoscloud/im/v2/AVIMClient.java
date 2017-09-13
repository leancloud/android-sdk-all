package com.avos.avoscloud.im.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVIMClientParcel;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVSession;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.IntentUtil;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.SignatureFactory;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avos.avoscloud.im.v2.callback.AVIMClientStatusCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.callback.AVIMOnlineClientsCallback;

/**
 * 实时聊天的实例类，一个实例表示一个用户的登录
 *
 * 每个用户的登录都是一个单例
 * 
 */
public class AVIMClient {

  String clientId;
  AVIMMessageStorage storage;
  private String tag;
  private String sessionToken;
  static ConcurrentHashMap<String, AVIMClient> clients =
      new ConcurrentHashMap<String, AVIMClient>();
  ConcurrentHashMap<String, AVIMConversation> conversationCache =
      new ConcurrentHashMap<String, AVIMConversation>();
  boolean isConversationSync = false;

  private static boolean isAutoOpen = true;

  public SignatureFactory getSignatureFactory() {
    return AVIMOptions.getGlobalOptions().getSignatureFactory();
  }

  /**
   * 设置AVIMClient通用的签名生成工厂
   * 
   * @param factory
   * @since 3.0
   */

  public static void setSignatureFactory(SignatureFactory factory) {
    AVIMOptions.getGlobalOptions().setSignatureFactory(factory);
  }

  /**
   * 设置实时通信的超时时间
   *
   * 默认为15s
   *
   * @param timeoutInSecs 设置超时时间
   */

  public static void setTimeoutInSecs(int timeoutInSecs) {
    AVIMOptions.getGlobalOptions().setTimeoutInSecs(timeoutInSecs);
  }

  /**
   * 设置实时通信是否要在 App 重新启动后自动登录
   * @param isAuto
   */
  public static void setAutoOpen(boolean isAuto) {
    isAutoOpen = isAuto;
  }

  /**
   * 实时通信是否要在 App 重新启动后自动登录
   */
  public static boolean isAutoOpen() {
    return isAutoOpen;
  }

  private AVIMClient(String clientId) {
    this.clientId = clientId;
    storage = AVIMMessageStorage.getInstance(clientId);
  }

  /**
   * 获取一个聊天客户端实例
   * 
   * @param clientId 当前客户端登录账号的id
   * @return
   * @since 3.0
   */

  public static AVIMClient getInstance(String clientId) {
    if (AVUtils.isBlankString(clientId)) {
      throw new IllegalArgumentException("clientId cannot be null.");
    }

    AVIMClient client = clients.get(clientId);
    if (client != null) {
      return client;
    } else {
      client = new AVIMClient(clientId);
      AVIMClient elderClient = clients.putIfAbsent(clientId, client);
      return elderClient == null ? client : elderClient;
    }
  }

  /**
   * 获取聊天客户端实例，并且标记这个客户端实例的tag
   * 
   * @param clientId 当前客户端登录账号的id
   * @param tag 用于标注客户端，以支持单点登录功能
   * @return
   */
  public static AVIMClient getInstance(String clientId, String tag) {
    AVIMClient client = getInstance(clientId);
    client.tag = tag;
    return client;
  }

  /**
   * Get the AVIMClient that is instantiated by AVUser
   * @param user the related AVUser
   * @return
   */
  public static AVIMClient getInstance(AVUser user) {
    if (null == user) {
      throw new IllegalArgumentException("user cannot be null.");
    }

    String clientId = user.getObjectId();
    String sessionToken = user.getSessionToken();
    if (AVUtils.isBlankString(clientId) || AVUtils.isBlankString(sessionToken)) {
      throw new IllegalArgumentException("user must login first.");
    }

    AVIMClient client = getInstance(clientId);
    client.sessionToken = sessionToken;
    return client;
  }

  /**
   * Get the AVIMClient that is instantiated by AVUser and tag
   * @param user the related AVUser
   * @param tag the related tag，used for single login
   * @return
   */
  public static AVIMClient getInstance(AVUser user, String tag) {
    AVIMClient client = getInstance(user);
    client.tag = tag;
    return client;
  }

  /**
   * 连接服务器
   * 
   * @param callback
   * @since 3.0
   */
  public void open(final AVIMClientCallback callback) {
    open(null, callback);
  }

  /**
   * 连接服务器
   * @param option 登陆选项
   * @param callback
   */
  public void open(AVIMClientOpenOption option, final AVIMClientCallback callback) {

    AVIMClientParcel parcel = new AVIMClientParcel();
    parcel.setClientTag(tag);
    parcel.setSessionToken(sessionToken);
    if (null != option) {
      parcel.setForceSingleLogin(option.isForceSingleLogin());
    }
    BroadcastReceiver receiver = null;

    if (callback != null) {
      receiver = new AVIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Intent intent, Throwable error) {
          callback.internalDone(AVIMClient.this, AVIMException.wrapperAVException(error));
        }
      };
    }
    this.sendClientCommand(parcel, receiver, AVIMOperation.CLIENT_OPEN);
  }

  /**
   * 获取client列表中间当前为在线的client
   *
   * 每次查询最多20个client对象，超出部分会被忽略
   * 
   * @param clients 需要检查的client列表
   * @param callback
   */
  public void getOnlineClients(List<String> clients, final AVIMOnlineClientsCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_ONLINE_CLIENTS, clients);

    BroadcastReceiver receiver = null;

    if (callback != null) {
      receiver = new AVIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Intent intent, Throwable error) {
          if (error != null) {
            callback.internalDone(null, AVIMException.wrapperAVException(error));
          } else {
            List<String> onlineClients =
                intent.getStringArrayListExtra(Conversation.callbackOnlineClients);
            callback.internalDone(onlineClients, null);
          }
        }
      };
    }
    this.sendClientCMDToPushService(JSON.toJSONString(params), receiver,
        AVIMOperation.CLIENT_ONLINE_QUERY);
  }

  /**
   * 
   * @return 返回clientId
   */
  public String getClientId() {
    return this.clientId;
  }

  /**
   * 创建一个聊天对话
   * 
   * @param conversationMembers 对话参与者
   * @param attributes 对话属性
   * @param callback
   * @since 3.0
   */

  public void createConversation(final List<String> conversationMembers,
      final Map<String, Object> attributes, final AVIMConversationCreatedCallback callback) {
    this.createConversation(conversationMembers, null, attributes, false, callback);
  }

  public void createConversation(final List<String> conversationMembers, String name,
      final Map<String, Object> attributes, final AVIMConversationCreatedCallback callback) {
    this.createConversation(conversationMembers, name, attributes, false, callback);
  }

  /**
   * 创建一个聊天对话
   * 
   * @param members 对话参与者
   * @param attributes 对话的额外属性
   * @param isTransient 是否创建
   * @param callback
   */
  public void createConversation(final List<String> members, final String name,
                                 final Map<String, Object> attributes, final boolean isTransient,
                                 final AVIMConversationCreatedCallback callback) {
    this.createConversation(members, name, attributes, isTransient, false, callback);
  }

  /**
   * 创建或查询一个已有 conversation
   *
   * @param members 对话的成员
   * @param name 对话的名字
   * @param attributes 对话的额外属性
   * @param isTransient 是否是暂态会话
   * @param isUnique 如果已经存在符合条件的会话，是否返回已有回话
   *                 为 false 时，则一直为创建新的回话
   *                 为 true 时，则先查询，如果已有符合条件的回话，则返回已有的，否则，创建新的并返回
   *                 为 true 时，仅 members 为有效查询条件
   * @param callback
   */
  public void createConversation(final List<String> members, final String name,
      final Map<String, Object> attributes, final boolean isTransient, final boolean isUnique,
      final AVIMConversationCreatedCallback callback) {
    try {
      AVUtils.ensureElementsNotNull(members, AVSession.ERROR_INVALID_SESSION_ID);
    } catch (Exception e) {
      if (callback != null) {
        callback.internalDone(null, AVIMException.wrapperAVException(e));
      }
      return;
    }

    final HashMap<String, Object> conversationAttributes = new HashMap<String, Object>();
    if (attributes != null) {
      conversationAttributes.putAll(attributes);
    }
    if (!AVUtils.isBlankString(name)) {
      conversationAttributes.put(Conversation.NAME, name);
    }
    final List<String> conversationMembers = new ArrayList<String>();
    conversationMembers.addAll(members);
    if (!conversationMembers.contains(clientId)) {
      conversationMembers.add(clientId);
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, conversationMembers);
    params.put(Conversation.PARAM_CONVERSATION_ISUNIQUE, isUnique);
    params.put(Conversation.PARAM_CONVERSATION_ISTRANSIENT, isTransient);
    if (conversationAttributes.size() > 0) {
      Map<String, Object> assembledAttributes = AVIMConversation.processAttributes(conversationAttributes, true);
      if (assembledAttributes != null) {
        params.put(Conversation.PARAM_CONVERSATION_ATTRIBUTE, assembledAttributes);
      }
    }

    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new AVIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Intent intent, Throwable error) {
          String conversationId =
              intent.getExtras().getString(Conversation.callbackConversationKey);
          String createdAt = intent.getExtras().getString(Conversation.callbackCreatedAt);
          AVIMConversation conversation = null;
          if (error == null) {
            conversation = getConversation(conversationId);
            conversation.setMembers(conversationMembers);
            conversation.setAttributesForInit(conversationAttributes);
            conversation.setTransientForInit(isTransient);
            conversation.setConversationId(conversationId);
            conversation.setCreator(clientId);
            conversation.setCreatedAt(createdAt);
            conversation.setUpdatedAt(createdAt);
            storage.insertConversations(Arrays.asList(conversation));
          }
          callback.internalDone(conversation, AVIMException.wrapperAVException(error));
        }
      };
    }
    this.sendClientCMDToPushService(JSON.toJSONString(params), receiver,
        AVIMOperation.CONVERSATION_CREATION);
  }

  /**
   * 获取某个特定的聊天对话
   * 
   * @param conversationId 对应的是_Conversation表中的objectId
   * @return
   * @since 3.0
   */
  public AVIMConversation getConversation(String conversationId) {

    if (!isConversationSync) {
      syncConversationCache();
    }

    AVIMConversation conversation = conversationCache.get(conversationId);
    if (conversation != null) {
      return conversation;
    } else {
      conversation = new AVIMConversation(this, conversationId);
      AVIMConversation elderConversation =
          conversationCache.putIfAbsent(conversationId, conversation);
      return elderConversation == null ? conversation : elderConversation;
    }
  }


  /**
   * 获取AVIMConversationQuery对象，以此来查询conversation
   * @deprecated 由于历史原因，AVIMConversationQuery 只能检索 _Conversation 表中 attr 列中的属性，
   * 而不能完整检索 _Conversation 表的其他自定义属性，所以这里废弃，推荐使用 {@link #getConversationsQuery()}
   * @return
   */
  public AVIMConversationQuery getQuery() {
    return new AVIMConversationQuery(this);
  }

  /**
   * 获取 AVIMConversationsQuery 对象，以此来查询 conversation
   * @return
   */
  public AVIMConversationsQuery getConversationsQuery() {
    return new AVIMConversationsQuery(this);
  }

  static AVIMClientEventHandler clientEventHandler;

  /**
   * 设置AVIMClient的事件处理单元，
   * 
   * 包括Client断开链接和重连成功事件
   * 
   * @param handler
   */
  public static void setClientEventHandler(AVIMClientEventHandler handler) {
    clientEventHandler = handler;
  }

  protected static AVIMClientEventHandler getClientEventHandler() {
    return clientEventHandler;
  }

  /**
   * 设置离线消息推送模式
   * @param isOnlyCount true 为仅推送离线消息数量，false 为推送离线消息
   * @deprecated Please use {@link #setUnreadNotificationEnabled(boolean)}
   */
  public static void setOfflineMessagePush(boolean isOnlyCount) {
    setUnreadNotificationEnabled(isOnlyCount);
  }

  /**
   * Set the mode of offline message push
   * @param enabled if the value is true，the number of unread messages will be pushed when the client open.
   *                if the value is false, the unread messages will be pushed when the client open.
   */
  public static void setUnreadNotificationEnabled(boolean enabled) {
    AVSession.setUnreadNotificationEnabled(enabled);
  }

  /**
   * 注销当前的聊天客户端链接
   * 
   * @param callback
   * @since 3.0
   */
  public void close(final AVIMClientCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new AVIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Intent intent, Throwable error) {
          callback.internalDone(AVIMClient.this, AVIMException.wrapperAVException(error));
          AVIMClient.this.close();
        }
      };
    }
    this.sendClientCMDToPushService(null, receiver, AVIMOperation.CLIENT_DISCONNECT);
  }

  /**
   * Local close to clean up
   */
  protected void close() {
    clients.remove(clientId);
    conversationCache.clear();
    storage.deleteClientData();
  }

  /**
   * 当前client的状态
   */
  public enum AVIMClientStatus {
    /**
     * 当前client尚未open，或者已经close
     */
    AVIMClientStatusNone(110),
    /**
     * 当前client已经打开，连接正常
     */
    AVIMClientStatusOpened(111),
    /**
     * 当前client由于网络因素导致的连接中断
     */
    AVIMClientStatusPaused(120);

    int code;

    AVIMClientStatus(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }

    static AVIMClientStatus getClientStatus(int code) {
      switch (code) {
        case 110:
          return AVIMClientStatusNone;
        case 111:
          return AVIMClientStatusOpened;
        case 120:
          return AVIMClientStatusPaused;
        default:
          return null;
      }
    };
  }

  public void getClientStatus(final AVIMClientStatusCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new AVIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Intent intent, Throwable error) {
          AVIMClientStatus status = null;
          if (intent.getExtras() != null
              && intent.getExtras().containsKey(Conversation.callbackClientStatus)) {
            status =
                AVIMClientStatus.getClientStatus(intent.getExtras().getInt(
                    Conversation.callbackClientStatus));
          }
          callback.internalDone(status, AVIMException.wrapperAVException(error));
        }
      };
    }
    this.sendClientCMDToPushService(null, receiver, AVIMOperation.CLIENT_STATUS);
  }

  protected void sendClientCommand(AVIMClientParcel parcel, BroadcastReceiver receiver, AVIMOperation operation) {
    int requestId = AVUtils.getNextIMRequestId();

    if (receiver != null) {
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(receiver,
        new IntentFilter(operation.getOperation() + requestId));
    }
    Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    i.putExtra(Conversation.INTENT_KEY_CLIENT_PARCEL, parcel);
    i.putExtra(Conversation.INTENT_KEY_CLIENT, clientId);
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
  }

  protected void sendClientCMDToPushService(String dataAsString, BroadcastReceiver receiver,
      AVIMOperation operation) {

    int requestId = AVUtils.getNextIMRequestId();

    if (receiver != null) {
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(receiver,
          new IntentFilter(operation.getOperation() + requestId));
    }
    Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    if (!AVUtils.isBlankString(dataAsString)) {
      i.putExtra(Conversation.INTENT_KEY_DATA, dataAsString);
    }

    i.putExtra(Conversation.INTENT_KEY_CLIENT, clientId);
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
  }

  static AVException validateNonEmptyConversationMembers(List<String> members) {
    if (members == null || members.isEmpty()) {
      return new AVException(AVException.UNKNOWN,
          "Conversation can't be created with empty members");
    }
    try {
      AVUtils.ensureElementsNotNull(members, AVSession.ERROR_INVALID_SESSION_ID);
    } catch (Exception e) {
      return new AVException(e);
    }
    return null;
  }

  /**
   * 同步 sqlite 中数据到 conversationCache，只需要同步一次就可以
   */
  private void syncConversationCache() {
    List<AVIMConversation> cachedConversations = storage.getAllCachedConversations();
    for (AVIMConversation conversation : cachedConversations) {
      conversationCache.put(conversation.getConversationId(), conversation);
    }
    isConversationSync = true;
  }

  protected void removeConversationCache(AVIMConversation conversation) {
    conversationCache.remove(conversation.getConversationId());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    AVIMClient anotherClient = (AVIMClient) object;
    if (clientId == null) {
      return anotherClient.clientId == null;
    }
    return this.clientId.equals(anotherClient.clientId);
  }

  static boolean messageQueryCacheEnabled = true;

  /**
   * 控制是否打开历史消息查询的本地缓存功能
   * 
   * @param enable
   */
  public static void setMessageQueryCacheEnable(boolean enable) {
    messageQueryCacheEnabled = enable;
  }
}
