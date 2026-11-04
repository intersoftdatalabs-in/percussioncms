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
 * Owner-only update for an MSSQL database delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code owner} must not clear host, port, userid, driver, name,
 * default flag, password, database name, or Oracle SID. Blank and overlong owners do not write.
 * MYSQL, Oracle, FTP, and Local drivers are rejected before properties change. Passwords are not
 * read or included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliveryOwnerUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_MSSQL_OWNER_LENGTH = 255;

  public static final String MSSQL_OWNER_REQUIRED = "MSSQL owner is required.";

  public static final String MSSQL_OWNER_TOO_LONG =
      "MSSQL owner must be 255 characters or fewer.";

  public static final String MSSQL_OWNER_DRIVER =
      "MSSQL owner can be set only on an MSSQL delivery server.";

  private PSDatabaseDeliveryOwnerUpdate() {}

  /**
   * True when the PUT body is only the MSSQL {@code owner} property. A named server, a type, a
   * description, a default flag, or any other property (including {@code database} and {@code
   * sid}) stays on the full or field-only update path.
   */
  public static boolean isOwnerOnly(PSPublishServerInfo info) {
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
    return only != null && IPSPubServerDao.PUBLISH_OWNER_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  public static boolean isMssqlDriver(String driver) {
    return driver != null && "MSSQL".equals(driver.trim().toUpperCase());
  }

  /**
   * Replace {@code owner} on an MSSQL server. Does not add or clear any other property. Throws
   * before writing when the driver is not MSSQL or the owner is blank or longer than {@link
   * #MAX_MSSQL_OWNER_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !isMssqlDriver(driver)) {
      reject(MSSQL_OWNER_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_OWNER_PROPERTY);
    String owner = raw == null ? "" : raw.trim();
    if (owner.isEmpty()) {
      reject(MSSQL_OWNER_REQUIRED);
      return;
    }
    if (owner.length() > MAX_MSSQL_OWNER_LENGTH) {
      reject(MSSQL_OWNER_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, owner);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateMssqlOwner")
        .reject(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
