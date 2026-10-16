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
 * Port-only update for an FTP delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code port} must not clear host, user, folder, or password. A blank
 * or non-numeric port does not write. Any driver other than FTP (including Local, FTPS, and SFTP)
 * is rejected before properties change. Passwords are not read or included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSFtpDeliveryPortUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_FTP_PORT_LENGTH = 255;

  public static final String FTP_PORT_REQUIRED = "FTP port is required.";

  public static final String FTP_PORT_NOT_NUMERIC = "FTP port must contain only digits.";

  public static final String FTP_PORT_TOO_LONG = "FTP port must be 255 characters or fewer.";

  public static final String FTP_PORT_DRIVER =
      "FTP port can be set only on an FTP delivery server.";

  private PSFtpDeliveryPortUpdate() {}

  /**
   * True when the PUT body is only the FTP port property. A named server, a type, a description,
   * a default flag, or any other property (including a host-only body) stays off this path.
   */
  public static boolean isPortOnly(PSPublishServerInfo info) {
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
    if (properties.size() != 1) {
      return false;
    }
    PSPublishServerProperty only = properties.get(0);
    return only != null && IPSPubServerDao.PUBLISH_PORT_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code port} on an FTP server. Does not add or clear any other property. Throws
   * before writing when the driver is not FTP or the port is blank, non-numeric, or longer than
   * {@link #MAX_FTP_PORT_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"FTP".equalsIgnoreCase(driver)) {
      reject(FTP_PORT_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY);
    String port = raw == null ? "" : raw.trim();
    if (port.isEmpty()) {
      reject(FTP_PORT_REQUIRED);
      return;
    }
    if (port.length() > MAX_FTP_PORT_LENGTH) {
      reject(FTP_PORT_TOO_LONG);
      return;
    }
    if (!port.matches("[0-9]+")) {
      reject(FTP_PORT_NOT_NUMERIC);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, port);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateFtpPort").reject("port", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
