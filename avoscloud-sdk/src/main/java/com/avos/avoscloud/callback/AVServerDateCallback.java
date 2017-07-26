package com.avos.avoscloud.callback;

import com.avos.avoscloud.AVCallback;
import com.avos.avoscloud.AVException;

import java.util.Date;

/**
 * Created by lbt05 on 8/7/15.
 */
public abstract class AVServerDateCallback extends AVCallback<Date> {

  public abstract void done(Date serverDate, AVException e);

  @Override
  protected void internalDone0(Date date, AVException avException) {
    this.done(date, avException);
  }
}
