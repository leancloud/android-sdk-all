package com.avos.avoscloud.im.v2;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.BuildConfig;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by fengjunwen on 2017/8/18.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "AndroidManifest.xml", sdk = 23)
public class AVIMMessageStorageTests {
  private String defaultClient = "ThisIsATestUserA";
  private String defaultConversation = "ThisIsAConversation";
  @Before
  public void setup() {
    AVOSCloud.setDebugLogEnabled(true);
    AVOSCloud.applicationContext = RuntimeEnvironment.application;
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testDummy() {
    long currentThreadId = Thread.currentThread().getId();
    long mainThreadId = Looper.getMainLooper().getThread().getId();
    System.out.println("mainThreadId=" + mainThreadId + ", currentThreadId=" + currentThreadId);
    AVIMMessageStorage storage = AVIMMessageStorage.getInstance(defaultClient);
    Assert.assertNotNull(storage);
  }

  private String generateRandomString(int length) {
    return AVUtils.getRandomString(length);
  }

  private List<AVIMMessage> generateMessages(int count) {
    ArrayList<AVIMMessage> results = new ArrayList<>(count);
    String msgIdPrefix = generateRandomString(5);
    for (int i = 0; i < count; i++) {
      AVIMTextMessage msg = new AVIMTextMessage();
      msg.setText(String.valueOf(i));
      msg.setMessageId(msgIdPrefix + i);
      msg.setConversationId(defaultConversation);
      msg.setFrom("Bob");
      msg.setMessageIOType(AVIMMessage.AVIMMessageIOType.AVIMMessageIOTypeIn);
      msg.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
      msg.setUpdateAt(System.currentTimeMillis());
      msg.setTimestamp(System.currentTimeMillis());
      results.add(msg);
    }
    return results;
  }

  @Test
  public void testInsertMessage() {
    AVIMMessageStorage storage = AVIMMessageStorage.getInstance(defaultClient);
//    storage.dumpMessages(defaultConversation);
    System.out.println("^^^^ should be empty.");

    List<AVIMMessage> msgs = generateMessages(1);
    storage.insertMessage(msgs.get(0), true);
//    storage.dumpMessages(defaultConversation);
    System.out.println("^^^^ should be one msg.");

    long msgCount = storage.getMessageCount(defaultConversation);
    Assert.assertTrue(msgCount == 1);

    msgs = generateMessages(10);
    storage.insertContinuousMessages(msgs, defaultConversation);
//    storage.dumpMessages(defaultConversation);
    System.out.println("^^^^ should be 11 msgs.");

    msgCount = storage.getMessageCount(defaultConversation);
    Assert.assertTrue(msgCount == 10);

    storage.deleteConversationData(defaultConversation);
  }

  @Test
  public void testInsertDuplicatedMessages() {
    AVIMMessageStorage storage = AVIMMessageStorage.getInstance(defaultClient);
    List<AVIMMessage> msgs = generateMessages(10);
    storage.insertContinuousMessages(msgs, defaultConversation);

    List<AVIMMessage> msg2 = generateMessages(5);
    List<AVIMMessage> msg3 = new ArrayList<>(6);
    msg3.add(msgs.get(9));
    msg3.addAll(msg2);
    storage.insertContinuousMessages(msg3, defaultConversation);
//    storage.dumpMessages(defaultConversation);
    System.out.println("^^^^ should be 15 msgs.");
    long msgCount = storage.getMessageCount(defaultConversation);
    Assert.assertTrue(msgCount == 15);

    storage.deleteConversationData(defaultConversation);
  }
}
