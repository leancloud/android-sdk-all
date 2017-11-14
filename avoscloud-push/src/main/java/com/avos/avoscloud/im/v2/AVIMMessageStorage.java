package com.avos.avoscloud.im.v2;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.im.v2.AVIMMessage.AVIMMessageStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lbt05 on 4/13/15.
 */
@TargetApi(8)
class AVIMMessageStorage {
  static final int MESSAGE_INNERTYPE_BIN = 1;
  static final int MESSAGE_INNERTYPE_PLAIN = 0;
  static final String DB_NAME_PREFIX = "com.avos.avoscloud.im.v2.";
  static final String MESSAGE_TABLE = "messages";
  static final String MESSAGE_INDEX = "message_index";
  static final int DB_VERSION = 9;
  static final String COLUMN_MESSAGE_ID = "message_id";
  static final String COLUMN_TIMESTAMP = "timestamp";
  static final String COLUMN_CONVERSATION_ID = "conversation_id";
  static final String COLUMN_FROM_PEER_ID = "from_peer_id";
  static final String COLUMN_MESSAGE_DELIVEREDAT = "receipt_timestamp";
  static final String COLUMN_MESSAGE_READAT = "readAt";
  static final String COLUMN_MESSAGE_UPDATEAT = "updateAt";
  static final String COLUMN_PAYLOAD = "payload";
  static final String COLUMN_STATUS = "status";
  static final String COLUMN_BREAKPOINT = "breakpoint";
  static final String COLUMN_DEDUPLICATED_TOKEN = "dtoken";
  static final String COLUMN_MSG_MENTION_ALL = "mentionAll";
  static final String COLUMN_MSG_MENTION_LIST = "mentionList";
  static final String COLUMN_MSG_INNERTYPE = "iType";

  static final String CONVERSATION_TABLE = "conversations";
  static final String COLUMN_EXPIREAT = "expireAt";
  static final String COLUMN_ATTRIBUTE = "attr";
  static final String COLUMN_INSTANCEDATA = "instanceData";
  static final String COLUMN_UPDATEDAT = "updatedAt";
  static final String COLUMN_CREATEDAT = "createdAt";
  static final String COLUMN_CREATOR = "creator";
  static final String COLUMN_MEMBERS = "members";
  static final String COLUMN_LM = "lm";
  static final String COLUMN_LASTMESSAGE = "last_message";
  static final String COLUMN_TRANSIENT = "isTransient";
  static final String COLUMN_UNREAD_COUNT = "unread_count";
  static final String COLUMN_CONV_MENTIONED = "mentioned";
  static final String COLUMN_CONVERSATION_READAT = "readAt";
  static final String COLUMN_CONVRESATION_DELIVEREDAT = "deliveredAt";
  static final String COLUMN_CONV_LASTMESSAGE_INNERTYPE = "last_msg_iType";

  static final String NUMBERIC = "NUMBERIC";
  static final String INTEGER = "INTEGER";
  static final String BLOB = "BLOB";
  static final String TEXT = "TEXT";
  static final String VARCHAR32 = "VARCHAR(32)";

  /**
   * table: messages
   * schema:
   * |--------------------------------------------------------------------------------------------------------------------------------|
   * |varchar    |numberic  |varchar         |text         |numberic          |number |number    |blob    |int    |int        |varchar|
   * |message_id |timestamp |conversation_id |from_peer_id |receipt_timestamp |readAt |updatedAt |payload |status |breakpoint |dtoken |
   * |not null   |          |not null        |not null     |                  |       |          |        |       |           |       |
   * |   *       |          |    *           |   <-- primary key                                                                      |
   * |--------------------------------------------------------------------------------------------------------------------------------|
   *
   * table: conversations
   * schema:
   * |---------------------------------------------------------------------------------------------------------------------------------------|
   * |expireAt |attr |instanceData |updatedAt |createdAt |creator |members |lm |last_message |isTransient |unread_count |readAt |deliveredAt |
   * |---------------------------------------------------------------------------------------------------------------------------------------|
   *
   */
  static class SQL {
    static final String TIMESTAMP_MORE_OR_TIMESTAMP_EQUAL_BUT_MESSAGE_ID_MORE_AND_CONVERSATION_ID =
        " ( " +
            COLUMN_TIMESTAMP + " > ? or (" + COLUMN_TIMESTAMP + " = ? and " + COLUMN_MESSAGE_ID
            + " > ? )) and " +
            COLUMN_CONVERSATION_ID + " = ? ";

    static final String TIMESTAMP_LESS_AND_CONVERSATION_ID = COLUMN_TIMESTAMP + " < ? and "
        + COLUMN_CONVERSATION_ID + " = ? ";

    // 在时间戳第一排序，MessageId 第二排序的情况下，找到时间戳小于，或者时间戳等于但MessageId小于的消息
    // 三条消息(时间戳/MessageId) 2/a、1/a、1/b， 则用 1/b 来找历史消息的时候，返回1/a
    static final String TIMESTAMP_LESS_OR_TIMESTAMP_EQUAL_BUT_MESSAGE_ID_LESS_AND_CONVERSATION_ID =
        " ( " +
            COLUMN_TIMESTAMP + " < ? or (" + COLUMN_TIMESTAMP + " = ? and " + COLUMN_MESSAGE_ID
            + " < ? )) and " +
            COLUMN_CONVERSATION_ID + " = ? ";

    static final String ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC =
        COLUMN_TIMESTAMP + " desc, " + COLUMN_MESSAGE_ID + " desc";

