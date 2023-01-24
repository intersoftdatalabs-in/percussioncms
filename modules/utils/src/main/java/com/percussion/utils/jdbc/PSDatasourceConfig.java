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
package com.percussion.utils.jdbc;

import com.percussion.utils.spring.IPSBeanConfig;
import com.percussion.utils.spring.PSSpringBeanUtils;
import com.percussion.utils.xml.PSInvalidXmlException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Objects;

/** 
 * Class to describe the configuration used to obtain and use a database
 * connection.
 */
public class PSDatasourceConfig implements IPSDatasourceConfig, IPSBeanConfig, 
   Cloneable
{
   /**
    * Empty ctor only for use by Spring framework.  If used, an invalid object
    * may result if all required members are not subsequently set on this object
    * before its first use.
    */
   public PSDatasourceConfig()
   {
   
   }
   
   /**
    * Construct this object from its data members.
    * 
    * @param name The name used to identify this configuration, may not be
    * <code>null</code> or empty.
    * @param dsName The name of the JNDI datasource this configuration 
    * references, may not be <code>null</code> or empty.
    * @param origin The origin or schema name to use when qualifying statements
    * against this datasource, may be <code>null</code> or empty.
    * @param database The database name to use when qualifying statements
    * against this datasource, may be <code>null</code> or empty.
    */
   public PSDatasourceConfig(String name, String dsName, String origin, 
      String database)
   {
      if (StringUtils.isEmpty(name))
         throw new IllegalArgumentException("name may not be null or empty");
      
      if (StringUtils.isEmpty(dsName))
         throw new IllegalArgumentException("dsName may not be null or empty");
      
      m_name = name;
      m_dsName = dsName;
      setOrigin(origin);
      setDatabase(database);
   }

   /**
    * Construct this object from its XML representation.  See 
    * {@link IPSBeanConfig#toXml(Document)} for details for the expected DTD.
    * 
    * @param sourceNode The source element, may not be <code>null</code>.
    * 
    * @throws PSInvalidXmlException if the supplied element does not 
    * conform to the expected DTD.
    */
   public PSDatasourceConfig(Element sourceNode) 
      throws PSInvalidXmlException
   {
      if (sourceNode == null)
         throw new IllegalArgumentException("sourceNode may not be null");
      
      fromXml(sourceNode);
   }
   
   /**
    * Construct this object as a shallow copy of another config.  Use 
    * {@link #clone()} for a deep copy.
    * 
    * @param config The config, may not be <code>null</code>.
    */
   public PSDatasourceConfig(IPSDatasourceConfig config)
   {
      if (config == null)
         throw new IllegalArgumentException("config may not be null");
      
      copyFrom(config);
   }
   
   // see IPSDatasourceConfig
   public String getDataSource()
   {
      return m_dsName;
   }
   
   // see IPSDatasourceConfig
   public String getOrigin()
   {
      return m_origin;
   }

   // see IPSDatasourceConfig
   public String getDatabase()
   {
      return m_database;
   }
   
   // see IPSDatasourceConfig
   public String getName()
   {
      return m_name;
   }
   
   
   /**
    * Modify the name of this config.
    * 
    * @param name The name, may not be <code>null</code> or empty.
    */
   public void setName(String name)
   {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name may not be null or empty");
      
      m_name = name;
   }
   
   /**
    * Modify the name of the JNDI datasource this configuration references.
    * 
    * @param dataSource The datasource name, may not be <code>null</code> or 
    * empty.
    */
   public void setDataSource(String dataSource)
   {
      if (StringUtils.isBlank(dataSource))
         throw new IllegalArgumentException(
            "dataSource may not be null or empty");
      
      m_dsName = dataSource;
   }
   
   /**
    * Modify the origin or schema of this configuration.
    * 
    * @param origin The origin or schema name, may be <code>null</code> or 
    * empty.
    */
   public void setOrigin(String origin)
   {
      m_origin = origin == null ? "" : origin.trim();
   }

   /**
    * Modify the database of this configuration.
    * 
    * @param database The database name, may be <code>null</code> or 
    * empty.
    */
   public void setDatabase(String database)
   {
      m_database = database == null ? "" : database.trim();
   }
   
   /**
    * Copy a datasource configuration to this object. A shallow copy is made of 
    * all mutable members - use {@link #clone()} for a deep copy
    *  
    * @param config Config to copy, must never be <code>null</code>
    */
   public void copyFrom(IPSDatasourceConfig config)
   {
      if (config == null)
      {
         throw new IllegalArgumentException("config must never be null");
      }

      m_name = config.getName();
      m_dsName = config.getDataSource();
      m_origin = config.getOrigin();
      m_database = config.getDatabase();
   }

   
   // see IPSBeanConfig interface
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");
      
      Element root = PSSpringBeanUtils.createBeanRootElement(
         this, doc);
      
      PSSpringBeanUtils.addBeanProperty(root, NAME_PROP, m_name);
      PSSpringBeanUtils.addBeanProperty(root, DS_NAME_PROP, m_dsName);
      PSSpringBeanUtils.addBeanProperty(root, DB_PROP, m_database);
      PSSpringBeanUtils.addBeanProperty(root, ORIGIN_PROP, m_origin);
      
      return root;
   }

   // see IPSBeanConfig interface
   public void fromXml(Element source) throws PSInvalidXmlException
   {
      if (source == null)
         throw new IllegalArgumentException("source may not be null");

      // need to get name first
      Element propEl = PSSpringBeanUtils.getNextPropertyElement(source, null, 
         NAME_PROP);
      m_name = PSSpringBeanUtils.getBeanPropertyValue(propEl, true);
      
      PSSpringBeanUtils.validateBeanRootElement(m_name, getClassName(), source);
      
      propEl = PSSpringBeanUtils.getNextPropertyElement(source, propEl, 
         DS_NAME_PROP);
      m_dsName = PSSpringBeanUtils.getBeanPropertyValue(propEl, true);
      
      propEl = PSSpringBeanUtils.getNextPropertyElement(source, propEl, 
         DB_PROP);
      m_database = PSSpringBeanUtils.getBeanPropertyValue(propEl, false);
      
      propEl = PSSpringBeanUtils.getNextPropertyElement(source, propEl, 
         ORIGIN_PROP);
      m_origin = PSSpringBeanUtils.getBeanPropertyValue(propEl, false);
   }
   

   // see IPSBeanConfig interface
   public String getBeanName()
   {
      return m_name;
   }   

   // see IPSBeanConfig interface
   public String getClassName()
   {
      return getClass().getName();
   }
   
   //see base class
   @Override
   public Object clone() throws CloneNotSupportedException
   {
      return super.clone();
   }


   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSDatasourceConfig)) return false;
      PSDatasourceConfig that = (PSDatasourceConfig) o;
      return Objects.equals(m_name, that.m_name) && Objects.equals(m_dsName, that.m_dsName) && Objects.equals(m_origin, that.m_origin) && Objects.equals(m_database, that.m_database);
   }

   @Override
   public int hashCode() {
      return Objects.hash(m_name, m_dsName, m_origin, m_database);
   }

   /**
    * The name supplied during construction, never <code>null</code> or empty.
    */
   private String m_name;
   
   /**
    * The datasource name supplied during construction, never <code>null</code> 
    * or empty.
    */
   private String m_dsName;
   
   /**
    * The origin supplied during construction, never <code>null</code>, may be 
    * empty.
    */
   private String m_origin;

   /**
    * The database supplied during construction, never <code>null</code>, may be 
    * empty.
    */
   private String m_database;
   
   /**
    * Version required by serializable
    */
   private static final long serialVersionUID = 1L;
   
   // private xml prop names
   private static final String NAME_PROP = "name";
   private static final String DS_NAME_PROP = "dataSource";
   private static final String DB_PROP = "database";
   private static final String ORIGIN_PROP = "origin";

    @Override
    public String toString() {
        return "PSDatasourceConfig{" +
                "m_name='" + m_name + '\'' +
                ", m_dsName='" + m_dsName + '\'' +
                ", m_origin='" + m_origin + '\'' +
                ", m_database='" + m_database + '\'' +
                '}';
    }
}
