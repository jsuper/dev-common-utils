package org.tony.common.solr.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Value post handler of solr field
 *
 * @author Tony
 * @date 2015/9/9
 */
@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostHandler {

  String defaultMethod = "process";

  /**
   * Handler class
   *
   * @return
   */
  Class<?> handler();

  /**
   * Handler class method name, default method is object#process
   * The post process handler method should be accept two arguments.
   * The method signature should be match the target bean and value type.
   *
   * @param first  the instance of the origin bean class
   * @param second the origin value of the field
   * @param solrDocument solr document instance
   * @return
   */
  String method() default defaultMethod;
}
