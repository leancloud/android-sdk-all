package com.avos.avoscloud;

import java.util.List;

/**
 * Created by wli on 2017/5/30.
 */

public abstract class AVLiveQueryEventHandler {

  public void done(AVLiveQuery.EventType eventType, AVObject avObject, List<String> updateKeyList) {
    switch (eventType) {
      case ENTER:
        onObjectEnter(avObject, updateKeyList);
        break;
      case UPDATE:
        onObjectUpdated(avObject, updateKeyList);
        break;
      case DELETE:
        onObjectDeleted(avObject.getObjectId());
        break;
      case LEAVE:
        onObjectLeave(avObject, updateKeyList);
        break;
      case LOGIN:
        if (avObject instanceof AVUser) {
          onUserLogin((AVUser)avObject);
        }
        break;
      case CREATE:
        onObjectCreated(avObject);
        break;
    }
  }

  /**
   * This method will be called when an associated AVObject created
   * @param avObject
   */
  public void onObjectCreated(AVObject avObject) {}

  /**
   * This method will be called when an associated AVObject updated
   * @param avObject
   */
  public void onObjectUpdated(AVObject avObject, List<String> updateKeyList) {}

  /**
   * This method will be called when an AVObject matched the associated AVQuery after update
   * @param avObject
   */
  public void onObjectEnter(AVObject avObject, List<String> updateKeyList) {}

  /**
   * This method will be called when an AVObject is modified and does not conform to the relevant query
   * @param avObject
   */
  public void onObjectLeave(AVObject avObject, List<String> updateKeyList) {}

  /**
   * This method will be called when a related AVObject is deleted
   * @param objectId
   */
  public void onObjectDeleted(String objectId) {}

  /**
   * This method will be called when a related user login
   * @param user
   */
  public void onUserLogin(AVUser user) {}
}