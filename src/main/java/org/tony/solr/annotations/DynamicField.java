package org.tony.solr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dynamic solr field. The target type should be subtype of {@link Iterable}
 *
 * @author Tony
 * @date 2015/9/9
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicField {

  /**
   * Dynamic solr field prefix
   *
   * @return
   */
  String prefix();

  String joinChar() default "_";

  /**
   * Method name which retrieve the key
   *
   * @return
   */
  String key() default "getKey";

  /**
   * Method name which retrieve the value of the key
   *
   * @return
   */
  String val() default "getValue";

  /**
   * Handler for each value
   *
   * @return
   */
  Class<?> handler() default Void.class;

  /**
   * Handle method for each value entry
   *
   * @return
   */
  String method() default "apply";
}
