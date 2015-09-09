package org.tony.solr;

import com.google.common.collect.Lists;
import org.tony.lang.Clazz;
import org.tony.solr.annotations.CompositeSolrField;
import org.tony.solr.annotations.PostHandler;
import org.tony.solr.annotations.SingleSolrField;
import org.tony.solr.exceptions.ValueAccessException;
import org.tony.solr.exceptions.ValueProcessException;
import org.tony.solr.exceptions.ValueSetException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Convert a Java Bean to solr document, or convert solr document to JavaBean
 *
 * @author Tony
 * @date 2015/9/9
 */
public class BeanConverter {

  private String setSolrFieldMethodName = "setField";

  /**
   * Convert a bean instance to a solr input document
   *
   * @param from The bean should be converted
   * @return solr accepted document
   */
  public <T> T toSolrDocument(Object from, Class<T> docClass) {
    if (from == null || docClass == null) {
      return null;
    }
    Class<?> beanClazz = from.getClass();
    Object docInstance;
    Method setFieldMethod;
    try {
      docInstance = docClass.newInstance();
      setFieldMethod =
              docClass.getDeclaredMethod(setSolrFieldMethodName, String.class, Object.class);
    } catch (Exception e) {
      throw new RuntimeException("Instantiate solr document instance failed", e);
    }
    if (docInstance == null || setFieldMethod == null) {
      return null;
    }

    Field[] beanFields = beanClazz.getDeclaredFields();
    Method[] methods = beanClazz.getDeclaredMethods();
    List<AccessibleObject> accessibleObjects = new ArrayList<>();
    accessibleObjects.addAll(Lists.newArrayList(beanFields));
    accessibleObjects.addAll(Lists.newArrayList(methods));

    Map<String, T> compositeDocs = new HashMap<>();

    for (AccessibleObject ao : accessibleObjects) {
      boolean isSingleSolrField = Clazz.isAnnotationPresent(ao, SingleSolrField.class);
      boolean isCompositeField = Clazz.isAnnotationPresent(ao, CompositeSolrField.class);
      boolean isField = ao instanceof Field;
      boolean isMethod = ao instanceof Method;
      if (isSingleSolrField) {
        SingleSolrField solrField = Clazz.getAnnotation(ao, SingleSolrField.class);
        String solrFieldName;
        if (isField) {
          solrFieldName =
                  solrField.name().length() == 0 ? ((Field) ao).getName() : solrField.name();
        } else if (isMethod) {
          if (solrField.name().length() == 0) {
            Method m = (Method) ao;
            String name = m.getName();
            if (name.startsWith("get")) {
              String tempName = name.substring(3);
              solrFieldName =
                      String.valueOf(tempName.charAt(0)).toLowerCase() + tempName.substring(1);
            } else {
              solrFieldName = name;
            }
          } else {
            solrFieldName = solrField.name();
          }
        } else {
          continue;
        }

        Method valueAccessMethod;
        if (isField) {
          try {
            String getter = Clazz.getGetterName(((Field) ao).getName());
            valueAccessMethod = beanClazz.getDeclaredMethod(getter, null);
          } catch (NoSuchMethodException e) {
            valueAccessMethod = null;
          }
        } else {
          valueAccessMethod = (Method) ao;
        }

        Object value = null;
        if (valueAccessMethod != null) {
          try {
            value = valueAccessMethod.invoke(from, null);
          } catch (Exception e) {
            throw new ValueAccessException("Access value failed!", e);
          }
        } else if (isField && valueAccessMethod == null) {
          try {
            value = Clazz.getValue(from, ((Field) ao));
          } catch (IllegalAccessException e) {
            throw new ValueAccessException(
                    "Cannot access value of field " + ((Field) ao).getName());
          }
        }
        if (value == null) {
          continue;
        }

        boolean hasPostHandler = Clazz.isAnnotationPresent(ao, PostHandler.class);
        if (hasPostHandler) {
          PostHandler ph = Clazz.getAnnotation(ao, PostHandler.class);
          value = postProcessValue(from, value, ph);
        }
        invokeSetFieldMethod(docInstance, setFieldMethod, solrFieldName, value);
      } else if (isCompositeField) {
        CompositeSolrField csf = Clazz.getAnnotation(ao, CompositeSolrField.class);
        Object compositeBean = null;
        try {
          if (isField) {
            compositeBean = Clazz.getValue(from, (Field) ao);
          } else if (isMethod) {
            compositeBean = ((Method) ao).invoke(from);
          }
        } catch (Exception e) {
          //ignore
        }
        T solrCompositeBean = toSolrDocument(compositeBean, docClass);
        if (solrCompositeBean != null) {
          compositeDocs.put(csf.prefix(), solrCompositeBean);
        }
      }
    }
    merge(docInstance, compositeDocs, setFieldMethod);
    return (T) docInstance;
  }

