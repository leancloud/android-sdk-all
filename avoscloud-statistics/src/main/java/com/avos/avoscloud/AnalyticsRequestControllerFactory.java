package com.avos.avoscloud;

class AnalyticsRequestControllerFactory {
  /**
   * 生成 RequestController 实例
   * @param sessionId
   * @param reportPolicy
   * @param implement
   * @return
   */
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
        requestController = new RealTimeRequestController(implement);//implement.realTimeController;
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
