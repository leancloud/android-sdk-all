package com.avos.avoscloud;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 10/10/13 Time: 8:50 PM To change this template
 * use File | Settings | File Templates.
 */
class AVDuration implements Parcelable {

  /*
   * Duration中间总共有4个状态 start,pause,resume,stop
   * start----->stop 时间存放在getLastTimeInterval是currentTimeStamp - resumeTimeStamp;
   * start----->pause 时间存放在getLastTimeInterval是pausedTimeStamp - resumeTimeStamp;
   * start----->pause ------>stop 时间放入duration中间，是pausedTimestamp - resumeTimeStamp
   * start----->pause ------>resume 将上一阶段的时常放入duration,即为pausedTimestamp -
   * resumeTimeStamp.之后再将resumeTimestamp值为当前时间
   * start----->pause ----->resume----> stop叠加上一阶段的时间到duration中间去，叠加时间为currentTS() - resumeTimestamp
   */


  private long createTimeStamp;
  private long resumeTimeStamp;
  private long pausedTimeStamp;

  private long duration;
  private boolean stopped;

  AVDuration() {

  }

  long getCreateTimeStamp() {
    return createTimeStamp;
  }

  // 改名的原因是在于序列化和反序列之后会，实际的duration = 真实的duration + 2 * getLastTimeInterval();从而出现错误
  long getActualDuration() {
    long tempDuration = duration + getLastTimeInterval();
    if (tempDuration < 0) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("Negative duration " + tempDuration);
      }
      tempDuration = 0;
    }
    return tempDuration;
  }

  public long getDuration() {
    return duration;
  }

  public synchronized void start() {
    stopped = false;
    createTimeStamp = currentTS();
    resumeTimeStamp = createTimeStamp;
    pausedTimeStamp = -1;
  }

  synchronized void stop() {
    sync();
    stopped = true;
  }

  boolean isStopped() {
    return stopped;
  }

  synchronized void resume() {
    if (stopped) {
      return;
    }
    sync();
    resumeTimeStamp = currentTS();
  }

  synchronized void pause() {
    pausedTimeStamp = currentTS();
  }

  void setDuration(long ms) {
    duration = ms;

  }

  public void addDuration(long ms) {
    duration += ms;
  }

  // accumulate duration
  void sync() {
    if (stopped) {
      return;
    }
    duration += getLastTimeInterval();
    pausedTimeStamp = -1;
  }

  private long getLastTimeInterval() {
    long d = 0;
    // resume---------->paused
    if (pausedTimeStamp > resumeTimeStamp) {
      d = pausedTimeStamp - resumeTimeStamp;
    } else if (!stopped) {
      // paused--------->resume--------->stop
      d = currentTS() - resumeTimeStamp;
    }
    return d;
  }

  private static long currentTS() {
    return System.currentTimeMillis();
  }

  protected long getResumeTimeStamp() {
    return resumeTimeStamp;
  }

  protected void setResumeTimeStamp(long resumeTimeStamp) {
    this.resumeTimeStamp = resumeTimeStamp;
  }

  protected void setCreateTimeStamp(long createTimeStamp) {
    this.createTimeStamp = createTimeStamp;
  }

  protected void setStopped(boolean stopped) {
    this.stopped = stopped;
  }

  long getPausedTimeStamp() {
    return pausedTimeStamp;
  }

  public void setPausedTimeStamp(long pausedTimeStamp) {
    this.pausedTimeStamp = pausedTimeStamp;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeLong(createTimeStamp);
    parcel.writeLong(resumeTimeStamp);
    parcel.writeLong(pausedTimeStamp);
    parcel.writeLong(duration);
    parcel.writeInt(stopped ? 1 : 0);
  }

  private AVDuration(Parcel in) {
    this.createTimeStamp = in.readLong();
    this.resumeTimeStamp = in.readLong();
    this.pausedTimeStamp = in.readLong();
    this.duration = in.readLong();
    this.stopped = in.readInt() == 1;
  }

  public static final Creator<AVDuration> CREATOR = new Creator<AVDuration>() {

    @Override
    public AVDuration createFromParcel(Parcel parcel) {
      return new AVDuration(parcel);
    }

    @Override
    public AVDuration[] newArray(int i) {
      return new AVDuration[i];
    }
  };
}
