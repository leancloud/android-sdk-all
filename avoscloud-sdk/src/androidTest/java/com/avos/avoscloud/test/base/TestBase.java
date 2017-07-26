package com.avos.avoscloud.test.base;

import com.alibaba.fastjson.parser.ParserConfig;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.test.mock.Game;
import com.avos.avoscloud.test.mock.GameScore;
import com.avos.avoscloud.test.utils.TestConstants;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;

/**
 * Created by zhangxiaobo on 15/2/6.
 */
public class TestBase {
  protected Certificate trusted;

  @Before
  public void setup() {
    // Add BKS to KeyStore, for SSL request
    Security.addProvider(new BouncyCastleProvider());
    trusted = getCertificate("serverCA.bks", "serverCA");

    AVOSCloud.setDebugLogEnabled(true);
    AVObject.registerSubclass(GameScore.class);
    AVObject.registerSubclass(Game.class);
    AVOSCloud.initialize(Robolectric.application, TestConstants.TEST_APP_ID,
        TestConstants.TEST_APP_KEY);


    ParserConfig.getGlobalInstance().setAsmEnable(false);
    ShadowLog.stream = System.out;
  }

  private Certificate getCertificate(String resourceName, String certificateAlias) {
    InputStream certAsStream = getClass().getClassLoader().getResourceAsStream(resourceName);
    try {
      KeyStore localTrustStore = KeyStore.getInstance("BKS");
      localTrustStore.load(certAsStream, null);
      return localTrustStore.getCertificate(certificateAlias);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
