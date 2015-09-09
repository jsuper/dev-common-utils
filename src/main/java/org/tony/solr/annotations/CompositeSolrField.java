package org.tony.solr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite solr field. This will include all solr field in the target type
 *
 * @author Tony
 * @date 2015/9/9
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CompositeSolrField {

  /**
   * composite solr field prefix
   *
   * @return
   */
  String prefix();
}
