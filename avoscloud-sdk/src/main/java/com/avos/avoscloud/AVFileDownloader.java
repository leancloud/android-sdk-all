package com.avos.avoscloud;

import android.os.AsyncTask;

import org.apache.http.Header;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;

import okhttp3.Request;

/**
 * Created by wli on 15/12/22.
 * 文件下载，避免 AVFile 代码太多，所以从 AVFile 剥离出来
 * 下载的内容会存到本地
 * 流程：
 * 如果本地包含缓存，则直接返回缓存数据，反正则从网络加载数据
 */
class AVFileDownloader extends AsyncTask<String, Integer, AVException> {

  private final GetDataCallback dataCallback;
  private final GetDataStreamCallback dataStreamCallback;
  private final ProgressCallback progressCallback;
  private static final int READ_BUF_SIZE = 1024*8;

  private byte[] fileData;
  private InputStream is;

  public AVFileDownloader() {
    this.dataCallback = null;
    this.dataStreamCallback = null;
    this.progressCallback = null;
  }

  public AVFileDownloader(ProgressCallback progressCallback, GetDataCallback dataCallback) {
    this.dataCallback = dataCallback;
    this.dataStreamCallback = null;
    this.progressCallback = progressCallback;
  }

  public AVFileDownloader(ProgressCallback progressCallback, GetDataStreamCallback dataStreamCallback) {
    this.dataCallback = null;
    this.dataStreamCallback = dataStreamCallback;
    this.progressCallback = progressCallback;
  }

  protected AVException doWork(final String url) {
    fileData = null;
    if (!AVUtils.isBlankContent(url)) {
      File cacheFile = getCacheFile(url);
      if (cacheFile.exists()) {
        this.publishProgress(100);
        if (null != this.dataCallback) {
          fileData = AVPersistenceUtils.readContentBytesFromFile(cacheFile);
        } else if (null != this.dataStreamCallback) {
          try {
            is = AVPersistenceUtils.getInputStreamFromFile(cacheFile);
          } catch (IOException e) {
            LogUtil.log.e("failed to invoke doWork() url:" + url, e);
            is = null;
          }
        }
        return null;
      } else {
        return downloadFileFromNetwork(url);
      }
    } else {
      return new AVException(new IllegalArgumentException("url is null"));
    }
  }

  @Override
  protected AVException doInBackground(String... sUrl) {
    return doWork(sUrl[0]);
  }

  @Override
  protected void onProgressUpdate(Integer... progress) {
    super.onProgressUpdate(progress);
    if (progressCallback != null) {
      progressCallback.internalDone(progress[0], null);
    }
  }

  @Override
  protected void onPostExecute(AVException e) {
    super.onPostExecute(e);
    if (dataCallback != null) {
      dataCallback.internalDone(fileData, e);
    } else if (null != dataStreamCallback) {
      dataStreamCallback.internalDone0(this.is, e);
    }
  }

  /**
   * 根据 url 从网络下载文件
   * @param url
   * @return
   */
  private AVException downloadFileFromNetwork(final String url) {
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("downloadFileFromNetwork: " + url);
    }
    final File cacheFile = getCacheFile(url);

    final AVException[] errors = new AVException[1];
    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(url);

    AVHttpClient.ProgressListener listener = new AVHttpClient.ProgressListener() {
      @Override
      public void update(long bytesRead, long contentLength, boolean done) {
        publishProgress((int) (98 * bytesRead / (float) contentLength));
      }
    };
    AVHttpClient client = AVHttpClient.progressClientInstance(listener);
    client.execute(requestBuilder.build(), true, new AsyncHttpStreamResponseHandler() {

      @Override
      public void onSuccess(int statusCode, Header[] headers, InputStream data) {
        if (statusCode / 100 == 2 && null != data) {
          // read data from InputStream and save to cache File
          byte[] content = new byte[READ_BUF_SIZE];

          Lock writeLock = AVPersistenceUtils.getLock(cacheFile.getAbsolutePath()).writeLock();
          FileOutputStream out = null;
          if (writeLock.tryLock()) {
            try {
              out = new FileOutputStream(cacheFile, false);
              int currentReadSize = data.read(content);
              while (currentReadSize > 0) {
                out.write(content, 0, currentReadSize);
                currentReadSize = data.read(content);
              }
            } catch (Exception e) {
              LogUtil.log.d(e.toString());
            } finally {
              try {
                data.close();
                if (out != null) {
                  out.close();
                }
              } catch (IOException e) {
                LogUtil.log.d(e.toString());
              }
              writeLock.unlock();
            }
          }
          if (null != dataCallback) {
            fileData = AVPersistenceUtils.readContentBytesFromFile(cacheFile);
          } else if (null != dataStreamCallback) {
            try {
              is = AVPersistenceUtils.getInputStreamFromFile(cacheFile);
            } catch (IOException e) {
              LogUtil.log.e("failed to invoke downloadFileFromNetwork() url:" + url, e);
              is = null;
            }
          }
        } else if (null != data) {
          errors[0] = new AVException(statusCode, "status code is invalid");
        } else {
          errors[0] = new AVException(statusCode, "data is empty!");
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable error) {
        errors[0] = new AVException(error);
      }
    });
    this.publishProgress(100);
    return errors[0] != null ? errors[0] : null;
  }

  /**
   * 获取 url 对应的本地缓存文件
   *
   * @param url
   * @return
   */
  static File getCacheFile(String url) {
    return new File(getAVFileCachePath(), AVUtils.md5(url));
  }

  /**
   * 获取缓存的文件夹路径
   *
   * @return
   */
  static String getAVFileCachePath() {
    String fileCacheDir = AVPersistenceUtils.getCacheDir() + "/avfile/";
    File dirFile = new File(fileCacheDir);
    if (!dirFile.exists()) {
      dirFile.mkdirs();
    }
    return fileCacheDir;
  }
}