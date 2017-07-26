package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/6/13 Time: 4:03 PM To change this template use
 * File | Settings | File Templates.
 */
class AnalyticsActivity implements Parcelable {

  private AVDuration duration = new AVDuration();
  private String activityName;
  private volatile boolean savedToServer;
  boolean isFragment = false;


  AnalyticsActivity(String name) {
    super();
    activityName = name;
    savedToServer = false;
  }

  AnalyticsActivity() {
    this("");
  }

  void start() {
    duration.start();
  }

  void stop() {
    duration.stop();
  }

  void setDurationValue(long ms) {
    duration.setDuration(ms);
  }

  double getStart() {
    return duration.getCreateTimeStamp();
  }

  String getActivityName() {
    return activityName;
  }

  boolean isStopped() {
    return duration.isStopped();
  }

  long myDuration() {
    return duration.getActualDuration();
  }

  Map<String, Object> jsonMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("name", activityName);
    map.put("du", myDuration());
    map.put("ts", duration.getCreateTimeStamp());
    return map;
  }

  // used for serialization
  protected AVDuration getDuration() {
    return duration;
  }

  protected void setDuration(AVDuration duration) {
    this.duration = duration;
  }

  protected void setActivityName(String activityName) {
    this.activityName = activityName;
  }

  boolean isSavedToServer() {
    return savedToServer;
  }

  void setSavedToServer(boolean savedToServer) {
    this.savedToServer = savedToServer;
  }

  protected boolean isFragment() {
    return isFragment;
  }

  void setFragment(boolean isFragment) {
    this.isFragment = isFragment;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeParcelable(duration, PARCELABLE_WRITE_RETURN_VALUE);
    parcel.writeString(activityName);
    parcel.writeInt(savedToServer ? 1 : 0);
    parcel.writeInt(isFragment ? 1 : 0);
  }

  private AnalyticsActivity(Parcel in) {
    this.duration = in.readParcelable(AVDuration.class.getClassLoader());
    this.activityName = in.readString();
    this.savedToServer = in.readInt() == 1;
    this.isFragment = in.readInt() == 1;
  }

  public static final Creator<AnalyticsActivity> CREATOR = new Creator<AnalyticsActivity>() {

    @Override
    public AnalyticsActivity createFromParcel(Parcel parcel) {
      return new AnalyticsActivity(parcel);
    }

    @Override
    public AnalyticsActivity[] newArray(int i) {
      return new AnalyticsActivity[i];
    }
  };
}
