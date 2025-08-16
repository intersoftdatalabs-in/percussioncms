// REFACTORED: CP-JAVA11
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
package com.percussion.rx.config;

import com.percussion.rx.config.data.PSConfigStatus;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Configuration service for applying, uninstalling, and validating configurations. Applies
 * configuration only when all required configuration files exist for the given config name.
 *
 * @author bjoginipally
 */
public interface IPSConfigService {

  /**
   * Applies the supplied configurations.
   *
   * @param configNames array of configuration names, must not be {@code null}.
   * @param deltasOnly if {@code true}, apply only local config changes; otherwise, apply the
   *     complete local config.
   * @return list of {@link PSPair} (config name and exception if occurs, or {@code null}), never
   *     {@code null}, may be empty.
   */
  List<PSPair<String, Exception>> applyConfiguration(String[] configNames, boolean deltasOnly);

  /**
   * Unregisters the local configuration file and deletes all configuration files for the specified
   * configuration.
   *
   * @param configName the configuration name, must not be {@code null} or empty.
   * @return map of undeleted files and exception, never {@code null}, may be empty.
   */
  Map<File, Exception> uninstallConfiguration(String configName);

  /**
   * De-applies previously applied configuration with the specified name.
   *
   * @param cfgName the name of the configuration, must not be {@code null} or empty.
   */
  void deApplyConfiguration(String cfgName);

  /**
   * Validates the supplied configuration against other successfully applied configurations.
   *
   * @param configName the name of the configuration, must not be blank.
   * @return list of configuration validation errors, never {@code null}, may be empty.
   */
  List<PSConfigValidation> validateConfiguartion(String configName);

  /**
   * Returns the name of the configuration for the given file, or {@code null} if not a
   * configuration file.
   *
   * @param configFile configuration file, must not be {@code null}.
   * @return name of the configuration or {@code null}.
   */
  String getConfigName(File configFile);

  /**
   * Returns the config registration manager associated with the config service.
   *
   * @return config registration manager, may be {@code null}.
   */
  IPSConfigRegistrationMgr getConfigRegistrationMgr();

  /**
   * Gets the configuration file from the specified type and package name.
   *
   * @param type the configuration type from {@link ConfigTypes}.
   * @param packageName the package name, cannot be {@code null} or empty.
   * @return the configuration file, never {@code null}.
   */
  File getConfigFile(ConfigTypes type, String packageName);

  /**
   * Returns the configuration status for a given configuration name, ordered by latest first.
   *
   * @param configName name of the configuration, if {@code null} returns all status entries.
   *     SQL-like wildcards (%) may be used.
   * @return list of configuration status objects, may be empty but never {@code null}.
   */
  List<PSConfigStatus> getConfigStatus(String configName);

  /**
   * Adds the supplied config change listener to the set of listeners.
   *
   * @param listener the configuration change listener, cannot be {@code null}.
   */
  void addConfigChangeListener(IPSConfigChangeListener listener);

  /**
   * Initializes the visibility repository for the given package.
   *
   * @param pkgName the name of the package, never {@code null} or empty.
   */
  void initVisibility(String pkgName);

  /**
   * Loads the Community Visibility for the specified package.
   *
   * @param pkgName the name of the package, never {@code null} or empty.
   * @return a collection of community names, may be empty, never {@code null}.
   */
  Collection<String> loadCommunityVisibility(String pkgName);

  /**
   * Saves the given Community Visibility for the specified package.
   *
   * @param communities new Community Visibility, never {@code null}, but may be empty.
   * @param pkgName the name of the package, never {@code null} or empty.
   * @param isReplace if {@code true}, replaces the communities of the package; otherwise, merges
   *     with existing.
   */
  void saveCommunityVisibility(Collection<String> communities, String pkgName, boolean isReplace);

  /** Enumeration of all configuration file types. */
  enum ConfigTypes {
    /** Local configuration file type. */
    LOCAL_CONFIG,
    /** Default configuration file type. */
    DEFAULT_CONFIG,
    /** Configuration definition file type. */
    CONFIG_DEF,
    /** Visibility configuration file type. */
    VISIBILITY
  }
}