    static final String ORDER_BY_TIMESTAMP_ASC_THEN_MESSAGE_ID_ASC =
        COLUMN_TIMESTAMP + " , " + COLUMN_MESSAGE_ID;

    static final String DELETE_LOCAL_MESSAGE = COLUMN_CONVERSATION_ID + " = ? and " + COLUMN_MESSAGE_ID + " = ? and "
        + COLUMN_STATUS + " = ? and " + COLUMN_DEDUPLICATED_TOKEN + " = ? ";
  }

  private DBHelper dbHelper;
  private static ConcurrentHashMap<String, AVIMMessageStorage> storages =
      new ConcurrentHashMap<String, AVIMMessageStorage>();

  static class DBHelper extends SQLiteOpenHelper {
    static final String MESSAGE_CREATE_SQL =
        "CREATE TABLE IF NOT EXISTS " + MESSAGE_TABLE + " ("
            + COLUMN_CONVERSATION_ID + " VARCHAR(32) NOT NULL, "
            + COLUMN_MESSAGE_ID + " VARCHAR(32) NOT NULL, "
            + COLUMN_TIMESTAMP + " NUMBERIC, "
            + COLUMN_FROM_PEER_ID + " TEXT NOT NULL, "
            + COLUMN_MESSAGE_DELIVEREDAT + " NUMBERIC, "
            + COLUMN_MESSAGE_READAT + " NUMBERIC, "
            + COLUMN_MESSAGE_UPDATEAT + " NUMBERIC, "
            + COLUMN_PAYLOAD + " BLOB, "
            + COLUMN_STATUS + " INTEGER, "
            + COLUMN_BREAKPOINT + " INTEGER, "
            + COLUMN_DEDUPLICATED_TOKEN + " VARCHAR(32), "
            + COLUMN_MSG_MENTION_ALL + " INTEGER default 0, "
            + COLUMN_MSG_MENTION_LIST + " TEXT NULL, "
            + COLUMN_MSG_INNERTYPE + " INTEGER default 0, "
            + "PRIMARY KEY(" + COLUMN_CONVERSATION_ID + "," + COLUMN_MESSAGE_ID + ")) ";

    static final String MESSAGE_UNIQUE_INDEX_SQL =
        "CREATE UNIQUE INDEX IF NOT EXISTS " + MESSAGE_INDEX + " on " + MESSAGE_TABLE + " ("
            + COLUMN_CONVERSATION_ID + ", " + COLUMN_TIMESTAMP + ", " + COLUMN_MESSAGE_ID + ") ";

    static final String CONVERSATION_CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
        + CONVERSATION_TABLE + " ("
        + COLUMN_CONVERSATION_ID + " VARCHAR(32) NOT NULL,"
        + COLUMN_EXPIREAT + " NUMBERIC,"
        + COLUMN_ATTRIBUTE + " BLOB,"
        + COLUMN_INSTANCEDATA + " BLOB,"
        + COLUMN_UPDATEDAT + " VARCHAR(32),"
        + COLUMN_CREATEDAT + " VARCHAR(32),"
        + COLUMN_CREATOR + " TEXT,"
        + COLUMN_MEMBERS + " TEXT,"
        + COLUMN_TRANSIENT + " INTEGER,"
        + COLUMN_UNREAD_COUNT + " INTEGER,"
        + COLUMN_CONVERSATION_READAT + " NUMBERIC,"
        + COLUMN_CONVRESATION_DELIVEREDAT + " NUMBERIC,"
        + COLUMN_LM + " NUMBERIC,"
        + COLUMN_LASTMESSAGE + " TEXT,"
        + COLUMN_CONV_MENTIONED + " INTEGER default 0,"
        + COLUMN_CONV_LASTMESSAGE_INNERTYPE + " INTEGER default 0, "
        + "PRIMARY KEY(" + COLUMN_CONVERSATION_ID + "))";

    public DBHelper(Context context, String clientId) {
      super(context, getDatabasePath(clientId), null, DB_VERSION);
    }

    private static String getDatabasePath(String clientId) {
      // 要 MD5 ?
      return DB_NAME_PREFIX + clientId;
    }

    private static String getAddColumnSql(String table, String column, String type) {
      return String.format("ALTER TABLE %s ADD COLUMN %s %s;", table, column, type);
    }

    private static String getAddColumnSql(String table, String column, String type, String defaultV) {
      return String.format("ALTER TABLE %s ADD COLUMN %s %s default %s;", table, column, type, defaultV);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
      sqLiteDatabase.execSQL(MESSAGE_CREATE_SQL);
      sqLiteDatabase.execSQL(MESSAGE_UNIQUE_INDEX_SQL);
      sqLiteDatabase.execSQL(CONVERSATION_CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
      if (oldVersion == 1) {
        upgradeToVersion2(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 2) {
        upgradeToVersion3(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 3) {
        upgradeToVersion4(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 4) {
        upgradeToVersion5(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 5) {
        upgradeToVersion6(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 6) {
        upgradeToVersion7(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 7) {
        upgradeToVersion8(sqLiteDatabase);
        oldVersion += 1;
      }
      if (oldVersion == 8) {
        upgradeToVersion9(sqLiteDatabase);
        oldVersion += 1;
      }
    }

    private void upgradeToVersion2(SQLiteDatabase db) {
      db.execSQL(CONVERSATION_CREATE_SQL);
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
      try {
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_DEDUPLICATED_TOKEN)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_DEDUPLICATED_TOKEN, VARCHAR32));
        }
      } catch (Exception e) {}
    }

    private void upgradeToVersion4(SQLiteDatabase db) {
      try {
        if (!columnExists(db, CONVERSATION_TABLE, COLUMN_LASTMESSAGE)) {
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_LASTMESSAGE, TEXT));
        }
      } catch (Exception e) {}
    }

    private void upgradeToVersion5(SQLiteDatabase db) {
      try {
        if (!columnExists(db, CONVERSATION_TABLE, COLUMN_INSTANCEDATA)) {
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_INSTANCEDATA, BLOB));
        }
      } catch (Exception e) {
      }
    }

