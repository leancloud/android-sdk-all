package com.avos.avoscloud;

/**
 * AVDeleteOption is a option value for AVObject delete operation
 */
public class AVDeleteOption {
  AVQuery matchQuery;

  /**
   * Only delete object when query matches AVObject instance data
   *
   * @param query
   * @return
   */

  public AVDeleteOption query(AVQuery query) {
    this.matchQuery = query;
    return this;
  }
}
