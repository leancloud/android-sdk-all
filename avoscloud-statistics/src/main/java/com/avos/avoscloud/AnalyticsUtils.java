package com.avos.avoscloud;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/13/13 Time: 3:37 PM To change this template use
 * File | Settings | File Templates.
 */
public class AnalyticsUtils {

  private static final String TAG = AnalyticsUtils.class.getSimpleName();

  static public Map<String, String> getNetworkInfo(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    Map<String, String> map = new HashMap<String, String>();
    NetworkInfo info = cm.getActiveNetworkInfo();
    if (info == null || !info.isConnectedOrConnecting() || withinInBlackList()) {
      map.put("access_subtype", "offline");
      map.put("access", "offline");
    } else {
      map.put("access_subtype", info.getSubtypeName());
      map.put("access", cleanNetworkTypeName(info.getTypeName()));
      TelephonyManager manager =
          (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      String carrierName = manager.getNetworkOperatorName();
      if (AVUtils.isBlankString(carrierName)) {
        map.put("carrier", "unknown");
      } else {
        map.put("carrier", carrierName);
      }
    }
    return map;
  }

  static private String cleanNetworkTypeName(String type) {
    if (AVUtils.isBlankString(type)) {
      return "offline";
    }
    String t = type.toUpperCase();
    if (t.contains("WIFI")) {
      return "WiFi";
    }
    if (type.contains("MOBILE")) {
      return "Mobile";
    }
    return type;
  }

  static public Map<String, Object> deviceInfo(Context context) {
    Map<String, Object> map = new HashMap<String, Object>();
    Map<String, String> networkInfo = getNetworkInfo(context);
    if (networkInfo != null) {
      map.putAll(networkInfo);
    }
    Map<String, Object> deviceInfo = getDeviceInfo(context);
    if (deviceInfo != null) {
      map.putAll(deviceInfo);
    }
    return map;
  }

  static public long getAvailableInternalMemorySize() {
    final File path = Environment.getDataDirectory();
    final StatFs stat = new StatFs(path.getPath());
    final long blockSize = stat.getBlockSize();
    final long availableBlocks = stat.getAvailableBlocks();
    return availableBlocks * blockSize;
  }

  static public long getTotalInternalMemorySize() {
    final File path = Environment.getDataDirectory();
    final StatFs stat = new StatFs(path.getPath());
    final long blockSize = stat.getBlockSize();
    final long totalBlocks = stat.getBlockCount();
    return totalBlocks * blockSize;
  }

  public static Map<String, Object> getDeviceInfo(Context context) {
    Map<String, Object> map = new HashMap<String, Object>();

    String packageName = context.getApplicationContext().getPackageName();
    map.put("package_name", packageName);
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
      map.put("app_version", info.versionName);
      map.put("version_code", info.versionCode);
      map.put("sdk_version", "Android " + PaasClient.sdkVersion);
    } catch (Exception exception) {
      exception.printStackTrace();
    }


    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    map.put("resolution", "" + width + "*" + height);

    map.put("device_model", Build.MODEL);
    map.put("device_manufacturer", Build.MANUFACTURER);
    map.put("os_version", Build.VERSION.RELEASE);
    map.put("device_name", Build.DEVICE);
    map.put("device_brand", Build.BRAND);
    map.put("device_board", Build.BOARD);
    map.put("device_manuid", Build.FINGERPRINT);

    map.put("cpu", getCPUInfo()); // TODO, cpu info is incorrect.
    map.put("os", "Android");
    map.put("sdk_type", "Android");

    map.put("device_id", getDeviceId(context));
    // Add Installation Id && userId if logined
    try {
      Class<?> installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
      Method getMethod = installationClass.getMethod("getCurrentInstallation");
      Method getInstallationIdMethod = installationClass.getMethod("getObjectId");
      Object installation = getMethod.invoke(installationClass);
      String installationId = (String) getInstallationIdMethod.invoke(installation);
      map.put("iid", installationId);
    } catch (Exception e) {}

    long offset = TimeZone.getDefault().getRawOffset();
    // Add for API 8 don't have this API issue
    // 为了避免部分API 8用户出现异常导致崩溃
    AVUser loginedUser = AVUser.getCurrentUser();
    if (loginedUser != null && !AVUtils.isBlankString(loginedUser.getObjectId())) {
      map.put("uid", loginedUser.getObjectId());
    }
    try {
      offset = TimeUnit.HOURS.convert(offset, TimeUnit.MILLISECONDS);
    } catch (java.lang.NoSuchFieldError e) {
      offset = offset / 3600000;
    }
    map.put("time_zone", offset);
    map.put("channel", AVAnalytics.getAppChannel());

    // 2.6.10版本，增加IMEI数据
    if (!withinInBlackList()
        && AVOSCloud.applicationContext
            .checkCallingPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
      map.put("imei", AVDeviceUtils.getIMEI());
    }
    return map;
  }

