package com.avos.avoscloud.im.v2.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.LogUtil;

import java.io.IOException;

/**
 * Created by fengjunwen on 2017/8/25.
 */

public class AVIMVideoCapture implements SurfaceHolder.Callback {
  public static final int REQUEST_VIDEO_CAPTURE = 1;

  /**
   * Call system camera app to take video.
   * it is necessary to implement onActivityResult method to retrieve result data, as following:
   * @Override
   * protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
   *   if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
   *     Uri videoUri = intent.getData();
   *     mVideoView.setVideoURI(videoUri);
   *   }
   * }
   *
   * @param context
   */
  public static void dispatchTakeVideoIntent(Activity context) {
    Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    if (takeVideoIntent.resolveActivity(context.getPackageManager()) != null) {
      context.startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
    }
  }

  private MediaRecorder mediaRecorder = null;
  private SurfaceView surfaceView = null;
  private CamcorderProfile profile = null;
  private String localPath = null;
  private int maxDuration; // seconds
  private int maxFileSize; // bytes
  private boolean recording = false;

  /**
   * constructor.
   *
   * @param profile
   * @param localPath    local file path to save capture video.
   * @param maxDuration  max duration(seconds)
   * @param maxFileSize  max file size(bytes)
   * @param surfaceView  surface view
   */
  public AVIMVideoCapture(CamcorderProfile profile, String localPath, int maxDuration, int maxFileSize, SurfaceView surfaceView) {
    if (AVUtils.isBlankString(localPath)) {
      throw new IllegalArgumentException("local path is empty.");
    }
    if (maxDuration <= 0 || maxFileSize <= 0) {
      throw new IllegalArgumentException("maxDuration and maxFileSize must great than 0.");
    }
    if (null == surfaceView) {
      throw new IllegalArgumentException("SurfaceView is null.");
    }

    this.profile = profile;
    this.localPath = localPath;
    this.maxDuration = maxDuration;
    this.maxFileSize = maxFileSize;
    this.surfaceView = surfaceView;
    initRecorder();
  }

  private void initRecorder() {
    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

    mediaRecorder.setProfile(this.profile);
    mediaRecorder.setOutputFile(this.localPath);
    mediaRecorder.setMaxDuration(this.maxDuration); // seconds
    mediaRecorder.setMaxFileSize(this.maxFileSize); // Approximately 5 megabytes

    SurfaceHolder holder = this.surfaceView.getHolder();
    holder.addCallback(this);
    holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  /**
   * start to capture
   */
  public void start() {
    if (recording) {
      return;
    } else if (null != mediaRecorder) {
      mediaRecorder.start();
      recording = true;
    }
  }

  /**
   * stop capture.
   * After stop, you can read video data from local path specified prior.
   */
  public void stop() {
    if (!recording) {
      return;
    }
    stopRecorder();
    recording = false;
    initRecorder();
    prepareRecorder(surfaceView.getHolder());
  }

  private void prepareRecorder(SurfaceHolder holder) {
    mediaRecorder.setPreviewDisplay(holder.getSurface());

    try {
      mediaRecorder.prepare();
    } catch (Exception e) {
      LogUtil.log.e("failed to prepare MediaRecorder. cause: ", e);
    }
  }

  private void stopRecorder() {
    if (null != mediaRecorder) {
      mediaRecorder.stop();
      mediaRecorder.release();
      mediaRecorder = null;
    }
  }

  /**
   * SurfaceHolder.Callback interface
   * @param holder
   */
  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    prepareRecorder(holder);
  }

  /**
   * SurfaceHolder.Callback interface
   * @param holder
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {
  }

  /**
   * SurfaceHolder.Callback interface
   * @param holder
   */
  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    stopRecorder();
  }

}
