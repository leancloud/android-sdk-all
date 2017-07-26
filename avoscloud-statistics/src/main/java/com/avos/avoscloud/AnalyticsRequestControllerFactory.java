package com.avos.avoscloud;

class AnalyticsRequestControllerFactory {
  static AnalyticsRequestController getAnalyticsRequestController(String sessionId,
      ReportPolicy reportPolicy, AnalyticsImpl implement) {
    AnalyticsRequestController requestController = null;
    switch (reportPolicy) {
      case SEND_INTERVAL:
        requestController =
            new IntervalRequestController(sessionId, implement, AnalyticsUtils.getRequestInterval());
        break;
      case REALTIME:
      case SENDWIFIONLY:
        requestController = implement.realTimeController;
        break;
      case SEND_ON_EXIT:
        requestController = new BoosterRequestController(sessionId, implement);
        break;
      case BATCH:
      default:
        // default is batch policy for common
        requestController =
            new BatchRequestController(sessionId, implement, AnalyticsUtils.getRequestInterval());
        break;
    }
    return requestController;
  }
}
