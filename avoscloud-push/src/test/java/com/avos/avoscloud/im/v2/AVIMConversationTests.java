package com.avos.avoscloud.im.v2;

import com.avos.avoscloud.AVRequestParams;
import com.avos.avoscloud.BuildConfig;
import com.avos.avoscloud.QueryConditions;
import com.avos.avoscloud.QueryOperation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by fengjunwen on 2017/12/19.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "AndroidManifest.xml", sdk = 23)
public class AVIMConversationTests {
  @Before
  public void setup() {
    ;
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testMemberInfoQueryConditions() {
    QueryConditions conditions = new QueryConditions();
    conditions.addWhereItem("conversationId", QueryOperation.EQUAL_OP, "this.conversationId");
    conditions.addWhereItem("peerId", QueryOperation.EQUAL_OP, "memberId");
    conditions.assembleParameters();
    Map<String, String> paramMap = conditions.getParameters();
    for( Entry<String, String> entry: paramMap.entrySet()) {
      System.out.println("key:" + entry.getKey() + ", value:" + entry.getValue());
    }
    AVRequestParams params = new AVRequestParams(conditions.getParameters());
    String queryString = params.getQueryString().trim();
    System.out.println("queryString=" + queryString);
    String whole_url = params.getWholeUrl("/1.1/classes/_ConversationMemberInfo");
    System.out.println("wholeUrl=" + whole_url);
    assert (null != queryString && queryString.length() > 0);
  }
}
