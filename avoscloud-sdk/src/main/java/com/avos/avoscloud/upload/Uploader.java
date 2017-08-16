package com.avos.avoscloud.upload;

import com.avos.avoscloud.AVException;

/**
 * Created by lbt05 on 6/12/16.
 */
public interface Uploader {
  AVException doWork();

  String getFinalUrl();
  String getFinalObjectId();

  void execute();

  void publishProgress(int percentage);

  boolean cancel(boolean interrupt);

  boolean isCancelled();

  static interface UploadCallback {
    void finishedWithResults(String finalObjectId, String finalUrl);
  }
}

