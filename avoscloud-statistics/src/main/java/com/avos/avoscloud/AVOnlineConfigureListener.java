package com.avos.avoscloud;

import org.json.JSONObject;

/**
 * 在线参数监听器，此接口只在在线参数有变化的时候才会回调
 * 
 * @since 1.4.2
 * @author dennis<xzhuang@avos.com>
 * 
 */
public interface AVOnlineConfigureListener {
  /**
   * 此接口只在在线参数有变化的时候才会回调，所有在线参数都是key-value结构，key和value都为字符串。
   * 
   * @param data 在线参数的JSON数据对象
   */
  public void onDataReceived(JSONObject data);
}
