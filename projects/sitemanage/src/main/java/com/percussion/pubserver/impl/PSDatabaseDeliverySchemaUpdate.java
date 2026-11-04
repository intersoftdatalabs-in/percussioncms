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

/**
 * Schema-only update for an Oracle database delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code schema} must not clear host, port, userid, driver, name,
 * default flag, password, SID, or MySQL/MSSQL database. Blank and overlong schemas do not write.
 * MYSQL, MSSQL, FTP, and Local drivers are rejected before properties change. Passwords are not
 * read or included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliverySchemaUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_ORACLE_SCHEMA_LENGTH = 255;

  public static final String ORACLE_SCHEMA_REQUIRED = "Oracle schema is required.";

  public static final String ORACLE_SCHEMA_TOO_LONG =
      "Oracle schema must be 255 characters or fewer.";

  public static final String ORACLE_SCHEMA_DRIVER =
      "Oracle schema can be set only on an Oracle delivery server.";

  private PSDatabaseDeliverySchemaUpdate() {}

  /**
   * True when the PUT body is only the Oracle {@code schema} property. A named server, a type, a
   * description, a default flag, or any other property (including {@code sid} and {@code database})
   * stays on the full or field-only update path.
   */
  public static boolean isSchemaOnly(PSPublishServerInfo info) {
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
    return only != null && IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code schema} on an Oracle server. Does not add or clear any other property. Throws
   * before writing when the driver is not Oracle or the schema is blank or longer than {@link
   * #MAX_ORACLE_SCHEMA_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !PSDatabaseDeliverySidUpdate.isOracleDriver(driver)) {
      reject(ORACLE_SCHEMA_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY);
    String schema = raw == null ? "" : raw.trim();
    if (schema.isEmpty()) {
      reject(ORACLE_SCHEMA_REQUIRED);
      return;
    }
    if (schema.length() > MAX_ORACLE_SCHEMA_LENGTH) {
      reject(ORACLE_SCHEMA_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, schema);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateOracleSchema")
        .reject(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
