/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.system.utils;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;

/**
 * Overrides {@link BasicDataSourceFactory#createDataSource(Properties)} in order to provide
 * datasources whose connections are established using drivers loaded from external files.
 */
public final class PSDataSourceFactory extends BasicDataSourceFactory {
  /** Property constants */
  public static final String URL_PROP_NAME = "url";

  public static final String DB_PROP_NAME = "database";
  public static final String USER_PROP_NAME = "username";
  public static final String PWD_PROP_NAME = "password";
  public static final String DRIVER_CLASS_PROP_NAME = "driverClassName";
  public static final String DRIVER_LOC_PROP_NAME = "driverLocation";

  // Private constructor to prevent instantiation
  private PSDataSourceFactory() {
    super();
  }

  /**
   * See {@link BasicDataSourceFactory#createDataSource(Properties)}.
   *
   * @param properties the database connection properties used to configure the datasource. Must
   *     include the following: url, database, username, password, driverLocation. Never <code>null
   *     </code>.
   * @return a new {@link PSDataSource} object.
   * @throws Exception if an error occurs.
   */
  public static DataSource createDataSource(Properties properties) throws Exception {
    Objects.requireNonNull(properties, "properties cannot be null");

    var url = getRequiredProperty(properties, URL_PROP_NAME);
    var username = getRequiredProperty(properties, USER_PROP_NAME);
    var password = getRequiredProperty(properties, PWD_PROP_NAME);
    var driverClass = getRequiredProperty(properties, DRIVER_CLASS_PROP_NAME);
    var driverLocation = getRequiredProperty(properties, DRIVER_LOC_PROP_NAME);

    var driverFromUrl = PSJdbcUtils.getDriverFromUrl(url);

    if (PSJdbcUtils.isExternalDriver(driverFromUrl)) {
      return new PSDataSource(
          url,
          properties.getProperty(DB_PROP_NAME),
          username,
          password,
          driverClass,
          driverLocation);
    } else {
      return BasicDataSourceFactory.createDataSource(properties);
    }
  }

  /**
   * Helper method to get a required property from the properties object.
   *
   * @param properties the properties object
   * @param propertyName the name of the required property
   * @return the property value
   * @throws IllegalArgumentException if the property is missing or empty
   */
  private static String getRequiredProperty(Properties properties, String propertyName) {
    return Optional.ofNullable(properties.getProperty(propertyName))
        .filter(value -> !value.trim().isEmpty())
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "properties must contain non-empty value for: " + propertyName));
  }
}
