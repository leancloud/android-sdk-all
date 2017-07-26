package com.avos.avoscloud;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

/**
 * Created by wli on 2016/12/27.
 */

public class AVDeviceUtils {
  /**
   * 需要权限 android.permission.READ_PHONE_STATE
   * 并且如果非手机的话有可能取不到值
   *
   * @return
   */
  public static String getIMEI() {
    String imeistring = null;
    try {
      TelephonyManager telephonyManager =
        (TelephonyManager) AVOSCloud.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
      imeistring = telephonyManager.getDeviceId();
    } catch (Exception e) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("failed to get imei " + e);
      }
    }
    return imeistring;
  }

  /**
   * 当前手机的唯一AndroidId，此值是根据 google account 来的
   * 所以不同的用户会有不同的值，但是国内部分厂商的手机的部分批次 rom 有可能都是一个值
   */
  public static String getAndroidId() {
    return Settings.Secure.getString(AVOSCloud.applicationContext.getContentResolver(),
      Settings.Secure.ANDROID_ID);
  }

  public static String getMacAddress() {
    String macAddress = getMacAddressWithWifiManager();
    if (AVUtils.isBlankString(macAddress)) {
      return getMacAddressWithNetworkInterface();
    } else {
      return macAddress;
    }
  }

  /**
   * Mac 地址，但是山寨机有可能会导致不同手机同一个 mac 地址
   * 部分手机有可能因为 wifi 处于被关闭状态而导致返回值为 null
   * 还有部分情况 mac地址 与 蓝牙地址 返回的都是一个没有意义的固定值而造成重复
   *
   */
  private static String getMacAddressWithWifiManager() {
    if (Build.VERSION.SDK_INT >= 23) {
      // 6.0 以上的机器因为不会返回正确 mac 地址,所以这里过滤掉
      return null;
    }

    String macAddress = null;
    try {
      WifiManager wifiManager =
        (WifiManager) AVOSCloud.applicationContext.getSystemService(Context.WIFI_SERVICE);
      WifiInfo wInfo = wifiManager.getConnectionInfo();
      macAddress = wInfo.getMacAddress();

      // 6.0 以上的机器 WifiInfo.getMacAddress 统一都返回 02:00:00:00:00:00, 这里要做判断
      if ("02:00:00:00:00:00".equals(macAddress)) {
        return null;
      }
    } catch (Exception e) {
      if (AVOSCloud.showInternalDebugLog()) {
        LogUtil.avlog.d("failed to get wifi mac address" + e);
      }
    }
    return macAddress;
  }

  /**
   * 被逼无奈下的获取 mac 地址的方法
   * 对于 6.0 以上的机器,如果通过 getMacAddressWithWifiManager() 不能获取都 mac 地址的话,用此备选方案
   * 如果获取不到 wlan0,则以 eth1 代替,如果再获取不到,那就没办法了
   *
   * @return
   */
  private static String getMacAddressWithNetworkInterface() {
    List<NetworkInterface> interfaceList = null;
    try {
      interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
      String wlan = getHardwareAddress(interfaceList, "wlan0");
      if (AVUtils.isBlankString(wlan)) {
        return getHardwareAddress(interfaceList, "eth1");
      } else {
        return wlan;
      }
    } catch (SocketException e) {
    }
    return null;
  }

  private static String getHardwareAddress(List<NetworkInterface> interfaceList, String name) {
    if (null != interfaceList) {
      try {
        for (NetworkInterface networkInterface : interfaceList) {
          String interfaceName = networkInterface.getName();
          if (!AVUtils.isBlankString(interfaceName) && interfaceName.equalsIgnoreCase(name)) {
            byte[] macBytes = networkInterface.getHardwareAddress();
            if (null != macBytes) {
              StringBuilder mac = new StringBuilder();
              for (byte b : macBytes) {
                mac.append(String.format("%02X:", b));
              }
              if (mac.length() > 0) {
                mac.deleteCharAt(mac.length() - 1);
              }
              return mac.toString();
            }
          }
        }
      } catch (Exception e) {
      }
    }
    return null;
  }
}
