package com.avos.avoscloud.test;

import android.util.Log;

import com.alibaba.fastjson.parser.ParserConfig;
import com.avos.avoscloud.AVACL;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVRelation;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.DeleteCallback;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.RefreshCallback;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.signature.Base64Decoder;
import com.avos.avoscloud.test.base.NetworkTestBase;
import com.avos.avoscloud.test.mock.Game;
import com.avos.avoscloud.test.mock.GameScore;
import com.avos.avoscloud.test.mock.MockFile;
import com.avos.avoscloud.test.mock.MockGeoPoint;
import com.avos.avoscloud.test.mock.MockObject;
import com.avos.avoscloud.test.mock.MockUser;
import com.avos.avoscloud.test.mock.MyUser;

import junit.framework.Assert;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.HttpResponseGenerator;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhangxiaobo on 15/3/4.
 */
@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class AvObjectTest extends NetworkTestBase {

  @Test
  public void testAdd() throws AVException {
    final AVObject gameScore = new AVObject(MockObject.CLASS_NAME);

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        try {
          String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
          Assert.assertTrue(data.contains("\"history\":[1000,1100,1200,1250,1300]"));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new TestHttpResponse(HttpStatus.SC_OK, String.format(MockObject.Result.BATCH_SAVE,
            gameScore.getUuid()),
            buildJsonHeader());
      }
    });

    gameScore.put("score", 1400);
    gameScore.put("playerNames", "steve");
    gameScore.add("history", 1000);
    gameScore.add("history", 1100);
    gameScore.addUnique("history", 1100);
    List historyList = new LinkedList();
    historyList.add(1200);
    historyList.add(1250);
    gameScore.addAll("history", historyList);
    historyList.add(1300);
    gameScore.addAllUnique("history", historyList);
    gameScore.save();

    Assert.assertTrue(gameScore.containsKey("history"));
    Assert.assertFalse(gameScore.containsKey("max"));
  }

  @Test
  public void testCreate() {
    AVObject avObject = AVObject.create(MockObject.CLASS_NAME);
    Assert.assertEquals(MockObject.CLASS_NAME, avObject.getClassName());
  }

  @Test
  public void testDelete() throws AVException {
    AVObject avObject = new AVObject(MockObject.CLASS_NAME);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject.getUuid()),
        buildJsonHeader());
    mockDeleteResp();
    avObject.save();
    avObject.delete();
  }

  @Test
  public void testDeleteInBackground() throws AVException {
    AVObject avObject = new AVObject(MockObject.CLASS_NAME);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject.getUuid()),
        buildJsonHeader());
    mockDeleteResp();
    avObject.save();
    avObject.deleteInBackground();
  }

  @Test
  public void testDeleteAll() throws AVException {
    List<AVObject> list = saveObjectList();
    AVObject.deleteAll(list);
  }

  @Test
  public void testDeleteAllInBackground() throws AVException {
    List<AVObject> list = saveObjectList();
    AVObject.deleteAllInBackground(list, new DeleteCallback() {
      @Override
      public void done(AVException exception) {
        Assert.assertNull(exception);
      }
    });
  }

  @Test
  public void testDeleteEventually() throws AVException {
    AVObject avObject = AVObject.create(MockObject.CLASS_NAME);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject.getUuid()),
        buildJsonHeader());
    mockDeleteResp();
    avObject.save();
    avObject.deleteEventually();
  }

  @Test
  public void testEquals() throws AVException {
    AVObject avObjectA = AVObject.create(MockObject.CLASS_NAME);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObjectA.getUuid()),
        buildJsonHeader());
    avObjectA.save();
    AVObject avObjectB = AVObject.create(MockObject.CLASS_NAME);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObjectB.getUuid()),
        buildJsonHeader());
    avObjectB.save();
    Assert.assertEquals(avObjectA, avObjectB);
  }

  @Test
  public void testFetch() throws Exception {
    AVObject gameScore = AVObject.create(MockObject.CLASS_NAME);
    gameScore.put("test", "hello world");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScore.getUuid()),
        buildJsonHeader());

    mockFetchResp();
    gameScore.save();
    gameScore.fetch();
    Assert.assertEquals(gameScore.getInt("score"), 1200);
    Assert.assertEquals(gameScore.getLong("score"), 1200);
    Assert.assertEquals(gameScore.getNumber("score"), 1200);
    Assert.assertEquals(MockUser.NICKNAME, new String(gameScore.getBytes("keyByte")));
    Calendar cal = Calendar.getInstance();
    cal.setTime(gameScore.getDate("birthday"));
    Assert.assertEquals(1, cal.get(Calendar.MONTH));
    Assert.assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    Assert.assertEquals(3.14, gameScore.getDouble("pie"));
    JSONArray history = gameScore.getJSONArray("history");
    Assert.assertEquals(2, history.length());
    Assert.assertEquals(2, gameScore.getList("history").size());
    Assert.assertEquals(MockUser.NICKNAME, gameScore.getJSONObject("winner").get("name"));
    Assert.assertTrue(gameScore.has("score"));

    gameScore.fetchIfNeededInBackground(new GetCallback<AVObject>() {
      @Override
      public void done(AVObject object, AVException exception) {
        Assert.assertEquals(object.get("score"), 1200);
      }
    });

    // include game
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.FETCH_INCLUDE,
            buildJsonHeader());
      }
    });
    gameScore.fetch("game");
    Assert.assertEquals("COC", gameScore.getAVObject("game").getString("name"));
    Assert.assertEquals("COC", gameScore.getAVObject("game", GameScore.class).getString("name"));
  }

  @Test
  public void testFetchAll() throws AVException {
    AVObject gameScoreA = AVObject.create(MockObject.CLASS_NAME);
    gameScoreA.put("test", "hello world");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScoreA.getUuid()),
        buildJsonHeader());
    gameScoreA.save();

    AVObject gameScoreB = AVObject.create(MockObject.CLASS_NAME);
    gameScoreB.put("test", "hello world");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScoreB.getUuid()),
        buildJsonHeader());
    gameScoreB.save();

    List<AVObject> list = new LinkedList<AVObject>();
    list.add(gameScoreA);
    list.add(gameScoreB);

    mockFetchResp();
    mockFetchResp();
    AVObject.fetchAll(list);
    Assert.assertEquals(gameScoreA.get("score"), 1200);
    Assert.assertEquals(gameScoreB.get("score"), 1200);

    AVObject.fetchAllIfNeeded(list);
    Assert.assertEquals(gameScoreA.get("score"), 1200);
    Assert.assertEquals(gameScoreB.get("score"), 1200);

    AVObject.fetchAllIfNeededInBackground(list, new FindCallback<AVObject>() {
      @Override
      public void done(List<AVObject> avObjects, AVException avException) {
        Assert.assertEquals(avObjects.get(0).get("score"), 1200);
        Assert.assertEquals(avObjects.get(1).get("score"), 1200);
      }
    });

    mockFetchResp();
    mockFetchResp();
    AVObject.fetchAllInBackground(list, new FindCallback<AVObject>() {
      @Override
      public void done(List<AVObject> avObjects, AVException avException) {
        Assert.assertEquals(avObjects.get(0).get("score"), 1200);
        Assert.assertEquals(avObjects.get(1).get("score"), 1200);
      }
    });
  }

  @Test
  public void testCreateWithoutData() throws AVException {
    AVObject avObject = AVObject.createWithoutData(MockObject.CLASS_NAME, MockObject.OBJECT_ID);
    Assert.assertFalse(avObject.isDataAvailable());
    mockFetchResp();
    avObject.fetch();
    Assert.assertTrue(avObject.isDataAvailable());

    avObject = AVObject.createWithoutData(GameScore.class, MockObject.OBJECT_ID);
    Assert.assertFalse(avObject.isDataAvailable());
    mockFetchResp();
    avObject.fetch();
    Assert.assertTrue(avObject.isDataAvailable());
  }

  @Test
  public void testGetAvFile() throws AVException {
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.SAVE, buildJsonHeader());

    // create block request
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockFile.Result.CREATE_BLOCK,
        buildJsonHeader());

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

    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockObject.Result.BATCH_SAVE,
        buildJsonHeader());

    AVObject gameScore = new AVObject(MockObject.CLASS_NAME);
    gameScore.put("file", new AVFile("record", MockFile.CONTENT.getBytes()));
    gameScore.save();

    Assert.assertEquals(MockFile.CONTENT, new String(gameScore.getAVFile("file").getData()));
  }

  @Test
  public void testGetAvGeoPoint() throws AVException {
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockObject.Result.BATCH_SAVE,
        buildJsonHeader());
    AVObject gameScore = new AVObject(MockObject.CLASS_NAME);
    gameScore.put("position", new AVGeoPoint(MockGeoPoint.latitude, MockGeoPoint.longtitude));
    gameScore.save();

    Assert.assertEquals(MockGeoPoint.latitude, gameScore.getAVGeoPoint("position").getLatitude());
    Assert
        .assertEquals(MockGeoPoint.longtitude, gameScore.getAVGeoPoint("position").getLongitude());
  }

  @Test
  public void testGetAvUser() throws AVException {
    mockLogin();
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockObject.Result.BATCH_SAVE,
        buildJsonHeader());

    AVObject gameScore = new AVObject(MockObject.CLASS_NAME);
    gameScore.put("user", AVUser.logIn(MockUser.NAME, MockUser.PASSWORD));
    gameScore.save();

    Assert.assertEquals(MockUser.NAME, gameScore.getAVUser("user").getUsername());

    mockLogin();
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockObject.Result.BATCH_SAVE,
        buildJsonHeader());
    AVObject gameScore2 = new AVObject(MockObject.CLASS_NAME);
    MyUser user = AVUser.logIn(MockUser.NAME, MockUser.PASSWORD, MyUser.class);
    gameScore2.put("user", user);
    gameScore2.save();

    ParserConfig.getGlobalInstance().setAsmEnable(false);
    Assert
        .assertEquals(MockUser.NAME, gameScore2.getAVUser("user", MyUser.class).getUsername());
  }

  private void mockLogin() {
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK, MockUser.Result.LOGIN, buildJsonHeader());
  }

  @Test
  public void testGetList() throws AVException {
    AVObject gameScore = AVObject.create(MockObject.CLASS_NAME);
    gameScore.put("test", "hello world");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScore.getUuid()),
        buildJsonHeader());

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.FETCH_LIST,
            buildJsonHeader());
      }
    });

    gameScore.save();
    gameScore.fetch();
    Assert.assertEquals(2, gameScore.getJSONArray("history").length());
    Assert.assertEquals(2, gameScore.getList("history").size());
    Assert.assertEquals(3, gameScore.getList("games", Game.class).size());
  }

  @Test
  public void testGetQuery() {
    AVQuery<Game> query = AVObject.getQuery(Game.class);
    Assert.assertEquals(Game.class.getSimpleName(), query.getClassName());
  }

  @Test
  public void testGetRelation() throws AVException {
    mockLogin();

    AVUser avUser = AVUser.logIn(MockUser.NAME, MockUser.PASSWORD);
    AVRelation<AVObject> relation = avUser.getRelation("likes");
    AVRelation<AVObject> relationNull = avUser.getRelation("code");

    Assert.assertEquals("likes", relation.getKey());
    Assert.assertNull(relationNull);
  }

  @Test
  public void testHasSameId() {
    AVObject post = AVObject.createWithoutData("Post", "123");
    AVObject post2 = AVObject.createWithoutData("Post", "456");
    AVObject post3 = AVObject.createWithoutData("Post", "123");
    Assert.assertTrue(post.hasSameId(post3));
    Assert.assertFalse(post.hasSameId(post2));
  }

  @Test
  public void testIncrement() {
    AVObject player = new AVObject("Player");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, player.getUuid()),
        buildJsonHeader());
    player.put("goldCoins", 1);
    player.increment("goldCoins");
    player.saveInBackground();

    Assert.assertEquals(2, player.getInt("goldCoins"));
    Assert.assertEquals(1, player.keySet().size());
  }

  @Test
  public void testRefresh() throws AVException {
    AVObject gameScore = AVObject.create(MockObject.CLASS_NAME);
    gameScore.put("test", "hello world");
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScore.getUuid()),
        buildJsonHeader());

    mockFetchResp();
    gameScore.save();
    gameScore.refresh();
    Assert.assertTrue(gameScore.has("score"));
    Assert.assertEquals(gameScore.getInt("score"), 1200);

    mockFetchResp();
    gameScore.refreshInBackground(new RefreshCallback<AVObject>() {
      @Override
      public void done(AVObject object, AVException exception) {
        Assert.assertEquals(object.get("score"), 1200);
      }
    });

    // include game
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.FETCH_INCLUDE,
            buildJsonHeader());
      }
    });
    gameScore.refresh("game");
    Assert.assertEquals("COC", gameScore.getAVObject("game").getString("name"));

    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.FETCH_INCLUDE,
            buildJsonHeader());
      }
    });
    gameScore.refreshInBackground("game", new RefreshCallback<AVObject>() {
      @Override
      public void done(AVObject object, AVException exception) {
        Assert.assertEquals("COC", object.getAVObject("game").getString("name"));
      }
    });
  }

  @Test
  public void testRemove() throws AVException {
    AVObject gameScore = AVObject.create(MockObject.CLASS_NAME);
    gameScore.put(MockObject.TEST_KEY, MockObject.TEST_VALUE);
    gameScore.remove(MockObject.TEST_KEY);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScore.getUuid()),
        buildJsonHeader());
    gameScore.save();

    Assert.assertFalse(gameScore.has(MockObject.TEST_KEY));


    List historyList = new LinkedList();
    historyList.add(1200);
    historyList.add(1250);
    gameScore.addAll("history", historyList);
    gameScore.removeAll("history", historyList);
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, gameScore.getUuid()),
        buildJsonHeader());
    gameScore.save();
    Assert.assertEquals(0, gameScore.getList("history").size());

  }

  @Test
  public void testSaveAll() throws AVException {
    mockSaveAll();
    mockSaveAll();
    mockSaveAll();

    Game coc = new Game("COC");
    Game candy = new Game("candy crush");
    Game sims = new Game("sims");

    List<Game> games = new LinkedList<Game>();
    games.add(coc);
    games.add(candy);
    games.add(sims);
    AVObject.saveAll(games);
    AVObject.saveAllInBackground(games);
    AVObject.saveAllInBackground(games, new SaveCallback() {
      @Override
      public void done(AVException exception) {
        Assert.assertNull(exception);
      }
    });
  }

  @Test
  public void testSave() throws AVException, JSONException {
    final Game coc = new Game("COC");
    mockLogin();
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        try {
          String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
          Assert.assertTrue(data.contains("{\"read\":true,\"write\":true}"));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new TestHttpResponse(HttpStatus.SC_OK,
            String.format(MockObject.Result.BATCH_SAVE, coc.getUuid()),
            buildJsonHeader());
      }
    });

    AVUser user = AVUser.logIn(MockUser.NAME, MockUser.PASSWORD);
    AVACL acl = new AVACL();
    acl.setReadAccess(user, true);
    acl.setWriteAccess(user, true);

    coc.setACL(acl);
    coc.saveEventually();

    Log.d("jacob", coc.toJSONObject().toString());
    Assert.assertNotNull(coc.toJSONObject().get("name"));
  }

  private void mockSaveAll() {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        try {
          String data = EntityUtils.toString(((HttpPost) httpRequest).getEntity());
          Assert.assertTrue(data.contains("\"name\":\"COC\""));
          Assert.assertTrue(data.contains("\"name\":\"candy crush\""));
          Assert.assertTrue(data.contains("\"name\":\"sims\""));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.BATCH_SAVE,
            buildJsonHeader());
      }
    });
  }

  private void mockFetchResp() {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        return new TestHttpResponse(HttpStatus.SC_OK, MockObject.Result.FETCH, buildJsonHeader());
      }
    });
  }

  private void mockDeleteResp() {
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals("DELETE", httpRequest.getRequestLine().getMethod());
        Assert.assertEquals(MockObject.Url.REST, httpRequest.getRequestLine().getUri());
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
  }

  private List<AVObject> saveObjectList() throws AVException {
    AVObject avObject1 = new AVObject(MockObject.CLASS_NAME);
    AVObject avObject2 = new AVObject(MockObject.CLASS_NAME);
    AVObject avObject3 = new AVObject(MockObject.CLASS_NAME);
    List<AVObject> list = new LinkedList<AVObject>();
    list.add(avObject1);
    list.add(avObject2);
    list.add(avObject3);

    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject1.getUuid()),
        buildJsonHeader());
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject2.getUuid()),
        buildJsonHeader());
    Robolectric.addPendingHttpResponse(HttpStatus.SC_OK,
        String.format(MockObject.Result.BATCH_SAVE, avObject3.getUuid()),
        buildJsonHeader());
    Robolectric.addPendingHttpResponse(new HttpResponseGenerator() {
      @Override
      public HttpResponse getResponse(HttpRequest httpRequest) {
        Assert.assertEquals("DELETE", httpRequest.getRequestLine().getMethod());
        Assert.assertTrue(httpRequest.getRequestLine().getUri().contains(MockObject.Url.BASE));
        return new TestHttpResponse(HttpStatus.SC_OK, "{}", buildJsonHeader());
      }
    });
    avObject1.save();
    avObject2.save();
    avObject3.save();
    return list;
  }

}
