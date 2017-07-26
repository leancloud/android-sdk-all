package com.avos.avoscloud.search;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Intent;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVConstants;
import com.avos.avoscloud.AVErrorUtils;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVExceptionHolder;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.AVRequestParams;

public class AVSearchQuery<T extends AVObject> {
  private String sid;
  private int limit = 100;
  private int skip = 0;
  private String hightlights;
  private static final String URL = "search/select";
  private List<String> fields;
  private String queryString;
  private String titleAttribute;
  private String className;
  private int hits;
  private String order;
  private AVSearchSortBuilder sortBuilder;
  private List<String> include;
  Class<T> clazz;


  /**
   * 获取当前的AVSearchSortBuilder对象
   * 
   * @return
   */
  public AVSearchSortBuilder getSortBuilder() {
    return sortBuilder;
  }

  /**
   * 设置查询的AVSearchSortBuilder，使用更丰富的排序选项。
   * 
   * @param sortBuilder
   */
  public void setSortBuilder(AVSearchSortBuilder sortBuilder) {
    this.sortBuilder = sortBuilder;
  }

  public AVSearchQuery() {
    this(null);
  }

  public AVSearchQuery(String queryString) {
    this(queryString, null);
  }

  public AVSearchQuery(String queryString, Class<T> clazz) {
    this.queryString = queryString;
    this.clazz = clazz;
    this.include = new LinkedList<String>();
    if (clazz == null) {
      this.className = AVUtils.getAVObjectClassName(AVObject.class);
    } else {
      this.className = AVUtils.getAVObjectClassName(clazz);
    }
  }

  /**
   * 获取查询的className，默认为null，即包括所有启用了应用内搜索的class
   * 
   * @return
   */
  public String getClassName() {
    return className;
  }


  /**
   * 设置查询字段列表，以逗号隔开的字符串，例如"a,b,c"，表示按照a,b,c三个字段的顺序排序，如果字段前面有负号，表示倒序，例如"a,-b"
   * 
   * @param order
   * @return this query.
   */
  public AVSearchQuery order(String order) {
    this.order = order;
    return this;
  }

  /**
   * 根据提供的key进行升序排序
   * 
   * @param key 需要排序的key
   */
  public AVSearchQuery orderByAscending(String key) {
    if (AVUtils.isBlankString(order)) {
      order = String.format("%s", key);
    } else {
      order = String.format("%s,%s", order, key);
    }
    return this;
  }

  /**
   * 根据提供的key进行降序排序
   * 
   * @param key The key to order by.
   * @return Returns the query, so you can chain this call.
   */
  public AVSearchQuery orderByDescending(String key) {
    if (AVUtils.isBlankString(order)) {
      order = String.format("-%s", key);
    } else {
      order = String.format("%s,-%s", order, key);
    }
    return this;
  }

  /**
   * Also sorts the results in ascending order by the given key. The previous sort keys have
   * precedence over this key.
   * 
   * 
   * @param key The key to order by
   * @return Returns the query so you can chain this call.
   */
  public AVSearchQuery addAscendingOrder(String key) {
    if (AVUtils.isBlankString(order)) {
      return this.orderByAscending(key);
    }

    order = String.format("%s,%s", order, key);
    return this;
  }

  /**
   * Also sorts the results in descending order by the given key. The previous sort keys have
   * precedence over this key.
   * 
   * @param key The key to order by
   * @return Returns the query so you can chain this call.
   */
  public AVSearchQuery addDescendingOrder(String key) {
    if (AVUtils.isBlankString(order)) {
      return orderByDescending(key);
    }

    order = String.format("%s,-%s", order, key);
    return this;
  }


  /**
   * 设置查询的className，否则将包括所有启用了应用内搜索的class
   * 
   * @param className
   */
  public void setClassName(String className) {
    this.className = className;
  }



  public static final String AVOSCLOUD_DATA_EXTRA_SEARCH_KEY = "com.avos.avoscloud.search.key";

  /**
   * 同步方法取得搜索结果
   * 
   * @return　List<AVObject>
   * @throws AVException
   */
  public List<AVObject> find() throws AVException {

    final List<AVObject> result = new LinkedList<AVObject>();

    getSearchResult(getParameters(queryString), true, new FindCallback<T>() {

      @Override
      public void done(List<T> avObjects, AVException avException) {

        if (avException != null || avObjects == null) {
          AVExceptionHolder.add(AVErrorUtils.createException(avException, null));
        } else {
          result.addAll(avObjects);
        }
      }

      @Override
      protected boolean mustRunOnUIThread() {
        return false;
      }
    });
    if (AVExceptionHolder.exists()) {
      throw AVExceptionHolder.remove();
    }
    return result;
  }


  @Deprecated
  public void findInBackgroud(FindCallback<T> callback) {
    this.findInBackground(callback);
  }

  /**
   *
   * @param callback
   */
  public void findInBackground(FindCallback callback) {
    getSearchResult(getParameters(queryString), false, callback);
  }

