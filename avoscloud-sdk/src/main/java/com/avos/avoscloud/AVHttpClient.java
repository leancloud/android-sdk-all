package com.avos.avoscloud;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by wli on 2017/5/16.
 */
public class AVHttpClient {
  private static AVHttpClient avHttpClient;
  public static final MediaType JSON = MediaType.parse(PaasClient.DEFAULT_CONTENT_TYPE);

  private OkHttpClient okHttpClient;

  private AVHttpClient(OkHttpClient client, int connectTimeout, ProgressInterceptor interceptor) {
    OkHttpClient.Builder builder;
    if (null != client) {
      // 避免直接 new OkHttpClient，这里虽然 okHttpClient 是多实例，但是是共享同一线程池的
      builder = client.newBuilder();
    } else {
      builder = new OkHttpClient.Builder();
      builder.dns(DNSAmendNetwork.getInstance());
      builder.addInterceptor(new RequestStatisticInterceptor());
    }
    builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
    if (null != interceptor) {
      builder.addNetworkInterceptor(interceptor);
    }
    okHttpClient = builder.build();
  }

  public static synchronized AVHttpClient clientInstance() {
    if (avHttpClient == null) {
      avHttpClient = new AVHttpClient(null, AVOSCloud.getNetworkTimeout(), null);
    }
    return avHttpClient;
  }

  public static synchronized AVHttpClient progressClientInstance(ProgressListener progressListener) {
    if (avHttpClient == null) {
      avHttpClient = new AVHttpClient(null, AVOSCloud.getNetworkTimeout(), null);
    }

    ProgressInterceptor progressInterceptor = new ProgressInterceptor(progressListener);
    return new AVHttpClient(avHttpClient.okHttpClient, AVOSCloud.getNetworkTimeout(), progressInterceptor);
  }

  /**
   * 获取新的
   * @param connectTimeout 单位毫秒
   * @return
   */
  public static synchronized AVHttpClient newClientInstance(int connectTimeout) {
    if (avHttpClient == null) {
      avHttpClient = new AVHttpClient(null, AVOSCloud.getNetworkTimeout(), null);
    }
    return new AVHttpClient(avHttpClient.okHttpClient, connectTimeout, null);
  }

  public synchronized OkHttpClient.Builder getOkHttpClientBuilder() {
    return okHttpClient.newBuilder();
  }

  public void execute(Request request, boolean sync, final Callback handler) {
    Call call = getCall(request);
    if (sync) {
      try {
        Response response = call.execute();
        handler.onResponse(call, response);
      } catch (IOException e) {
        handler.onFailure(call, e);
      }
    } else {
      call.enqueue(handler);
    }
  }

  private synchronized Call getCall(Request request) {
    return okHttpClient.newCall(request);
  }

  private static class RequestStatisticInterceptor implements Interceptor {

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
      Request request = chain.request();
      long requestStartTime = System.currentTimeMillis();
      boolean requestStatistics = !AVUtils.isBlankString(request.header(PaasClient.REQUEST_STATIS_HEADER));
      try {
        Response response = chain.proceed(request);

        if (requestStatistics) {
          long timeInterval = System.currentTimeMillis() - requestStartTime;
          RequestStatisticsUtil.getInstance().recordRequestTime(response.code(), false,
            timeInterval);
        }
        return response;
      } catch (IOException e) {
        if (requestStatistics) {
          long timeInterval = System.currentTimeMillis() - requestStartTime;
          RequestStatisticsUtil.getInstance().recordRequestTime(0, e instanceof SocketTimeoutException, timeInterval);
        }
        throw e;
      }
    }
  }

  private static class ProgressInterceptor implements Interceptor {
    private ProgressListener progressListener;

    public ProgressInterceptor(ProgressListener progressListener) {
      super();
      this.progressListener = progressListener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
      Response originalResponse = chain.proceed(chain.request());
      return originalResponse.newBuilder()
        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
        .build();
    }
  }

  private static class ProgressResponseBody extends ResponseBody {

    private final ResponseBody responseBody;
    private final ProgressListener progressListener;
    private BufferedSource bufferedSource;

    ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
      this.responseBody = responseBody;
      this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
      return responseBody.contentType();
    }

    @Override
    public long contentLength() {
      return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
      if (bufferedSource == null) {
        bufferedSource = Okio.buffer(source(responseBody.source()));
      }
      return bufferedSource;
    }

    private Source source(Source source) {
      return new ForwardingSource(source) {
        long totalBytesRead = 0L;

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
          long bytesRead = super.read(sink, byteCount);
          totalBytesRead += bytesRead != -1 ? bytesRead : 0;
          progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
          return bytesRead;
        }
      };
    }
  }

  interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
  }
}