package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


/**
 * Created by wli on 16/10/13.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVUserTest {

  private static String DEFAULT_TEST_USER_NAME = "default_test_user_name";
  private static String DEFAULT_TEST_USER_PWD = "default_test_user_pwd";
  private static String DEFAULT_TEST_USER_EMAIL = "xxxxxx@xxx.com";
  private static String DEFAULT_TEST_USER_CUSTOM_KEY = "default_test_user_key";
  private static String DEFAULT_TEST_USER_CUSTOM_VALUE = "default_test_user_value";

  @Before
  public void initAvos() {
    AVOSCloud.initialize(RuntimeEnvironment.application, TestConfig.TEST_APP_ID, TestConfig.TEST_APP_KEY);
  }

  /**
   * 注册初始用户，修改 app 后手动执行一边就行了
   */
  @Ignore
  public void initDefaultTestUser() {
    AVUser user = new AVUser();
    user.setUsername(DEFAULT_TEST_USER_NAME);
    user.setPassword(DEFAULT_TEST_USER_PWD);
    user.setEmail(DEFAULT_TEST_USER_EMAIL);
    user.put(DEFAULT_TEST_USER_CUSTOM_KEY, DEFAULT_TEST_USER_CUSTOM_VALUE);
    try {
      user.signUp();
    } catch (AVException e) {
      Assert.assertEquals(e.getCode(), 202);
    }
  }

  /**
   * 必须要先执行 initDefaultTestUser 后才能执行此初始化操作。
   * 与 initDefaultTestUser 相同，只有在切换 app 后才需要执行此操作，而且只用执行一次
   * @throws AVException
   */
  @Ignore
  public void initDefaultAVRole() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    AVACL roleACL = new AVACL();
    roleACL.setPublicReadAccess(true);
    roleACL.setWriteAccess(user, true);
    AVRole administrator = new AVRole("Administrator", roleACL);
    administrator.getUsers().add(user);
    administrator.save();
  }

  @Test
  public void testGetFacebookToken() {
    String faceBookToken = "facebooktoken";
    AVUser user = new AVUser();
    user.setFacebookToken(faceBookToken);
    Assert.assertEquals(user.getFacebookToken(), faceBookToken);
  }

  @Test
  public void testGetTwitterToken() {
    String twitterToken = "twitterToken";
    AVUser user = new AVUser();
    user.setTwitterToken(twitterToken);
    Assert.assertEquals(user.getTwitterToken(), twitterToken);
  }

  @Test
  public void testGetQqWeiboToken() {
    String qqWeiboToken = "qqWeiboToken";
    AVUser user = new AVUser();
    user.setQqWeiboToken(qqWeiboToken);
    Assert.assertEquals(user.getQqWeiboToken(), qqWeiboToken);
  }

  @Test
  @Ignore
  public void testAnonymousLogIn() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVAnonymousUtils.logIn(new LogInCallback<AVUser>() {
      @Override
      public void done(AVUser user, AVException e) {
        Assert.assertTrue(!user.getObjectId().isEmpty());
        AVUser user1 = AVUser.getCurrentUser();
        Assert.assertNotNull(user1);
        latch.countDown();
      }
    });

    AVUser user = AVUser.getCurrentUser();
    Assert.assertNotNull(user);

    user.fetch();
    Assert.assertTrue(user.isAnonymous());
    AVUser.changeCurrentUser(null, false);
    Assert.assertTrue(AVUser.getCurrentUser().isAnonymous());
  }

  @Test
  public void testGetUserQuery() {
    Assert.assertNotNull(AVUser.getQuery());
  }

  @Test
  public void testFriendshipQuery() {
    AVUser user = new AVUser();
    Assert.assertNotNull(user.friendshipQuery());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLogIn_nullName() throws Exception {
    AVUser.logIn("", "");
  }

  @Test
  public void testLogin_wrongPwd() {
    AVException exception = null;
    try {
      AVUser.logIn("fsdfsdffsdffsdfsdf", "");
    } catch (AVException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getCode(), 211);
  }

  @Test
  public void testSignUp_withoutPwd() {
    AVException exception = null;
    AVUser user = new AVUser();
    user.setUsername("just_test_signup");
    try {
      user.signUp();
    } catch (AVException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getCode(), 201);
  }

  @Test
  public void testSignUp() throws AVException {
    AVUser user = new AVUser();
    user.setUsername(UUID.randomUUID().toString());
    user.setPassword("test");
    user.signUp();
  }

  @Test
  public void testSignUp_existAccount() {
    AVUser user = new AVUser();
    user.setUsername(DEFAULT_TEST_USER_NAME);
    user.setPassword(DEFAULT_TEST_USER_PWD);
    try {
      user.signUp();
    } catch (AVException e) {
      Assert.assertEquals(e.getCode(), 202);
    }
  }

  @Test
  public void testLogin() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    Assert.assertEquals(user.getUsername(), DEFAULT_TEST_USER_NAME);
    Assert.assertNotNull(user.getSessionToken());
    Assert.assertEquals(user.get(DEFAULT_TEST_USER_CUSTOM_KEY), DEFAULT_TEST_USER_CUSTOM_VALUE);
  }

  @Test
  public void testLogInInBackground() {
    final CountDownLatch latch = new CountDownLatch(1);
    AVUser.logInInBackground(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD, new LogInCallback<AVUser>() {
      @Override
      public void done(AVUser user, AVException e) {
        Assert.assertNull(e);
        Assert.assertEquals(user.getUsername(), DEFAULT_TEST_USER_NAME);
        Assert.assertNotNull(user.getSessionToken());
        Assert.assertEquals(user.get(DEFAULT_TEST_USER_CUSTOM_KEY), DEFAULT_TEST_USER_CUSTOM_VALUE);
        latch.countDown();
      }
    });
  }

  @Test
  public void testBecomeWithSessionToken() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    AVUser newUser = AVUser.becomeWithSessionToken(user.getSessionToken());
    Assert.assertEquals(user, newUser);
  }

  @Test
  public void testGetRoles() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    AVQuery<AVRole> roleQuery = new AVQuery<AVRole>(AVRole.className);
    roleQuery.whereEqualTo(AVUser.AVUSER_ENDPOINT, user);
    List<AVRole> roleList = roleQuery.find();
    Assert.assertNotNull(roleList);
    Assert.assertTrue(roleList.size() > 0);
  }

  @Test
  public void getRolesInBackground() throws Exception {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    AVQuery<AVRole> roleQuery = new AVQuery<AVRole>(AVRole.className);
    roleQuery.whereEqualTo(AVUser.AVUSER_ENDPOINT, user);

    final CountDownLatch latch = new CountDownLatch(1);
    roleQuery.findInBackground(new FindCallback<AVRole>() {
      @Override
      public void done(List<AVRole> avObjects, AVException avException) {
        Assert.assertNotNull(avObjects);
        Assert.assertTrue(avObjects.size() > 0);
        latch.countDown();
      }
    });
  }

  /**
   * 测试本地文件缓存中的 AVUser 是否可以正常读取
   * @throws AVException
   */
  @Test
  public void testLocalUser() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    PaasClient.storageInstance().setCurrentUser(null);
    AVUser newUser = AVUser.getCurrentUser();
    Assert.assertEquals(user, newUser);
  }

  /**
   * 测试本地文件缓存中的 AVUser 子类是否可以正常读取
   * @throws AVException
   */
  @Test
  public void testLocalChildUser() throws AVException {
    AVUser.registerSubclass(ChildAVUser.class);
    ChildAVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD, ChildAVUser.class);
    PaasClient.storageInstance().setCurrentUser(null);
    ChildAVUser newUser = AVUser.getCurrentUser(ChildAVUser.class);
    Assert.assertEquals(user, newUser);
  }

  @Test
  @Ignore
  public void testAVUserFastJson() {
    AVUser avUser = new AVUser();
    String content = JSON.toJSONString(avUser);
    AVUser newUser = JSON.parseObject(content, AVUser.class);
    Assert.assertEquals(avUser, newUser);
  }

  @Test
  public void testRefreshSessionToken() throws AVException {
    AVUser.alwaysUseSubUserClass(ChildAVUser.class);
    AVUser.registerSubclass(ChildAVUser.class);
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD, ChildAVUser.class);
    AVUser newUser = user.refreshSessionToken();
    Assert.assertNotNull(newUser.getSessionToken());
    Assert.assertTrue(!newUser.getSessionToken().equals(user.getSessionToken()));
  }

  @Test
  public void testRefreshSessionToken_childAVUser() throws AVException {
    AVUser.alwaysUseSubUserClass(ChildAVUser.class);
    AVUser.registerSubclass(ChildAVUser.class);
    String testNickName = UUID.randomUUID().toString();

    ChildAVUser user = ChildAVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD, ChildAVUser.class);
    user.setNickName(testNickName);
    user.save();

    ChildAVUser newUser = user.refreshSessionToken();
    Assert.assertNotNull(newUser.getSessionToken());
    Assert.assertTrue(!newUser.getSessionToken().equals(user.getSessionToken()));
    Assert.assertTrue(testNickName.equals(user.getNickName()));
  }

  @Test
  public void testAVUserUpdate() throws AVException {
    String testKey = "testKey";
    String testValue = "testValue";
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);
    user.put(testKey, "1");
    user.save();
    user.put(testKey, testValue);
    user.save();
    Assert.assertEquals(testValue, user.get(testKey));

    user.getFollowersAndFolloweesInBackground(new FollowersAndFolloweesCallback() {
      @Override
      public void done(Map avObjects, AVException avException) {
        if (null != avException || null == avObjects) {
          ;
        } else {
          List<AVUser> followers = (List<AVUser>) avObjects.get("followers");
          List<AVUser> followees = (List<AVUser>) avObjects.get("followees");
        }
      }

      @Override
      protected void internalDone0(Object o, AVException avException) {

      }
    });
  }

  @Test
  public void testAVUserSerialize() throws AVException {
    AVUser user = AVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD);

    String jsonString = JSON.toJSONString(user, ObjectValueFilter.instance,
        SerializerFeature.WriteClassName,
        SerializerFeature.DisableCircularReferenceDetect);

    AVUser newUser = (AVUser) JSON.parse(jsonString);
    Assert.assertEquals(user.getSessionToken(), newUser.getSessionToken());
    Assert.assertEquals(user.getObjectId(), newUser.getObjectId());
    Assert.assertEquals(user.getUsername(), newUser.getUsername());
    Assert.assertEquals(user.getEmail(), newUser.getEmail());
  }

  @Test
  public void testChildAVUserSerialize() throws AVException {
    AVUser.registerSubclass(ChildAVUser.class);
    ChildAVUser user = ChildAVUser.logIn(DEFAULT_TEST_USER_NAME, DEFAULT_TEST_USER_PWD, ChildAVUser.class);

    String jsonString = JSON.toJSONString(user, ObjectValueFilter.instance,
      SerializerFeature.WriteClassName,
      SerializerFeature.DisableCircularReferenceDetect);

    ChildAVUser newUser = (ChildAVUser) JSON.parse(jsonString);
    Assert.assertEquals(user.getSessionToken(), newUser.getSessionToken());
    Assert.assertEquals(user.getObjectId(), newUser.getObjectId());
    Assert.assertEquals(user.getUsername(), newUser.getUsername());
    Assert.assertEquals(user.getEmail(), newUser.getEmail());
  }
}