  private void merge(Object docInstance, Map<String, ?> innerDoc, Method setField) {
    if (innerDoc == null || innerDoc.isEmpty()) {
      return;
    }
    Method keySet;
    Method getFieldValue;
    try {
      keySet = docInstance.getClass().getDeclaredMethod("keySet");
      getFieldValue = docInstance.getClass().getDeclaredMethod("getFieldValue", String.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    for (Map.Entry<String, ?> doc : innerDoc.entrySet()) {
      try {
        String prefix = doc.getKey();
        Object value = doc.getValue();
        Set<String> allKeys = (Set<String>) keySet.invoke(value);
        for (String key : allKeys) {
          setField.invoke(docInstance, prefix + "_" + key, getFieldValue.invoke(value, key));
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Merge document failed!" + doc);
      }
    }
  }

  /**
   * Post-process handler to process the origin value
   *
   * @param instance    Origin bean class instance
   * @param value       Value should be process
   * @param postHandler PostHandler annotation instance, see {@link PostHandler}
   * @return
   */
  private Object postProcessValue(Object instance, Object value, PostHandler postHandler) {
    try {
      Class<?> postHandlerClass = postHandler.handler();
      Method valueProcess = postHandlerClass
              .getDeclaredMethod(postHandler.method(), value.getClass(), instance.getClass());
      return valueProcess.invoke(null, value, instance);
    } catch (Exception e) {
      throw new ValueProcessException("Process value for failed!", e);
    }
  }

  /**
   * Invoke the add field method to add a field to it
   *
   * @param instance  document instance
   * @param invoke    target method
   * @param fieldName field name
   * @param value     field value
   */
  private void invokeSetFieldMethod(Object instance, Method invoke, String fieldName,
          Object value) {
    try {
      invoke.invoke(instance, fieldName, value);
    } catch (Exception e) {
      throw new ValueSetException("Set value for solr document failed!", e);
    }
  }

  public <T> T toBean(Object solrDocument, Class<T> beanClass) {
    Map<String, AccessibleObject> solrFields = getAllSolrFields(beanClass);
    Method keySets;
    Set<String> allKeys;
    try {
      keySets = solrDocument.getClass().getDeclaredMethod("keySet", null);
      allKeys = (Set<String>) keySets.invoke(solrDocument);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Cannot get all solr document keys", e);
    }

    if (allKeys == null) {
      return null;
    }
    Method getFieldValue;
    try {
      getFieldValue = solrDocument.getClass().getDeclaredMethod("getFieldValue", String.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No method named getFieldValue", e);
    }
    T returnBean;
    try {
      returnBean = beanClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Instantiate target class instance failed", e);
    }
    for (String field : allKeys) {
      if (solrFields.containsKey(field)) {
        AccessibleObject target = solrFields.get(field);
        try {
          Object value = getFieldValue.invoke(solrDocument, field);
          if (target instanceof Method) {
            ((Method) target).invoke(returnBean, value);
          } else if (target instanceof Field) {
            Clazz.setValue(returnBean, (Field) target, value);
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
          //ignore for field
        }
      }
    }

    return returnBean;
  }

  private Map<String, AccessibleObject> getAllSolrFields(Class<?> beanClass) {
    Map<String, AccessibleObject> solrField = new HashMap<>();
    Field[] fields = beanClass.getDeclaredFields();
    for (Field field : fields) {
      if (Clazz.isAnnotationPresent(field, SingleSolrField.class)) {
        SingleSolrField sf = Clazz.getAnnotation(field, SingleSolrField.class);
        String name = sf.name().length() == 0 ? field.getName() : sf.name();
        try {
          Method setMethod = beanClass
                  .getDeclaredMethod(Clazz.getSetterName(field.getName()), field.getType());
          solrField.put(name, setMethod);
        } catch (NoSuchMethodException e) {
          solrField.put(name, field);
        }
      }
    }

    Method[] methods = beanClass.getDeclaredMethods();
    for (Method method : methods) {
      if (Clazz.isAnnotationPresent(method, SingleSolrField.class)) {
        SingleSolrField sf = Clazz.getAnnotation(method, SingleSolrField.class);
        String name = sf.name();
        if (name.length() == 0) {
          name = method.getName();
          if (method.getName().startsWith("get")) {
            String tempName = name.substring(3);
            name = String.valueOf(tempName.charAt(0)).toLowerCase() + tempName.substring(1);
          }
        }
        Class<?> valueType = method.getReturnType();
        try {
          Method setMethod = beanClass.getDeclaredMethod(Clazz.getSetterName(name), valueType);
          solrField.put(name, setMethod);
        } catch (NoSuchMethodException e) {
          try {
            Field field = beanClass.getDeclaredField(name);
            if (field != null && (field.getType() == valueType)) {
              solrField.put(name, field);
            }
          } catch (NoSuchFieldException e1) {
            //no set method, no field
          }
        }
      }
    }

    return solrField;
  }

  public String getSetSolrFieldMethodName() {
    return setSolrFieldMethodName;
  }

  public void setSetSolrFieldMethodName(String setSolrFieldMethodName) {
    this.setSolrFieldMethodName = setSolrFieldMethodName;
  }
}
