package com.avos.avoscloud;

/**
 * Created with IntelliJ IDEA. User: tangxiaomin Date: 4/12/13 Time: 5:07 PM
 */
public final class AVConstants {
  public static final String AV_PUSH_SERVICE_NOTIFICATION_MSG = "AV_NOTIFICATION_MSG";
  public static final String AV_PUSH_SERVICE_APPLICATION_ID = "AV_APPLICATION_ID";
  public static final String AV_PUSH_SERVICE_DEFAULT_CALLBACK = "AV_DEFAULT_CALLBACK";
  public static final String AV_PUSH_SERVICE_ACTION_MESSAGE = "AV_PUSH_SERVICE_ACTION_MESSAGE";
  public static final String AV_PUSH_SERVICE_SETTINGS_KEY = "AV_PUSH_SERVICE_SETTINGS_KEY";

  public static final String PUSH_INTENT_KEY = "com.avoscloud.push";
  public static final String AVSEARCH_HIGHTLIGHT = "highlight_avoscloud_";
  public static final String AVSEARCH_APP_URL = "app_url_avoscloud_";
  public static final String AVSEARCH_DEEP_LINK = "deep_link_avoscloud_";

  public static final String AV_MIXPUSH_MI_NOTIFICATION_ACTION = "com.avos.avoscloud.mi_notification_action";
  public static final String AV_MIXPUSH_MI_NOTIFICATION_ARRIVED_ACTION = "com.avos.avoscloud.mi_notification_arrived_action";
  public static final String AV_MIXPUSH_FLYME_NOTIFICATION_ACTION = "com.avos.avoscloud.flyme_notification_action";
}


enum AVOperationType {
  kAVOperationSnapshot, kAVOperationPendingOperation,
};
