package com.avos.avoscloud.im.v2.messages;

import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

import java.util.Map;

/**
 * Created by lbt05 on 1/22/15.
 */
@AVIMMessageType(type = AVIMMessageType.LOCATION_MESSAGE_TYPE)
public class AVIMLocationMessage extends AVIMTypedMessage {
  public AVIMLocationMessage() {

  }


  @AVIMMessageField(name = "_lcloc")
  AVGeoPoint location;
  @AVIMMessageField(name = "_lctext")
  String text;
  @AVIMMessageField(name = "_lcattrs")
  Map<String, Object> attrs;

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Map<String, Object> getAttrs() {
    return this.attrs;
  }

  public void setAttrs(Map<String, Object> attr) {
    this.attrs = attr;
  }

  public AVGeoPoint getLocation() {
    return location;
  }

  public void setLocation(AVGeoPoint location) {
    this.location = location;
  }

  public static final Creator<AVIMLocationMessage> CREATOR = new AVIMMessageCreator<>(AVIMLocationMessage.class);
}
