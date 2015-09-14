package org.tony.common.solr;

import org.tony.common.solr.annotations.BasicField;

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

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Book{");
    sb.append("id=").append(id);
    sb.append(", name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
