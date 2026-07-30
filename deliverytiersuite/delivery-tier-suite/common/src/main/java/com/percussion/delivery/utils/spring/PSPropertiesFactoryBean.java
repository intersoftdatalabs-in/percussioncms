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
package com.percussion.delivery.utils.spring;

import com.percussion.delivery.utils.security.PSSecureProperty;
import com.percussion.security.error.PSExceptionUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;

/**
 * Extended the Spring PropertiesFactoryBean to add the ability to automatically encrypt properties
 * that should be password protected. All location files will be modified if they contain a field
 * specified to be encrypted in the "securedProperties" property and if the "autoSecure" property is
 * set to <code>true</code>, the default is <code>false</code>.
 *
 * <pre>
 *
 * Example Bean file declaration:
 *
 * &lt;bean id="propertyPlaceholderProps" class="com.percussion.delivery.utils.spring.PSPropertiesFactoryBean">
 *       &lt;property name="ignoreResourceNotFound" value="false" />
 *       &lt;property name="autoSecure" value="true"/>
 *       &lt;property name="securedProperties">
 *          &lt;list>
 *             &lt;value>password&lt;/value>
 *          &lt;/list>
 *       &lt;/property>
 *       &lt;property name="locations">
 *               &lt;!-- One or more locations of the property files. Properties with the
 *                    same name override based on the order the file appears in the list
 *                    last one defined wins
 *                -->
 *               &lt;list>
 *                  &lt;value>classpath:/WEB-INF/cacheManage.properties&lt;/value>
 *               &lt;/list>
 *       &lt;/property>
 *       &lt;!-- Local properties to default to if no file exists or the properties do not exist in the file -->
 *       &lt;property name="properties">
 *          &lt;props>
 *             &lt;prop key="cacheManagerHost">https://localhost:8843&lt;/prop>
 *             &lt;prop key="username">ps_manager&lt;/prop>
 *             &lt;prop key="password">abc123&lt;/prop>
 *             &lt;prop key="cacheRegion">&lt;/prop>
 *             &lt;prop key="interRequestWait">5&lt;/prop>
 *             &lt;prop key="maxWait">360&lt;/prop>
 *
 *             &lt;prop key="encryption.type">Default;/prop>
 *          &lt;/props>
 *       &lt;/property>
 *
 * &lt;/bean>
 * </pre>
 *
 * @author erikserating
 */
public class PSPropertiesFactoryBean extends PropertiesFactoryBean {

  /** Default constructor. */
  public PSPropertiesFactoryBean() {}

  private static final Logger log = LogManager.getLogger(PSPropertiesFactoryBean.class);

  /**
   * All resources that have been assigned to this factory bean via {@link #setLocation} or {@link
   * #setLocations}.
   */
  private final List<Resource> resList = new ArrayList<>();

  /** Names of properties whose values should be encrypted when {@link #autoSecure} is enabled. */
  private String[] securedProperties;

  /**
   * When <code>true</code>, the factory bean will encrypt the configured {@link #securedProperties}
   * on load.
   */
  private boolean autoSecure;

  /** Decryption key used when securing properties. */
  private String key;

  /* (non-Javadoc)
   * @see org.springframework.core.io.support.PropertiesLoaderSupport#setLocation(org.springframework.core.io.Resource)
   */
  @Override
  public void setLocation(Resource location) {
    if (location != null) resList.add(location);
    super.setLocation(location);
  }

  /* (non-Javadoc)
   * @see org.springframework.core.io.support.PropertiesLoaderSupport#setLocations(org.springframework.core.io.Resource[])
   */
  @Override
  public void setLocations(Resource... locations) {
    if (locations != null) {
      for (Resource r : locations) resList.add(r);
    }
    super.setLocations(locations);
  }

  /* (non-Javadoc)
   * @see org.springframework.core.io.support.PropertiesLoaderSupport#loadProperties(java.util.Properties)
   */
  @Override
  protected void loadProperties(Properties props) throws IOException {
    super.loadProperties(props);
    if (autoSecure && securedProperties != null && securedProperties.length > 0) {
      String encryptionType =
          props.getProperty("encryption.type") == null
              ? "ENC"
              : props.getProperty("encryption.type");
      encryptProps(encryptionType);
    }
  }

  /**
   * Returns the configured property names that should be encrypted when auto-secure is enabled.
   *
   * @return the secured property names, may be <code>null</code>.
   */
  public String[] getSecuredProperties() {
    return securedProperties;
  }

  /**
   * Sets the configured property names that should be encrypted when auto-secure is enabled.
   *
   * @param securedProperties the property names to secure.
   */
  public void setSecuredProperties(String[] securedProperties) {
    this.securedProperties = securedProperties;
  }

  /**
   * Returns whether auto-secure is enabled.
   *
   * @return <code>true</code> when auto-secure will encrypt the configured properties on load.
   */
  public boolean isAutoSecure() {
    return autoSecure;
  }

  /**
   * Configures whether the factory bean should automatically encrypt the configured {@link
   * #setSecuredProperties secured properties} on load.
   *
   * @param autoSecure <code>true</code> to enable auto-secure, <code>false</code> to disable.
   */
  public void setAutoSecure(boolean autoSecure) {
    this.autoSecure = autoSecure;
  }

  /**
   * Returns the decryption key used when securing properties.
   *
   * @return the configured key, may be <code>null</code> to use the default key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the decryption key used when securing properties.
   *
   * @param key the key to use, may be <code>null</code> to use the default key.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Loops through all resources and secures the properties by encryption.
   *
   * @param encryptionType Type of encryption
   */
  private void encryptProps(String encryptionType) {
    Collection<String> names = Arrays.asList(securedProperties);

    for (Resource r : resList) {
      if (r.exists()) {
        try {
          PSSecureProperty.secureProperties(r.getFile(), names, key, encryptionType);
        } catch (IOException e) {
          log.error(PSExceptionUtils.getMessageForLog(e));
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
      }
    }
  }
}
