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
 * User-id-only update for an FTP delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code userid} must not clear host, port, folder, or password. A
 * blank user id does not write. Any driver other than FTP (including Local, FTPS, SFTP, and S3)
 * is rejected before properties change. Passwords are not read or included in the error.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSFtpDeliveryUserUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_FTP_USER_LENGTH = 255;

  public static final String FTP_USER_REQUIRED = "FTP user id is required.";

  public static final String FTP_USER_TOO_LONG = "FTP user id must be 255 characters or fewer.";

  public static final String FTP_USER_DRIVER =
      "FTP user id can be set only on an FTP delivery server.";

  private PSFtpDeliveryUserUpdate() {}

  /**
   * True when the PUT body is only the FTP user id. A named server, a type, a description, a
   * default flag, or any other property (including a host-only or port-only body) stays off this
   * path.
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
    if (properties.size() != 1) {
      return false;
    }
    PSPublishServerProperty only = properties.get(0);
    return only != null && IPSPubServerDao.PUBLISH_USER_ID_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code userid} on an FTP server. Does not add or clear any other property. Throws
   * before writing when the driver is not FTP or the user id is blank or longer than {@link
   * #MAX_FTP_USER_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"FTP".equalsIgnoreCase(driver)) {
      reject(FTP_USER_DRIVER);
      return;
    }
    String raw = info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY);
    String userId = raw == null ? "" : raw.trim();
    if (userId.isEmpty()) {
      reject(FTP_USER_REQUIRED);
      return;
    }
    if (userId.length() > MAX_FTP_USER_LENGTH) {
      reject(FTP_USER_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateFtpUser").reject("userid", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
