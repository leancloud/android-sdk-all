package com.avos.sns;

/**
 * <p>
 * A SNSException gets raised whenever a AVObject issues an invalid request, such as deleting or
 * editing an object that no longer exists on the server, or when there is a network failure
 * preventing communication with the AVOSCloud server.
 * </p>
 */
public class SNSException extends Exception {
  private static final long serialVersionUID = 1L;
  private int code;
  public static final int OTHER_CAUSE = -1;

  /**
   * Error code indicating that user has cancelled the login.
   */
  public static final int USER_CANCEL = 1;

  /**
   * Error code indicating that caller has not provided any app key.
   */
  public static final int NO_APP_KEY = 2;

  /**
   * Construct a new SNSException with a particular error code.
   * 
   * @param theCode The error code to identify the type of exception.
   * @param theMessage A message describing the error in more detail.
   */
  public SNSException(int theCode, String theMessage) {
    super(theMessage);
    this.code = theCode;
  }

  public static SNSException noAppKeyException() {
    return new SNSException(NO_APP_KEY, "No App Key");
  }

  /**
   * Construct a new SNSException with an external cause.
   * 
   * @param message A message describing the error in more detail.
   * @param cause The cause of the error.
   */
  public SNSException(String message, Throwable cause) {
    super(message, cause);
  }


  /**
   * Construct a new SNSException with an external cause.
   * 
   * @param cause The cause of the error.
   */
  public SNSException(Throwable cause) {
    super(cause);
  }

  /**
   * Access the code for this error.
   * 
   * @return The numerical code for this error.
   */
  public int getCode() {
    return code;
  }
}
