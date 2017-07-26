package com.avos.avoscloud;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.*;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/13/13 Time: 3:29 PM To change this template use
 * File | Settings | File Templates.
 */
class AnalyticsSession implements Parcelable {

  private List<AnalyticsActivity> activities;
  private List<AnalyticsEvent> events;
  private String sessionId = "";
  private AVDuration duration = new AVDuration();


  private static final String sessionIdTag = "sessionId";
  private static final String TAG = AnalyticsSession.class.getSimpleName();

  AnalyticsSession() {
    super();
    activities = new ArrayList<AnalyticsActivity>();
    events = new ArrayList<AnalyticsEvent>();
  }

  synchronized void beginSession() {
    sessionId = AnalyticsUtils.uniqueId();
    duration.start();
  }

  synchronized void endSession() {
    if (AVUtils.isBlankString(sessionId)) {
      return;
    }
    for (AnalyticsActivity a : getActivities()) {
      if (!a.isStopped()) {
        a.stop();
      }
    }
    for (AnalyticsEvent e : getEvents()) {
      if (!e.getDuration().isStopped()) {
        e.stop();
      }
    }
    duration.stop();
  }

  boolean isSessionFinished() {
    return duration.isStopped();
  }

  synchronized void pauseSession() {
    duration.pause();
  }

  public long getSessionStart() {
    return duration.getCreateTimeStamp();
  }

  String getSessionId() {
    return sessionId;
  }

  void setSessionDuration(long ms) {
    duration.setDuration(ms);
  }

  private AnalyticsActivity activityByName(String name, boolean create) {
    for (AnalyticsActivity activity : activities) {
      if (activity.getActivityName().equalsIgnoreCase(name) && !activity.isStopped()) {
        return activity;
      }
    }

    AnalyticsActivity activity = null;
    if (create) {
      activity = new AnalyticsActivity(name);
      activities.add(activity);
    }
    return activity;
  }

  private AnalyticsEvent eventByName(String name, String label, String key, boolean create) {
    for (AnalyticsEvent event : events) {
      if (event.isMatch(name, label, key)) {
        return event;
      }
    }

    AnalyticsEvent event = null;
    if (create) {
      event = new AnalyticsEvent(name);
      event.setLabel(label);
      event.setPrimaryKey(key);
      events.add(event);
    }
    return event;
  }

  synchronized void addActivity(String name, long ms) {
    AnalyticsActivity activity = activityByName(name, true);
    activity.setDurationValue(ms);
  }

  synchronized void beginActivity(String name) {
    AnalyticsActivity activity = activityByName(name, true);
    activity.start();
    activity.setSavedToServer(false);
    duration.resume();

  }

  synchronized void beginFragment(String name) {
    AnalyticsActivity fragment = activityByName(name, true);
    fragment.setFragment(true);
    fragment.start();
    duration.resume();
  }

  synchronized AnalyticsEvent beginEvent(Context context, String name, String label,
      String key) {
    AnalyticsEvent event = eventByName(name, label, key, true);
    if (!AVUtils.isBlankString(label)) {
      event.setLabel(label);
    }
    event.start();
    duration.resume();
    return event;
  }

  synchronized void endEvent(Context context, String name, String label, String key) {
    AnalyticsEvent event = eventByName(name, label, key, false);
    if (event == null) {
      return;
    }
    event.stop();
  }

  synchronized void endActivity(String name) {
    AnalyticsActivity activity = activityByName(name, false);
    if (activity == null) {
      // wrong.
      Log.i("", "Please call begin activity before using endActivity");
      return;
    }
    activity.setSavedToServer(false);
    activity.stop();
  }

  synchronized void endFragment(String name) {
    AnalyticsActivity fragment = activityByName(name, false);
    if (fragment == null) {
      // wrong.
      Log.i("", "Please call begin Fragment before using endFragment");
      return;
    }
    fragment.stop();
  }


