package org.tony.common.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class SolrDocument {
  private Map<String, Object> field = new HashMap<>();

  public void setField(String name, Object value) {
    this.field.put(name, value);
  }

  public Set<String> keySet() {
    return field.keySet();
  }

  public Object getFieldValue(String key) {
    if (field.containsKey(key)) {
      return field.get(key);
    }
    return null;
  }

  public void putAll(Map<String, Object> all) {
    this.field.putAll(all);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("SolrDocument{");
    sb.append("field=").append(field);
    sb.append('}');
    return sb.toString();
  }
}
