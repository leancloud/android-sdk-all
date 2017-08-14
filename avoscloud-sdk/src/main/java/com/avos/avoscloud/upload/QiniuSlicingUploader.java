package com.avos.avoscloud.upload;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.ProgressCallback;
import com.avos.avoscloud.SaveCallback;

/**
 * Use one thread to upload file to qiniu, slicing with 4MB chunk.
 *
 * Created by fengjunwen on 2017/8/14.
 */

class QiniuSlicingUploader extends HttpClientUploader {
  private final String token;
  private final String fileKey;

  QiniuSlicingUploader(AVFile avFile, String token, String fileKey, SaveCallback saveCallback, ProgressCallback progressCallback) {
    super(avFile, saveCallback, progressCallback);
    this.token = token;
    this.fileKey = fileKey;
  }

  @Override
  public AVException doWork() {
    return null;
  }
}
