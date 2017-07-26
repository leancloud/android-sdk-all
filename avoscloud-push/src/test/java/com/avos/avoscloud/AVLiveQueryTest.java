package com.avos.avoscloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wli on 2017/6/7.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVLiveQueryTest {

  private AVLiveQuery liveQuery;
  private AVLiveQuery liveQueryForAVUser;

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_LIVEQUERY_APP_ID, TestConfig.TEST_LIVEQUERY_APP_KEY);
  }

  @Ignore
  public void testSubscribeInBackground() throws InterruptedException {
    AVQuery<AVObject> avQuery = new AVQuery<>("test_livequery");
    avQuery.whereEqualTo("name", "livequery");
    AVLiveQuery liveQuery = AVLiveQuery.initWithQuery(avQuery);
    final CountDownLatch latch = new CountDownLatch(1);
    liveQuery.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
        Assert.assertNull(e);
      }
    });
    latch.await();
  }

  @Ignore
  public void testUnsubscribeInBackground() throws InterruptedException {
    AVQuery<AVObject> avQuery = new AVQuery<>("test_livequery");
    avQuery.whereEqualTo("name", "livequery");
    AVLiveQuery liveQuery = AVLiveQuery.initWithQuery(avQuery);
    final CountDownLatch latch = new CountDownLatch(1);
    liveQuery.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
        Assert.assertNull(e);
      }
    });
    latch.await();

    final CountDownLatch latch1 = new CountDownLatch(1);
    liveQuery.unsubscribeInBackground(new AVLiveQuerySubscribeCallback() {
      @Override
      public void done(AVException e) {
        latch.countDown();
        Assert.assertNull(e);
      }
    });
    latch1.await();
  }

  @Ignore
  public void initLiveQuery() {
    if (null == liveQuery) {
      AVQuery<AVObject> avQuery = new AVQuery<>("test_livequery");
      avQuery.whereEqualTo("name", "livequery");
      liveQuery = AVLiveQuery.initWithQuery(avQuery);

      liveQuery.setEventHandler(new AVLiveQueryEventHandler() {
        @Override
        public void onObjectCreated(AVObject avObject) {
        }

        @Override
        public void onObjectUpdated(AVObject avObject, List<String> updateKeyList) {
        }

        @Override
        public void onObjectEnter(AVObject avObject, List<String> updateKeyList) {
        }

        @Override
        public void onObjectLeave(AVObject avObject, List<String> updateKeyList) {
        }

        @Override
        public void onObjectDeleted(String objectId) {
        }

        @Override
        public void onUserLogin(AVUser user) {
        }
      });
      liveQuery.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
        @Override
        public void done(AVException e) {
        }
      });
    }
  }

  @Ignore
  public void initAVUserQuery() {
    AVQuery<AVUser> avQuery = AVUser.getQuery();
    avQuery.whereEqualTo("username", "livequery_2");
    liveQueryForAVUser = AVLiveQuery.initWithQuery(avQuery);
    liveQueryForAVUser.setEventHandler(new AVLiveQueryEventHandler() {
      @Override
      public void onUserLogin(AVUser user) {
      }
    });
    liveQueryForAVUser.subscribeInBackground(new AVLiveQuerySubscribeCallback() {
      @Override
      public void done(AVException e) {
      }
    });
  }

  @Ignore
  public void testOnObjectCreated() {
    AVObject object = new AVObject("test_livequery");
    object.put("name", "livequery");
    object.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
      }
    });
  }

  @Ignore
  public void testOnObjectUpdated() {
    final AVObject object = new AVObject("test_livequery");
    object.put("name", "livequery");
    object.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        object.put("test", "111");
        object.saveInBackground(new SaveCallback() {
          @Override
          public void done(AVException e) {
          }
        });
      }
    });
  }

  @Ignore
  public void testOnObjectEnter() {
    final AVObject object = new AVObject("test_livequery");
    object.put("name", "livequery1");
    object.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        object.put("name", "livequery");
        object.saveInBackground(new SaveCallback() {
          @Override
          public void done(AVException e) {
          }
        });
      }
    });
  }

  @Ignore
  public void testOnObjectLeave() {
    final AVObject object = new AVObject("test_livequery");
    object.put("name", "livequery");
    object.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        object.put("name", "livequery1");
        object.saveInBackground(new SaveCallback() {
          @Override
          public void done(AVException e) {
          }
        });
      }
    });
  }

  @Ignore
  public void testOnObjectDeleted() {
    final AVObject object = new AVObject("test_livequery");
    object.put("name", "livequery");
    object.saveInBackground(new SaveCallback() {
      @Override
      public void done(AVException e) {
        object.deleteInBackground(new DeleteCallback() {
          @Override
          public void done(AVException e) {
          }
        });
      }
    });
  }

  @Ignore
  public void testOnUserLogin() {
    AVUser.logInInBackground("livequery_1", "livequery_1", new LogInCallback<AVUser>() {
      @Override
      public void done(AVUser user, AVException e) {
      }
    });

    AVUser.logInInBackground("livequery_2", "livequery_2", new LogInCallback<AVUser>() {
      @Override
      public void done(AVUser user, AVException e) {
      }
    });
  }

}