  public static String collectMemInfo() {

    final StringBuilder meminfo = new StringBuilder();
    InputStream in = null;
    InputStreamReader reader = null;
    BufferedReader bufferedReader = null;
    try {
      final List<String> commandLine = new ArrayList<String>();
      commandLine.add("dumpsys");
      commandLine.add("meminfo");
      commandLine.add(Integer.toString(android.os.Process.myPid()));

      final Process process =
          Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
      in = process.getInputStream();
      reader = new InputStreamReader(in);
      bufferedReader = new BufferedReader(reader, 8192);

      while (true) {
        final String line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        meminfo.append(line);
        meminfo.append("\n");
      }
      AVPersistenceUtils.closeQuietly(bufferedReader);
      AVPersistenceUtils.closeQuietly(reader);
      AVPersistenceUtils.closeQuietly(in);


      in = process.getErrorStream();
      reader = new InputStreamReader(in);
      bufferedReader = new BufferedReader(reader, 8192);

      StringBuilder errorInfo = new StringBuilder();
      while (true) {
        final String line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        errorInfo.append(line);
        // ignore error stream.
      }
      if (process.waitFor() != 0) {
        Log.e(TAG, errorInfo.toString());
      }

    } catch (Exception e) {
      Log.e(TAG, "DumpSysCollector.meminfo could not retrieve data", e);
    } finally {
      AVPersistenceUtils.closeQuietly(bufferedReader);
      AVPersistenceUtils.closeQuietly(reader);
      AVPersistenceUtils.closeQuietly(in);
    }

    return meminfo.toString();
  }

  static public String getCPUInfo() {
    StringBuffer sb = new StringBuffer();
    BufferedReader br = null;
    if (new File("/proc/cpuinfo").exists()) {
      try {
        br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
        String line;
        while ((line = br.readLine()) != null) {
          if (line.contains("Processor")) {
            int position = line.indexOf(":");
            if (position >= 0 && position < line.length() - 1) {
              sb.append(line.substring(position + 1).trim());
            }
            break;
          }
        }
      } catch (IOException e) {
        Log.e(TAG, "getCPUInfo", e);
      } finally {
        AVPersistenceUtils.closeQuietly(br);
      }
    }
    return sb.toString();
  }

  public static String getLocalIpAddress() {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
          .hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
            .hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            if (!first) {
              result.append('\n');
            }
            result.append(inetAddress.getHostAddress().toString());
            first = false;
          }
        }
      }
    } catch (SocketException ex) {
      Log.i(TAG, ex.toString());
    }
    return result.toString();
  }

  public static String getApplicationFilePath(Context context) {
    final File filesDir = context.getFilesDir();
    if (filesDir != null) {
      return filesDir.getAbsolutePath();
    }
    return "Couldn't retrieve ApplicationFilePath";
  }

  public static long getCurrentTimestamp() {
    return System.currentTimeMillis();
  }

  public static String getRandomString(int length) {
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder randomString = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      randomString.append(letters.charAt(new Random().nextInt(letters.length())));
    }

    return randomString.toString();
  }

  public static String uniqueId() {
    return UUID.randomUUID().toString();
  }

  public static boolean isStringEqual(final String src, final String target) {
    if (src == null && target == null) {
      return true;
    }
    if (src != null) {
      return src.equals(target);
    } else {
      return false;
    }
  }

  static List<String> CELLPHONEBLACKLIST = Arrays.asList("d2spr");

  private static boolean withinInBlackList() {
    // 以后靠积累了
    if (CELLPHONEBLACKLIST.contains(Build.DEVICE)) {
      return true;
    }
    return false;
  }

  static boolean inDebug(final Context context) {
    if (context != null) {
      boolean debug = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
      if (debug) {
        Log.i(TAG, "in debug: " + debug);
      }
      return debug;
    } else {
      return false;
    }
  }

  private static final long sendIntervalInDebug = 15 * 1000;
  private static final long sendIntervalInProd = 120 * 1000;

  protected static long getRequestInterval() {
    return inDebug(AVOSCloud.applicationContext)
        ? sendIntervalInDebug
        : sendIntervalInProd;
  }

  private static String DEVICE_ID_KEY = "com.avos.avoscloud.deviceId";
  private static String ANALYSIS_KEY = "com.avos.avoscloud.analysis";

  private static String getDeviceId(Context context) {
    String cachedDeviceId =
        AVPersistenceUtils.sharedInstance().getPersistentSettingString(ANALYSIS_KEY, DEVICE_ID_KEY,
            null);
    if (AVUtils.isBlankString(cachedDeviceId)) {
      // app的包名
      String packageName = AVOSCloud.applicationContext.getPackageName();
      String androidId = AVDeviceUtils.getAndroidId();
      // 这是当前手机的内部批次ID，同型号的手机应该是一样的
      String buildId = Build.ID;
      String macAddress = AVDeviceUtils.getMacAddress();

      String imeiStr = AVDeviceUtils.getIMEI();
      String additionalStr = null;
      if (AVUtils.isBlankString(macAddress) && AVUtils.isBlankString(imeiStr)) {
        additionalStr = UUID.randomUUID().toString();
      }

      String deviceId = AVUtils.md5(packageName + androidId + buildId + macAddress + imeiStr + additionalStr);
      AVPersistenceUtils.sharedInstance().savePersistentSettingString(ANALYSIS_KEY, DEVICE_ID_KEY,
        deviceId);
      return deviceId;
    }
    return cachedDeviceId;
  }
}
