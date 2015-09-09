package org.tony.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

/**
 * @author Tony
 * @date 2015/9/9
 */
public final class Clazz {

  public static String getGetterName(String field) {
    String first = String.valueOf(field.charAt(0));
    String others = field.substring(1);
    return "get" + first.toUpperCase() + others;
  }

  public static String getSetterName(String fieldName) {
    String first = String.valueOf(fieldName.charAt(0));
    String others = fieldName.substring(1);
    return "set" + first.toUpperCase() + others;
  }

  /**
   * Get {@link Annotation} of the given annotation type from an {@link AccessibleObject}.
   * Compatible for JDK 1.5
   *
   * @param ao              The AccessibleObject, like {@link Field}, {@link java.lang.reflect.Method}
   * @param annotationClass Annotation class type
   * @param <T>
   * @return
   */
  public static <T extends Annotation> T getAnnotation(AccessibleObject ao,
          Class<T> annotationClass) {
    Annotation[] annotations = ao.getDeclaredAnnotations();
    for (Annotation annotation : annotations) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  /**
   * Check if an annotation exist in an {@link AccessibleObject}
   *
   * @param ao
   * @param annotationClass
   * @return
   */
  public static boolean isAnnotationPresent(AccessibleObject ao,
          Class<? extends Annotation> annotationClass) {
    return ao.isAnnotationPresent(annotationClass);
  }

  /**
   * Get the value from an Field
   *
   * @param bean
   * @param field
   * @return
   * @throws IllegalAccessException
   */
  public static Object getValue(Object bean, Field field) throws IllegalAccessException {
    boolean acc = field.isAccessible();
    if (!acc) {
      field.setAccessible(true);
    }
    Object value = field.get(bean);
    field.setAccessible(acc);
    return value;
  }

  /**
   * Set value for a Field
   *
   * @param bean
   * @param field
   * @param value
   * @throws IllegalAccessException
   */
  public static void setValue(Object bean, Field field, Object value)
          throws IllegalAccessException {
    boolean acc = field.isAccessible();
    if (!acc) {
      field.setAccessible(true);
    }
    field.set(bean, value);
    field.setAccessible(acc);
  }
}
