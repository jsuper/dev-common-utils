package org.tony.solr;

import org.tony.lang.Clazz;
import org.tony.solr.annotations.BasicField;
import org.tony.solr.annotations.CompositeField;
import org.tony.solr.annotations.DynamicField;
import org.tony.solr.annotations.PostHandler;
import org.tony.solr.exceptions.ValueAccessException;
import org.tony.solr.exceptions.ValueProcessException;
import org.tony.solr.exceptions.ValueSetException;

import java.lang.annotation.Annotation;
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

  private String setField = "setField";
  private static final Map<Class, Class> implement = new HashMap<>();
  private static final Set<Class<?>> solrFields = new HashSet<>();

  {
    solrFields.add(BasicField.class);
    solrFields.add(CompositeField.class);
    solrFields.add(DynamicField.class);
  }

  {
    implement.put(Set.class, HashSet.class);
    implement.put(List.class, ArrayList.class);
    implement.put(Collection.class, ArrayList.class);
  }



  private static final boolean isSolrField(AccessibleObject ao) {
    Annotation[] annotations = ao.getDeclaredAnnotations();
    for (Annotation annotation : annotations) {
      if (solrFields.contains(annotation.annotationType())) {
        return true;
      }
    }
    return false;
  }

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
    T docInstance;
    Method setFieldMethod;
    try {
      docInstance = docClass.newInstance();
      setFieldMethod = docClass.getDeclaredMethod(setField, String.class, Object.class);
    } catch (Exception e) {
      throw new RuntimeException("Instantiate solr document instance failed", e);
    }
    if (docInstance == null || setFieldMethod == null) {
      return null;
    }
    List<AccessibleObject> accessibleObjects = Clazz.getAccessibleObject(beanClazz);

    Map<String, T> compositeDocs = new HashMap<>();

    for (AccessibleObject ao : accessibleObjects) {
      boolean isSingleSolrField = Clazz.isAnnotationPresent(ao, BasicField.class);
      boolean isCompositeField = Clazz.isAnnotationPresent(ao, CompositeField.class);
      boolean isDynamicField = Clazz.isAnnotationPresent(ao, DynamicField.class);


      if (isSingleSolrField || isCompositeField || isDynamicField) {
        boolean isField = ao instanceof Field;
        boolean isMethod = ao instanceof Method;
        Object value = getValue(from, ao, isField);
        if (value == null) {
          continue;
        }

        boolean hasPostHandler = Clazz.isAnnotationPresent(ao, PostHandler.class);
        if (hasPostHandler) {
          PostHandler ph = Clazz.getAnnotation(ao, PostHandler.class);
          value = postProcessValue(from, value, ph);
        }

        if (isSingleSolrField) {
          BasicField solrField = Clazz.getAnnotation(ao, BasicField.class);
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
          invokeSetFieldMethod(docInstance, setFieldMethod, solrFieldName, value);
        } else if (isCompositeField) {
          CompositeField csf = Clazz.getAnnotation(ao, CompositeField.class);
          T solrCompositeBean = toSolrDocument(value, docClass);
          if (solrCompositeBean != null) {
            compositeDocs.put(csf.prefix() + csf.joinChar(), solrCompositeBean);
          }
        } else if (isDynamicField) {
          DynamicField dynamicField = Clazz.getAnnotation(ao, DynamicField.class);
          Class<?> entryHandler =
                  dynamicField.handler() == Void.class ? null : dynamicField.handler();
          String method = dynamicField.method();
          if (value instanceof Map) {
            Method applyMethod = null;
            Map actualValue = (Map) value;
            Iterator<Map.Entry> iterator = actualValue.entrySet().iterator();
            while (iterator.hasNext()) {
              Map.Entry entry = iterator.next();
              if (entryHandler != null) {
                if (applyMethod == null) {
                  try {
                    applyMethod = entryHandler.getDeclaredMethod(method, entry.getClass());
                  } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("No such method of handler:" + entryHandler, e);
                  }
                }
                if (method != null) {
                  try {
                    entry = (Map.Entry) applyMethod.invoke(null, entry, from);
                  } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Apply handler method on DynamicField failed", e);
                  }
                }
              }
              try {
                setFieldMethod.invoke(docInstance,
                        dynamicField.prefix() + dynamicField.joinChar() + entry.getKey(),
                        entry.getValue());
              } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Handle dynamic field error!", e);
              }
            }
          } else {
            throw new IllegalArgumentException(
                    "DynamicField only support type of Map or its subclass!");
          }
        }
      }
    }
    merge(docInstance, compositeDocs, setFieldMethod);
    return docInstance;
  }

  private Object getValue(Object bean, AccessibleObject ao, boolean isField) {
    Object value = null;
    Method valueAccessMethod;
    if (isField) {
      try {
        String getter = Clazz.getGetterName(((Field) ao).getName());
        valueAccessMethod = bean.getClass().getDeclaredMethod(getter, null);
      } catch (NoSuchMethodException e) {
        valueAccessMethod = null;
      }
    } else {
      valueAccessMethod = (Method) ao;
    }


    if (valueAccessMethod != null) {
      try {
        value = valueAccessMethod.invoke(bean, null);
      } catch (Exception e) {
        throw new ValueAccessException("Access value failed!", e);
      }
    } else if (isField && valueAccessMethod == null) {
      try {
        value = Clazz.getValue(bean, ((Field) ao));
      } catch (IllegalAccessException e) {
        throw new ValueAccessException("Cannot access value of field " + ((Field) ao).getName());
      }
    }
    return value;
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
          setField.invoke(docInstance, prefix + key, getFieldValue.invoke(value, key));
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
    if (solrDocument == null) {
      return null;
    }
    Method getFieldValue;
    Set<String> allKeys;
    try {
      getFieldValue = solrDocument.getClass().getDeclaredMethod("getFieldValue", String.class);
      allKeys = (Set<String>) solrDocument.getClass().getDeclaredMethod("keySet", null)
              .invoke(solrDocument);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No method named getFieldValue", e);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Get all keys from solr document failed!", e);
    }
    T returnBean;
    try {
      returnBean = beanClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Instantiate target class instance failed", e);
    }

    List<AccessibleObject> accessibleObjects = Clazz.getAccessibleObject(beanClass);
    for (AccessibleObject ao : accessibleObjects) {
      boolean isBasicField = Clazz.isAnnotationPresent(ao, BasicField.class);
      boolean isCompositeField = Clazz.isAnnotationPresent(ao, CompositeField.class);
      boolean isDynamicField = Clazz.isAnnotationPresent(ao, DynamicField.class);

      boolean isField = ao instanceof Field;
      boolean isMethod = ao instanceof Method;


      if (isBasicField) {
        BasicField basicField = Clazz.getAnnotation(ao, BasicField.class);
        String fieldName = basicField.name();
        try {
          Object value = getFieldValue.invoke(solrDocument, fieldName);
          if (value != null) {
            if (isField) {
              Clazz.setValue(returnBean, (Field) ao, value);
            } else if (isMethod) {
              setValueBySetter(returnBean, (Method) ao, fieldName, value);
            }
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
          e.printStackTrace();
        }
      } else if (isDynamicField || isCompositeField) {

        DynamicField dynamicField;
        CompositeField compositeField;
        String fieldPrefix;
        Class valueType = isField ? ((Field) ao).getType() : ((Method) ao).getReturnType();
        if (isCompositeField) {
          compositeField = Clazz.getAnnotation(ao, CompositeField.class);
          fieldPrefix = compositeField.prefix() + compositeField.joinChar();
        } else {
          dynamicField = Clazz.getAnnotation(ao, DynamicField.class);
          fieldPrefix = dynamicField.prefix() + dynamicField.joinChar();
        }

        List<String> fieldNames = new ArrayList<>();
        for (String key : allKeys) {
          if (key.startsWith(fieldPrefix)) {
            fieldNames.add(key);
          }
        }

        Map<String, Object> fieldValues = new HashMap<>();
        for (String field : fieldNames) {
          try {
            Object fieldValue = getFieldValue.invoke(solrDocument, field);
            if (fieldValue != null) {
              fieldValues.put(field, fieldValue);
            }
          } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
          }
        }

        if (isDynamicField) {
          Object dynamicFieldValue;
          if (Map.class.isAssignableFrom(valueType)) {
            Map value = new HashMap();
            for (String field : fieldValues.keySet()) {
              String fieldName = field.substring(fieldPrefix.length());
              Object fieldVal = fieldValues.get(field);
              if (fieldVal != null) {
                value.put(fieldName, fieldVal);
              }
            }
            dynamicFieldValue = value;
            //<editor-fold desc="Not support for other type">
          /*} else if (implement.containsKey(valueType)) {
            Class container = implement.get(valueType);
            if (container != null) {
              try {
                Collection o = (Collection) container.newInstance();
                Class<T> genClassType =
                        (Class<T>) ((ParameterizedType) valueType.getGenericSuperclass())
                                .getActualTypeArguments()[0];
                Method setKey = genClassType.getDeclaredMethod("setKey", String.class);
                Method setValue = genClassType.getDeclaredMethod("setValue", Object.class);
                for (String field : fieldValues.keySet()) {
                  String fieldName = field.substring(fieldPrefix.length());
                  try {
                    Object fieldValue = fieldValues.get(field);
                    if (fieldValue != null) {
                      T innerBean = genClassType.newInstance();
                      setKey.invoke(innerBean, fieldName);
                      setValue.invoke(innerBean, fieldValue);
                      o.add(innerBean);
                    }
                  } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                  }
                }
                dynamicFieldValue = o;
              } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
              } catch (NoSuchMethodException e) {
                e.printStackTrace();
              }
            }*/
            //</editor-fold>
          } else {
            throw new RuntimeException("Dynamic field must be subclass of Map");
          }
          if (dynamicFieldValue != null) {
            try {
              if (isField) {
                Clazz.setValue(returnBean, (Field) ao, dynamicFieldValue);
              } else {
                Method writeMethod = Clazz.getSetterByGetter((Method) ao, beanClass);
                if (writeMethod != null) {
                  writeMethod.invoke(returnBean, dynamicFieldValue);
                }
              }
            } catch (IllegalAccessException | InvocationTargetException e) {
              e.printStackTrace();
            }
          }
        } else {
          Object solrDoc = null;
          Method setFieldMethod = null;
          try {
            solrDoc = solrDocument.getClass().newInstance();
            setFieldMethod =
                    solrDocument.getClass().getDeclaredMethod(setField, String.class, Object.class);
          } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("InstantiationException", e);
          } catch (NoSuchMethodException e) {
            e.printStackTrace();
          }
          if (solrDoc != null && setFieldMethod != null) {
            for (String key : fieldValues.keySet()) {
              String fieldName = key.substring(fieldPrefix.length());
              Object value = fieldValues.get(key);
              try {
                setFieldMethod.invoke(solrDoc, fieldName, value);
              } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
              }
            }

            //for composite filed, convert these values to given composite bean, then set value to
            //that field
            Object innerBean = toBean(solrDoc, valueType);
            try {
              if (isField) {
                Clazz.setValue(returnBean, (Field) ao, innerBean);
              } else {
                Method writeMethod = Clazz.getSetterByGetter((Method) ao, beanClass);
                if (writeMethod != null) {
                  writeMethod.invoke(returnBean, innerBean);
                }
              }
            } catch (IllegalAccessException | InvocationTargetException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    return returnBean;
  }

  private void setValueBySetter(Object bean, Method getter, String fieldName, Object value) {
    Class<?> valueType = getter.getReturnType();
    Method setMethod;
    try {
      setMethod = bean.getClass().getDeclaredMethod(Clazz.getSetterName(fieldName), valueType);
      setMethod.invoke(bean, value);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("No such method exception", e);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Set value for " + fieldName + " failed!", e);
    }
  }

  private List<AccessibleObject> getSolrFields(Class<?> beanClass) {
    List<AccessibleObject> accessibleObjects = new ArrayList<>();
    Field[] fields = beanClass.getDeclaredFields();
    for (Field field : fields) {
      accessibleObjects.add(field);
    }
    Method[] methods = beanClass.getDeclaredMethods();
    for (Method method : methods) {
      accessibleObjects.add(method);
    }

    return accessibleObjects;
  }

  private Map<String, AccessibleObject> getAllSolrFields(Class<?> beanClass) {
    Map<String, AccessibleObject> solrField = new HashMap<>();
    Field[] fields = beanClass.getDeclaredFields();
    for (Field field : fields) {
      if (Clazz.isAnnotationPresent(field, BasicField.class)) {
        BasicField sf = Clazz.getAnnotation(field, BasicField.class);
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
      if (Clazz.isAnnotationPresent(method, BasicField.class)) {
        BasicField sf = Clazz.getAnnotation(method, BasicField.class);
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

  public String getSetField() {
    return setField;
  }

  public void setSetField(String setField) {
    this.setField = setField;
  }
}
