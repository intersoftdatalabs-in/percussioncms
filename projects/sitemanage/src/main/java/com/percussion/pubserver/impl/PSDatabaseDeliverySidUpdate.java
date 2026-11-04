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
 * SID-only update for an Oracle database delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code sid} must not clear host, port, userid, driver, name, default
 * flag, password, schema, or MySQL/MSSQL database. Blank and overlong SIDs do not write. MYSQL,
 * MSSQL, FTP, and Local drivers are rejected before properties change. Passwords are not read or
 * included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliverySidUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_ORACLE_SID_LENGTH = 255;

  public static final String ORACLE_SID_REQUIRED = "Oracle SID is required.";

  public static final String ORACLE_SID_TOO_LONG = "Oracle SID must be 255 characters or fewer.";

  public static final String ORACLE_SID_DRIVER =
      "Oracle SID can be set only on an Oracle delivery server.";

  private PSDatabaseDeliverySidUpdate() {}

  /**
   * True when the PUT body is only the Oracle {@code sid} property. A named server, a type, a
   * description, a default flag, or any other property (including {@code database}) stays on the
   * full or field-only update path.
   */
  public static boolean isSidOnly(PSPublishServerInfo info) {
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
    return only != null && IPSPubServerDao.PUBLISH_SID_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  public static boolean isOracleDriver(String driver) {
    return driver != null && "ORACLE".equals(driver.trim().toUpperCase());
  }

  /**
   * Replace {@code sid} on an Oracle server. Does not add or clear any other property. Throws
   * before writing when the driver is not Oracle or the SID is blank or longer than {@link
   * #MAX_ORACLE_SID_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !isOracleDriver(driver)) {
      reject(ORACLE_SID_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY);
    String sid = raw == null ? "" : raw.trim();
    if (sid.isEmpty()) {
      reject(ORACLE_SID_REQUIRED);
      return;
    }
    if (sid.length() > MAX_ORACLE_SID_LENGTH) {
      reject(ORACLE_SID_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, sid);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateOracleSid")
        .reject(IPSPubServerDao.PUBLISH_SID_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
