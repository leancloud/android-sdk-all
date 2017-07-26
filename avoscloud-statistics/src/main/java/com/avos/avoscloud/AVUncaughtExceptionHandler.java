package com.avos.avoscloud;

import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.AVUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Created with IntelliJ IDEA. User: zhuzeng Date: 8/12/13 Time: 2:35 PM To change this template use
 * File | Settings | File Templates.
 */
public class AVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private final Thread.UncaughtExceptionHandler defaultExceptionHandler;
  private boolean enabled = false;
  private final String LOG_TAG = AVUncaughtExceptionHandler.class.getSimpleName();
  private final Context context;
  private Thread brokenThread;
  private Throwable unhandledThrowable;

  public AVUncaughtExceptionHandler(Context c) {
    context = c;
    defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(this);
  }

  public void enableCrashHanlder(boolean e) {
    enabled = e;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    try {
      if (!enabled) {
        if (defaultExceptionHandler != null) {
          Log.w(LOG_TAG, "AVUncaughtExceptionHandler is disabled and fallback to default handler.");
          defaultExceptionHandler.uncaughtException(t, e);
        } else {
          Log.w(LOG_TAG,
              "AVUncaughtExceptionHandler is disabled and there is no default handler, good luck.");
        }
        return;
      }

      brokenThread = t;
      unhandledThrowable = e;

      Log.e(LOG_TAG, "AVUncaughtExceptionHandler caught a " + e.getClass().getSimpleName()
          + " exception ");
      handleException(unhandledThrowable, false, true);
    } catch (Throwable fatality) {
      // failed. Prevent any recursive call to
      // uncaughtException(), let the native reporter do its job.
      if (defaultExceptionHandler != null) {
        defaultExceptionHandler.uncaughtException(t, e);
      }
    }
  }

  public void handleException(Throwable e, boolean endApplication) {
    handleException(e, false, endApplication);
  }

  /**
   * Send a report for a {@link Throwable} with the reporting interaction mode configured by the
   * developer, the application is then killed and restarted by the system.
   * 
   * @param e The {@link Throwable} to be reported. If null the report will contain a new
   *          Exception("Report requested by developer").
   */
  public void handleException(Throwable e) {
    handleException(e, false, false);
  }

  /**
   * Try to send a report, if an error occurs stores a report file for a later attempt.
   * 
   * @param e Throwable to be reported. If null the report will contain a new
   *          Exception("Report requested by developer").
   * @param forceSilentReport This report is to be sent silently, whatever mode has been configured.
   * @param endApplication Whether to end the application once the error has been handled.
   */
  private void handleException(Throwable e, final boolean forceSilentReport,
      final boolean endApplication) {
    if (!enabled) {
      return;
    }

    if (e == null) {
      e = new Exception("Report requested by developer");
    }

    Map<String, Object> map = crashData(context, e);
    AVAnalytics.reportError(context, map, null);

    if (endApplication) {
      endApplication();
    }
  }


  private String getStackTrace(Throwable throwable) {
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);

    // If the exception was thrown in a background thread inside
    // AsyncTask, then the actual exception can be found with getCause
    Throwable cause = throwable;
    while (cause != null) {
      cause.printStackTrace(printWriter);
      cause = cause.getCause();
    }
    final String stacktraceAsString = result.toString();
    printWriter.close();
    return stacktraceAsString;
  }

  private Map<String, Object> crashData(Context context, Throwable throwable) {
    Map<String, Object> crashReportData = new HashMap<String, Object>();
    try {
      crashReportData.put("reason", throwable.toString());
      crashReportData.put("stack_trace", getStackTrace(throwable));
      crashReportData.put("date", AVUtils.stringFromDate(new Date()));

      try {
        Class<?> installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
        Method getMethod = installationClass.getMethod("getCurrentInstallation");
        Method getInstallationIdMethod = installationClass.getMethod("getInstallationId");
        Object installation = getMethod.invoke(installationClass);
        String installationId = (String) getInstallationIdMethod.invoke(installation);
        crashReportData.put("installationId", installationId);
      } catch (Exception e) {}

      // Application Package name
      crashReportData.put("packageName", context.getPackageName());
      crashReportData.putAll(AnalyticsUtils.getDeviceInfo(context));
      crashReportData.put("memInfo", AnalyticsUtils.collectMemInfo());
      crashReportData.put("totalDiskSpace", AnalyticsUtils.getTotalInternalMemorySize());
      crashReportData.put("availableDiskSpace", AnalyticsUtils.getAvailableInternalMemorySize());
      crashReportData.put("appFilePath", AnalyticsUtils.getApplicationFilePath(context));
      crashReportData.put("ipAddress", AnalyticsUtils.getLocalIpAddress());
    } catch (RuntimeException e) {
      Log.e(LOG_TAG, "Error while retrieving crash data", e);
    }
    return crashReportData;
  }

  private void endApplication() {
    AVAnalytics.impl.pauseSession();
    AVAnalytics.impl.archiveCurrentSession();

    if (defaultExceptionHandler != null) {
      // If using silent mode, let the system default handler do it's job
      // and display the force close dialog.
      defaultExceptionHandler.uncaughtException(brokenThread, unhandledThrowable);
    } else {
      Log.e(LOG_TAG,
          context.getPackageName() + " fatal error : " + unhandledThrowable.getMessage(),
          unhandledThrowable);

      android.os.Process.killProcess(android.os.Process.myPid());
      System.exit(10);
    }
  }
}
