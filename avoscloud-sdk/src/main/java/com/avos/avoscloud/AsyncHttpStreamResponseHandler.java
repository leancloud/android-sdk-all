package com.avos.avoscloud;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by jwfing on 8/10/17.
 */
public abstract class AsyncHttpStreamResponseHandler implements Callback {
  protected GenericObjectCallback callback;

  public AsyncHttpStreamResponseHandler(GenericObjectCallback callback) {
    this.callback = callback;
  }

  public AsyncHttpStreamResponseHandler() {

  }

  protected GenericObjectCallback getCallback() {
    return callback;
  }

  protected void setCallback(GenericObjectCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    onFailure(0, getHeaders(call.request().headers()), e);
  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    this.onSuccess(response.code(), getHeaders(response.headers()), response.body().byteStream());
  }

  public abstract void onSuccess(int statusCode, Header[] headers, InputStream body);

  public abstract void onFailure(int statusCode, Header[] headers, Throwable error);

  static Header[] getHeaders(Headers headers) {
    if (headers != null && headers.size() > 0) {
      Header[] httpHeaders = new Header[headers.size()];
      for (int index = 0; index < headers.size(); index++) {
        final String key = headers.name(index);
        final String value = headers.get(key);
        httpHeaders[index] = new BasicHeader(key, value);
      }
      return httpHeaders;
    }
    return null;
  }
}
