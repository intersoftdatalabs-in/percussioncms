/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.pubserver.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;
import java.util.Set;

/**
 * Database-name-only update for a MySQL or MSSQL delivery server on the existing publish-server
 * PUT.
 *
 * <p>A body that carries only {@code database} must not clear host, port, userid, driver, name,
 * default flag, password, Oracle SID, or MSSQL owner. Blank and overlong names do not write.
 * Oracle, FTP, and Local drivers are rejected before properties change. Passwords are not read or
 * included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliveryDatabaseNameUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_DATABASE_NAME_LENGTH = 255;

  public static final String DATABASE_NAME_REQUIRED = "Database name is required.";

  public static final String DATABASE_NAME_TOO_LONG =
      "Database name must be 255 characters or fewer.";

  public static final String DATABASE_NAME_DRIVER =
      "Database name can be set only on a MySQL or MSSQL delivery server.";

  private static final Set<String> MYSQL_MSSQL_DRIVERS = Set.of("MYSQL", "MYSQL8", "MSSQL");

  private PSDatabaseDeliveryDatabaseNameUpdate() {}

  /**
   * True when the PUT body is only the database name property. A named server, a type, a
   * description, a default flag, or any other property (including Oracle {@code sid}) stays on the
   * full or field-only update path.
   */
  public static boolean isDatabaseNameOnly(PSPublishServerInfo info) {
    if (info == null) {
      return false;
    }
    if (isNotBlank(info.getServerName())
        || isNotBlank(info.getType())
        || isNotBlank(info.getServerType())) {
      return false;
    }
    if (info.getDescription() != null || info.getIsDefault() != null) {
      return false;
    }
    List<PSPublishServerProperty> properties = info.getProperties();
    if (properties == null || properties.size() != 1) {
      return false;
    }
    PSPublishServerProperty only = properties.get(0);
    return only != null
        && IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  public static boolean isMysqlOrMssqlDriver(String driver) {
    return driver != null && MYSQL_MSSQL_DRIVERS.contains(driver.trim().toUpperCase());
  }

  /**
   * Replace {@code database} on a MySQL or MSSQL server. Does not add or clear any other property.
   * Throws before writing when the driver is not MYSQL/MYSQL8/MSSQL or the name is blank or longer
   * than {@link #MAX_DATABASE_NAME_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !isMysqlOrMssqlDriver(driver)) {
      reject(DATABASE_NAME_DRIVER);
      return;
    }
    String raw =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY);
    String databaseName = raw == null ? "" : raw.trim();
    if (databaseName.isEmpty()) {
      reject(DATABASE_NAME_REQUIRED);
      return;
    }
    if (databaseName.length() > MAX_DATABASE_NAME_LENGTH) {
      reject(DATABASE_NAME_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, databaseName);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateDatabaseName")
        .reject(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
