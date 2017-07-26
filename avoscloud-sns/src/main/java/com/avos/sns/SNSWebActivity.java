package com.avos.sns;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.alibaba.fastjson.JSON;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 10/31/13 Time: 5:20 PM To change this template
 * use File | Settings | File Templates.
 */

public class SNSWebActivity extends Activity {

  private static final String TAG = SNSWebActivity.class.getSimpleName();

  static class AVSNSWebViewCallback {
    Activity mActivity;

    public AVSNSWebViewCallback(Activity activity) {
      this.mActivity = activity;
    }

    @JavascriptInterface
    public void setData(String data) {
      Intent returnIntent = new Intent();

      try {
        returnIntent.putExtra(SNSBase.AUTHORIZE_RESULT, java.net.URLDecoder.decode(data, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        HashMap<String, Object> errorMap = new HashMap<String, Object>();
        errorMap.put("error", e.getMessage());
        returnIntent.putExtra(SNSBase.AUTHORIZE_RESULT, JSON.toJSONString(errorMap));
      }
      mActivity.setResult(RESULT_OK, returnIntent);
      mActivity.finish();
    }
  }

  @Override
  protected void onPause() {
    super.onPause(); // To change body of overridden methods use File | Settings | File Templates.

  }

  @Override
  protected void onResume() {
    super.onResume(); // To change body of overridden methods use File | Settings | File Templates.
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState); // To change body of overridden methods use File | Settings
                                        // | File Templates.
    setContentView(this.getResourceId("layout", "avoscloud_sns_web_activity"));
    Intent intent = getIntent();
    String url = intent.getStringExtra(SNSBase.urlTag);
    if (url != null) {
      WebView myWebView = (WebView) findViewById(this.getResourceId("id", "webview"));

      // to hide address bar.
      myWebView.setWebViewClient(new MyWebViewClient());
      myWebView.addJavascriptInterface(new AVSNSWebViewCallback(this), "snsCallback");
      myWebView.getSettings().setJavaScriptEnabled(true);
      myWebView.loadUrl(url);
    }
  }

  private int getResourceId(String type, String resourcesId) {
    return getResources()
        .getIdentifier(resourcesId, type, getApplicationContext().getPackageName());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy(); // To change body of overridden methods use File | Settings | File Templates.
  }

  class MyWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Log.i(TAG, url);
      return false;
      // view.loadUrl(url);
      // return true;
      // return super.shouldOverrideUrlLoading(view, url); //To change body of overridden methods
      // use File | Settings | File Templates.
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      // TODO Auto-generated method stub
      Log.i(TAG, url);
      super.onPageStarted(view, url, favicon);

    }

    @Override
    public void onPageFinished(WebView view, String url) {
      // TODO Auto-generated method stub
      Log.i(TAG, url);
      super.onPageFinished(view, url);
    }
  }
}