  private Map<String, Object> launchMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put(sessionIdTag, sessionId);
    map.put("date", duration.getCreateTimeStamp());
    return map;
  }

  // 在这里使用了side effect，一边生成请求的参数又一边删除已经得到的参数的事件数据
  // 在这里side effect应该是最简单的保证一致性的方法
  // 但是又有几处其他地方会调用这几个方法，所以增加一个布尔值变量来控制这个side
  // effect的发生开关。。一个session实例同一时间只会有一次cleanUpAnalysisData为true的调用

  private synchronized Map<String, Object> activitiesMap(boolean cleanUpAnalysisData) {
    List<Object> array = new LinkedList<Object>();
    long activitiesDuration = 0;
    for (AnalyticsActivity a : activities) {
      synchronized (a) {
        if (a.isStopped() && !a.isSavedToServer()) {
          array.add(a.jsonMap());
          if (cleanUpAnalysisData) {
            a.setSavedToServer(true);
          }
        }
      }
      if (!a.isFragment) {
        activitiesDuration += a.myDuration();
      }
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("activities", array);
    map.put(sessionIdTag, sessionId);
    map.put("duration", this.getDuration().getActualDuration());
    return map;
  }

  private synchronized List<Object> eventArray(boolean cleanUpAnalysisData) {
    List<Object> array = new LinkedList<Object>();
    List<AnalyticsEvent> toDelete = new LinkedList<AnalyticsEvent>();
    for (AnalyticsEvent e : events) {
      if (e.getDuration().isStopped()) {
        array.add(e.jsonMap());
        toDelete.add(e);
      }
    }
    if (cleanUpAnalysisData) {
      events.removeAll(toDelete);
    }
    return array;
  }

  private boolean hasNewData() {
    for (AnalyticsActivity a : activities) {
      if (a.isStopped() && !a.isSavedToServer()) {
        return true;
      }
    }
    for (AnalyticsEvent e : events) {
      if (e.getDuration().isStopped()) {
        return true;
      }
    }
    return false;
  }

  int getMessageCount() {
    int messageCount = 0;
    for (AnalyticsActivity a : activities) {
      if (a.isStopped() && !a.isSavedToServer()) {
        messageCount += 2;
      } else if (!a.isSavedToServer() && !a.isStopped()) {
        messageCount++;
      }
    }
    for (AnalyticsEvent e : events) {
      if (e.getDuration().isStopped()) {
        messageCount += 2;
      } else {
        messageCount++;
      }
    }
    return messageCount;
  }

  Map<String, Object> jsonMap(Context context, Map<String, String> customInfo,
      boolean cleanUpAnalysisData) {
    if (hasNewData()) {
      Map<String, Object> result = new HashMap<String, Object>();
      Map<String, Object> events = new HashMap<String, Object>();
      events.put("launch", launchMap());
      events.put("terminate", activitiesMap(cleanUpAnalysisData));
      events.put("event", eventArray(cleanUpAnalysisData));

      result.put("events", events);
      Map<String, Object> devInfo = AnalyticsUtils.deviceInfo(context);
      result.put("device", devInfo);
      if (customInfo != null) {
        result.put("customInfo", customInfo);
      }
      return result;

    } else {
      return null;
    }
  }


  Map<String, Object> firstBootMap(Context context, Map<String, String> customInfo) {

    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, Object> events = new HashMap<String, Object>();
    events.put("launch", launchMap());
    events.put("terminate", activitiesMap(false));
    result.put("events", events);
    Map<String, Object> devInfo = AnalyticsUtils.deviceInfo(context);
    result.put("device", devInfo);
    if (customInfo != null) {
      result.put("customInfo", customInfo);
    }
    return result;


  }

  // never use these getter/setter just used for archive serialization

  private List<AnalyticsActivity> getActivities() {
    return activities;
  }

  protected void setActivities(List<AnalyticsActivity> activities) {
    this.activities = activities;
  }

  private List<AnalyticsEvent> getEvents() {
    return events;
  }

  protected void setEvents(List<AnalyticsEvent> events) {
    this.events = events;
  }


  AVDuration getDuration() {
    return duration;
  }

  protected void setDuration(AVDuration duration) {
    this.duration = duration;
  }

  protected void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int i) {
    out.writeParcelableArray(activities.toArray(new AnalyticsActivity[] {}),
        PARCELABLE_WRITE_RETURN_VALUE);
    out.writeParcelableArray(events.toArray(new AnalyticsEvent[] {}), PARCELABLE_WRITE_RETURN_VALUE);
    out.writeParcelable(duration, PARCELABLE_WRITE_RETURN_VALUE);
    out.writeString(sessionId);
  }

  AnalyticsSession(Parcel in) {
    this();
    Parcelable[] parcelActivities =
        in.readParcelableArray(AnalyticsActivity.class.getClassLoader());
    Parcelable[] parcelEvents = in.readParcelableArray(AnalyticsEvent.class.getClassLoader());
    for (Parcelable activity : parcelActivities) {
      activities.add((AnalyticsActivity) activity);
    }

    for (Parcelable event : parcelEvents) {
      events.add((AnalyticsEvent) event);
    }
    this.duration = in.readParcelable(AVDuration.class.getClassLoader());
    this.sessionId = in.readString();
  }

  public static final Creator<AnalyticsSession> CREATOR = new Creator<AnalyticsSession>() {

    @Override
    public AnalyticsSession createFromParcel(Parcel parcel) {
      return new AnalyticsSession(parcel);
    }

    @Override
    public AnalyticsSession[] newArray(int i) {
      return new AnalyticsSession[i];
    }
  };
}
