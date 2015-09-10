package org.tony.solr;

import org.tony.solr.annotations.BasicField;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class Book {
  @BasicField(name = "id")
  private int id;
  @BasicField(name = "name")
  private String name;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
