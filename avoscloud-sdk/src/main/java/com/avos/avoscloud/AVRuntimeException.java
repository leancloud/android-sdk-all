package com.avos.avoscloud;

/**
 * AVOSCloud runtime exception.
 * 
 * @author dennis<xzhuang@avos.com>
 * 
 */
public class AVRuntimeException extends RuntimeException {

  /**
     * 
     */
  private static final long serialVersionUID = 1L;

  public AVRuntimeException() {
    super();

  }

  public AVRuntimeException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);

  }

  public AVRuntimeException(String detailMessage) {
    super(detailMessage);

  }

  public AVRuntimeException(Throwable throwable) {
    super(throwable);

  }

}
