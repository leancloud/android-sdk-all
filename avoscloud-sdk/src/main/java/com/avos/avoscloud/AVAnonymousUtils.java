package com.avos.avoscloud;

import java.util.UUID;

/**
 * <p>
 * Provides utility functions for working with Anonymously logged-in users. Anonymous users have
 * some unique characteristics:
 * </p>
 * <ul>
 * <li>Anonymous users don't need a user name or password.</li>
 * <li>Once logged out, an anonymous user cannot be recovered.</li>
 * <li>When the current user is anonymous, the following methods can be used to switch to a
 * different user or convert the anonymous user into a regular one:
 * <ul>
 * <li>signUp converts an anonymous user to a standard user with the given username and password.
 * Data associated with the anonymous user is retained.</li>
 * <li>logIn switches users without converting the anonymous user. Data associated with the
 * anonymous user will be lost.</li>
 * <li>Service logIn (e.g. Facebook, Twitter) will attempt to convert the anonymous user into a
 * standard user by linking it to the service. If a user already exists that is linked to the
 * service, it will instead switch to the existing user.</li>
 * <li>Service linking (e.g. Facebook, Twitter) will convert the anonymous user into a standard user
 * by linking it to the service.</li>
 * </ul>
 * </li>
 * </ul>
 */
public class AVAnonymousUtils {


  private static String anonymousAuthData() {
    String json =
        String.format("{\"authData\": {\"anonymous\" : {\"id\": \"%s\"}}}", UUID.randomUUID()
            .toString().toLowerCase());
    return json;
  }

  /**
   * Whether the user is logged in anonymously.
   * 
   * @param user User to check for anonymity. The user must be logged in on this device.
   * @return True if the user is anonymous. False if the user is not the current user or is not
   *         anonymous.
   */
  public static boolean isLinked(AVUser user) {
    return user == AVUser.getCurrentUser() && user.isAuthenticated() && user.isAnonymous();
  }

  /**
   * Creates an anonymous user.
   * 
   * @param callback The callback to execute when anonymous user creation is complete.
   */
  public static void logIn(LogInCallback<AVUser> callback) {
    final LogInCallback<AVUser> internalCallback = callback;
    String string = anonymousAuthData();
    PaasClient.storageInstance().postObject(AVUser.AVUSER_ENDPOINT, string, false, new GenericObjectCallback() {
      @Override
      public void onSuccess(String content, AVException e) {
        AVUser user = AVUser.newAVUser();
        AVUtils.copyPropertiesFromJsonStringToAVObject(content, user);
        user.setAnonymous(true);
        AVUser.changeCurrentUser(user, true);
        if (internalCallback != null) {
          internalCallback.internalDone(user, null);
        }
      }

      @Override
      public void onFailure(Throwable error, String content) {
        if (internalCallback != null) {
          internalCallback.internalDone(null, AVErrorUtils.createException(error, content));
        }
      }
    });
  }
}
