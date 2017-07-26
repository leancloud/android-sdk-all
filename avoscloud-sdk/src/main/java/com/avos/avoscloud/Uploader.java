package com.avos.avoscloud;

/**
 * Created by lbt05 on 6/12/16.
 */
public interface Uploader {
  AVException doWork();

  void execute();

  void publishProgress(int percentage);

  boolean cancel(boolean interrupt);

  boolean isCancelled();
}
