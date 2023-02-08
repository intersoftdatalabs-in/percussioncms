/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Object representation of the hibernate dialect map configuration.  This 
 * specifies a set of mappings between JDBC driver names and their corresponding
 * default Hibernate dialect class name.  
 */
public class PSHibernateDialectConfig implements IPSBeanConfig, IPSHibernateDialectConfig
{
   /**
    * Set the dialect class for a driver.  If dialect is already mapped to
    * the supplied driver, it is replaced by the new dialect value.
    * 
    * @param driverName The name of the JDBC driver, may not be 
    * <code>null</code> or empty.
    * @param dialectClassName The dialect class name, may not be 
    * <code>null</code> or empty.
    */
   public void setDialect(String driverName, String dialectClassName)
   {
      if (StringUtils.isBlank(driverName))
         throw new IllegalArgumentException(
            "driverName may not be null or empty");
      
      if (StringUtils.isBlank(dialectClassName))
         throw new IllegalArgumentException(
            "dialectClassName may not be null or empty");
      
      m_sqlDialects.put(driverName, dialectClassName);
   }

   /**
    * Set the dialect classes for multiple drivers.  All current mappings are 
    * cleared and replaced with the supplied dialects.
    * 
    * @param dialects Map of dialects where key is the driver name, and value
    * is the dialect class name, may not be <code>null</code>, and keys and 
    * values may not be <code>null</code> or empty.  See 
    * {@link #setDialect(String, String)} for more info.
    */
   public void setDialects(Map<String, String> dialects)
   {
      if (dialects == null)
         throw new IllegalArgumentException("dialects may not be null");
      
      m_sqlDialects.clear();
      for (Map.Entry<String, String> entry : dialects.entrySet())
      {
         setDialect(entry.getKey(), entry.getValue());
      }
   }
   
   /**
    * Get a copy of the internal dialect map.  See {@link #setDialects(Map)}.
    * 
    * @return The map, never <code>null</code>.
    */
   public Map<String, String> getDialects()
   {
      return new HashMap<>(m_sqlDialects);
   }

   /**
    * Get the dialect class name mapped to the supplied JDBC driver name.
    * 
    * @param driverName The name of the JDBC driver, may not be 
    * <code>null</code> or empty.
    * 
    * @return The dialect, or <code>null</code> if no mapping is found.
    */
   public String getDialectClassName(String driverName)
   {
      return m_sqlDialects.get(driverName);
   }

   // see IPSBeanConfig 
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");
      
      Element root = PSSpringBeanUtils.createBeanRootElement(
         this, doc);
      
      PSSpringBeanUtils.addBeanProperty(root, DIALECTS_PROP_NAME, 
         m_sqlDialects);
      
      return root;
   }

   // see IPSBeanConfig 
   public void fromXml(Element source) throws PSInvalidXmlException
   {
      if (source == null)
         throw new IllegalArgumentException("source may not be null");

      PSSpringBeanUtils.validateBeanRootElement(getBeanName(), getClassName(), 
         source);
      
      Element propEl = PSSpringBeanUtils.getNextPropertyElement(source, null, 
         DIALECTS_PROP_NAME);
      m_sqlDialects = PSSpringBeanUtils.getBeanPropertyValueMap(propEl);
   }

   // see IPSBeanConfig
   public String getBeanName()
   {
      return "sys_hibernateDialects";
   }

   // see IPSBeanConfig
   public String getClassName()
   {
      return getClass().getName();
   }
   
   /**
    * Map of jdbc driver name to hibernate sql dialect, never <code>null</code>,
    * may be empty. Modified by calls to {@link #setDialects(Map)}.
    */
   private Map<String, String> m_sqlDialects = new HashMap<>();
   
   /** 
    * Dialect property name
    */
   private static final String DIALECTS_PROP_NAME = "dialects";
}
