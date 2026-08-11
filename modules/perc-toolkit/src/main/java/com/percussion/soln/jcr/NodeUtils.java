/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.soln.jcr;

// REFACTORED: CP-JAVA11
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.services.contentmgr.IPSNodeDefinition;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NodeUtils class.
 */
public class NodeUtils {
  /**
   * Creates a new NodeUtils.
   */
  public NodeUtils() {
    // default
  }


  public abstract static class BeanPropertyToNodePropertyNameTranslator {
    /**
     * Returns the property name.
     *
     * @param d the d
     * @return the result
     */
    public abstract String getPropertyName(PropertyDescriptor d);
  }

  /**
   * DefaultTranslator class.
   */
  public static class DefaultTranslator extends BeanPropertyToNodePropertyNameTranslator {

    // REFACTORED: CP-JAVA11
    private String fieldPrefix;
    private Collection<String> ignoreFields;
    private Map<String, String> nameMap;

    /**
     * Creates a new DefaultTranslator.
     *
     * @param fieldPrefix the field prefix
     * @param ignoreFields the ignore fields
     * @param nameMap the name map
     */
    public DefaultTranslator(
        String fieldPrefix, Collection<String> ignoreFields, Map<String, String> nameMap) {
      super();
      this.fieldPrefix = fieldPrefix == null ? "" : fieldPrefix;
      this.ignoreFields = ignoreFields == null ? Collections.<String>emptyList() : ignoreFields;
      this.nameMap = nameMap;
    }

    /**
     * Returns the property name.
     *
     * @param d the d
     * @return the result
     */
    @Override
    public String getPropertyName(PropertyDescriptor d) {
      String name = d.getName();
      if (ignoreFields.contains(name)) return null;
      if (nameMap != null && nameMap.containsKey(name)) return nameMap.get(name);
      return fieldPrefix + name;
    }
  }

  /**
   * copyFromNode operation.
   *
   * @param node the node
   * @param object the object
   * @param t the t
   */
  public static <T> void copyFromNode(
      Node node, T object, BeanPropertyToNodePropertyNameTranslator t) {
    notNull(node);
    notNull(object);
    PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(object);
    for (PropertyDescriptor d : descriptors) {
      String propName = t.getPropertyName(d);
      if (propName == null) continue;
      try {
        if (!node.hasProperty(propName)) {
          continue;
        }
        Property p = node.getProperty(propName);
        Class<?> type = d.getPropertyType();
        if (type.isAssignableFrom(Long.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getLong());
        } else if (type.isAssignableFrom(Double.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getDouble());
        } else if (type.isAssignableFrom(String.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getString());
        } else if (type.isAssignableFrom(Boolean.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getBoolean());
        } else if (type.isAssignableFrom(Date.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getDate().getTime());
        } else if (type.isAssignableFrom(Calendar.class)) {
          PropertyUtils.setProperty(object, d.getName(), p.getDate());
        } else if (type.isAssignableFrom(List.class)) {
          List<String> vs = new ArrayList<String>();
          if (p.getDefinition().isMultiple()) {
            Value[] values = p.getValues();
            for (Value v : values) {
              vs.add(v.getString());
            }
          } else {
            vs.add(p.getString());
          }
          PropertyUtils.setProperty(object, d.getName(), vs);
        } else {
          log.warn("Do not know how to convert type: " + type + " for property: " + propName);
        }
      } catch (Exception e) {
        log.error("Failure to copy property " + propName);
      }
    }
  }

  /**
   * Returns the string.
   *
   * @param node the node
   * @param name the name
   * @return the result
   */
  public static String getString(Node node, String name) {
    notNull(node);
    notEmpty(name);
    try {
      return node.getProperty(name).getString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the date.
   *
   * @param node the node
   * @param name the name
   * @return the result
   */
  public static Date getDate(Node node, String name) {
    notNull(node);
    notEmpty(name);
    try {
      return node.getProperty(name).getDate().getTime();
    } catch (Exception e) {
      return new Date();
    }
  }

  /**
   * Returns the content type.
   *
   * @param node the node
   * @return the result
   */
  public static String getContentType(Node node) {
    try {
      String contentType = ((IPSNodeDefinition) node.getDefinition()).getInternalName();
      return contentType;
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * copyFromNode operation.
   *
   * @param node the node
   * @param object the object
   * @param propertyPrefix the property prefix
   * @param ignoreProperties the ignore properties
   */
  public static <T> void copyFromNode(
      Node node, T object, String propertyPrefix, Collection<String> ignoreProperties) {
    copyFromNode(node, object, new DefaultTranslator(propertyPrefix, ignoreProperties, null));
  }

  /**
   * copyFromNode operation.
   *
   * @param node the node
   * @param object the object
   * @param propertyPrefix the property prefix
   * @param ignoreProperties the ignore properties
   * @param nameMap the name map
   */
  public static <T> void copyFromNode(
      Node node,
      T object,
      String propertyPrefix,
      Collection<String> ignoreProperties,
      Map<String, String> nameMap) {
    copyFromNode(node, object, new DefaultTranslator(propertyPrefix, ignoreProperties, nameMap));
  }

  /**
   * NodeUtilsException class.
   */
  public static class NodeUtilsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new NodeUtilsException.
     *
     * @param message the message
     */
    public NodeUtilsException(String message) {
      super(message);
    }

    /**
     * Creates a new NodeUtilsException.
     *
     * @param message the message
     * @param cause the cause
     */
    public NodeUtilsException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Creates a new NodeUtilsException.
     *
     * @param cause the cause
     */
    public NodeUtilsException(Throwable cause) {
      super(cause);
    }
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(NodeUtils.class);
}
