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

import com.percussion.cms.IPSConstants;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSConnectionInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import java.util.Optional;

/**
 * An adapter bean that allows wiring a new session factory using a Rhythmyx
 * datasource configuration with modern Java 11 enhancements.
 *
 * <p>Rhythmyx provides a code level API, but for those users who wish to use
 * Spring wiring rather than creating their own session factory in code, this
 * adapter makes it easy with Optional-based safe access and enhanced validation.</p>
 *
 * <p>If the <code>dataSourceName</code> property is not specified, or is
 * <code>empty</code> then the Rhythmyx repository datasource is used.</p>
 *
 * <p>To use this class, wire a new sessionFactory (with a new, unique name) and
 * wire in any Hibernate mapping files that you need. You will then wire this
 * bean into the sessionFactory property of your application beans.</p>
 *
 * <p>Note that the wiring must use the <code>mappingResources</code> attribute
 * and cannot contain any wild card characters, as the JBoss classloaders cannot
 * load resources with wild card names from jar files.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * <bean id="mySessionFactory"
 *    class="com.percussion.services.datasource.PSDatasourceSessionFactoryBean"
 *    singleton="true">
 *    <property name="datasourceName" value="myDatasource"/>
 *    <property name="mappingResources">
 *       <list>
 *          <value>/com/percussion/pso/some/class/name.hbm.xml</value>
 *       </list>
 *    </property>
 * </bean>
 * }</pre>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSDatasourceSessionFactoryBean extends LocalSessionFactoryBean {

   /**
    * Logger for this class using modern logging patterns.
    */
   private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);

   /**
    * Datasource name for connections, may be <code>null</code> or empty if the 
    * repository database is to be used.
    */
   private String dataSourceName;

   /**
    * Gets the name of the datasource used for connections with Optional wrapper.
    *
    * @return Optional containing the datasource name, or empty if using Rhythmyx repository
    */
   public Optional<String> getDataSourceNameSafely() {
      return StringUtils.isBlank(dataSourceName) ?
         Optional.empty() : Optional.of(dataSourceName);
   }

   /**
    * Gets the name of the datasource used for connections.
    * 
    * @return The datasource name, may be <code>null</code> or empty to indicate
    * the Rhythmyx repository.
    * @deprecated Use {@link #getDataSourceNameSafely()} for null-safe access
    */
   @Deprecated
   public String getDataSourceName() {
      return dataSourceName;
   }

   /**
    * Sets the name of the datasource to use for connections with enhanced validation.
    *
    * @param name The name, may be <code>null</code> or empty to
    * use the Rhythmyx repository.
    */
   public void setDataSourceName(String name) {
      this.dataSourceName = StringUtils.trimToNull(name);

      if (log.isDebugEnabled()) {
         if (StringUtils.isBlank(name)) {
            log.debug("Using Rhythmyx repository datasource (default)");
         } else {
            log.debug("Using configured datasource: {}", name);
         }
      }
   }

   /**
    * Replaces the hibernate properties using those specified by the datasource 
    * name returned from {@link #getDataSourceName()} with enhanced error handling.
    *
    * <p>This method calls {@link PSConnectionHelper#getDbConnection(IPSConnectionInfo)}
    * to obtain the properties and provides comprehensive error handling with detailed logging.</p>
    *
    * @throws HibernateException if properties cannot be set or connection fails
    */
   @Override
   public void afterPropertiesSet() throws HibernateException {
      log.debug("Setting hibernate properties for datasource: {}",
         getDataSourceNameSafely().orElse("repository"));

      try {
         var connectionInfo = new PSConnectionInfo(dataSourceName);
         var hibernateProperties = PSConnectionHelper.getHibernateProperties(connectionInfo);

         setHibernateProperties(hibernateProperties);

         log.info("Successfully configured hibernate properties for datasource: {}",
            getDataSourceNameSafely().orElse("repository"));

         super.afterPropertiesSet();

      } catch (Exception e) {
         var errorMsg = String.format("Failed to configure connection properties for datasource: %s",
            getDataSourceNameSafely().orElse("repository"));
         log.error(errorMsg, e);
         throw new HibernateException(errorMsg, e);
      }
   }

   @Override
   public String toString() {
      return String.format("PSDatasourceSessionFactoryBean{dataSourceName='%s'}",
         getDataSourceNameSafely().orElse("repository"));
   }
}
