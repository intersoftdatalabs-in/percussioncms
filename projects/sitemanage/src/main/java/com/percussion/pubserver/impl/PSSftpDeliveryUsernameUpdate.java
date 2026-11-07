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
 * Username-only update for an SFTP delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code userid} must not clear host, port, folder, passphrase,
 * private key, driver, or name. A blank or overlong username does not write. Any driver other
 * than SFTP (including FTP, FTPS, Local, database drivers, and S3) is rejected before properties
 * change. Passphrases and the private key are not read or included in the error.
 *
 * <p>The SFTP username shares the {@code userid} body shape with the FTP and database
 * username-only paths, so the stored driver selects this apply versus {@link
 * PSFtpDeliveryUserUpdate} and {@link PSDatabaseDeliveryUserUpdate} (see {@code
 * PSPubServerService#updateDeliveryUserOnly}).
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSSftpDeliveryUsernameUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_SFTP_USERNAME_LENGTH = 255;

  public static final String SFTP_USERNAME_REQUIRED = "SFTP username is required.";

  public static final String SFTP_USERNAME_TOO_LONG =
      "SFTP username must be 255 characters or fewer.";

  public static final String SFTP_USERNAME_DRIVER =
      "SFTP username can be set only on an SFTP delivery server.";

  private PSSftpDeliveryUsernameUpdate() {}

  /**
   * True when the PUT body is only the SFTP username property. A named server, a type, a
   * description, a default flag, or any other property stays on the full update path. The shape
   * matches {@link PSFtpDeliveryUserUpdate#isUserOnly}; the stored driver selects the apply.
   */
  public static boolean isUsernameOnly(PSPublishServerInfo info) {
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
    return only != null && IPSPubServerDao.PUBLISH_USER_ID_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code userid} on an SFTP server. Does not add or clear any other property. Throws
   * before writing when the driver is not SFTP or the username is blank (after trim) or longer
   * than {@link #MAX_SFTP_USERNAME_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"SFTP".equalsIgnoreCase(driver)) {
      reject(SFTP_USERNAME_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY);
    String username = raw == null ? "" : raw.trim();
    if (username.isEmpty()) {
      reject(SFTP_USERNAME_REQUIRED);
      return;
    }
    if (username.length() > MAX_SFTP_USERNAME_LENGTH) {
      reject(SFTP_USERNAME_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, username);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateSftpUsername").reject("userid", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
