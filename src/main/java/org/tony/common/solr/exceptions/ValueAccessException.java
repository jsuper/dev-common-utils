package org.tony.common.solr.exceptions;

/**
 * Access field value exception
 *
 * @author Tony
 * @date 2015/9/9
 */
public class ValueAccessException extends RuntimeException {
  public ValueAccessException() {
  }

  public ValueAccessException(String message) {
    super(message);
  }

  public ValueAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValueAccessException(Throwable cause) {
    super(cause);
  }

  public ValueAccessException(String message, Throwable cause, boolean enableSuppression,
          boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