    private void upgradeToVersion6(SQLiteDatabase db) {
      try {
        if (!columnExists(db, CONVERSATION_TABLE, COLUMN_UNREAD_COUNT)) {
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_UNREAD_COUNT, INTEGER));
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_CONVERSATION_READAT, NUMBERIC));
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_CONVRESATION_DELIVEREDAT, NUMBERIC));
        }
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_MESSAGE_READAT)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_MESSAGE_READAT, NUMBERIC));
        }
      } catch (Exception e) {
      }
    }

    private void upgradeToVersion7(SQLiteDatabase db) {
      try {
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_MESSAGE_UPDATEAT)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_MESSAGE_UPDATEAT, NUMBERIC));
        }
      } catch (Exception e) {
      }
    }

    private void upgradeToVersion8(SQLiteDatabase db) {
      try {
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_MSG_MENTION_ALL)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_MSG_MENTION_ALL, INTEGER, "0"));
        }
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_MSG_MENTION_LIST)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_MSG_MENTION_LIST, TEXT));
        }
        if (!columnExists(db, CONVERSATION_TABLE, COLUMN_CONV_MENTIONED)) {
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_CONV_MENTIONED, INTEGER, "0"));
        }
      } catch (Exception e) {
      }
    }

    private void upgradeToVersion9(SQLiteDatabase db) {
      try {
        if (!columnExists(db, MESSAGE_TABLE, COLUMN_MSG_INNERTYPE)) {
          db.execSQL(getAddColumnSql(MESSAGE_TABLE, COLUMN_MSG_INNERTYPE, INTEGER, "0"));
        }
        if (!columnExists(db, CONVERSATION_TABLE, COLUMN_CONV_LASTMESSAGE_INNERTYPE)) {
          db.execSQL(getAddColumnSql(CONVERSATION_TABLE, COLUMN_CONV_LASTMESSAGE_INNERTYPE, INTEGER, "0"));
        }
      } catch (Exception e) {
      }
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String column) {
      try {
        Cursor cursor = db.query(table, null, null, null, null, null, null);
        return cursor.getColumnIndex(column) != -1;
      } catch (Exception e) {
        return false;
      }
    }
  }

  String clientId;

  private AVIMMessageStorage(Context context, String clientId) {
    dbHelper = new DBHelper(context, clientId);
    
    // FIXME: 2017/8/22 it is not need to invoke onUpgrade manually.
    dbHelper.onUpgrade(dbHelper.getWritableDatabase(), dbHelper.getWritableDatabase().getVersion(),
        DB_VERSION);
    this.clientId = clientId;
  }

  public synchronized static AVIMMessageStorage getInstance(String clientId) {
    AVIMMessageStorage storage = storages.get(clientId);
    if (storage != null) {
      return storage;
    } else {
      storage = new AVIMMessageStorage(AVOSCloud.applicationContext, clientId);
      AVIMMessageStorage elderStorage = storages.putIfAbsent(clientId, storage);
      return elderStorage == null ? storage : elderStorage;
    }
  }

  private static String getWhereClause(String... columns) {
    List<String> conditions = new ArrayList<String>();
    for (String column : columns) {
      conditions.add(column + " = ? ");
    }
    return TextUtils.join(" and ", conditions);
  }

  public void insertMessage(AVIMMessage message, boolean breakpoint) {
    if (null == message) {
      return;
    }
    insertMessages(Arrays.asList(message), breakpoint);
  }

  /*
   * 这个代码是用于插入由本地发送的消息，由于存在deduplicated Message 所以有些特殊，需要做额外的token检查
   */
  public synchronized boolean insertLocalMessage(AVIMMessage message) {
    if (null == message
        || !AVUtils.isBlankString(message.getMessageId())
        || AVUtils.isBlankString(message.conversationId)
        || AVUtils.isBlankString(message.uniqueToken)) {
      if (null == message) {
        LogUtil.avlog.e("message is null");
      } else {
        LogUtil.avlog.e(String.format("invalid state. msgId=%s, convId=%s, uniToken=%s", message.getMessageId(), message.conversationId, message.uniqueToken));
      }
      return false;
    }

    String internalMessageId = generateInternalMessageId(message.uniqueToken);
    try {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      ContentValues values = new ContentValues();
      values.put(COLUMN_CONVERSATION_ID, message.conversationId);
      values.put(COLUMN_MESSAGE_ID, internalMessageId);
      values.put(COLUMN_TIMESTAMP, message.getTimestamp());
      values.put(COLUMN_FROM_PEER_ID, message.getFrom());
      if (message instanceof AVIMBinaryMessage) {
        values.put(COLUMN_PAYLOAD, ((AVIMBinaryMessage)message).getBytes());
        values.put(COLUMN_MSG_INNERTYPE, MESSAGE_INNERTYPE_BIN);
      } else {
        values.put(COLUMN_PAYLOAD, message.getContent().getBytes());
      }
      values.put(COLUMN_MESSAGE_DELIVEREDAT, message.getDeliveredAt());
      values.put(COLUMN_MESSAGE_READAT, message.getReadAt());
      values.put(COLUMN_MESSAGE_UPDATEAT, message.getUpdateAt());
      values.put(COLUMN_STATUS, AVIMMessageStatus.AVIMMessageStatusFailed.getStatusCode());
      values.put(COLUMN_BREAKPOINT, 0);
      values.put(COLUMN_DEDUPLICATED_TOKEN, message.uniqueToken);

      values.put(COLUMN_MSG_MENTION_ALL, message.isMentionAll()? 1: 0);
      values.put(COLUMN_MSG_MENTION_LIST, message.getMentionListString());

      long ret = db.insertWithOnConflict(MESSAGE_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
      if (ret < 0) {
        LogUtil.avlog.e("failed to insert Message table. values=" + values.toString());
      }
      return ret != -1;
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * remove local message.
   *
   * @param message
   */
  public synchronized boolean removeLocalMessage(AVIMMessage message) {
    if (null == message
        || AVUtils.isBlankString(message.conversationId)
        || AVUtils.isBlankString(message.uniqueToken)) {
      return false;
    }
    String internalMessageId = generateInternalMessageId(message.uniqueToken);
    String status = String.valueOf(AVIMMessageStatus.AVIMMessageStatusFailed.getStatusCode());
    try {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      int ret = db.delete(MESSAGE_TABLE, SQL.DELETE_LOCAL_MESSAGE,
          new String[]{message.conversationId, internalMessageId, status, message.uniqueToken});
      return ret > 0;
    } catch (Exception ex) {
      return false;
    }
  }

  private String generateInternalMessageId(String uniqueToken) {
    if (AVUtils.isBlankString(uniqueToken)) {
      return "";
    }
    return uniqueToken;
  }

  private synchronized int insertMessages(List<AVIMMessage> messages, boolean breakpoint) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    int insertCount = 0;
    for (AVIMMessage message : messages) {
      ContentValues values = new ContentValues();
      values.put(COLUMN_CONVERSATION_ID, message.getConversationId());
      values.put(COLUMN_MESSAGE_ID, message.getMessageId());
      values.put(COLUMN_TIMESTAMP, message.getTimestamp());
      values.put(COLUMN_FROM_PEER_ID, message.getFrom());
      if (message instanceof AVIMBinaryMessage) {
        values.put(COLUMN_PAYLOAD, ((AVIMBinaryMessage)message).getBytes());
        values.put(COLUMN_MSG_INNERTYPE, MESSAGE_INNERTYPE_BIN);
      } else {
        values.put(COLUMN_PAYLOAD, message.getContent().getBytes());
        values.put(COLUMN_MSG_INNERTYPE, MESSAGE_INNERTYPE_PLAIN);
      }
      values.put(COLUMN_MESSAGE_DELIVEREDAT, message.getDeliveredAt());
      values.put(COLUMN_MESSAGE_READAT, message.getReadAt());
      values.put(COLUMN_MESSAGE_UPDATEAT, message.getUpdateAt());
      values.put(COLUMN_STATUS, message.getMessageStatus().getStatusCode());
      values.put(COLUMN_BREAKPOINT, breakpoint ? 1 : 0);

      values.put(COLUMN_MSG_MENTION_ALL, message.isMentionAll()? 1: 0);
      values.put(COLUMN_MSG_MENTION_LIST, message.getMentionListString());

      try {
        long itemId =
            db.insertWithOnConflict(MESSAGE_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        boolean insert = itemId > -1;
        if (insert) {
          insertCount++;
        }
      } catch (SQLException e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          e.printStackTrace();
        }
      }
    }
    db.setTransactionSuccessful();
    db.endTransaction();
    return insertCount;
  }

  /**
   * 顺序由调用者保证，需要按照时间升序排列
   * @param messages
   * @param conversationId
   */
  public void insertContinuousMessages(List<AVIMMessage> messages, String conversationId) {
    if (null == messages || messages.isEmpty() || AVUtils.isBlankString(conversationId)) {
      return;
    }

    AVIMMessage firstMessage = messages.get(0);
    List<AVIMMessage> tailMessages = messages.subList(1, messages.size());
    AVIMMessage lastMessage = messages.get(messages.size() - 1);
    if (!containMessage(lastMessage)) {
      AVIMMessage eldestMessage = getNextMessage(lastMessage);
      if (eldestMessage != null) {
        updateBreakpoints(Arrays.asList(eldestMessage), true, conversationId);
      }
    }
    if (!tailMessages.isEmpty()) {
      insertMessages(tailMessages, false);
      // remove breakpoints
      updateBreakpoints(tailMessages, false, conversationId);
    }
    insertMessage(firstMessage, true);
  }

  public boolean containMessage(AVIMMessage message) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.query(MESSAGE_TABLE, new String[] {},
        getWhereClause(COLUMN_CONVERSATION_ID, COLUMN_MESSAGE_ID),
        new String[] {message.conversationId, message.getMessageId()}, null, null, null);
    boolean contained = cursor.getCount() > 0;
    cursor.close();
    return contained;
  }

  protected synchronized void updateBreakpoints(List<AVIMMessage> messages,
      boolean breakpoint, String conversationId) {
    // sqlite max ? variable size = 999
    int batchSize = 900;
    if (messages.size() > batchSize) {
      updateBreakpointsForBatch(messages.subList(0, batchSize), breakpoint, conversationId);
      updateBreakpoints(messages.subList(batchSize, messages.size()), breakpoint, conversationId);
    } else {
      updateBreakpointsForBatch(messages, breakpoint, conversationId);
    }
  }

  private synchronized int updateBreakpointsForBatch(List<AVIMMessage> messages,
      boolean breakpoint, String conversationId) {
    String[] arguments = new String[messages.size()];
    List<String> placeholders = new ArrayList<String>();
    int i;
    for (i = 0; i < messages.size(); i++) {
      AVIMMessage message = messages.get(i);
      arguments[i] = message.getMessageId();
      placeholders.add("?");
    }
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put(COLUMN_BREAKPOINT, breakpoint ? 1 : 0);
    String joinedPlaceholders = TextUtils.join(",", placeholders);
    int updateCount = db.update(MESSAGE_TABLE, cv, COLUMN_MESSAGE_ID
        + " in (" + joinedPlaceholders + ") ", arguments);
    return updateCount;
  }

  public synchronized boolean updateMessage(AVIMMessage message, String originalId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COLUMN_TIMESTAMP, message.getTimestamp());
    values.put(COLUMN_STATUS, message.getMessageStatus().getStatusCode());
    values.put(COLUMN_MESSAGE_DELIVEREDAT, message.getDeliveredAt());
    values.put(COLUMN_MESSAGE_READAT, message.getReadAt());
    values.put(COLUMN_MESSAGE_UPDATEAT, message.getUpdateAt());
    values.put(COLUMN_MESSAGE_ID, message.getMessageId());

    values.put(COLUMN_MSG_MENTION_ALL, message.isMentionAll()? 1: 0);
    values.put(COLUMN_MSG_MENTION_LIST, message.getMentionListString());

    long itemId =
        db.update(MESSAGE_TABLE, values, getWhereClause(COLUMN_MESSAGE_ID),
            new String[] {originalId});
    return itemId > -1;
  }

  synchronized boolean updateMessageForPatch(AVIMMessage message) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    if (message instanceof AVIMBinaryMessage) {
      values.put(COLUMN_PAYLOAD, ((AVIMBinaryMessage)message).getBytes());
      values.put(COLUMN_MSG_INNERTYPE, MESSAGE_INNERTYPE_BIN);
    } else {
      values.put(COLUMN_PAYLOAD, message.getContent());
      values.put(COLUMN_MSG_INNERTYPE, MESSAGE_INNERTYPE_PLAIN);
    }
    values.put(COLUMN_STATUS, message.getMessageStatus().getStatusCode());
    values.put(COLUMN_MESSAGE_UPDATEAT, message.getUpdateAt());
    long itemId = db.update(MESSAGE_TABLE, values, getWhereClause(COLUMN_MESSAGE_ID),
        new String[] {message.getMessageId()});
    return itemId > -1;
  }

  public synchronized void deleteMessages(List<AVIMMessage> messages, String conversationId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    for (AVIMMessage message : messages) {
      String messageId = message.getMessageId();
      AVIMMessage nextMessage = getNextMessage(message);
      if (nextMessage != null) {
        updateBreakpoints(Arrays.asList(message), true, conversationId);
      }
      db.delete(MESSAGE_TABLE, getWhereClause(COLUMN_MESSAGE_ID),
          new String[] {messageId});
    }
  }

  public synchronized void deleteConversationData(String conversationId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.delete(MESSAGE_TABLE, getWhereClause(COLUMN_CONVERSATION_ID),
        new String[] {conversationId});
    db.delete(CONVERSATION_TABLE, getWhereClause(COLUMN_CONVERSATION_ID),
        new String[] {conversationId});
  }

  public synchronized void deleteClientData() {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.delete(MESSAGE_TABLE, null, null);
    db.delete(CONVERSATION_TABLE, null, null);
  }

  /**
   * 查询该消息是否为断点
   */
  void getMessage(String msgId, long timestamp, String conversationId,
      StorageMessageCallback callback) {
    if (timestamp == 0) {
      callback.done(null, false);
    } else {
      SQLiteDatabase db = dbHelper.getReadableDatabase();
      Cursor cursor;
      if (msgId == null) {
        cursor =
            db.query(MESSAGE_TABLE, null, getWhereClause(COLUMN_TIMESTAMP, COLUMN_CONVERSATION_ID),
                new String[] {Long.toString(timestamp), conversationId}, null, null, null, "1");
      } else {
        cursor = db.query(MESSAGE_TABLE, null, getWhereClause(COLUMN_MESSAGE_ID),
            new String[] {msgId}, null, null, null, "1");
      }
      AVIMMessage message = null;
      boolean breakpoint = false;
      if (cursor.moveToFirst()) {
        message = createMessageFromCursor(cursor);
        breakpoint = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKPOINT)) != 0;
      }
      cursor.close();
      callback.done(message, breakpoint);
    }
  }

  void dumpMessages(String conversationId) {
    long curTs = System.currentTimeMillis();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String condition = SQL.TIMESTAMP_LESS_AND_CONVERSATION_ID;
    String[] conditionArgs = new String[]{Long.toString(curTs), conversationId};
    Cursor cursor = db.query(MESSAGE_TABLE, null, condition, conditionArgs, null, null, SQL.ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC);
    if (cursor.moveToFirst()) {
      while (!cursor.isAfterLast()) {
        AVIMMessage message = createMessageFromCursor(cursor);
        boolean breakpoint = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKPOINT)) != 0;
        System.out.println("msg: {id=" + message.getMessageId() + ", ts=" + message.getTimestamp() + ", breakpoint=" + breakpoint + "}");
        cursor.moveToNext();
      }
    }
  }

  /**
   * 根据条件查询本地缓存消息，消息有可能不连续，是否连续会通过 callback 返回
   *
   * @param msgId
   * @param timestamp
   * @param limit
   * @param conversationId
   * @param callback
   */
  public void getMessages(String msgId, long timestamp, int limit, String conversationId,
      StorageQueryCallback callback) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String selection;
    String[] selectionArgs;
    if (timestamp > 0) {
      if (msgId == null) {
        selection = SQL.TIMESTAMP_LESS_AND_CONVERSATION_ID;
        selectionArgs = new String[] {Long.toString(timestamp), conversationId};
      } else {
        selection = SQL.TIMESTAMP_LESS_OR_TIMESTAMP_EQUAL_BUT_MESSAGE_ID_LESS_AND_CONVERSATION_ID;
        selectionArgs =
            new String[] {Long.toString(timestamp), Long.toString(timestamp), msgId, conversationId};
      }
    } else {
      selection = getWhereClause(COLUMN_CONVERSATION_ID);
      selectionArgs = new String[] {conversationId};
    }
    Cursor cursor = db.query(MESSAGE_TABLE, null, selection, selectionArgs, null, null,
        SQL.ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC, limit + "");
    processMessages(cursor, callback);
  }

  public long getMessageCount(String conversationId) {
    AVIMMessage lastBreakPointMessage = this.getLatestMessageWithBreakpoint(conversationId, true);
    SQLiteDatabase db = dbHelper.getReadableDatabase();

    long messageCount = 0;
    if (lastBreakPointMessage == null) {
      messageCount =
          DatabaseUtils.longForQuery(db, "select count(*) from " + MESSAGE_TABLE + " where "
              + COLUMN_CONVERSATION_ID + " = ?", new String[] {conversationId});
    } else {
      messageCount = DatabaseUtils.longForQuery(
          db,
          "select count(*) from " + MESSAGE_TABLE + " where "
              + COLUMN_CONVERSATION_ID + " = ? and (" + COLUMN_TIMESTAMP + " > ? or ( "
              + COLUMN_TIMESTAMP + " = ? and "
              + COLUMN_MESSAGE_ID + " >= ? )) order by "
              + SQL.ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC,
          new String[] {conversationId, String.valueOf(lastBreakPointMessage.timestamp),
              String.valueOf(lastBreakPointMessage.timestamp),
              lastBreakPointMessage.messageId});
    }
    return messageCount;
  }

  private AVIMMessage createMessageFromCursor(Cursor cursor) {
    String mid = cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_ID));
    long timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP));
    String cid = cursor.getString(cursor.getColumnIndex(COLUMN_CONVERSATION_ID));
    String from = cursor.getString(cursor.getColumnIndex(COLUMN_FROM_PEER_ID));
    long deliveredAt = cursor.getLong(cursor.getColumnIndex(COLUMN_MESSAGE_DELIVEREDAT));
    long readAt = cursor.getLong(cursor.getColumnIndex(COLUMN_MESSAGE_READAT));
    long updateAt = cursor.getLong(cursor.getColumnIndex(COLUMN_MESSAGE_UPDATEAT));
    byte[] payload = cursor.getBlob(cursor.getColumnIndex(COLUMN_PAYLOAD));
    String uniqueToken = cursor.getString(cursor.getColumnIndex(COLUMN_DEDUPLICATED_TOKEN));
    int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
    int mentionAll = cursor.getInt(cursor.getColumnIndex(COLUMN_MSG_MENTION_ALL));
    String mentionListStr = cursor.getString(cursor.getColumnIndex(COLUMN_MSG_MENTION_LIST));
    int innerType = cursor.getInt(cursor.getColumnIndex(COLUMN_MSG_INNERTYPE));

    AVIMMessage message = null;
    if (innerType == MESSAGE_INNERTYPE_BIN) {
      message = new AVIMBinaryMessage(cid, from, timestamp, deliveredAt, readAt);
      ((AVIMBinaryMessage)message).setBytes(payload);
    } else {
      message = new AVIMMessage(cid, from, timestamp, deliveredAt, readAt);
      message.setContent(new String(payload));
    }
    message.setMessageId(mid);
    message.setUniqueToken(uniqueToken);
    message.setMessageStatus(AVIMMessage.AVIMMessageStatus.getMessageStatus(status));
    message.setUpdateAt(updateAt);
    message.setMentionAll( mentionAll == 1);
    message.setCurrentClient(this.clientId);
    if (!AVUtils.isBlankString(mentionListStr)) {
      message.setMentionListString(mentionListStr);
    }
    return AVIMMessageManager.parseTypedMessage(message);
  }

  private void processMessages(Cursor cursor, StorageQueryCallback callback) {
    List<AVIMMessage> messages = Collections.EMPTY_LIST;
    List<Boolean> breakpoints = Collections.EMPTY_LIST;
    if (cursor.moveToFirst()) {
      messages = new ArrayList<AVIMMessage>();
      breakpoints = new ArrayList<Boolean>();
      while (!cursor.isAfterLast()) {
        AVIMMessage message = createMessageFromCursor(cursor);
        messages.add(message);
        boolean breakpoint = cursor.getInt(cursor.getColumnIndex(COLUMN_BREAKPOINT)) != 0;
        breakpoints.add(breakpoint);
        cursor.moveToNext();
      }
    }
    cursor.close();
    callback.done(messages, breakpoints);
  }

  protected AVIMMessage getNextMessage(AVIMMessage currentMessage) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor =
        db.query(
            MESSAGE_TABLE,
            null,
            SQL.TIMESTAMP_MORE_OR_TIMESTAMP_EQUAL_BUT_MESSAGE_ID_MORE_AND_CONVERSATION_ID,
            new String[] {Long.toString(currentMessage.getTimestamp()),
                Long.toString(currentMessage.getTimestamp()),
                currentMessage.getMessageId(), currentMessage.getConversationId()}, null, null,
            SQL.ORDER_BY_TIMESTAMP_ASC_THEN_MESSAGE_ID_ASC, "1");
    AVIMMessage message = null;
    if (cursor.moveToFirst()) {
      message = createMessageFromCursor(cursor);
    }
    cursor.close();
    return message;
  }

  AVIMMessage getLatestMessage(String conversationId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.query(MESSAGE_TABLE, null, getWhereClause(COLUMN_CONVERSATION_ID),
      new String[] {conversationId}, null, null,
      SQL.ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC, "1");
    AVIMMessage message = null;
    if (cursor.moveToFirst()) {
      message = createMessageFromCursor(cursor);
    }
    cursor.close();
    return message;
  }

  AVIMMessage getLatestMessageWithBreakpoint(String conversationId, boolean breakpoint) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.query(MESSAGE_TABLE, null,
        getWhereClause(COLUMN_CONVERSATION_ID, COLUMN_BREAKPOINT),
        new String[] {conversationId, breakpoint ? "1" : "0"}, null, null,
        SQL.ORDER_BY_TIMESTAMP_DESC_THEN_MESSAGE_ID_DESC, "1");
    AVIMMessage message = null;
    if (cursor.moveToFirst()) {
      message = createMessageFromCursor(cursor);
    }
    cursor.close();
    return message;
  }

  public interface StorageQueryCallback {
    void done(List<AVIMMessage> messages, List<Boolean> breakpoints);
  }

  public interface StorageMessageCallback {
    void done(AVIMMessage message, boolean breakpoint);
  }

  public void insertConversations(List<AVIMConversation> conversations) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    for (AVIMConversation conversation : conversations) {
      ContentValues values = new ContentValues();
      values.put(COLUMN_ATTRIBUTE, JSON.toJSONString(conversation.attributes));
      values.put(COLUMN_INSTANCEDATA, JSON.toJSONString(conversation.instanceData));
      values.put(COLUMN_CREATEDAT, conversation.createdAt);
      values.put(COLUMN_UPDATEDAT, conversation.updatedAt);
      values.put(COLUMN_CREATOR, conversation.creator);
      values.put(COLUMN_EXPIREAT, System.currentTimeMillis()
          + Conversation.DEFAULT_CONVERSATION_EXPIRE_TIME_IN_MILLS);
      if (conversation.lastMessageAt != null) {
        values.put(COLUMN_LM, conversation.lastMessageAt.getTime());
      }

      final AVIMMessage message = conversation.getLastMessage();
      if (null != message) {
        if (message instanceof AVIMBinaryMessage) {
          byte[] bytes = ((AVIMBinaryMessage)message).getBytes();
          String base64Msg = AVUtils.base64Encode(bytes);
          values.put(COLUMN_LASTMESSAGE, base64Msg);
          values.put(COLUMN_CONV_LASTMESSAGE_INNERTYPE, MESSAGE_INNERTYPE_BIN);
        } else {
          String lastMessage = JSON.toJSONString(message);
          values.put(COLUMN_LASTMESSAGE, lastMessage);
          values.put(COLUMN_CONV_LASTMESSAGE_INNERTYPE, MESSAGE_INNERTYPE_PLAIN);
        }
      }

      values.put(COLUMN_MEMBERS, JSON.toJSONString(conversation.getMembers()));
      values.put(COLUMN_TRANSIENT, conversation.isTransient ? 1 : 0);
      values.put(COLUMN_UNREAD_COUNT, conversation.getUnreadMessagesCount());

      values.put(COLUMN_CONV_MENTIONED, conversation.unreadMessagesMentioned()? 1:0);

      values.put(COLUMN_CONVERSATION_READAT, conversation.getLastReadAt());
      values.put(COLUMN_CONVRESATION_DELIVEREDAT, conversation.getLastDeliveredAt());
      values.put(COLUMN_CONVERSATION_ID, conversation.getConversationId());
      db.insertWithOnConflict(CONVERSATION_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    db.setTransactionSuccessful();
    db.endTransaction();
  }

  boolean updateConversationTimes(AVIMConversation conversation) {
    if (getConversation(conversation.getConversationId()) != null) {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      ContentValues values = new ContentValues();
      values.put(COLUMN_CONVERSATION_READAT, conversation.getLastReadAt());
      values.put(COLUMN_CONVRESATION_DELIVEREDAT, conversation.getLastDeliveredAt());
      long dbId = db.update(CONVERSATION_TABLE, values, getWhereClause(COLUMN_CONVERSATION_ID),
        new String[] {conversation.getConversationId()});
      return dbId != -1;
    }
    return false;
  }

  boolean updateConversationUreadCount(String conversationId, long unreadCount, boolean mentioned) {
    if (getConversation(conversationId) != null) {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      ContentValues values = new ContentValues();
      values.put(COLUMN_UNREAD_COUNT, unreadCount);

      values.put(COLUMN_CONV_MENTIONED, mentioned? 1:0);

      long dbId = db.update(CONVERSATION_TABLE, values, getWhereClause(COLUMN_CONVERSATION_ID),
        new String[] {conversationId});
      return dbId != -1;
    }
    return false;
  }

  public boolean updateConversationLastMessageAt(AVIMConversation conversation) {
    if (getConversation(conversation.getConversationId()) != null
        && conversation.getLastMessageAt() != null) {
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      ContentValues values = new ContentValues();
      values.put(COLUMN_LM, conversation.getLastMessageAt().getTime());
      long dbId = db.update(CONVERSATION_TABLE, values, getWhereClause(COLUMN_CONVERSATION_ID),
          new String[] {conversation.getConversationId()});
      return dbId != -1;
    }
    return false;
  }

  public List<AVIMConversation> getAllCachedConversations() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor =
        db.query(CONVERSATION_TABLE, null, COLUMN_EXPIREAT + " > ?",
            new String[] {String.valueOf(System.currentTimeMillis())}, null, null, null,
            null);
    cursor.moveToFirst();
    List<AVIMConversation> conversations = new LinkedList<AVIMConversation>();
    while (!cursor.isAfterLast()) {
      conversations.add(parseConversationFromCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return conversations;
  }

  private AVIMConversation parseConversationFromCursor(Cursor cursor) {
    String conversationId = cursor.getString(cursor.getColumnIndex(COLUMN_CONVERSATION_ID));
    String createdAt = cursor.getString(cursor.getColumnIndex(COLUMN_CREATEDAT));
    String updatedAt = cursor.getString(cursor.getColumnIndex(COLUMN_UPDATEDAT));
    String membersStr = cursor.getString(cursor.getColumnIndex(COLUMN_MEMBERS));
    String attrsStr = cursor.getString(cursor.getColumnIndex(COLUMN_ATTRIBUTE));
    String instanceData = cursor.getString(cursor.getColumnIndex(COLUMN_INSTANCEDATA));
    String creator = cursor.getString(cursor.getColumnIndex(COLUMN_CREATOR));
    long lastMessageTS = cursor.getLong(cursor.getColumnIndex(COLUMN_LM));
    int transientValue = cursor.getInt(cursor.getColumnIndex(COLUMN_TRANSIENT));
    int unreadCount = cursor.getInt(cursor.getColumnIndex(COLUMN_UNREAD_COUNT));

    int mentioned = cursor.getInt(cursor.getColumnIndex(COLUMN_CONV_MENTIONED));

    long readAt = cursor.getLong(cursor.getColumnIndex(COLUMN_CONVERSATION_READAT));
    long deliveredAt = cursor.getLong(cursor.getColumnIndex(COLUMN_CONVRESATION_DELIVEREDAT));
    String lastMessage = cursor.getString(cursor.getColumnIndex(COLUMN_LASTMESSAGE));
    int lastMessageInnerType = cursor.getInt(cursor.getColumnIndex(COLUMN_CONV_LASTMESSAGE_INNERTYPE));

    AVIMConversation conversation =
        new AVIMConversation(AVIMClient.getInstance(clientId), conversationId);
    conversation.createdAt = createdAt;
    conversation.updatedAt = updatedAt;
    try {
      conversation.members.clear();
      if (!AVUtils.isBlankContent(membersStr)) {
        conversation.members.addAll(JSON.parseObject(membersStr, Set.class));
      }

      conversation.attributes.clear();
      if (!AVUtils.isBlankContent(attrsStr)) {
        conversation.attributes.putAll(JSON.parseObject(attrsStr, HashMap.class));
      }

      conversation.instanceData.clear();
      if (!AVUtils.isBlankContent(instanceData)) {
        conversation.instanceData.putAll(JSON.parseObject(instanceData, HashMap.class));
      }
      if (lastMessageInnerType != MESSAGE_INNERTYPE_BIN) {
        AVIMMessage msg = JSON.parseObject(lastMessage, AVIMMessage.class);
        conversation.lastMessage = msg;
      } else {
        AVIMBinaryMessage binaryMsg = new AVIMBinaryMessage(conversationId, null);// don't care who sent message.
        binaryMsg.setBytes(AVUtils.base64Decode(lastMessage));
        conversation.lastMessage = binaryMsg;
      }
    } catch (Exception e) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.e("error during conversation cache parse:" + e.getMessage());
      }
    }
    conversation.creator = creator;
    conversation.lastMessageAt = new Date(lastMessageTS);
    conversation.isTransient = transientValue == 1;
    conversation.unreadMessagesCount = unreadCount;
    conversation.unreadMessagesMentioned = mentioned == 1;
    conversation.lastReadAt = readAt;
    conversation.lastDeliveredAt = deliveredAt;
    return conversation;
  }

  public AVIMConversation getConversation(String conversationId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor =
        db.query(CONVERSATION_TABLE, null, getWhereClause(COLUMN_CONVERSATION_ID) + " and "
            + COLUMN_EXPIREAT + " > ?",
            new String[] {conversationId, String.valueOf(System.currentTimeMillis())}, null, null,
            null,
            null);
    cursor.moveToFirst();
    AVIMConversation conversation = null;
    if (!cursor.isAfterLast()) {
      conversation = parseConversationFromCursor(cursor);
    }
    cursor.close();
    return conversation;
  }

  public List<AVIMConversation> getCachedConversations(List<String> conversationIds) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor =
        db.rawQuery("SELECT * FROM " + CONVERSATION_TABLE + " WHERE " + COLUMN_CONVERSATION_ID
            + " in ('" + AVUtils.joinCollection(conversationIds, "','") + "')", null);
    cursor.moveToFirst();
    List<AVIMConversation> conversations = new LinkedList<AVIMConversation>();
    while (!cursor.isAfterLast()) {
      conversations.add(parseConversationFromCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return conversations;
  }

  public void deleteConversation(String conversationId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.delete(CONVERSATION_TABLE, getWhereClause(COLUMN_CONVERSATION_ID),
        new String[] {conversationId});
  }
}
