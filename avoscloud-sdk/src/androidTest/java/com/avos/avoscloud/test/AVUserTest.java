package com.avos.avoscloud.test;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFriendship;
import com.avos.avoscloud.AVFriendshipQuery;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.FollowCallback;
import com.avos.avoscloud.test.base.NetworkTestBase;
import com.avos.avoscloud.test.mock.MyUser;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = "build/intermediates/manifests/debug/AndroidManifest.xml",
    resourceDir = "../../../../build/intermediates/res/debug",
    emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class AVUserTest extends NetworkTestBase {

  @Test
  public void testAVFriendship() throws AVException {
    // setup user friendship A->B->C
    AVUser userC = new AVUser();
    userC.setUsername("FriendshipTest" + AVUtils.getRandomString(3) + System.currentTimeMillis());
    userC.setPassword(AVUtils.getRandomString(10));
    userC.put("v", "C");
    userC.save();
    String userCId = userC.getObjectId();

    AVUser userB = new AVUser();
    userB.setUsername("FriendshipTest" + AVUtils.getRandomString(3) + System.currentTimeMillis());
    userB.setPassword(AVUtils.getRandomString(10));
    userB.put("v", "B");
    userB.save();

    String userBId = userB.getObjectId();
    userB.followInBackground(userCId, new FollowCallback() {
      @Override
      public void done(AVObject object, AVException e) {

      }
    });

    AVUser userA = new AVUser();
    userA.setUsername("FriendshipTest" + AVUtils.getRandomString(3) + System.currentTimeMillis());
    userA.setPassword(AVUtils.getRandomString(10));
    userA.put("v", "A");
    userA.save();

    userA.followInBackground(userBId, new FollowCallback() {
      @Override
      public void done(AVObject object, AVException e) {

      }
    });

    AVFriendshipQuery query = AVUser.friendshipQuery(userBId, MyUser.class);
    query.include("followee");
    query.include("follower");
    AVFriendship<MyUser> friendship = query.get();
    Assert.assertEquals(1, friendship.getFollowers().size());
    Assert.assertEquals(userA.getObjectId(), friendship.getFollowers().get(0).getObjectId());
    Assert.assertEquals("A", friendship.getFollowers().get(0).getString("v"));

    Assert.assertEquals(1, friendship.getFollowees().size());
    Assert.assertEquals(userC.getObjectId(), friendship.getFollowees().get(0).getObjectId());
    Assert.assertEquals("C", friendship.getFollowees().get(0).getString("v"));

  }
}
