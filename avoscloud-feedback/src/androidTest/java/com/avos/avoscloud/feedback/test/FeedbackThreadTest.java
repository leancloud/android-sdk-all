package com.avos.avoscloud.feedback.test;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.feedback.Comment;
import com.avos.avoscloud.feedback.FeedbackThread;
import com.avos.avoscloud.feedback.test.base.NetworkTestBase;
import com.avos.avoscloud.feedback.test.mock.MockComment;
import com.avos.avoscloud.feedback.test.mock.MockFeedbackThread;

import junit.framework.Assert;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.HttpResponseGenerator;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.io.IOException;
import java.util.List;

/**
 * Created by zhangxiaobo on 15/3/25.
 */
@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class FeedbackThreadTest extends NetworkTestBase {
  @Test
  public void testSync() throws AVException {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Assert.assertEquals(MockFeedbackThread.Url.FEEDBACK, uri);

        // String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFeedbackThread.Result.POST_FEEDBACK,
            buildJsonHeader());
      }
    });

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Assert.assertEquals(MockFeedbackThread.Url.THREAD, uri);

        // String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFeedbackThread.Result.GET,
            buildJsonHeader());
      }
    });

    FeedbackThread fbt = FeedbackThread.getInstance();
    fbt.add(new Comment(MockComment.CONTENT));
    fbt.sync(new FeedbackThread.SyncCallback() {
      @Override
      public void onCommentsSend(List<Comment> comments, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertEquals(MockComment.CONTENT, comments.get(0).getContent());
      }

      @Override
      public void onCommentsFetch(List<Comment> comments, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertEquals(MockComment.CONTENT, comments.get(0).getContent());
      }
    });


    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Assert.assertEquals(MockFeedbackThread.Url.THREAD, uri);

        try {
          String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
          Assert.assertFalse(data.contains("iid"));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new TestHttpResponse(HttpStatus.SC_OK, MockFeedbackThread.Result.POST_FEEDBACK,
            buildJsonHeader());
      }
    });

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Assert.assertEquals(MockFeedbackThread.Url.THREAD, uri);

        // String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFeedbackThread.Result.GET,
            buildJsonHeader());
      }
    });

    // add more comments
    fbt.add(new Comment(MockComment.CONTENT_ANOTHER));
    fbt.sync(new FeedbackThread.SyncCallback() {
      @Override
      public void onCommentsSend(List<Comment> comments, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertEquals(MockComment.CONTENT, comments.get(0).getContent());
      }

      @Override
      public void onCommentsFetch(List<Comment> comments, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertEquals(MockComment.CONTENT, comments.get(0).getContent());
      }
    });
  }

}
