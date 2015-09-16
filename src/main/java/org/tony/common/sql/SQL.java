package org.tony.common.sql;

/**
 * @author Tony
 * @date 2015/9/16
 */
public final class SQL {

  public static final String placeholder(int size) {
    StringBuilder placeHolder = new StringBuilder();
    for (int i = 0; i < size - 1; i++) {
      placeHolder.append("?,");
    }
    placeHolder.append("?");
    return placeHolder.toString();
  }
}
