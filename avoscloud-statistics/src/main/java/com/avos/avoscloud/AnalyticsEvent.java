package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/6/13 Time: 4:03 PM To change this template use
 * File | Settings | File Templates.
 */
class AnalyticsEvent implements Parcelable {

  private AVDuration duration = new AVDuration();
  private Map<String, Object> attributes;
  private String eventName;
  private String labelName;
  private String primaryKey;
  private int accumulation;

  static private final String eventTag = "name";
  static private final String labelTag = "tag";
  static private final String accTag = "acc";
  static private final String primaryKeyTag = "primaryKey";
  static private final String attributesTag = "attributes";

  AnalyticsEvent(final String name) {
    super();
    eventName = name;
    attributes = new HashMap<String, Object>();
    accumulation = 1;
  }

  public AnalyticsEvent() {
    this("");
  }

  public void start() {
    duration.start();
  }

  void stop() {
    duration.stop();
  }

  public String getEventName() {
    return eventName;
  }

  void setDurationValue(long ms) {
    duration.setDuration(ms);
  }

  void setAccumulation(int acc) {
    if (acc > 0) {
      accumulation = acc;
    }
  }

  void setLabel(final String label) {
    labelName = label;
  }

  void setPrimaryKey(final String key) {
    primaryKey = key;
  }

  void addAttributes(Map<String, String> map) {
    if (map != null) {
      attributes.putAll(map);
    }
  }

  boolean isMatch(final String name, final String label, final String key) {
    if (!eventName.equals(name)) {
      return false;
    }

    if (!AnalyticsUtils.isStringEqual(labelName, label)) {
      return false;
    }

    if (!AnalyticsUtils.isStringEqual(primaryKey, key)) {
      return false;
    }

    if (duration.isStopped()) {
      return false;
    }
    return true;
  }

  private long myDuration() {
    return duration.getActualDuration();
  }

  Map<String, Object> jsonMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(eventTag, eventName);
    if (!AVUtils.isBlankString(labelName)) {
      map.put(labelTag, labelName);
    } else {
      map.put(labelTag, eventName);
    }
    if (!AVUtils.isBlankString(primaryKey)) {
      map.put(primaryKeyTag, primaryKey);
    }
    if (accumulation > 1) {
      map.put(accTag, accumulation);
    }
    if (attributes.size() > 0) {
      try {
        map.put(attributesTag, attributes);
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
    map.put("du", myDuration());
    map.put("ts", duration.getCreateTimeStamp());
    return map;
  }

  // never use these getter/setter just used for archive serialization
  AVDuration getDuration() {
    return duration;
  }

  protected void setDuration(AVDuration duration) {
    this.duration = duration;
  }

  protected Map<String, Object> getAttributes() {
    return attributes;
  }

  protected void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  protected String getLabelName() {
    return labelName;
  }

  protected void setLabelName(String labelName) {
    this.labelName = labelName;
  }

  protected String getPrimaryKey() {
    return primaryKey;
  }

  protected int getAccumulation() {
    return accumulation;
  }

  protected void setEventName(String eventName) {
    this.eventName = eventName;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeParcelable(duration, PARCELABLE_WRITE_RETURN_VALUE);
    parcel.writeMap(attributes);
    parcel.writeString(eventName);
    parcel.writeString(labelName);
    parcel.writeString(primaryKey);
    parcel.writeInt(accumulation);
  }

  private AnalyticsEvent(Parcel in) {
    this.duration = in.readParcelable(AnalyticsEvent.class.getClassLoader());
    this.attributes = in.readHashMap(Map.class.getClassLoader());
    this.eventName = in.readString();
    this.labelName = in.readString();
    this.primaryKey = in.readString();
    this.accumulation = in.readInt();
  }

  public static final Creator<AnalyticsEvent> CREATOR = new Creator<AnalyticsEvent>() {

    @Override
    public AnalyticsEvent createFromParcel(Parcel parcel) {
      return new AnalyticsEvent(parcel);
    }

    @Override
    public AnalyticsEvent[] newArray(int i) {
      return new AnalyticsEvent[i];
    }
  };

}
