package org.tony.common.solr.exceptions;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class ValueProcessException extends RuntimeException {
  public ValueProcessException() {
  }

  public ValueProcessException(String message) {
    super(message);
  }

  public ValueProcessException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValueProcessException(Throwable cause) {
    super(cause);
  }

  public ValueProcessException(String message, Throwable cause, boolean enableSuppression,
          boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