  protected void getSearchResult(AVRequestParams params, boolean sync,
      final FindCallback<T> callback) {
    PaasClient.storageInstance().getObject(URL, params, sync, null, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        try {
          callback.internalDone(processContent(content), e);
        } catch (Exception ex) {
          callback.internalDone(null, e);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        callback.internalDone(null, new AVException(error));
      }
    });
  }

  private List<T> processContent(String content) throws Exception {
    if (AVUtils.isBlankContent(content)) {
      return Collections.emptyList();
    }
    AVSearchResponse resp = new AVSearchResponse();
    resp = JSON.parseObject(content, resp.getClass());
    this.sid = resp.sid;
    this.hits = resp.hits;
    List<T> result = new LinkedList<T>();
    for (Map item : resp.results) {
      if (item != null && !item.isEmpty()) {
        AVObject object;
        if (clazz == null) {
          object =
              new AVObject(AVUtils.isBlankString(className)
                  ? (String) item.get("className")
                  : className);
        } else {
          object = clazz.newInstance();
        }
        AVUtils.copyPropertiesFromMapToAVObject(item, object);
        object.put(AVConstants.AVSEARCH_HIGHTLIGHT, item.get("_highlight"));
        object.put(AVConstants.AVSEARCH_APP_URL, item.get("_app_url"));
        object.put(AVConstants.AVSEARCH_DEEP_LINK, item.get("_deeplink"));
        result.add((T) object);
      }
    }
    return result;
  }

  /**
   * 设置搜索的结果单页大小限制,默认值为100，最大为1000
   * 
   * @param limit
   */
  public void setLimit(int limit) {
    this.limit = limit;
  }

  /**
   * 获得搜索结果的单页大小限制
   * 
   * @return
   */
  public int getLimit() {
    return this.limit;
  }

  /**
   * 返回当前返回集合的其实位置
   */
  public int getSkip() {
    return skip;
  }

  /**
   * 设置返回集合的起始位置，一般用于分页
   *
   * @return this query
   */
  public void setSkip(int skip) {
    this.skip = skip;
  }

  /**
   * 设置返回的高亮语法，默认为"*"
   * 语法规则可以参考　　http://www.elasticsearch.org/guide/en/elasticsearch/reference/current
   * /search-request-highlighting.html#highlighting-settings
   * 
   * @param hightlights
   */
  public void setHightLights(String hightlights) {
    this.hightlights = hightlights;
  }

  /**
   * 获取当前设定的语法高亮
   * 
   * @return
   */

  public String getHightLights() {
    return this.hightlights;
  }

  public void search() {
    Intent intent = new Intent(AVOSCloud.applicationContext, SearchActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(AVSearchQuery.AVOSCLOUD_DATA_EXTRA_SEARCH_KEY, JSON.toJSONString(this));
    AVOSCloud.applicationContext.startActivity(intent);
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public List<String> getFields() {
    return this.fields;
  }

  private AVRequestParams getParameters(String query) {
    AVRequestParams params = new AVRequestParams();
    params.put("q", query);
    if (!AVUtils.isBlankString(sid)) {
      params.put("sid", sid);
    }
    if (!AVUtils.isBlankString(hightlights)) {
      params.put("highlights", hightlights);
    } else {
      params.put("highlights", "*");
    }
    if (fields != null && fields.size() > 0) {
      params.put("fields", AVUtils.joinCollection(fields, ","));
    }
    if (limit > 0) {
      params.put("limit", String.valueOf(limit));
    }
    if (skip > 0) {
      params.put("skip", String.valueOf(skip));
    }
    if (!AVUtils.isBlankString(order)) {
      params.put("order", order);
    }
    if (sortBuilder != null) {
      params.put("sort", AVUtils.jsonStringFromObjectWithNull(sortBuilder.getSortFields()));
    }
    if (!include.isEmpty()) {
      String value = AVUtils.joinCollection(include, ",");
      params.put("include", value);
    }
    if (!AVUtils.isBlankString(className)) {
      params.put("clazz", className);
    }
    return params;
  }

  /**
   * lastId是作为分页的依据，根据设置lastId需要查询的页数 设置为null回到第一页
   * 
   * @deprecated 使用setSid(String)替代
   * @param lastId
   */
  @Deprecated
  public void setLastId(String lastId) {
    this.sid = lastId;
  }

  /**
   * 设置查询id，通常您都不需要调用这个方法来设置，只要不停调用find就可以实现分页。不过如果需要将查询分页传递到其他Activity，则可能需要通过传递sid来实现。
   * 
   * @param sid
   */
  public void setSid(String sid) {
    this.sid = sid;
  }

  /**
   * 获取本次查询的id，注意，它不是返回结果中对象的objectId，而是表示本次AVSearchQuery查询的id
   * 
   * @deprecated 请使用getSid()替代。
   * @return
   */
  @Deprecated
  public String getLastId() {
    return this.sid;
  }

  /**
   * 获取本次查询的id，注意，它不是返回结果中对象的objectId，而是表示本次AVSearchQuery查询的id
   * 
   * @return
   */
  public String getSid() {
    return this.sid;
  }



  /**
   * 此选项为AVOSCloud SearchActivity使用 指定Title所对应的Field
   * 
   * @param titleAttribute
   */
  public void setTitleAttribute(String titleAttribute) {
    this.titleAttribute = titleAttribute;
  }

  /**
   * 获取当前指定的title 对应的Field
   * 
   * @return
   */
  public String getTitleAttribute() {
    return titleAttribute;
  }

  /**
   * 设置搜索的查询语句。
   * 详细语法可以参考http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl
   * -query-string-query.html#query-string-syntax
   * 
   * @param query
   */
  public void setQueryString(String query) {
    if (!((this.queryString == null && query == null) || (this.queryString != null && this.queryString
        .equals(query)))) {
      this.sid = null;
    }
    this.queryString = query;

  }

  public String getQueryString() {
    return queryString;
  }

  public int getHits() {
    return this.hits;
  }

  public void include(String key) {
    this.include.add(key);
  }
}
