package com.avos.avospush.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.avos.avoscloud.AVPersistenceUtils;
import com.avos.avoscloud.AVUtils;

public class MessageReceiptCache {
  private static final String MESSAGE_ZONE = "com.avoscloud.chat.receipt.";
  private static final String QUEUE_KEY = "com.avoscloud.chat.message.receipt";

  public static void add(String sessionId, String key, Object value) {
    String queueString =
        JSON.toJSONString(value, SerializerFeature.SkipTransientField,
            SerializerFeature.WriteClassName, SerializerFeature.QuoteFieldNames,
            // SerializerFeature.DisableCircularReferenceDetect,
            SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse);
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(MESSAGE_ZONE + sessionId,
        QUEUE_KEY + key,
        queueString);
  }

  public static Object get(String sessionId, String key) {
    String valueString =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(MESSAGE_ZONE + sessionId,
            QUEUE_KEY + key, null);
    AVPersistenceUtils.sharedInstance()
        .removePersistentSettingString(MESSAGE_ZONE, QUEUE_KEY + key);
    if (AVUtils.isBlankString(valueString)) {
      return null;
    }
    return JSON.parse(valueString);
  }

  public static void clean(String sessionId) {
    AVPersistenceUtils.sharedInstance().removeKeyZonePersistentSettings(MESSAGE_ZONE + sessionId);
  }
}
