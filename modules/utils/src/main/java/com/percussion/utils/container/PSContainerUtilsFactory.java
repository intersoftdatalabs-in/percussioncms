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

package com.percussion.utils.container;

import com.percussion.utils.container.adapters.JBossConnectorConfigurationAdapter;
import com.percussion.utils.container.adapters.JBossDatasourceConfigurationAdapter;
import com.percussion.utils.container.adapters.JettyDatasourceConfigurationAdapter;
import com.percussion.utils.container.adapters.JettyInstallationPropertiesConfigurationAdapter;
import com.percussion.utils.container.config.model.impl.BaseContainerUtils;
import com.percussion.utils.io.PathUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory for container configuration contexts. All factory methods are static; instances expose
 * per-path save/load convenience operations.
 */
public class PSContainerUtilsFactory {

  /** Creates a factory. All factory methods are static. */
  public PSContainerUtilsFactory() {}

  private static final ConcurrentHashMap<String, DefaultConfigurationContextImpl> factoryInstances =
      new ConcurrentHashMap<>();

  private static final List<IPSConfigurationAdapter<DefaultConfigurationContextImpl>> adapterList =
      new CopyOnWriteArrayList<>();

  /** Logger */
  private static final Logger ms_log = LogManager.getLogger(PSContainerUtilsFactory.class);

  static {
    adapterList.add(new JBossDatasourceConfigurationAdapter());
    adapterList.add(new JBossConnectorConfigurationAdapter());

    adapterList.add(new JettyDatasourceConfigurationAdapter());
    adapterList.add(new JettyInstallationPropertiesConfigurationAdapter());
    // adapterList.add(new LoggingContainerConfigurationAdapter());
  }

  /**
   * Returns the configuration context for the supplied path, creating it on first use.
   *
   * @param path the container root path, never <code>null</code>
   * @return the configuration context, never <code>null</code>
   */
  public static DefaultConfigurationContextImpl getConfigurationContextInstance(Path path) {
    DefaultConfigurationContextImpl value =
        factoryInstances.computeIfAbsent(
            path.normalize().toAbsolutePath().toString(),
            k -> addNew(k /* no legacy key required */));
    return value;
  }

  /**
   * Returns the configuration context for the default installation path.
   *
   * @return the configuration context, never <code>null</code>
   */
  public static DefaultConfigurationContextImpl getConfigurationContextInstance() {
    return getConfigurationContextInstance(PathUtils.getRxDir().toPath());
  }

  /**
   * Returns the container configuration for the default installation path.
   *
   * @return the container configuration, never <code>null</code>
   */
  public static BaseContainerUtils getInstance() {
    return getConfigurationContextInstance().getConfig();
  }

  /**
   * Returns the container configuration for the supplied root.
   *
   * @param root the container root path, never <code>null</code>
   * @return the container configuration, never <code>null</code>
   */
  public static BaseContainerUtils getInstance(Path root) {
    return getConfigurationContextInstance(root).getConfig();
  }

  /**
   * Saves the configuration context for the supplied path.
   *
   * @param path the container root path, never <code>null</code>
   */
  public void save(Path path) {
    getConfigurationContextInstance(path).save();
  }

  /**
   * Loads the configuration context for the supplied path.
   *
   * @param path the container root path, never <code>null</code>
   */
  public void load(Path path) {
    getConfigurationContextInstance(path).load();
  }

  /**
   * Saves the configuration context for the default installation path.
   */
  public void save() {
    getConfigurationContextInstance().save();
  }

  /**
   * Loads the configuration context for the default installation path.
   */
  public void load() {
    getConfigurationContextInstance().load();
  }

  private static DefaultConfigurationContextImpl addNew(String path) {
    ms_log.info("Creating new Configuration context for path " + path);
    // Use empty encryption key by default; adapters may override/configure as needed
    DefaultConfigurationContextImpl newContext =
        new DefaultConfigurationContextImpl(Paths.get(path), "", BaseContainerUtils::new);
    getAdapterList().forEach(newContext::addConfigurationAdapter);
    newContext.load();
    return newContext;
  }

  /**
   * Returns the registered configuration adapters.
   *
   * @return the adapter list, never <code>null</code>
   */
  public static List<IPSConfigurationAdapter<DefaultConfigurationContextImpl>> getAdapterList() {
    return adapterList;
  }

  /**
   * Replaces the registered configuration adapters.
   *
   * @param adapterList the adapters to register, never <code>null</code>
   */
  public static void setAdapterList(
      List<IPSConfigurationAdapter<DefaultConfigurationContextImpl>> adapterList) {
    PSContainerUtilsFactory.adapterList.clear();
    PSContainerUtilsFactory.adapterList.addAll(adapterList);
  }
}
