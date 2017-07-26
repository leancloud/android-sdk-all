package com.avos.avoscloud.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVConstants;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.FindCallback;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class SearchActivity extends Activity {
  ListView listView;
  LinkedList<AVObject> searchResults = new LinkedList<AVObject>();
  SearchResultAdapter adapter;
  AVSearchQuery search;
  static final int HIGHLIGHTS_MAX_LENGTH = 200;
  FindCallback<AVObject> searchCallback;
  View loadingView;
  View emtpyResult;
  public static String highlightFontStyle = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(Resources.layout.avoscloud_search_activity(this));
    setupActionBar();
    listView = (ListView) findViewById(Resources.id.avoscloud_search_result_listview(this));
    loadingView =
        LayoutInflater.from(this).inflate(Resources.layout.avoscloud_search_loading(this), null);
    emtpyResult = findViewById(Resources.id.avoscloud_search_emtpy_result(this));
    listView.addFooterView(loadingView);
    loadingView.setVisibility(View.INVISIBLE);
    if (this.getIntent().getExtras() != null) {
      String searchString =
          getIntent().getExtras().getString(AVSearchQuery.AVOSCLOUD_DATA_EXTRA_SEARCH_KEY);
      search = JSON.parseObject(searchString, AVSearchQuery.class);
      searchCallback = new FindCallback<AVObject>() {

        @Override
        public void done(List<AVObject> avObjects, AVException avException) {
          if (avException == null) {
            searchResults.addAll(avObjects);
            if (adapter == null) {
              adapter = new SearchResultAdapter();
              listView.setAdapter(adapter);
              listView.setOnScrollListener(adapter);
            } else {
              adapter.notifyDataSetChanged();
              hideLoadingView();
            }
            if (searchResults.size() == 0) {
              emtpyResult.setVisibility(View.VISIBLE);
              listView.setVisibility(View.GONE);
            } else {
              emtpyResult.setVisibility(View.GONE);
              listView.setVisibility(View.VISIBLE);
            }

          }
        }
      };
      search.findInBackgroud(searchCallback);
    }
  }

  @SuppressLint("NewApi")
  private void setupActionBar() {
    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setCustomView(Resources.layout.avoscloud_search_actionbar(this));
      actionBar.setDisplayShowCustomEnabled(true);
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowTitleEnabled(false);
      View backButton =
          actionBar.getCustomView()
              .findViewById(Resources.id.avoscloud_search_actionbar_back(this));
      backButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          onBackPressed();
          finish();
        }
      });
    }
  }

  public void showLoadingView() {
    if (loadingView != null) {
      loadingView.setVisibility(View.VISIBLE);
    }

  }

  public void hideLoadingView() {
    if (loadingView != null) {
      loadingView.setVisibility(View.INVISIBLE);
    }
  }

  public class SearchResultAdapter extends BaseAdapter implements OnScrollListener {
    Context mContext = SearchActivity.this;
    int lastVisibleItemId;
    volatile boolean loading;

    @Override
    public int getCount() {
      return searchResults.size();
    }

    @Override
    public Object getItem(int position) {
      return searchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


      final AVObject item = (AVObject) getItem(position);
      ViewHolder holder = null;
      if (convertView == null) {
        convertView =
            LayoutInflater.from(SearchActivity.this).inflate(
                Resources.layout.avoscloud_search_result_item(mContext), null);
        holder = new ViewHolder();
        holder.title =
            (TextView) convertView.findViewById(Resources.id
                .avoscloud_search_result_title(mContext));
        holder.description =
            (TextView) convertView.findViewById(Resources.id
                .avoscloud_search_result_description(mContext));
        holder.openApp =
            (TextView) convertView.findViewById(Resources.id
                .avoscloud_search_result_open_app(mContext));
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      if (!AVUtils.isBlankString(search.getTitleAttribute())) {
        holder.title.setText(Html.fromHtml(item.get(search.getTitleAttribute()).toString()));
        holder.description.setVisibility(View.VISIBLE);
        holder.description.setText(Html
            .fromHtml(highlightStringMerge((Map<String, List<String>>) item
                .get(AVConstants.AVSEARCH_HIGHTLIGHT))));
      } else {
        holder.title.setText(Html.fromHtml(highlightStringMerge((Map<String, List<String>>) item
            .get(AVConstants.AVSEARCH_HIGHTLIGHT))));
        holder.description.setVisibility(View.GONE);
      }
      if (!AVUtils.isBlankString(item.getString(AVConstants.AVSEARCH_APP_URL))) {
        holder.openApp.setOnClickListener(new OnClickListener() {

          @Override
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            String link =
                AVUtils.isBlankString(item.getString(AVConstants.AVSEARCH_DEEP_LINK)) ? item
                    .getString(AVConstants.AVSEARCH_APP_URL) : item
                    .getString(AVConstants.AVSEARCH_DEEP_LINK);
            i.setData(Uri.parse(link));
            startActivity(i);
          }
        });
      }

      return convertView;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
        int totalItemCount) {
      lastVisibleItemId = firstVisibleItem + visibleItemCount;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

      if (lastVisibleItemId >= searchResults.size()
          && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
        showLoadingView();
        search.findInBackgroud(searchCallback);
      } else {
        hideLoadingView();
      }
    }
  }

  public static String highlightStringMerge(Map<String, List<String>> highlights) {
    if (highlights != null) {
      StringBuilder sb = new StringBuilder();
      for (String key : highlights.keySet()) {
        sb.append(AVUtils.joinCollection(highlights.get(key), "..."));
      }
      if (sb.length() > HIGHLIGHTS_MAX_LENGTH) {
        String tempString = sb.substring(0, HIGHLIGHTS_MAX_LENGTH);
        StringBuilder tempSB = new StringBuilder(tempString).reverse();
        int lastHeadTagIndex = tempSB.indexOf("<me>");
        int lastTailTagIndex = tempSB.indexOf("<me/>");
        if (lastHeadTagIndex < lastTailTagIndex) {
          return highLightStringStyle(tempSB.reverse().substring(0,
              HIGHLIGHTS_MAX_LENGTH - lastTailTagIndex));
        }
        return highLightStringStyle(tempString);
      }
      return highLightStringStyle(sb.toString());

    } else {
      return "";
    }
  }

  public static String highLightStringStyle(String string) {
    string =
        string.replaceAll("<em>", AVUtils.isBlankString(highlightFontStyle)
            ? "<font color='#E68A00'>"
            : highlightFontStyle);
    string = string.replaceAll("</em>", "</font>");
    return string;
  }

  /**
   * 您可以通过设定类似html font style来设定匹配高亮的风格
   * 
   * example:<font color='#E68A00'>
   * 
   * @param style
   */
  public static void setHighLightStyle(String style) {
    highlightFontStyle = style;
  }

  public class ViewHolder {
    TextView title;
    TextView description;
    TextView openApp;
  }
}
