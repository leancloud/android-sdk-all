package com.avos.avoscloud.test;

import android.os.Environment;

import com.avos.avoscloud.AVACL;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.DeleteCallback;
import com.avos.avoscloud.GetDataCallback;
import com.avos.avoscloud.GetFileCallback;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.signature.Base64Decoder;
import com.avos.avoscloud.test.base.NetworkTestBase;
import com.avos.avoscloud.test.mock.MockFile;
import com.avos.avoscloud.test.mock.MockObject;
import com.avos.avoscloud.test.mock.MockUser;

import junit.framework.Assert;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.HttpResponseGenerator;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


/**
 * Created by zhangxiaobo on 15/2/3.
 */


@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class FileTest extends NetworkTestBase {
  private AVFile avFile;

  @Before
  public void setUp() throws IOException {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    makeTmpFile();
  }

  @After
  public void tearDown() {
    deleteTmpFile();
  }

  @Test
  public void testGetFileWithObjectIdAsync() throws InterruptedException {
    // Mock the REST API response.
    getAvFileAsync();
    assertFile();

    // test result error

  }

  @Test
  public void testGetFileWithObjectId() throws AVException, FileNotFoundException {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    avFile = AVFile.withObjectId(MockFile.OBJECT_ID);

    assertFile();

    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    avFile = AVFile.parseFileWithObjectId(MockFile.OBJECT_ID);

    assertFile();
  }

  @Test
  public void testBuildAvFileWithFile() throws IOException {
    // Make a File with content 'Leancloud'.
    File directory = Environment.getExternalStorageDirectory();
    File file = new File(directory, MockFile.NAME);
    AVFile avFile = AVFile.withFile(MockFile.NAME, file);

    Assert.assertEquals(MockFile.NAME, avFile.getName());
    Assert.assertEquals(file.length(), avFile.getSize());


    // Test build failed.
    try {
      AVFile.withFile("test", new File("/abc.test"));
    } catch (IOException exception) {
      Assert.assertTrue(exception instanceof FileNotFoundException);
    }

    AVFile avFile2 = AVFile.parseFileWithFile(MockFile.NAME, file);
    Assert.assertEquals(MockFile.NAME, avFile.getName());
    Assert.assertEquals(file.length(), avFile.getSize());
  }

  @Test
  public void testWithAvObject() {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockObject.Result.QUERY, buildJsonHeader());

    AVQuery<AVObject> query = new AVQuery<AVObject>("TestObject");
    AVObject testObject = null;
    try {
      testObject = query.get(MockObject.OBJECT_ID);
    } catch (AVException exception) {
      exception.printStackTrace();
    }

    avFile = AVFile.withAVObject(testObject);
    Assert.assertEquals(MockObject.OBJECT_ID, avFile.getObjectId());
  }

  @Test
  public void testAbsoluteLocalPath() throws Exception {
    AVFile file = AVFile.withAbsoluteLocalPath("file", MockFile.PATH);
    Assert.assertTrue(file.isDirty());

    assertMockSaveResponse(false);
    file.save();

    Assert.assertFalse(file.isDirty());


    file = AVFile.parseFileWithAbsoluteLocalPath("file", MockFile.PATH);
    Assert.assertTrue(file.isDirty());

    assertMockSaveResponse(false);
    file.save();

    Assert.assertFalse(file.isDirty());
  }

  @Test
  public void testParseFileWithAvObject() {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK,
            MockObject.Result.QUERY, buildJsonHeader());

    AVQuery<AVObject> query = new AVQuery<AVObject>("TestObject");
    AVObject testObject = null;
    try {
      testObject = query.get(MockObject.OBJECT_ID);
    } catch (AVException exception) {
      exception.printStackTrace();
    }

    avFile = AVFile.parseFileWithAVObject(testObject);
    Assert.assertEquals(avFile.getObjectId(), MockObject.OBJECT_ID);
  }

  @Test
  public void testGetThumbnailUrl() {
    getAvFileAsync();
    // Check avFile already construct.
    Assert.assertNotNull(avFile);
    String thumbnailUrl = avFile.getThumbnailUrl(false, 200, 100);

    Assert.assertEquals(MockFile.Url.THUMBNAILL, thumbnailUrl);
  }

  @Test
  public void testSaveSync() throws AVException {
    // Mock login response
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockUser.Result.LOGIN, buildJsonHeader());
    assertMockSaveResponse(false);

    avFile = new AVFile(MockFile.NAME, MockFile.CONTENT.getBytes());

    Assert.assertTrue(avFile.isDirty());

    AVUser user = AVUser.logIn(MockUser.NAME, MockUser.PASSWORD);
    AVACL acl = new AVACL();
    acl.setReadAccess(user, true);
    acl.setWriteAccess(user, true);

    avFile.setACL(acl);
    avFile.save();

    Assert.assertFalse(avFile.isDirty());
    Assert.assertEquals(MockFile.NAME, avFile.getOriginalName());
  }

  @Test
  public void testSaveAsync() throws AVException {
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockUser.Result.LOGIN, buildJsonHeader());
    assertMockSaveResponse(false);

    avFile = new AVFile(MockFile.NAME, MockFile.CONTENT.getBytes());

    AVUser user = AVUser.logIn(MockUser.NAME, MockUser.PASSWORD);
    AVACL acl = new AVACL();
    acl.setReadAccess(user, true);
    acl.setWriteAccess(user, true);

    avFile.setACL(acl);

    avFile.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException exception) {
        Assert.assertNotNull(exception);
      }
    });
  }

  @Test
  public void testGetDataSync() throws AVException {
    getAvFileAsync();
    // Check avFile already construct.
    Assert.assertNotNull(avFile);

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.GET, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFile.CONTENT, buildJsonHeader());
      }
    });

    avFile.getData();
    Assert.assertTrue(avFile.isDataAvailable());
  }

  @Test
  public void testGetDataAsync() throws AVException {
    getAvFileAsync();
    // Check avFile already construct.
    Assert.assertNotNull(avFile);

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.GET, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFile.CONTENT, buildJsonHeader());
      }
    });

    avFile.getDataInBackground(new GetDataCallback() {
      @Override
      public void done(byte[] data, AVException exception) {
        Assert.assertNull(exception);
        Assert.assertEquals(MockFile.MD5, AVUtils.computeMD5(data));
      }
    });
    Assert.assertTrue(avFile.isDataAvailable());
  }

  @Test
  public void testAddMetaData() throws AVException {
    assertMockSaveResponse(true);

    avFile = new AVFile(MockFile.NAME, MockFile.CONTENT.getBytes());
    avFile.addMetaData("width", 100);
    avFile.addMetaData("height", 200);

    avFile.save();
  }

  @Test
  public void testRemoveMetaData() throws AVException {
    assertMockSaveResponse(true);

    avFile = new AVFile(MockFile.NAME, MockFile.CONTENT.getBytes());
    avFile.addMetaData("width", 100);
    avFile.addMetaData("height", 200);
    avFile.save();

    assertMockSaveResponse(false);
    avFile.removeMetaData("width");
    avFile.removeMetaData("height");
    avFile.save();

    Assert.assertNull(avFile.getMetaData("width"));
    Assert.assertNull(avFile.getMetaData("height"));
  }

  @Test
  public void testClearMetaData() throws AVException {
    assertMockSaveResponse(false);

    avFile = new AVFile(MockFile.CONTENT.getBytes());
    avFile.addMetaData("width", 100);
    avFile.addMetaData("height", 200);
    avFile.clearMetaData();
    avFile.save();
  }

  @Test
  public void testNetworkFile() throws AVException {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String content = null;
        try {
          content = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
        } catch (IOException e) {
          e.printStackTrace();
        }
        Assert.assertTrue(content.contains(MockFile.Result.METADATA_WIDTH));
        Assert.assertTrue(content.contains(MockFile.Result.METADATA_HEIGHT));
        Assert.assertEquals(MockFile.Url.Files, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFile.Result.SAVE, buildJsonHeader());
      }
    });

    HashMap<String, Object> metaData = new HashMap<String, Object>();
    metaData.put("width", 100);
    metaData.put("height", 200);
    avFile = new AVFile(MockFile.NAME, MockFile.URL, metaData);
    avFile.save();
  }

  @Test
  public void testCancel() throws AVException {
    assertMockSaveResponse(false);

    avFile = new AVFile(MockFile.NAME, MockFile.CONTENT.getBytes());
    avFile.saveInBackground();

    avFile.cancel();
  }

  @Test
  public void testDelete() throws AVException, FileNotFoundException {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.DELETE, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
    avFile = AVFile.withObjectId(MockFile.OBJECT_ID);
    avFile.delete();

  }

  @Test
  public void testDeleteEventually() throws AVException, FileNotFoundException {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.DELETE, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.DELETE, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });

    avFile = AVFile.withObjectId(MockFile.OBJECT_ID);
    avFile.deleteEventually();

    avFile.deleteEventually(new DeleteCallback() {
      @Override
      public void done(AVException exception) {
        Assert.assertNull(exception);
      }
    });
  }

  @Test
  public void testDeleteAsync() throws AVException, FileNotFoundException {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.DELETE, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
    avFile = AVFile.withObjectId(MockFile.OBJECT_ID);
    avFile.deleteInBackground();

    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.DELETE, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
    avFile = AVFile.withObjectId(MockFile.OBJECT_ID);
    avFile.deleteInBackground(new DeleteCallback() {
      @Override
      public void done(AVException exception) {
        Assert.assertNull(exception);
      }
    });

  }

  private void assertFile() {
    Assert.assertEquals(MockFile.Url.GET, avFile.getUrl());
    Assert.assertEquals(MockFile.BUCKET, avFile.getBucket());
    Assert.assertEquals(MockFile.SIZE, avFile.getSize());
    Assert.assertEquals(MockFile.OBJECT_ID, avFile.getObjectId());
    Assert.assertEquals(MockObject.OBJECT_ID, avFile.getOwnerObjectId());
  }

  private void getAvFileAsync() {
    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());

    AVFile.withObjectIdInBackground(MockFile.OBJECT_ID,
        new GetFileCallback<AVFile>() {
          @Override
          public void done(AVFile file, AVException exception) {
            if (exception == null) {
              avFile = file;
            } else {
              exception.printStackTrace();
            }
          }
        });


    Robolectric
        .addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.GET, buildJsonHeader());

    AVFile.parseFileWithObjectIdInBackground(MockFile.OBJECT_ID,
        new GetFileCallback<AVFile>() {
          @Override
          public void done(AVFile file, AVException exception) {
            if (exception == null) {
              avFile = file;
            } else {
              exception.printStackTrace();
            }
          }
        });
  }

  private void assertMockSaveResponse(final boolean addMetadata) {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        if (addMetadata) {
          try {
            String content = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
            Assert.assertTrue(content.contains(MockFile.Result.METADATA_WIDTH));
            Assert.assertTrue(content.contains(MockFile.Result.METADATA_HEIGHT));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        Assert.assertEquals(MockFile.Url.QINIU, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFile.Result.SAVE, buildJsonHeader());
      }
    });

    // create block request
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals(MockFile.Url.QINIU_CREATE_BLOCK, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, MockFile.Result.CREATE_BLOCK);
      }
    });

    // merge block request
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        String uri = httpRequest.getRequestLine().getUri();
        Assert.assertTrue(uri.startsWith(MockFile.Url.QINIU_MKFILE));

        // verify key
        return new TestHttpResponse(HttpStatus.SC_OK, String.format(MockFile.Result.UPLOAD,
            Base64Decoder.decode(uri.substring(MockFile.Url.QINIU_MKFILE.length()))));
      }
    });
  }

  private void makeTmpFile() throws IOException {
    File directory = Environment.getExternalStorageDirectory();
    File file = new File(directory, MockFile.NAME);
    FileWriter fileWriter = new FileWriter(file, false);
    fileWriter.write(MockFile.CONTENT);
    fileWriter.close();
  }

  private void deleteTmpFile() {
    File directory = Environment.getExternalStorageDirectory();
    File file = new File(directory, MockFile.NAME);
    if (file.exists()) {
      file.delete();
    }
  }

}
