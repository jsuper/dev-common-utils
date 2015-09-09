package org.tony.solr;

import org.tony.solr.annotations.SingleSolrField;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class Book {
  @SingleSolrField
  private int id;
  @SingleSolrField
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
