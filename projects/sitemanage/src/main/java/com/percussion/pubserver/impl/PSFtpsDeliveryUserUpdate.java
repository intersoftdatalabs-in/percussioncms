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
 * User-id-only update for an FTPS delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code userid} must not clear host, port, folder, passphrase,
 * private key, driver, or name. A blank or overlong user id does not write. Any driver other
 * than FTPS (including FTP, SFTP, Local, database drivers, and S3) is rejected before properties
 * change. Passwords and the private key are not read or included in the error.
 *
 * <p>The FTPS user id shares the {@code userid} body shape with the FTP, SFTP, and database
 * user-id-only paths, so the stored driver selects this apply versus {@link
 * PSFtpDeliveryUserUpdate}, {@link PSSftpDeliveryUsernameUpdate}, and {@link
 * PSDatabaseDeliveryUserUpdate} (see {@code PSPubServerService#updateDeliveryUserOnly}).
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSFtpsDeliveryUserUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_FTPS_USER_LENGTH = 255;

  public static final String FTPS_USER_REQUIRED = "FTPS user id is required.";

  public static final String FTPS_USER_TOO_LONG =
      "FTPS user id must be 255 characters or fewer.";

  public static final String FTPS_USER_DRIVER =
      "FTPS user id can be set only on an FTPS delivery server.";

  private PSFtpsDeliveryUserUpdate() {}

  /**
   * True when the PUT body is only the FTPS user id property. A named server, a type, a
   * description, a default flag, or any other property (including a host-only or port-only body)
   * stays off this path. The shape matches {@link PSFtpDeliveryUserUpdate#isUserOnly}; the stored
   * driver selects the apply.
   */
  public static boolean isUserOnly(PSPublishServerInfo info) {
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
   * Replace {@code userid} on an FTPS server. Does not add or clear any other property. Throws
   * before writing when the driver is not FTPS or the user id is blank (after trim) or longer
   * than {@link #MAX_FTPS_USER_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"FTPS".equalsIgnoreCase(driver)) {
      reject(FTPS_USER_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY);
    String userId = raw == null ? "" : raw.trim();
    if (userId.isEmpty()) {
      reject(FTPS_USER_REQUIRED);
      return;
    }
    if (userId.length() > MAX_FTPS_USER_LENGTH) {
      reject(FTPS_USER_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateFtpsUser").reject("userid", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
