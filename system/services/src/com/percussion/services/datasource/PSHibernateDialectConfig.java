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

package com.percussion.services.datasource;

import com.percussion.utils.container.IPSHibernateDialectConfig;
import com.percussion.utils.spring.IPSBeanConfig;
import com.percussion.utils.spring.PSSpringBeanUtils;
import com.percussion.utils.xml.PSInvalidXmlException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Object representation of the hibernate dialect map configuration using modern Java 11 patterns.
 *
 * <p>This configuration specifies thread-safe mappings between JDBC driver names and their
 * corresponding default Hibernate dialect class names with enhanced validation and
 * Optional-based safe access.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSHibernateDialectConfig implements IPSBeanConfig, IPSHibernateDialectConfig {

   /**
    * Property name for dialects in XML configuration.
    */
   private static final String DIALECTS_PROP_NAME = "dialects";

   /**
    * Thread-safe map of JDBC driver name to hibernate SQL dialect.
    * Uses ConcurrentHashMap for better concurrent performance.
    */
   private final Map<String, String> sqlDialects = new ConcurrentHashMap<>();

   /**
    * Set the dialect class for a driver using modern validation patterns.
    * If dialect is already mapped to the supplied driver, it is replaced by the new dialect value.
    *
    * @param driverName The name of the JDBC driver, may not be
    * <code>null</code> or empty.
    * @param dialectClassName The dialect class name, may not be 
    * <code>null</code> or empty.
    * @throws IllegalArgumentException if any parameter is null or empty
    */
   public void setDialect(String driverName, String dialectClassName) {
      if (StringUtils.isBlank(driverName)) {
         throw new IllegalArgumentException("driverName may not be null or empty");
      }

      if (StringUtils.isBlank(dialectClassName)) {
         throw new IllegalArgumentException("dialectClassName may not be null or empty");
      }

      sqlDialects.put(driverName, dialectClassName);
   }

   /**
    * Set the dialect classes for multiple drivers using forEach for validation.
    * All current mappings are cleared and replaced with the supplied dialects.
    *
    * @param dialects Map of dialects where key is the driver name, and value
    * is the dialect class name, may not be <code>null</code>, and keys and 
    * values may not be <code>null</code> or empty.
    * @throws IllegalArgumentException if dialects is null or contains invalid entries
    */
   public void setDialects(Map<String, String> dialects) {
      Objects.requireNonNull(dialects, "dialects may not be null");

      // Validate all entries before clearing existing mappings
      dialects.entrySet().forEach(entry -> {
         if (StringUtils.isBlank(entry.getKey())) {
            throw new IllegalArgumentException("Driver name may not be null or empty");
         }
         if (StringUtils.isBlank(entry.getValue())) {
            throw new IllegalArgumentException("Dialect class name may not be null or empty");
         }
      });

      sqlDialects.clear();
      sqlDialects.putAll(dialects);
   }

   /**
    * Get a copy of the internal dialect map using modern collection patterns.
    *
    * @return The map, never <code>null</code>, immutable view for safety.
    */
   public Map<String, String> getDialects() {
      return Map.copyOf(sqlDialects);
   }

   /**
    * Get the dialect class name mapped to the supplied JDBC driver name with Optional wrapper.
    *
    * @param driverName The name of the JDBC driver, may not be
    * <code>null</code> or empty.
    *
    * @return Optional containing the dialect, or empty if no mapping is found.
    * @throws IllegalArgumentException if driverName is null or empty
    */
   public Optional<String> getDialectClassNameSafely(String driverName) {
      if (StringUtils.isBlank(driverName)) {
         throw new IllegalArgumentException("driverName may not be null or empty");
      }
      return Optional.ofNullable(sqlDialects.get(driverName));
   }

   /**
    * Get the dialect class name mapped to the supplied JDBC driver name.
    * 
    * @param driverName The name of the JDBC driver, may not be 
    * <code>null</code> or empty.
    * 
    * @return The dialect, or <code>null</code> if no mapping is found.
    */
   public String getDialectClassName(String driverName) {
      return getDialectClassNameSafely(driverName).orElse(null);
   }

   @Override
   public Element toXml(Document doc) {
      Objects.requireNonNull(doc, "doc may not be null");

      var root = PSSpringBeanUtils.createBeanRootElement(this, doc);
      PSSpringBeanUtils.addBeanProperty(root, DIALECTS_PROP_NAME, sqlDialects);

      return root;
   }

   @Override
   public void fromXml(Element source) throws PSInvalidXmlException {
      Objects.requireNonNull(source, "source may not be null");

      PSSpringBeanUtils.validateBeanRootElement(getBeanName(), getClassName(), source);

      var propEl = PSSpringBeanUtils.getNextPropertyElement(source, null, DIALECTS_PROP_NAME);
      var dialectsFromXml = PSSpringBeanUtils.getBeanPropertyValueMap(propEl);

      // Use setDialects for validation
      setDialects(dialectsFromXml);
   }

   @Override
   public String getBeanName() {
      return "sys_hibernateDialects";
   }

   @Override
   public String getClassName() {
      return getClass().getName();
   }

   @Override
   public String toString() {
      return String.format("PSHibernateDialectConfig{dialectCount=%d, dialects=%s}",
         sqlDialects.size(), sqlDialects);
   }
}
