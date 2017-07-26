package com.avos.avoscloud;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lbt05 on 9/14/15.
 */
public class DNSAmendNetwork implements Dns {

  static final long TWENTY_MIN_IN_MILLS = 20 * 60 * 1000L;
  static final String AVOS_SERVER_HOST_ZONE = "avoscloud_server_host_zone";
  public static final String EXPIRE_TIME = ".expireTime";

  // DNS 请求的超时时间设置为两秒
  private static final int DNS_REQUEST_TIME_OUT = 2 * 1000;

  private static DNSAmendNetwork instance = new DNSAmendNetwork();

  private DNSAmendNetwork() {

  }

  public static DNSAmendNetwork getInstance() {
    return instance;
  }


  @Override
  public List<InetAddress> lookup(String host) throws UnknownHostException {
    if (!AVUtils.checkPermission(AVOSCloud.applicationContext, "android.permission.INTERNET")) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.e("Please add <uses-permission android:name=\"android.permission.INTERNET\"/> in your AndroidManifest file");
      }
      throw new UnknownHostException();
    }

    try {
      InetAddress[] addresses = InetAddress.getAllByName(host);
      return Arrays.asList(addresses);
    } catch (UnknownHostException e) {
      try {
        String response = getCacheDNSResult(host);
        boolean isCacheValid = !AVUtils.isBlankString(response);
        if (!isCacheValid) {
          response = getIPByHostSync(host);
        }
        InetAddress[] addresses = getIPAddress(host, response);
        if (!isCacheValid) {
          cacheDNS(host, response);
        }
        return Arrays.asList(addresses);
      } catch (Exception e1) {
        throw new UnknownHostException();
      }
    }
  }

  public static String getIPByHostSync(String host) throws Exception {
    HttpUrl httpUrl = new HttpUrl.Builder().scheme("http").host("119.29.29.29")
      .addPathSegment("d").addQueryParameter("dn", host).build();

    OkHttpClient.Builder builder = AVHttpClient.clientInstance().getOkHttpClientBuilder();
    builder.connectTimeout(DNS_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
    builder.dns(Dns.SYSTEM);
    OkHttpClient okHttpClient = builder.build();
    Request request = new Request.Builder().url(httpUrl).get().build();

    try {
      Response response = okHttpClient.newCall(request).execute();
      if (null != response && response.isSuccessful()) {
        return response.body().string();
      } else {
        return "";
      }
    } catch (IOException e) {
      if (AVOSCloud.isDebugLogEnabled()) {
        LogUtil.avlog.e("getIPByHostSync error", e);
      }
      return "";
    }
  }

  private void cacheDNS(String host, String response) {
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(AVOS_SERVER_HOST_ZONE,
        host, response);
    AVPersistenceUtils.sharedInstance().savePersistentSettingString(AVOS_SERVER_HOST_ZONE,
        host + EXPIRE_TIME, String.valueOf(System.currentTimeMillis() + TWENTY_MIN_IN_MILLS));
  }

  private String getCacheDNSResult(String url) {
    String response =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(AVOS_SERVER_HOST_ZONE, url,
            null);
    String expiredAt =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(AVOS_SERVER_HOST_ZONE,
            url + EXPIRE_TIME, "0");

    if (!AVUtils.isBlankString(response) && System.currentTimeMillis() < Long.parseLong(expiredAt)) {
      return response;
    } else {
      return null;
    }
  }

  private static InetAddress[] getIPAddress(String url, String response) throws Exception {
    String[] ips = response.split(";");
    InetAddress[] addresses = new InetAddress[ips.length];
    Constructor constructor =
        InetAddress.class.getDeclaredConstructor(int.class, byte[].class, String.class);
    constructor.setAccessible(true);
    for (int i = 0; i < ips.length; i++) {
      String ip = ips[i];
      String[] ipSegment = ip.split("\\.");
      if (ipSegment.length == 4) {
        byte[] ipInBytes =
        {(byte) Integer.parseInt(ipSegment[0]), (byte) Integer.parseInt(ipSegment[1]),
            (byte) Integer.parseInt(ipSegment[2]), (byte) Integer.parseInt(ipSegment[3])};
        addresses[i] = (InetAddress) constructor.newInstance(2, ipInBytes, url);
      }
    }
    return addresses;
  }
}
