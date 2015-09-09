package org.tony.solr.exceptions;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class ValueSetException extends RuntimeException {
  public ValueSetException() {
  }

  public ValueSetException(String message) {
    super(message);
  }

  public ValueSetException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValueSetException(Throwable cause) {
    super(cause);
  }

  public ValueSetException(String message, Throwable cause, boolean enableSuppression,
          boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
