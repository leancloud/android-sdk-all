package com.avos.avoscloud.im.v2;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVErrorUtils;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVPowerfulUtils;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVRequestParams;
import com.avos.avoscloud.AVResponse;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.IntentUtil;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.PushServiceParcel;
import com.avos.avoscloud.QueryOperation;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.QueryConditions;
import com.avos.avoscloud.im.v2.Conversation.AVIMOperation;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMOperationFailure;
import com.avos.avoscloud.im.v2.callback.AVIMConversationMemberCountCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationMemberQueryCallback;
import com.avos.avoscloud.im.v2.callback.AVIMOperationPartiallySucceededCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationSimpleResultCallback;
import com.avos.avoscloud.im.v2.callback.AVIMMessageRecalledCallback;
import com.avos.avoscloud.im.v2.callback.AVIMMessageUpdatedCallback;
import com.avos.avoscloud.im.v2.callback.AVIMMessagesQueryCallback;
import com.avos.avoscloud.im.v2.callback.AVIMSingleMessageQueryCallback;
import com.avos.avoscloud.im.v2.conversation.AVIMConversationMemberInfo;
import com.avos.avoscloud.im.v2.conversation.MemberRole;
import com.avos.avoscloud.im.v2.messages.AVIMFileMessage;
import com.avos.avoscloud.im.v2.messages.AVIMFileMessageAccessor;
import com.avos.avoscloud.im.v2.messages.AVIMRecalledMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AVIMConversation {

  /**
   * 暂存消息
   * <p/>
   * 只有在消息发送时，对方也是在线的才能收到这条消息
   */
  public static final int TRANSIENT_MESSAGE_FLAG = 0x10;
  /**
`   * 回执消息
   * <p/>
   * 当消息送到到对方以后，发送方会收到消息回执说明消息已经成功达到接收方
   */
  public static final int RECEIPT_MESSAGE_FLAG = 0x100;

  private static final String ATTR_PERFIX = Conversation.ATTRIBUTE + ".";


  String conversationId;
  Set<String> members;
  Map<String, Object> attributes;
  Map<String, Object> pendingAttributes;
  AVIMClient client;
  String creator;
  boolean isTransient;
  AVIMMessageStorage storage;

  // 注意，sqlite conversation 表中的 lastMessageAt、lastMessage 的来源是 AVIMConversationQuery
  // 所以并不一定是最新的，返回时要与 message 表中的数据比较，然后返回最新的，
  Date lastMessageAt;
  AVIMMessage lastMessage;

  String createdAt;
  String updatedAt;

  Map<String, Object> instanceData = new HashMap<>();
  Map<String, Object> pendingInstanceData = new HashMap<>();

  // 是否与数据库中同步了 lastMessage，避免多次走 sqlite 查询
  private boolean isSyncLastMessage = false;

  /**
   * 未读消息数量
   */
  int unreadMessagesCount = 0;
  boolean unreadMessagesMentioned = false;

  /**
   * 对方最后收到消息的时间，此处仅针对双人会话有效
   */
  long lastDeliveredAt;

  /**
   * 对方最后读消息的时间，此处仅针对双人会话有效
   */
  long lastReadAt;

  /**
   * 是否是服务号
   */
  boolean isSystem = false;

  /**
   * 是否是服务号
   */
  public boolean isSystem() {
    return isSystem;
  }

  /**
   * 是否是临时对话
   */
  boolean isTemporary = false;

  /**
   * 是否是临时对话
   */
  public boolean isTemporary() {
    return isTemporary;
  }

  void setTemporary(boolean temporary) {
    isTemporary = temporary;
  }

  /**
   * 临时对话过期时间
   */
  long temporaryExpiredat = 0l;

  /**
   * 获取临时对话过期时间（以秒为单位）
   */
  public long getTemporaryExpiredat() {
    return temporaryExpiredat;
  }

  /**
   * 设置临时对话过期时间（以秒为单位）
   * 仅对 临时对话 有效
   */
  public void setTemporaryExpiredat(long temporaryExpiredat) {
    if (this.isTemporary()) {
      this.temporaryExpiredat = temporaryExpiredat;
    }
  }

  protected int getType() {
    if (isSystem()) {
      return Conversation.CONV_TYPE_SYSTEM;
    } else if (isTransient()) {
      return Conversation.CONV_TYPE_TRANSIENT;
    } else if (isTemporary()) {
      return Conversation.CONV_TYPE_TEMPORARY;
    } else {
      return Conversation.CONV_TYPE_NORMAL;
    }
  }

  protected AVIMConversation(AVIMClient client, List<String> members,
      Map<String, Object> attributes, boolean isTransient) {
    this.members = new HashSet<String>();
    if (members != null) {
      this.members.addAll(members);
    }
    this.attributes = new HashMap<String, Object>();
    if (attributes != null) {
      this.attributes.putAll(attributes);
    }
    this.client = client;
    pendingAttributes = new HashMap<String, Object>();
    this.isTransient = isTransient;

    this.storage = AVIMMessageStorage.getInstance(client.clientId);
  }

  protected AVIMConversation(AVIMClient client, String conversationId) {
    this(client, null, null, false);
    this.conversationId = conversationId;
  }

  public String getConversationId() {
    return this.conversationId;
  }

  protected void setConversationId(String id) {
    this.conversationId = id;
  }

  protected void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * 获取聊天对话的创建者
   *
   * @return
   * @since 3.0
   */
  public String getCreator() {
    return this.creator;
  }

  /**
   * 发送一条非暂存消息
   *
   * @param message
   * @param callback
   * @since 3.0
   */
  public void sendMessage(AVIMMessage message, final AVIMConversationCallback callback) {
    sendMessage(message, null, callback);
  }

  /**
   * 发送消息
   * @param message
   * @param messageOption
   * @param callback
   */
  public void sendMessage(final AVIMMessage message, final AVIMMessageOption messageOption, final AVIMConversationCallback callback) {
    message.setConversationId(conversationId);
    message.setFrom(client.clientId);
    message.generateUniqueToken();
    message.setTimestamp(System.currentTimeMillis());
    if (!AVUtils.isConnected(AVOSCloud.applicationContext)) {
      message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusFailed);
      if (callback != null) {
        callback.internalDone(new AVException(AVException.CONNECTION_FAILED, "Connection lost"));
      }
      return;
    }

    message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSending);
    if (AVIMFileMessage.class.isAssignableFrom(message.getClass())) {
      AVIMFileMessageAccessor.upload((AVIMFileMessage) message, new SaveCallback() {
        public void done(AVException e) {
          if (e != null) {
            message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusFailed);
            if (callback != null) {
              callback.internalDone(e);
            }
          } else {
            sendCMDToPushService(null, message, messageOption, AVIMOperation.CONVERSATION_SEND_MESSAGE,
              callback, null);
          }
        }
      });
    } else {
      sendCMDToPushService(null, message, messageOption, AVIMOperation.CONVERSATION_SEND_MESSAGE,
        callback, null);
    }
  }

  /**
   * 发送一条消息。
   *
   * @param message
   * @param messageFlag 消息发送选项。
   * @param callback
   * @since 3.0
   * @deprecated Please use {@link #sendMessage(AVIMMessage, AVIMMessageOption, AVIMConversationCallback)}
   */
  public void sendMessage(AVIMMessage message, int messageFlag, AVIMConversationCallback callback) {
    AVIMMessageOption option = new AVIMMessageOption();
    option.setReceipt((messageFlag & AVIMConversation.RECEIPT_MESSAGE_FLAG) == AVIMConversation.RECEIPT_MESSAGE_FLAG);
    option.setTransient((messageFlag & AVIMConversation.TRANSIENT_MESSAGE_FLAG) == AVIMConversation.TRANSIENT_MESSAGE_FLAG);
    sendMessage(message, option, callback);
  }

  /**
   * update message content
   * @param oldMessage the message need to be modified
   * @param newMessage the content of the old message will be covered by the new message's
   * @param callback
   */
  public void updateMessage(AVIMMessage oldMessage, AVIMMessage newMessage, AVIMMessageUpdatedCallback callback) {
    PushServiceParcel parcel = new PushServiceParcel();
    parcel.setOldMessage(oldMessage);
    parcel.setNewMessage(newMessage);
    sendParcelToPushService(parcel, AVIMOperation.CONVERSATION_UPDATE_MESSAGE, callback);
  }

  /**
   * racall message
   * @param message the message need to be recalled
   * @param callback
   */
  public void recallMessage(AVIMMessage message, AVIMMessageRecalledCallback callback) {
    PushServiceParcel parcel = new PushServiceParcel();
    parcel.setRecallMessage(message);
    sendParcelToPushService(parcel, AVIMOperation.CONVERSATION_RECALL_MESSAGE, callback);
  }

  /**
   * save local message which failed to send to LeanCloud server.
   * Notice: this operation perhaps to block the main thread because that database operation is executing.
   *
   * @param message the message need to be saved to local.
   */
  public void addToLocalCache(AVIMMessage message) {
    this.storage.insertLocalMessage(message);
  }

  /**
   * remove local message from cache.
   * Notice: this operation perhaps to block the main thread because that database operation is executing.
   *
   * @param message
   */
  public void removeFromLocalCache(AVIMMessage message) {
    this.storage.removeLocalMessage(message);
  }

  /**
   * 查询最近的20条消息记录
   * 
   * @param callback
   */
  public void queryMessages(final AVIMMessagesQueryCallback callback) {
    this.queryMessages(20, callback);
  }

  /**
   * 从服务器端拉取最新消息
   * @param limit
   * @param callback
   */
  public void queryMessagesFromServer(int limit, final AVIMMessagesQueryCallback callback) {
    queryMessagesFromServer(null, 0, limit, null, 0, new AVIMMessagesQueryCallback() {
      @Override
      public void done(List<AVIMMessage> messages, AVIMException e) {
        if (null == e) {
          if (AVIMClient.messageQueryCacheEnabled) {
            processContinuousMessages(messages);
          }
          callback.internalDone(messages, null);
        } else {
          callback.internalDone(null, e);
        }
      }
    });
  }

  /**
   * 从本地缓存中拉取消息
   * @param limit
   * @param callback
   */
  public void queryMessagesFromCache(int limit, AVIMMessagesQueryCallback callback) {
    queryMessagesFromCache(null, 0, limit, callback);
  }

  private void queryMessagesFromServer(String msgId, long timestamp, int limit,
      String toMsgId, long toTimestamp, AVIMMessagesQueryCallback callback) {
    queryMessagesFromServer(msgId, timestamp, false, toMsgId, toTimestamp, false,
        AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromNewToOld, limit, callback);
  }

  /**
   * 获取特停类型的历史消息。
   * 注意：这个操作总是会从云端获取记录。
   * 另，该函数和 queryMessagesByType(type, msgId, timestamp, limit, callback) 配合使用可以实现翻页效果。
   *
   * @param msgType     消息类型，可以参看  `AVIMMessageType` 里的定义。
   * @param limit       本批次希望获取的消息数量。
   * @param callback    结果回调函数
   */
  public void queryMessagesByType(int msgType, int limit, final AVIMMessagesQueryCallback callback) {
    queryMessagesByType(msgType, null, 0, limit, callback);
  }

  /**
   * 获取特定类型的历史消息。
   * 注意：这个操作总是会从云端获取记录。
   * 另，如果不指定 msgId 和 timestamp，则该函数效果等同于 queryMessageByType(type, limit, callback)
   *
   * @param msgType     消息类型，可以参看  `AVIMMessageType` 里的定义。
   * @param msgId       消息id，从特定消息 id 开始向前查询（结果不会包含该记录）
   * @param timestamp   查询起始的时间戳，返回小于这个时间的记录，必须配合 msgId 一起使用。
   *                    要从最新消息开始获取时，请用 0 代替客户端的本地当前时间（System.currentTimeMillis()）
   * @param limit       返回条数限制
   * @param callback    结果回调函数
   */
  public void queryMessagesByType(int msgType, final String msgId, final long timestamp, final int limit,
                                  final AVIMMessagesQueryCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_MESSAGE_QUERY_MSGID, msgId);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP, timestamp);
    params.put(Conversation.PARAM_MESSAGE_QUERY_STARTCLOSED, false);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TO_MSGID, "");
    params.put(Conversation.PARAM_MESSAGE_QUERY_TO_TIMESTAMP, 0);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TOCLOSED, false);
    params.put(Conversation.PARAM_MESSAGE_QUERY_DIRECT, AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromNewToOld.getCode());
    params.put(Conversation.PARAM_MESSAGE_QUERY_LIMIT, limit);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TYPE, msgType);
    sendNonSideEffectCommand(JSON.toJSONString(params),
        AVIMOperation.CONVERSATION_MESSAGE_QUERY, callback);
  }

  private void queryMessagesFromServer(String msgId, long timestamp, boolean startClosed,
                                       String toMsgId, long toTimestamp, boolean toClosed,
                                       AVIMMessageQueryDirection direction, int limit,
                                       AVIMMessagesQueryCallback cb) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_MESSAGE_QUERY_MSGID, msgId);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP, timestamp);
    params.put(Conversation.PARAM_MESSAGE_QUERY_STARTCLOSED, startClosed);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TO_MSGID, toMsgId);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TO_TIMESTAMP, toTimestamp);
    params.put(Conversation.PARAM_MESSAGE_QUERY_TOCLOSED, toClosed);
    params.put(Conversation.PARAM_MESSAGE_QUERY_DIRECT, direction.getCode());
    params.put(Conversation.PARAM_MESSAGE_QUERY_LIMIT, limit);
    sendCMDToPushService(JSON.toJSONString(params),
        AVIMOperation.CONVERSATION_MESSAGE_QUERY, cb);
  }

  private void queryMessagesFromCache(final String msgId, final long timestamp, final int limit,
                                      final AVIMMessagesQueryCallback callback) {
    if (null != callback) {
      storage.getMessages(msgId, timestamp, limit, conversationId,
        new AVIMMessageStorage.StorageQueryCallback() {
          @Override
          public void done(List<AVIMMessage> messages, List<Boolean> breakpoints) {
            Collections.reverse(messages);
            callback.internalDone(messages, null);
          }
        });
    }
  }

  /**
   * 获取最新的消息记录
   * 
   * @param limit
   * @param callback
   */
  public void queryMessages(final int limit, final AVIMMessagesQueryCallback callback) {
    if (limit <= 0 || limit > 1000) {
      if (callback != null) {
        callback.internalDone(null, new AVException(new IllegalArgumentException(
            "limit should be in [1, 1000]")));
      }
    }
    // 如果屏蔽了本地缓存则全部走网络
    if (!AVIMClient.messageQueryCacheEnabled) {
      queryMessagesFromServer(null, 0, limit, null, 0, new AVIMMessagesQueryCallback() {

        @Override
        public void done(List<AVIMMessage> messages, AVIMException e) {
          if (callback != null) {
            if (e != null) {
              callback.internalDone(e);
            } else {
              callback.internalDone(messages, null);
            }
          }
        }
      });
      return;
    }
    if (!AVUtils.isConnected(AVOSCloud.applicationContext)) {
      queryMessagesFromCache(null, 0, limit, callback);
    } else {
      // 选择最后一条有 breakpoint 为 false 的消息做截断，因为是 true 的话，会造成两次查询。
      // 在 queryMessages 还是遇到 breakpoint，再次查询了
      long cacheMessageCount = storage.getMessageCount(conversationId);
      long toTimestamp = 0;
      String toMsgId = null;
      // 如果本地的缓存的量都不够的情况下，应该要去服务器查询，以免第一次查询的时候出现limit跟返回值不一致让用户认为聊天记录已经到头的问题
      if (cacheMessageCount >= limit) {
        final AVIMMessage latestMessage =
            storage.getLatestMessageWithBreakpoint(conversationId, false);

        if (latestMessage != null) {
          toMsgId = latestMessage.getMessageId();
          toTimestamp = latestMessage.getTimestamp();
        }
      }

      // 去服务器查询最新消息，看是否在其它终端产生过消息。为省流量，服务器会截断至 toMsgId 、toTimestamp
      queryMessagesFromServer(null, 0, limit, toMsgId, toTimestamp,
          new AVIMMessagesQueryCallback() {
            @Override
            public void done(List<AVIMMessage> messages, AVIMException e) {
              if (e != null) {
                // 如果遇到本地错误或者网络错误，直接返回缓存数据
                if (e.getCode() == AVIMException.TIMEOUT || e.getCode() == 0 || e.getCode() == 3000) {
                  queryMessagesFromCache(null, 0, limit, callback);
                } else {
                  if (callback != null) {
                    callback.internalDone(e);
                  }
                }
              } else {
                if (AVUtils.isEmptyList(messages)) {
                  // 这种情况就说明我们的本地消息缓存是最新的
                } else {
                  /*
                   * 1.messages.size()<=limit && messages.contains(latestMessage)
                   * 这种情况就说明在本地客户端退出后，该用户在其他客户端也产生了聊天记录而没有缓存到本地来,且产生了小于一页的聊天记录
                   * 2.messages==limit && !messages.contains(latestMessage)
                   * 这种情况就说明在本地客户端退出后，该用户在其他客户端也产生了聊天记录而没有缓存到本地来,且产生了大于一页的聊天记录
                   */

                  processContinuousMessages(messages);
                }
                queryMessagesFromCache(null, 0, limit, callback);
              }
            }
          });
    }
  }

  /**
   * 查询消息记录，上拉时使用。
   *
   * @param msgId 消息id，从消息id开始向前查询
   * @param timestamp 查询起始的时间戳，返回小于这个时间的记录。
   *          客户端时间不可靠，请用 0 代替 System.currentTimeMillis()
   * @param limit 返回条数限制
   * @param callback
   */
  public void queryMessages(final String msgId, final long timestamp, final int limit,
      final AVIMMessagesQueryCallback callback) {
    if (AVUtils.isBlankString(msgId) && timestamp == 0) {
      this.queryMessages(limit, callback);
      return;
    }
    // 如果屏蔽了本地缓存则全部走网络
    if (!AVIMClient.messageQueryCacheEnabled) {
      queryMessagesFromServer(msgId, timestamp, limit, null, 0, new AVIMMessagesQueryCallback() {

        @Override
        public void done(List<AVIMMessage> messages, AVIMException e) {
          if (callback != null) {
            if (e != null) {
              callback.internalDone(e);
            } else {
              callback.internalDone(messages, null);
            }
          }
        }
      });
      return;
    }

    // 先去本地缓存查询消息
    storage.getMessage(msgId, timestamp, conversationId,
        new AVIMMessageStorage.StorageMessageCallback() {

          @Override
          public void done(final AVIMMessage indicatorMessage,
              final boolean isIndicateMessageBreakPoint) {
            if (indicatorMessage == null || isIndicateMessageBreakPoint) {
              String startMsgId = msgId;
              long startTS = timestamp;
              int requestLimit = limit;
              queryMessagesFromServer(startMsgId, startTS, requestLimit, null, 0,
                  new AVIMMessagesQueryCallback() {
                    @Override
                    public void done(List<AVIMMessage> messages, AVIMException e) {
                      if (e != null) {
                        callback.internalDone(e);
                      } else {
                        List<AVIMMessage> cachedMsgs = new LinkedList<AVIMMessage>();
                        if (indicatorMessage != null) {
                          // add indicatorMessage to remove breakpoint.
                          cachedMsgs.add(indicatorMessage);
                        }
                        if (messages != null) {
                          cachedMsgs.addAll(messages);
                        }
                        processContinuousMessages(cachedMsgs);
                        queryMessagesFromCache(msgId, timestamp, limit, callback);
                      }
                    }
                  });
            } else {
              // 本地缓存过而且不是breakPoint
              storage.getMessages(msgId, timestamp, limit, conversationId,
                  new AVIMMessageStorage.StorageQueryCallback() {
                    @Override
                    public void done(List<AVIMMessage> messages, List<Boolean> breakpoints) {
                      processStorageQueryResult(messages, breakpoints, msgId, timestamp, limit,
                          callback);
                    }
                  });
            }
          }
        });
  }

  /**
   * 根据指定的区间来查询历史消息，可以指定区间开闭、查询方向以及最大条目限制
   * @param interval  - 区间，由起止 AVIMMessageIntervalBound 组成
   * @param direction - 查询方向，支持向前（AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromNewToOld）
   *                    或向后（AVIMMessageQueryDirection.AVIMMessageQueryDirectionFromOldToNew）查询
   * @param limit     - 结果最大条目限制
   * @param callback  - 结果回调函数
   */
  public void queryMessages(final AVIMMessageInterval interval, AVIMMessageQueryDirection direction, final int limit,
                            final AVIMMessagesQueryCallback callback) {
    if (null == interval || limit < 0) {
      if (null != callback) {
        callback.internalDone(null,
            new AVException(new IllegalArgumentException("interval must not null, or limit must great than 0.")));
      }
      return;
    }
    String mid = null;
    long ts = 0;
    boolean startClosed = false;
    String tmid = null;
    long tts = 0;
    boolean endClosed = false;
    if (null != interval.startIntervalBound) {
      mid = interval.startIntervalBound.messageId;
      ts = interval.startIntervalBound.timestamp;
      startClosed = interval.startIntervalBound.closed;
    }
    if (null != interval.endIntervalBound) {
      tmid = interval.endIntervalBound.messageId;
      tts = interval.endIntervalBound.timestamp;
      endClosed = interval.endIntervalBound.closed;
    }
    queryMessagesFromServer(mid, ts, startClosed, tmid, tts, endClosed, direction, limit, callback);
  }

  /**
   * 获取本聊天室的最后一条消息
   *
   * 如果AVIMClient.setMessageQueryCacheEnable(false)则强制走网络
   *
   * 否则只会取本地的缓存而不走网络，所以在一定程度上缺乏实时性
   * 
   * @param callback
   */
  public void getLastMessage(final AVIMSingleMessageQueryCallback callback) {
    if (AVIMClient.messageQueryCacheEnabled) {
      queryMessagesFromCache(null, 0, 1, new AVIMMessagesQueryCallback() {
        @Override
        public void done(List<AVIMMessage> messages, AVIMException e) {
          processLastMessageResult(messages, e, callback);
        }
      });
    } else {
      queryMessagesFromServer(null, 0, 1, null, 0, new AVIMMessagesQueryCallback() {
        @Override
        public void done(List<AVIMMessage> messages, AVIMException e) {
          processLastMessageResult(messages, e, callback);
        }
      });
    }
  }

  private void processLastMessageResult(List<AVIMMessage> resultMessages, AVIMException e,
      AVIMSingleMessageQueryCallback callback) {
    if (e == null) {
      if (resultMessages != null && resultMessages.size() > 0) {
        callback.internalDone(resultMessages.get(0), null);
      } else {
        callback.done(null, null);
      }
    } else {
      callback.internalDone(null, e);
    }
  }

  /**
   * 若发现有足够的连续消息，则直接返回。否则去服务器查询消息，同时消除断点。
   * */
  private void processStorageQueryResult(List<AVIMMessage> cachedMessages,
      List<Boolean> breakpoints, String originMsgId, long originMsgTS, int limit,
      final AVIMMessagesQueryCallback callback) {

    final List<AVIMMessage> continuousMessages = new ArrayList<AVIMMessage>();
    int firstBreakPointIndex = -1;
    for (int index = 0; index < cachedMessages.size(); index++) {
      if (breakpoints.get(index)) {
        firstBreakPointIndex = index;
        break;
      } else {
        continuousMessages.add(cachedMessages.get(index));
      }
    }
    final boolean connected = AVUtils.isConnected(AVOSCloud.applicationContext);
    // 如果只是最后一个消息是breakPoint，那还走啥网络
    if (!connected || continuousMessages.size() >= limit/* - 1*/) {
      // in case of wifi is invalid, and thre query list contain breakpoint, the result is error.
      Collections.sort(continuousMessages, messageComparator);
      callback.internalDone(continuousMessages, null);
    } else {
      final int restCount;
      final AVIMMessage startMessage;
      if (!continuousMessages.isEmpty()) {
        // 这里是缓存里面没有breakPoint，但是limit不够的情况下
        restCount = limit - continuousMessages.size();
        startMessage = continuousMessages.get(continuousMessages.size() - 1);
      } else {
        startMessage = null;
        restCount = limit;
      }
      queryMessagesFromServer(startMessage == null ? originMsgId : startMessage.messageId,
          startMessage == null ? originMsgTS : startMessage.timestamp, restCount, null, 0,
          new AVIMMessagesQueryCallback() {
            @Override
            public void done(List<AVIMMessage> serverMessages, AVIMException e) {
              if (e != null) {
                // 不管如何，先返回缓存里面已有的有效数据
                if (continuousMessages.size() > 0) {
                  callback.internalDone(continuousMessages, null);
                } else {
                  callback.internalDone(e);
                }
              } else {
                if (serverMessages == null) {
                  serverMessages = new ArrayList<AVIMMessage>();
                }
                continuousMessages.addAll(serverMessages);
                processContinuousMessages(continuousMessages);
                callback.internalDone(continuousMessages, null);
              }
            }
          });
    }
  }

  /**
   * 获取当前对话的所有角色信息
   * @param callback  结果回调函数
   */
  public void getAllMemberInfo(final AVIMConversationMemberQueryCallback callback) {
    QueryConditions conditions = new QueryConditions();
    conditions.addWhereItem("conversationId", QueryOperation.EQUAL_OP, this.conversationId);
    queryMemberInfo(conditions, callback);
  }

  /**
   * 获取对话内指定成员的角色信息
   * @param memberId  成员的 clientid
   * @param callback  结果回调函数
   */
  public void getMemberInfo(final String memberId, final AVIMConversationMemberQueryCallback callback) {
    QueryConditions conditions = new QueryConditions();
    conditions.addWhereItem("conversationId", QueryOperation.EQUAL_OP, this.conversationId);
    conditions.addWhereItem("peerId", QueryOperation.EQUAL_OP, memberId);
    queryMemberInfo(conditions, callback);
  }

  /**
   * 更新成员的角色信息
   * @param memberId  成员的 client id
   * @param role      角色
   * @param callback  结果回调函数
   */
  public void updateMemberRole(final String memberId, final MemberRole role, final AVIMConversationCallback callback) {
    AVIMConversationMemberInfo info = new AVIMConversationMemberInfo(this.conversationId, memberId, role);
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER_DETAILS, info.getUpdateAttrs());
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_PROMOTE_MEMBER,
        callback, null);
  }

  private void updateNickName(final String nickname, final AVIMConversationCallback callback) {
    ;
  }

  private void updateMemberComment(final String memberComment, final AVIMConversationCallback callback) {
    ;
  }

  private void queryMemberInfo(final QueryConditions queryConditions, final AVIMConversationMemberQueryCallback callback) {
    if (null == callback) {
      return;
    }
    this.client.queryConversationMemberInfo(queryConditions, callback);
  }

  /**
   * 将部分成员禁言
   * @param memberIds  成员列表
   * @param callback   结果回调函数
   */
  public void muteMembers(final List<String> memberIds, final AVIMOperationPartiallySucceededCallback callback) {
    if (null == memberIds || memberIds.size() < 1) {
      if (null != callback) {
        callback.done(new AVIMException(new IllegalArgumentException("memberIds is null")), null, null);
      }
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, memberIds);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_MUTE_MEMBER,
        callback, null);
  }

  /**
   * 将部分成员解除禁言
   * @param memberIds  成员列表
   * @param callback   结果回调函数
   */
  public void unmuteMembers(final List<String> memberIds, final AVIMOperationPartiallySucceededCallback callback) {
    if (null == memberIds || memberIds.size() < 1) {
      if (null != callback) {
        callback.done(new AVIMException(new IllegalArgumentException("memberIds is null")), null, null);
      }
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, memberIds);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_UNMUTE_MEMBER,
        callback, null);
  }

  /**
   * 查询所有被禁言的成员列表
   * @param callback  结果回调函数
   */
  public void queryMutedMembers(int offset, int limit, final AVIMConversationSimpleResultCallback callback) {
    if (null == callback) {
      return;
    } else if (offset < 0 || limit > 100) {
      callback.internalDone(null, new AVIMException(new IllegalArgumentException("offset/limit is illegal.")));
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.QUERY_PARAM_LIMIT, limit);
    params.put(Conversation.QUERY_PARAM_OFFSET, offset);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY,
        callback, null);
  }

  /**
   * 将部分成员加入黑名单
   * @param memberIds  成员列表
   * @param callback   结果回调函数
   */
  public void blockMembers(final List<String> memberIds, final AVIMOperationPartiallySucceededCallback callback) {
    if (null == memberIds || memberIds.size() < 1) {
      if (null != callback) {
        callback.done(new AVIMException(new IllegalArgumentException("memberIds is null")), null, null);
      }
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, memberIds);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_BLOCK_MEMBER,
        callback, null);
  }

  /**
   * 将部分成员从黑名单移出来
   * @param memberIds  成员列表
   * @param callback   结果回调函数
   */
  public void unblockMembers(final List<String> memberIds, final AVIMOperationPartiallySucceededCallback callback) {
    if (null == memberIds || memberIds.size() < 1) {
      if (null != callback) {
        callback.done(new AVIMException(new IllegalArgumentException("memberIds is null")), null, null);
      }
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, memberIds);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_UNBLOCK_MEMBER,
        callback, null);
  }

  /**
   * 查询黑名单的成员列表
   * @param callback  结果回调函数
   */
  public void queryBlockedMembers(int offset, int limit, final AVIMConversationSimpleResultCallback callback) {
    if (null == callback) {
      return;
    } else if (offset < 0 || limit > 100) {
      callback.internalDone(null, new AVIMException(new IllegalArgumentException("offset/limit is illegal.")));
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.QUERY_PARAM_LIMIT, limit);
    params.put(Conversation.QUERY_PARAM_OFFSET, offset);
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY,
        callback, null);
  }

  /**
   * 查询成员数量
   * @param callback
   */
  public void getMemberCount(AVIMConversationMemberCountCallback callback) {
    sendCMDToPushService(null, AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY, callback);
  }

  /**
   * 在聊天对话中间增加新的参与者
   *
   * @param friendsList
   * @param callback
   * @since 3.0
   */

  public void addMembers(final List<String> friendsList, final AVIMConversationCallback callback) {
    AVException membersCheckException = AVIMClient.validateNonEmptyConversationMembers(friendsList);
    if (membersCheckException != null) {
      if (callback != null) {
        callback.internalDone(null, membersCheckException);
      }
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, friendsList);

    this.sendCMDToPushService(JSON.toJSONString(params),
        AVIMOperation.CONVERSATION_ADD_MEMBER, callback, new OperationCompleteCallback() {

          @Override
          public void onComplete() {
            members.addAll(friendsList);
            storage.insertConversations(Arrays.asList(AVIMConversation.this));
          }

          @Override
          public void onFailure() {}
        });
  }

  /**
   * 在聊天记录中间剔除参与者
   *
   * @param friendsList
   * @param callback
   * @since 3.0
   */

  public void kickMembers(final List<String> friendsList, final AVIMConversationCallback callback) {

    AVException membersCheckException = AVIMClient.validateNonEmptyConversationMembers(friendsList);
    if (membersCheckException != null) {
      if (callback != null) {
        callback.internalDone(null, membersCheckException);
      }
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, friendsList);

    this.sendCMDToPushService(JSON.toJSONString(params),
        AVIMOperation.CONVERSATION_RM_MEMBER, callback, new OperationCompleteCallback() {

          @Override
          public void onComplete() {
            members.removeAll(friendsList);
            storage.insertConversations(Arrays.asList(AVIMConversation.this));
          }

          @Override
          public void onFailure() {}
        });
  }

  /**
   * 获取conversation当前的参与者
   *
   * @return
   * @since 3.0
   */
  public List<String> getMembers() {
    List<String> allList = new ArrayList<String>();
    allList.addAll(members);

    return Collections.unmodifiableList(allList);
  }

  /**
   * 静音，客户端拒绝收到服务器端的离线推送通知
   *
   * @param callback
   */
  public void mute(final AVIMConversationCallback callback) {
    this.sendCMDToPushService(null,
        AVIMOperation.CONVERSATION_MUTE, callback, null);
  }

  /**
   * 取消静音，客户端取消静音设置
   * 
   * @param callback
   */
  public void unmute(final AVIMConversationCallback callback) {
    this.sendCMDToPushService(null,
        AVIMOperation.CONVERSATION_UNMUTE, callback, null);
  }

  protected void setMembers(List<String> m) {
    members.clear();
    if (m != null) {
      members.addAll(m);
    }
  }

  /**
   * get the latest readAt timestamp
   * @return
   */
  public long getLastReadAt() {
    return lastReadAt;
  }

  /**
   * get the latest deliveredAt timestamp
   * @return
   */
  public long getLastDeliveredAt() {
    if (lastReadAt > lastDeliveredAt) {
      // 既然已读，肯定已经送到了
      return lastReadAt;
    }
    return lastDeliveredAt;
  }

  void setLastReadAt(long timeStamp, boolean saveToLocal) {
    if (timeStamp > lastReadAt) {
      lastReadAt = timeStamp;
      if (saveToLocal) {
        storage.updateConversationTimes(this);
      }
    }
  }

  void setLastDeliveredAt(long timeStamp, boolean saveToLocal) {
    if (timeStamp > lastDeliveredAt) {
      lastDeliveredAt = timeStamp;
      if (saveToLocal) {
        storage.updateConversationTimes(this);
      }
    }
  }

  /**
   * 退出当前的聊天对话
   *
   * @param callback
   * @since 3.0
   */
  public void quit(final AVIMConversationCallback callback) {
    this.sendCMDToPushService(null, AVIMOperation.CONVERSATION_QUIT,
        callback, new OperationCompleteCallback() {
          @Override
          public void onComplete() {
            storage.deleteConversationData(conversationId);
            members.remove(client.getClientId());
          }

          @Override
          public void onFailure() {

          }
        });
  }

  /**
   * Add a key-value pair to this conversation
   * @param key   Keys must be alphanumerical plus underscore, and start with a letter.
   * @param value Values may be numerical, String, JSONObject, JSONArray, JSONObject.NULL, or other
   *              AVObjects. value may not be null.
   */
  public void set(String key, Object value) {
    if (!AVUtils.isBlankString(key) && null != value) {
      pendingInstanceData.put(key, value);
    }
  }

  /**
   * Access a value
   * @param key
   * @return
   */
  public Object get(String key) {
    if (!AVUtils.isBlankString(key)) {
      if (pendingInstanceData.containsKey(key)) {
        return pendingInstanceData.get(key);
      }
      if (instanceData.containsKey(key)) {
        return instanceData.get(key);
      }
    }
    return null;
  }

  /**
   * 获取当前聊天对话的属性
   *
   * @return
   * @since 3.0
   */
  @Deprecated
  public Object getAttribute(String key) {
    Object value;
    if (pendingAttributes.containsKey(key)) {
      value = pendingAttributes.get(key);
    } else {
      value = attributes.get(key);
    }
    return value;
  }

  @Deprecated
  public void setAttribute(String key, Object value) {
    if (!AVUtils.isBlankContent(key)) {
      // 以往的 sdk 支持 setAttribute("attr.key", "attrValue") 这种格式，这里兼容一下
      if (key.startsWith(ATTR_PERFIX)) {
        this.pendingAttributes.put(key.substring(ATTR_PERFIX.length()), value);
      } else {
        this.pendingAttributes.put(key, value);
      }
    }
  }

  /**
   * 设置当前聊天对话的属性
   *
   * @param attr
   * @since 3.0
   */
  @Deprecated
  public void setAttributes(Map<String, Object> attr) {
    pendingAttributes.clear();
    pendingAttributes.putAll(attr);
  }

  /**
   * 设置当前聊天对话的属性，仅用于初始化时
   * 因为 attr 涉及到本地缓存，所以初始化时与主动调用 setAttributes 行为不同
   * @param attr
   */
  void setAttributesForInit(Map<String, Object> attr) {
    this.attributes.clear();
    if (attr != null) {
      this.attributes.putAll(attr);
    }
  }

  /**
   * 获取conversation的名字
   * 
   * @return
   */
  public String getName() {
    return (String) getAttribute(Conversation.NAME);
  }

  public void setName(String name) {
    pendingAttributes.put(Conversation.NAME, name);
  }

  /**
   * 获取最新一条消息的时间
   * 
   * @return
   */
  public Date getLastMessageAt() {
    AVIMMessage lastMessage = getLastMessage();
    if (null != lastMessage) {
      setLastMessageAt(new Date(lastMessage.getReceiptTimestamp()));
    }
    return lastMessageAt;
  }

  void setLastMessageAt(Date messageTime) {
    if (null != messageTime && (null == lastMessageAt || messageTime.after(this.lastMessageAt))) {
      this.lastMessageAt = messageTime;
    }
  }

  /**
   * 获取最新一条消息的时间
   *
   * @return
   */
  public AVIMMessage getLastMessage() {
    if (AVIMClient.messageQueryCacheEnabled && !isSyncLastMessage) {
      setLastMessage(getLastMessageFromLocal());
    }
    return lastMessage;
  }

  private AVIMMessage getLastMessageFromLocal() {
    if (AVIMClient.messageQueryCacheEnabled) {
      AVIMMessage lastMessageInLocal = storage.getLatestMessage(getConversationId());
      isSyncLastMessage = true;
      return lastMessageInLocal;
    }
    return null;
  }

  /**
   * lastMessage 的来源：
   * 1. sqlite conversation 表中的 lastMessage，conversation query 后存储下来的
   * 2. sqlite message 表中存储的最新的消息
   * 3. 实时通讯推送过来的消息也要及时更新 lastMessage
   * @param lastMessage
   */
  void setLastMessage(AVIMMessage lastMessage) {
    if (null != lastMessage) {
      if (null == this.lastMessage) {
        this.lastMessage = lastMessage;
      } else {
        if(this.lastMessage.getTimestamp() <= lastMessage.getTimestamp()) {
          this.lastMessage = lastMessage;
        }
      }
    }
  }

  void increaseUnreadCount(int num, boolean mentioned) {
    unreadMessagesCount = getUnreadMessagesCount() + num;
    if (mentioned) {
      unreadMessagesMentioned = mentioned;
    }

  }

  void updateUnreadCountAndMessage(AVIMMessage lastMessage, int unreadCount, boolean mentioned) {
    if (null != lastMessage) {
      setLastMessage(lastMessage);
      storage.insertMessage(lastMessage, true);
    }

    if (unreadMessagesCount != unreadCount) {
      unreadMessagesCount = unreadCount;
      unreadMessagesMentioned = mentioned;
      storage.updateConversationUreadCount(conversationId, unreadMessagesCount, mentioned);
    }
  }

  /**
   * 获取当前未读消息数量
   * @return
   */
  public int getUnreadMessagesCount() {
    return unreadMessagesCount;
  }

  /**
   * 判断当前未读消息中是否有提及当前用户的消息存在。
   * @return
   */
  public boolean unreadMessagesMentioned() {
    return unreadMessagesMentioned;
  }

  /**
   * 清除未读消息
   */
  public void read() {
    if (!isTransient) {
      AVIMMessage lastMessage = getLastMessage();
      Map<String, Object> params = new HashMap<String, Object>();
      if (null != lastMessage) {
        params.put(Conversation.PARAM_MESSAGE_QUERY_MSGID, lastMessage.getMessageId());
        params.put(Conversation.PARAM_MESSAGE_QUERY_TIMESTAMP, lastMessage.getTimestamp());
      }
      this.sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_READ, null, null);
    }
  }

  /**
   * 获取Conversation的创建时间
   * 
   * @return
   */
  public Date getCreatedAt() {
    return AVUtils.dateFromString(createdAt);
  }

  void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * 获取Conversation的更新时间
   * 
   * @return
   */
  public Date getUpdatedAt() {
    return AVUtils.dateFromString(updatedAt);
  }

  void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void fetchReceiptTimestamps(AVIMConversationCallback callback) {
    this.sendCMDToPushService(null, AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME, callback, null);
  }

  /**
   * 更新当前对话的属性至服务器端
   *
   * @param callback
   * @since 3.0
   */

  public void updateInfoInBackground(AVIMConversationCallback callback) {
    if (!pendingAttributes.isEmpty() || !pendingInstanceData.isEmpty()) {

      Map<String, Object> attributesMap = new HashMap<>();
      if (!pendingAttributes.isEmpty()) {
        final Map<String, Object> pendingAttrMap = processAttributes(pendingAttributes, false);
        if (null != pendingAttrMap) {
          attributesMap.putAll(pendingAttrMap);
        }
      }

      if (!pendingInstanceData.isEmpty()) {
        attributesMap.putAll(pendingInstanceData);
      }

      Map<String, Object> params = new HashMap<String, Object>();
      if (!attributesMap.isEmpty()) {
        params.put(Conversation.PARAM_CONVERSATION_ATTRIBUTE, attributesMap);
      }

      this.sendCMDToPushService(JSON.toJSONString(params),
        AVIMOperation.CONVERSATION_UPDATE, callback, new OperationCompleteCallback() {

          @Override
          public void onComplete() {
            attributes.putAll(pendingAttributes);
            pendingAttributes.clear();
            instanceData.putAll(pendingInstanceData);
            pendingInstanceData.clear();
            storage.insertConversations(Arrays.asList(AVIMConversation.this));
          }

          @Override
          public void onFailure() {

          }
        });
    } else {
      callback.internalDone(null);
    }
  }

  /**
   * 从服务器同步对话的属性
   * 
   * @param callback
   */

  public void fetchInfoInBackground(final AVIMConversationCallback callback) {
    if (AVUtils.isBlankString(conversationId)) {
      if (callback != null) {
        callback.internalDone(null, new AVException(AVException.INVALID_QUERY, "ConversationId is empty"));
      } else {
        LogUtil.avlog.e("ConversationId is empty");
      }
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    if (conversationId.startsWith(Conversation.TEMPCONV_ID_PREFIX)) {
      params.put(Conversation.QUERY_PARAM_TEMPCONV, conversationId);
    } else {
      Map<String, Object> whereMap = new HashMap<String, Object>();
      whereMap.put("objectId", conversationId);
      params.put(Conversation.QUERY_PARAM_WHERE, whereMap);
    }
    sendCMDToPushService(JSON.toJSONString(params), AVIMOperation.CONVERSATION_QUERY, callback);
  }

  /**
   * 加入当前聊天对话
   *
   * @param callback
   */

  public void join(AVIMConversationCallback callback) {
    this.sendCMDToPushService(null, AVIMOperation.CONVERSATION_JOIN,
        callback, new OperationCompleteCallback() {
          @Override
          public void onComplete() {
            members.add(client.getClientId());
          }

          @Override
          public void onFailure() {

          }
        });
  }

  public boolean isTransient() {
    return isTransient;
  }

  void setTransientForInit(boolean isTransient) {
    this.isTransient = isTransient;
  }


  /**
   * 超时的时间间隔设置为一个小时，即 fetch 操作并且返回了错误，则一个小时内 sdk 不再进行调用 fetch
   */
  int FETCH_TIME_INTERVEL = 3600 * 1000;

  /**
   * 最近的 sdk 调用的 fetch 操作的时间
   */
  long latestConversationFetch = 0;

  /**
   * 判断当前 Conversation 是否有效，因为 AVIMConversation 为客户端创建，有可能因为没有同步造成数据丢失
   * 可以根据此函数来判断，如果无效，则需要调用 fetchInfoInBackground 同步数据
   * 如果 fetchInfoInBackground 出错（比如因为 acl 问题造成 Forbidden to find by class permissions ），
   * 客户端就会在收到消息后一直做 fetch 操作，所以这里加了一个判断，如果在 FETCH_TIME_INTERVEL 内有业务类型的
   * error code 返回，则不在请求
   */
  boolean isShouldFetch() {
    return null == getCreatedAt() &&
      (System.currentTimeMillis() - latestConversationFetch > FETCH_TIME_INTERVEL);
  }

  private void processContinuousMessages(List<AVIMMessage> messages) {
    if (null != messages && !messages.isEmpty()) {
      Collections.sort(messages, messageComparator);
      setLastMessage(messages.get(messages.size() - 1));
      storage.insertContinuousMessages(messages, conversationId);
    }
  }

  void updateLocalMessage(AVIMMessage message) {
    storage.updateMessageForPatch(message);
  }

  private void sendParcelToPushService(final PushServiceParcel pushServiceParcel, final AVIMOperation operation, AVCallback callback) {
    final int requestId = AVUtils.getNextIMRequestId();
    Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_PARCEL_ACTION);
    i.putExtra(Conversation.INTENT_KEY_DATA, pushServiceParcel);
    i.putExtra(Conversation.INTENT_KEY_CLIENT, client.clientId);
    i.putExtra(Conversation.INTENT_KEY_CONVERSATION, this.conversationId);
    i.putExtra(Conversation.INTENT_KEY_CONV_TYPE, this.getType());
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
    if (callback != null) {
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(
        new AVIMBaseBroadcastReceiver(callback) {
          @Override
          public void execute(Intent intent, Throwable error) {
            if (null == error) {
              long patchTime = intent.getLongExtra(Conversation.PARAM_MESSAGE_PATCH_TIME, 0);
              if (operation.equals(AVIMOperation.CONVERSATION_RECALL_MESSAGE)) {
                onMessageRecalled(pushServiceParcel, patchTime, callback);
              } else {
                onMessageUpdated(pushServiceParcel, patchTime, callback);
              }
            } else {
              callback.internalDone(new AVException(error));
            }
          }
        }, new IntentFilter(operation.getOperation() + requestId));
    }
  }

  private void onMessageRecalled(PushServiceParcel pushServiceParcel, long patchTime, AVCallback callback) {
    AVIMMessage oldMessage = pushServiceParcel.getRecallMessage();
    AVIMMessage recalledMessage = new AVIMRecalledMessage();
    copyMessageWithoutContent(oldMessage, recalledMessage);
    recalledMessage.setUpdateAt(patchTime);
    recalledMessage.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusRecalled);
    updateLocalMessage(recalledMessage);
    callback.internalDone(recalledMessage, null);
  }

  private void onMessageUpdated(PushServiceParcel pushServiceParcel, long patchTime, AVCallback callback) {
    AVIMMessage oldMessage = pushServiceParcel.getOldMessage();
    AVIMMessage newMessage = pushServiceParcel.getNewMessage();
    copyMessageWithoutContent(oldMessage, newMessage);
    newMessage.setUpdateAt(patchTime);
    updateLocalMessage(newMessage);
    callback.internalDone(newMessage, null);
  }

  private void copyMessageWithoutContent(AVIMMessage oldMessage, AVIMMessage newMessage) {
    newMessage.setMessageId(oldMessage.getMessageId());
    newMessage.setConversationId(oldMessage.getConversationId());
    newMessage.setFrom(oldMessage.getFrom());
    newMessage.setDeliveredAt(oldMessage.getDeliveredAt());
    newMessage.setReadAt(oldMessage.getReadAt());
    newMessage.setTimestamp(oldMessage.getTimestamp());
    newMessage.setMessageStatus(oldMessage.getMessageStatus());
    newMessage.setMessageIOType(oldMessage.getMessageIOType());
  }

  private void sendCMDToPushService(String dataInString,
                                    final AVIMOperation operation,
                                    final AVCallback callback, final OperationCompleteCallback occ) {
    sendCMDToPushService(dataInString, null, null, operation, callback, occ);
  }

  protected void sendCMDToPushService(String dataInString, final AVIMOperation operation,
                                    AVCallback callback) {
    sendCMDToPushService(dataInString, null, null, operation, callback, null);
  }

  private void sendNonSideEffectCommand(String dataInString, final AVIMOperation operation, AVCallback callback) {
    if (null == callback) {
      return;
    }
    final int requestId = AVUtils.getNextIMRequestId();
    Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    if (!AVUtils.isBlankString(dataInString)) {
      i.putExtra(Conversation.INTENT_KEY_DATA, dataInString);
    }
    i.putExtra(Conversation.INTENT_KEY_CLIENT, client.clientId);
    i.putExtra(Conversation.INTENT_KEY_CONVERSATION, this.conversationId);
    i.putExtra(Conversation.INTENT_KEY_CONV_TYPE, this.getType());
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
    LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(
        new AVIMBaseBroadcastReceiver(callback) {
          @Override
          public void execute(Intent intent, Throwable ex) {
            // 处理历史消息查询
            if (operation.getCode() == AVIMOperation.CONVERSATION_MESSAGE_QUERY.getCode()) {
              List<AVIMMessage> historyMessages =
                  intent.getParcelableArrayListExtra(Conversation.callbackHistoryMessages);

              if (ex != null) {
                callback.internalDone(null, new AVIMException(ex));
              } else {
                if (historyMessages == null) {
                  historyMessages = Collections.EMPTY_LIST;
                }
                callback.internalDone(historyMessages, null);
              }
              return;
            }
          }
        }, new IntentFilter(operation.getOperation() + requestId));
  }

  private void sendCMDToPushService(String dataInString, final AVIMMessage message, final AVIMMessageOption messageOption, final AVIMOperation operation,
                                    final AVCallback callback, final OperationCompleteCallback occ) {
    final int requestId = AVUtils.getNextIMRequestId();
    Intent i = new Intent(AVOSCloud.applicationContext, PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    if (!AVUtils.isBlankString(dataInString)) {
      i.putExtra(Conversation.INTENT_KEY_DATA, dataInString);
    }
    if (message != null) {
      i.putExtra(Conversation.INTENT_KEY_DATA, message);
      if (null != messageOption) {
        i.putExtra(Conversation.INTENT_KEY_MESSAGE_OPTION, messageOption);

        // 消息等级仅针对聊天室有效，如非聊天室，此处打印 log 提示
        if (!isTransient && null != messageOption.getPriority() && AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.d("Message priority is invalid in transient conversation");
        }
      }
    }
    i.putExtra(Conversation.INTENT_KEY_CLIENT, client.clientId);
    i.putExtra(Conversation.INTENT_KEY_CONVERSATION, this.conversationId);
    i.putExtra(Conversation.INTENT_KEY_CONV_TYPE, this.getType());
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);

    AVOSCloud.applicationContext.startService(IntentUtil.setupIntentFlags(i));
    if (callback != null) {
      LocalBroadcastManager.getInstance(AVOSCloud.applicationContext).registerReceiver(
          new AVIMBaseBroadcastReceiver(callback) {
            @Override
            public void execute(Intent intent, Throwable error) {
              // 处理退出命令时
              if (operation.getCode() == AVIMOperation.CONVERSATION_QUIT.getCode()) {
                client.removeConversationCache(AVIMConversation.this);
              }
              // 处理side effect调用
              if (error == null && occ != null) {
                occ.onComplete();
              } else if (error != null && occ != null) {
                occ.onFailure();
              }
              // 消息命令
              if (message != null) {
                if (error == null) {
                  // 处理发送成功的消息
                  long timestamp =
                      intent.getExtras().getLong(Conversation.callbackMessageTimeStamp, -1);
                  String messageId = intent.getStringExtra(Conversation.callbackMessageId);
                  message.setMessageId(messageId);
                  message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSent);
                  if (timestamp != -1) {
                    message.setTimestamp(timestamp);
                  }
                  if ((null == messageOption || !messageOption.isTransient())
                      && AVIMClient.messageQueryCacheEnabled) {
                    setLastMessage(message);
                    storage.insertMessage(message, false);
                  } else {
                    LogUtil.avlog.d("skip inserting into local storage.");
                  }
                  AVIMConversation.this.lastMessageAt = new Date(timestamp);
                  storage.updateConversationLastMessageAt(AVIMConversation.this);
                } else {
                  message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusFailed);
                }
              }
              // 处理历史消息查询
              if (operation.getCode() == AVIMOperation.CONVERSATION_MESSAGE_QUERY.getCode()) {
                List<AVIMMessage> historyMessages =
                    intent.getParcelableArrayListExtra(Conversation.callbackHistoryMessages);

                if (error != null) {
                  // modify by jwfing[2017/8/23] not return exception to external caller.
                  callback.internalDone(null, new AVIMException(AVIMException.TIMEOUT, AVException.CONNECTION_FAILED, error.getMessage()));
                } else {
                  setLastReadAt(intent.getLongExtra(Conversation.callbackReadAt, 0L), false);
                  setLastDeliveredAt(intent.getLongExtra(Conversation.callbackDeliveredAt, 0L), false);
                  storage.updateConversationTimes(AVIMConversation.this);
                  if (historyMessages == null) {
                    historyMessages = Collections.EMPTY_LIST;
                  }
                  callback.internalDone(historyMessages, null);
                }
                return;
              }
              // 刷新Conversation 更新时间
              if (operation.getCode() == AVIMOperation.CONVERSATION_UPDATE.getCode()) {
                if (intent.getExtras().containsKey(Conversation.callbackUpdatedAt)) {
                  String updatedAt = intent.getExtras().getString(Conversation.callbackUpdatedAt);
                  AVIMConversation.this.updatedAt = updatedAt;
                }
              }

              // 处理成员数量查询
              if (operation.getCode() == AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY.getCode()) {
                int memberCount = intent.getIntExtra(Conversation.callbackMemberCount, 0);
                callback.internalDone(memberCount, AVIMException.wrapperAVException(error));
                return;
              }

              if (operation.getCode() == AVIMOperation.CONVERSATION_FETCH_RECEIPT_TIME.getCode()) {
                setLastReadAt(intent.getLongExtra(Conversation.callbackReadAt, 0L), false);
                setLastDeliveredAt(intent.getLongExtra(Conversation.callbackDeliveredAt, 0L), false);
                storage.updateConversationTimes(AVIMConversation.this);
                callback.internalDone(null, AVIMException.wrapperAVException(error));
                return;
              }

              // 处理对成员禁言/拉黑系操作
              if (operation.getCode() == AVIMOperation.CONVERSATION_MUTE_MEMBER.getCode()
                  || operation.getCode() == AVIMOperation.CONVERSATION_UNMUTE_MEMBER.getCode()
                  || operation.getCode() == AVIMOperation.CONVERSATION_BLOCK_MEMBER.getCode()
                  || operation.getCode() == AVIMOperation.CONVERSATION_UNBLOCK_MEMBER.getCode()) {
                String[] allowedList = intent.getStringArrayExtra(Conversation.callbackConvMemberMuted_SUCC);
                ArrayList<AVIMOperationFailure> failedList = intent.getParcelableArrayListExtra(Conversation.callbackConvMemberMuted_FAIL);
                Map<String, Object> result = new HashMap<>();
                result.put(Conversation.callbackConvMemberMuted_SUCC, allowedList);
                result.put(Conversation.callbackConvMemberMuted_FAIL, failedList);
                callback.internalDone(result, AVIMException.wrapperAVException(error));
                return;
              }

              // 处理被禁言成员列表查询
              if (operation.getCode() == AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY.getCode()
                  || operation.getCode() == AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY.getCode()) {
                String[] result = intent.getStringArrayExtra(Conversation.callbackData);
                callback.internalDone(null != result? Arrays.asList(result): null, AVIMException.wrapperAVException(error));
                return;
              }

              if (operation.getCode() == AVIMOperation.CONVERSATION_QUERY.getCode()) {
                if (null != error) {
                  callback.internalDone(null, AVIMException.wrapperAVException(error));
                } else {
                  Exception exception = processQueryResult(intent.getExtras().getSerializable(Conversation.callbackData));
                  callback.internalDone(null, AVIMException.wrapperAVException(exception));
                }
                return;
              }
              callback.internalDone(null, AVIMException.wrapperAVException(error));
            }
          }, new IntentFilter(operation.getOperation() + requestId));
    }
  }

  /**
   * 处理 query 的返回结果
   * @param serializable
   * @return
   */
  private Exception processQueryResult(Serializable serializable) {
    if (null != serializable) {
      try {
        String result = (String)serializable;
        JSONArray jsonArray = JSON.parseArray(String.valueOf(result));
        if (null != jsonArray && !jsonArray.isEmpty()) {
          updateConversation(this, jsonArray.getJSONObject(0));
          client.conversationCache.put(conversationId, this);
          storage.insertConversations(Arrays.asList(this));
        }
      } catch (Exception e) {
        return e;
      }
    }
    return null;
  }

  /**
   * parse AVIMConversation from jsonObject
   * @param client
   * @param jsonObj
   * @return
   */
  public static AVIMConversation parseFromJson(AVIMClient client, JSONObject jsonObj) {
    if (null == jsonObj || null == client) {
      return null;
    }

    String conversationId = jsonObj.getString(AVObject.OBJECT_ID);
    if (AVUtils.isBlankContent(conversationId)) {
      return null;
    }
    boolean systemConv = false;
    boolean transientConv = false;
    boolean tempConv = false;
    if (jsonObj.containsKey(Conversation.SYSTEM)) {
      systemConv = jsonObj.getBoolean(Conversation.SYSTEM);
    }
    if (jsonObj.containsKey(Conversation.TRANSIENT)) {
      transientConv = jsonObj.getBoolean(Conversation.TRANSIENT);
    }
    if (jsonObj.containsKey(Conversation.TEMPORARY)) {
      tempConv = jsonObj.getBoolean(Conversation.TEMPORARY);
    }
    AVIMConversation originConv = null;
    if (systemConv) {
      originConv = new AVIMServiceConversation(client, conversationId);
    } else if (tempConv) {
      originConv = new AVIMTemporaryConversation(client, conversationId);
    } else if (transientConv) {
      originConv = new AVIMChatRoom(client, conversationId);
    } else {
      originConv = new AVIMConversation(client, conversationId);
    }

    return updateConversation(originConv, jsonObj);
  }

  private static AVIMConversation updateConversation(AVIMConversation conversation, JSONObject jsonObj) {
    if (null == jsonObj || null == conversation) {
      return conversation;
    }

    String conversationId = jsonObj.getString(AVObject.OBJECT_ID);
    List<String> m = jsonObj.getObject(Conversation.MEMBERS, List.class);
    conversation.setMembers(m);
    conversation.setCreator(jsonObj.getString(Conversation.CREATOR));
    HashMap<String, Object> attributes = new HashMap<String, Object>();
    if (jsonObj.containsKey(Conversation.NAME)) {
      attributes.put(Conversation.NAME, jsonObj.getString(Conversation.NAME));
    }
    if (jsonObj.containsKey(Conversation.ATTRIBUTE)) {
      JSONObject moreAttributes = jsonObj.getJSONObject(Conversation.ATTRIBUTE);
      if (moreAttributes != null) {
        Map<String, Object> moreAttributesMap = JSON.toJavaObject(moreAttributes, Map.class);
        attributes.putAll(moreAttributesMap);
      }
    }
    conversation.setAttributesForInit(attributes);

    conversation.instanceData.clear();
    Set<String> keySet = jsonObj.keySet();
    if (!keySet.isEmpty()) {
      for (String key : keySet) {
        if (!Arrays.asList(Conversation.CONVERSATION_COLUMNS).contains(key)) {
          conversation.instanceData.put(key, jsonObj.get(key));
        }
      }
    }

    if (jsonObj.containsKey(Conversation.SYSTEM)) {
      conversation.instanceData.put(Conversation.SYSTEM, jsonObj.get(Conversation.SYSTEM));
    }

    if (jsonObj.containsKey(Conversation.MUTE)) {
      conversation.instanceData.put(Conversation.MUTE, jsonObj.get(Conversation.MUTE));
    }

    if (jsonObj.containsKey(AVObject.CREATED_AT)) {
      conversation.setCreatedAt(jsonObj.getString(AVObject.CREATED_AT));
    }

    if (jsonObj.containsKey(AVObject.UPDATED_AT)) {
      conversation.setUpdatedAt(jsonObj.getString(AVObject.UPDATED_AT));
    }

    AVIMMessage message = AVIMTypedMessage.parseMessage(conversationId, jsonObj);
    conversation.setLastMessage(message);

    if (jsonObj.containsKey(Conversation.LAST_MESSAGE_AT)) {
      conversation.setLastMessageAt(AVUtils.dateFromMap(jsonObj.getObject(Conversation.LAST_MESSAGE_AT, Map.class)));
    }

    if (jsonObj.containsKey(Conversation.TRANSIENT)) {
      conversation.isTransient = jsonObj.getBoolean(Conversation.TRANSIENT);
    }
    return conversation;
  }

  /**
   * 处理 AVIMConversation attr 列
   * 因为 sdk 支持增量更新与覆盖更新，而增量更新与覆盖更新需要的结构不一样，所以这里处理一下
   * 具体格式可参照下边的注释，注意，两种格式不能同时存在，否则 server 会报错
   * @param attributes
   * @param isCovered
   * @return
   */
  static Map<String, Object> processAttributes(Map<String, Object> attributes, boolean isCovered) {
    if (isCovered) {
      return processAttributesForCovering(attributes);
    } else {
      return processAttributesForIncremental(attributes);
    }
  }

  /**
   * 增量更新 attributes
   * 这里处理完的格式应该类似为 {"attr.key1":"value2", "attr.key2":"value2"}
   * @param attributes
   * @return
   */
  static Map<String, Object> processAttributesForIncremental(Map<String, Object> attributes) {
    Map<String, Object> attributeMap = new HashMap<>();
    if (attributes.containsKey(Conversation.NAME)) {
      attributeMap.put(Conversation.NAME, attributes.get(Conversation.NAME));
    }
    for (String k : attributes.keySet()) {
      if (!Arrays.asList(Conversation.CONVERSATION_COLUMNS).contains(k)) {
        attributeMap.put(ATTR_PERFIX + k, attributes.get(k));
      }
    }
    if (attributeMap.isEmpty()) {
      return null;
    }
    return attributeMap;
  }

  /**
   * 覆盖更新 attributes
   * 这里处理完的格式应该类似为 {"attr":{"key1":"value1","key2":"value2"}}
   * @param attributes
   * @return
   */
  static JSONObject processAttributesForCovering(Map<String, Object> attributes) {
    HashMap<String, Object> attributeMap = new HashMap<String, Object>();
    if (attributes.containsKey(Conversation.NAME)) {
      attributeMap.put(Conversation.NAME,
        attributes.get(Conversation.NAME));
    }
    Map<String, Object> innerAttribute = new HashMap<String, Object>();
    for (String k : attributes.keySet()) {
      if (!Arrays.asList(Conversation.CONVERSATION_COLUMNS).contains(k)) {
        innerAttribute.put(k, attributes.get(k));
      }
    }
    if (!innerAttribute.isEmpty()) {
      attributeMap.put(Conversation.ATTRIBUTE, innerAttribute);
    }
    if (attributeMap.isEmpty()) {
      return null;
    }
    return new JSONObject(attributeMap);
  }

  static Comparator<AVIMMessage> messageComparator = new Comparator<AVIMMessage>() {
    @Override
    public int compare(AVIMMessage m1, AVIMMessage m2) {
      if (m1.getTimestamp() < m2.getTimestamp()) {
        return -1;
      } else if (m1.getTimestamp() > m2.getTimestamp()) {
        return 1;
      } else {
        return m1.messageId.compareTo(m2.messageId);
      }
    }
  };

  interface OperationCompleteCallback {
    void onComplete();

    void onFailure();
  }
}
