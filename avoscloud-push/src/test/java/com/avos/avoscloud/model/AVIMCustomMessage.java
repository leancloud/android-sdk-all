package com.avos.avoscloud.model;

import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

/**
 * Created by wli on 2017/8/29.
 */
@AVIMMessageType(type = -100)
public class AVIMCustomMessage extends AVIMTypedMessage {

  public AVIMCustomMessage() {
  }

  @AVIMMessageField(name = "text")
  String text;

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public static final Creator<AVIMCustomMessage> CREATOR = new AVIMMessageCreator<>(AVIMCustomMessage.class);
}
