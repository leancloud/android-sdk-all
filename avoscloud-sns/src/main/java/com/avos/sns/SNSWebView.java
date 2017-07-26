package com.avos.sns;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 11/4/13 Time: 5:26 PM To change this template use
 * File | Settings | File Templates.
 */
public class SNSWebView extends WebView {

  private static final String TAG = SNSWebView.class.getSimpleName();

  public SNSWebView(Context context) {
    super(context);
  }

  public SNSWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SNSWebView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void loadUrl(String url) {
    Log.i(TAG, url);
    super.loadUrl(url); // To change body of overridden methods use File | Settings | File
                        // Templates.
  }

}
