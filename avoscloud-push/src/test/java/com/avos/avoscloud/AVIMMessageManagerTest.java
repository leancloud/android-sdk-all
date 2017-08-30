package com.avos.avoscloud;

import android.util.SparseArray;

import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.MessageHandler;
import com.avos.avoscloud.model.AVIMCustomMessage;
import com.avos.avoscloud.model.AVIMCustomMessageHandler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wli on 2017/8/29.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 21)
public class AVIMMessageManagerTest {

  @Test
  public void testRegisterAVIMMessageType() {
    AVIMMessageManager.registerAVIMMessageType(AVIMCustomMessage.class);
    SparseArray<AVIMTypedMessage> messageTypesRepository =
      Whitebox.getInternalState(AVIMMessageManager.class, "messageTypesRepository");
    Assert.assertEquals(messageTypesRepository.get(new AVIMCustomMessage().getMessageType()), AVIMCustomMessage.class);
  }

  @Test
  public void testRegisterMessageHandler() {
    ConcurrentHashMap<Class<AVIMMessage>, Set<MessageHandler>> handlerMap =
      Whitebox.getInternalState(AVIMMessageManager.class, "messageHandlerRepository");
    handlerMap.clear();

    AVIMCustomMessageHandler handler = new AVIMCustomMessageHandler();
    AVIMMessageManager.registerAVIMMessageType(AVIMCustomMessage.class);

    AVIMMessageManager.registerMessageHandler(AVIMCustomMessage.class, handler);
    Assert.assertEquals(handlerMap.get(AVIMCustomMessage.class).iterator().next(), handler);
  }

  @Test
  public void testUnregisterMessageHandler() {
    ConcurrentHashMap<Class<AVIMMessage>, Set<MessageHandler>> handlerMap =
      Whitebox.getInternalState(AVIMMessageManager.class, "messageHandlerRepository");
    handlerMap.clear();

    AVIMCustomMessageHandler handler = new AVIMCustomMessageHandler();
    AVIMMessageManager.registerAVIMMessageType(AVIMCustomMessage.class);

    AVIMMessageManager.registerMessageHandler(AVIMCustomMessage.class, handler);
    AVIMMessageManager.unregisterMessageHandler(AVIMCustomMessage.class, handler);
    Assert.assertTrue(handlerMap.get(AVIMCustomMessage.class).isEmpty());
  }
}
