package com.avos.avoscloud.feedback;

import android.content.Context;

public class Resources {
  protected static String getPackageName(Context context) {
    return context.getApplicationContext().getPackageName();
  }

  protected static int getResourceId(Context context, String resourceId, String type) {
    return context.getResources().getIdentifier(resourceId, type, getPackageName(context));
  }

  public static class layout {
    public static int avoscloud_feedback_activity_conversation(Context context) {
      return getResourceId(context, "avoscloud_feedback_activity_conversation", "layout");
    }

    public static int avoscloud_feedback_user_reply(Context context) {
      return getResourceId(context, "avoscloud_feedback_user_reply", "layout");
    }

    public static int avoscloud_feedback_dev_reply(Context context) {
      return getResourceId(context, "avoscloud_feedback_dev_reply", "layout");
    }

    public static int avoscloud_feedback_thread_actionbar(Context context) {
      return getResourceId(context, "avoscloud_feedback_thread_actionbar", "layout");
    }
  }

  public static class drawable {
    public static int avoscloud_feedback_notification(Context context) {
      return getResourceId(context, "avoscloud_feedback_notification", "drawable");
    }
  }

  public static class id {
    public static int avoscloud_feedback_thread_list(Context context) {
      return getResourceId(context, "avoscloud_feedback_thread_list", "id");
    }

    public static int avoscloud_feedback_send(Context context) {
      return getResourceId(context, "avoscloud_feedback_send", "id");
    }

    public static int avoscloud_feedback_input(Context context) {
      return getResourceId(context, "avoscloud_feedback_input", "id");
    }

    public static int avoscloud_feedback_contact(Context context) {
      return getResourceId(context, "avoscloud_feedback_contact", "id");
    }

    public static int avoscloud_feedback_actionbar_back(Context context) {
      return getResourceId(context, "avoscloud_feedback_actionbar_back", "id");
    }

    public static int avoscloud_feedback_content(Context context) {
      return getResourceId(context, "avoscloud_feedback_content", "id");
    }

    public static int avoscloud_feedback_timestamp(Context context) {
      return getResourceId(context, "avoscloud_feedback_timestamp", "id");
    }

    public static int avoscloud_feedback_add_image(Context context) {
      return getResourceId(context, "avoscloud_feedback_add_image", "id");
    }

    public static int avoscloud_feedback_image(Context context) {
      return getResourceId(context, "avoscloud_feedback_image", "id");
    }
  }


  public static class string {
    public static int avoscloud_feedback_new_item(Context context) {
      return getResourceId(context, "avoscloud_feedback_new_item", "string");
    }

    public static int avoscloud_feedback_just_now(Context context) {
      return getResourceId(context, "avoscloud_feedback_just_now", "string");
    }

    public static int avoscloud_feedback_select_image(Context context) {
      return getResourceId(context, "avoscloud_feedback_select_image", "string");
    }

  }

}
