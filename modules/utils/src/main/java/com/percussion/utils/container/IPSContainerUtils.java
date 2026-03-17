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

/** */
package com.percussion.utils.container;

import com.percussion.utils.container.config.ContainerConfig;
import com.percussion.utils.jdbc.IPSDatasourceResolver;
import java.util.List;

/**
 * Provides an interface contract for Application Container utilities that should be provided for
 * each container.
 *
 * @author natechadwick
 */
public interface IPSContainerUtils extends ContainerConfig {

  /**
   * Gets the connector information for this container.
   *
   * @return the connector info, never <code>null</code>.
   */
  PSAbstractConnectors getConnectorInfo();

  /**
   * Load all JNDI datasources from the configured file. Any settings configured that are not
   * supported by the PSJndiDatasource class are loaded and preserved when the datasource is saved
   * (see setDatasources).
   *
   * @return A list of datasources, never <code>null</code>, may be empty.
   */
  List<IPSJndiDatasource> getDatasources();

  /**
   * Saves the supplied JNDI datasource configurations to the configured files, replacing any
   * existing configurations. Any settings that were configured but not supported by the
   * PSJndiDatasource class are saved intact. Note that PSJndiDatasource.setSecurityDomain(String)
   * will be called on all supplied datasources.
   *
   * @param datasources The list of datasources to save, may not be <code>null</code>, may be empty.
   */
  void setDatasources(List<IPSJndiDatasource> datasources);

  /**
   * Gets the datasource resolver for this container.
   *
   * @return the datasource resolver, may be <code>null</code>.
   */
  IPSDatasourceResolver getDatasourceResolver();

  /**
   * Sets the datasource resolver for this container.
   *
   * @param resolver the resolver to use, may be <code>null</code>.
   */
  void setDatasourceResolver(IPSDatasourceResolver resolver);

  /**
   * Checks if this container is enabled.
   *
   * @return <code>true</code> if enabled, <code>false</code> otherwise.
   */
  boolean isEnabled();

  /**
   * Sets whether this container is enabled.
   *
   * @param enabled <code>true</code> to enable, <code>false</code> to disable.
   */
  void setEnabled(boolean enabled);

  /**
   * Checks if the configuration has been loaded.
   *
   * @return <code>true</code> if loaded, <code>false</code> otherwise.
   */
  boolean isLoaded();

  /**
   * Sets whether the configuration has been loaded.
   *
   * @param loaded <code>true</code> if loaded, <code>false</code> otherwise.
   */
  void setLoaded(boolean loaded);
}
