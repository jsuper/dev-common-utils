package org.tony.solr.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Mark a field as solr document field
 *
 * @author Tony
 * @date 2015/9/9
 */
@Target({FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SingleSolrField {

  String name() default "";
}
