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

package com.ibm.cadf.cfg;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.util.Constants;
import java.io.IOException;
import java.util.Properties;

/**
 * Process-wide configuration holder for the CADF audit middleware. Loads the audit mapping
 * configuration file lazily on first construction and exposes typed accessors for individual keys.
 * Non-instantiable by callers; use {@link #getInstance()}.
 */
public class Config {
  /**
   * The mutable properties bundle, may be {@code null} until {@link #loadDefaultSettings()} runs.
   */
  private Properties properties;

  /** Process-wide singleton instance, initialized once at class load time. */
  private static Config config = new Config();

  private Config() {
    loadDefaultSettings();
  }

  /**
   * Returns the singleton {@link Config} instance.
   *
   * @return the shared configuration instance, never {@code null}.
   */
  public static Config getInstance() {
    return config;
  }

  /**
   * Merges the supplied properties into the existing configuration. When the bundle is {@code null}
   * or empty the call is a no-op.
   *
   * @param properties the properties to merge, may be {@code null}.
   */
  public void setProperties(Properties properties) {
    // Update the existing files
    if (properties != null && !properties.isEmpty()) {
      this.properties.putAll(properties);
    }
  }

  private void loadDefaultSettings() {
    String confFile = System.getProperty(Constants.API_AUDIT_MAP, Constants.API_AUDIT_MAP_CONF);
    try {
      this.properties = PropertyUtil.loadProperties(confFile);
    } catch (IOException e) {
      throw new CADFException(e);
    }
  }

  /**
   * Returns the configuration value associated with the given key.
   *
   * @param key the property key to look up, never {@code null}.
   * @return the corresponding value, or {@code null} when the properties bundle has not been loaded
   *     or the key is missing.
   */
  public String getProperty(String key) {
    if (properties == null) {
      return null;
    }
    return properties.getProperty(key);
  }

  /**
   * Sets a single property, creating the underlying {@link Properties} instance on demand.
   *
   * @param key the property key, never {@code null}.
   * @param value the value to associate with {@code key}.
   */
  public void registerProperty(String key, String value) {
    if (properties == null) {
      properties = new Properties();
    }
    properties.setProperty(key, value);
  }
}
