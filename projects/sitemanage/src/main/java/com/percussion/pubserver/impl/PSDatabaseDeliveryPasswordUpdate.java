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

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Password-only update for a database delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code password} must not clear host, port, user id,
 * database/sid, driver, name, or the default flag. A blank password does not write. The masked
 * placeholder ({@code passwordEntry}) that GET responses carry is never persisted as the stored
 * secret. Any driver that is not a database driver (including FTP, Local, FTPS, SFTP, and S3)
 * is rejected before properties change. The stored secret is never read into an error message.
 *
 * <p>The {@code info} value is the base64-decoded password (product norm): {@code
 * PSPubServerService#updateDeliveryPasswordOnly} runs {@code convertPasswordFromBase64} before
 * this apply, mirroring the full-edit path.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliveryPasswordUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_DATABASE_PASSWORD_LENGTH = 255;

  public static final String DATABASE_PASSWORD_REQUIRED = "Database password is required.";

  public static final String DATABASE_PASSWORD_TOO_LONG =
      "Database password must be 255 characters or fewer.";

  public static final String DATABASE_PASSWORD_DRIVER =
      "Database password can be set only on a database delivery server.";

  private PSDatabaseDeliveryPasswordUpdate() {}

  /**
   * True when the PUT body is only the database (or FTP) password. A named server, a type, a
   * description, a default flag, or any other property (including a host-only, port-only, or
   * user-only body) stays off this path. The stored driver selects database vs FTP apply.
   */
  public static boolean isPasswordOnly(PSPublishServerInfo info) {
    if (info == null) {
      return false;
    }
    if (StringUtils.isNotBlank(info.getServerName())
        || StringUtils.isNotBlank(info.getType())
        || StringUtils.isNotBlank(info.getServerType())) {
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
        && IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code password} on a database server. Does not add or clear any other property.
   * Throws before writing when the driver is not a database driver, the password is blank, the
   * masked placeholder is submitted, or the password is longer than {@link
   * #MAX_DATABASE_PASSWORD_LENGTH}.
   *
   * <p>The publish-server wire layer ({@code PSPublishServerProperty#setValue}) trims surrounding
   * whitespace, so a padded password is stored trimmed — the same norm as the full-server edit.
   * A blank (empty after trim) password is rejected and leaves the stored secret untouched.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver(driver)) {
      reject(DATABASE_PASSWORD_DRIVER);
      return;
    }
    String password =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY);
    if (StringUtils.isBlank(password)
        || PSPubServerService.PASSWORD_ENTRY.equals(password)) {
      reject(DATABASE_PASSWORD_REQUIRED);
      return;
    }
    if (password.length() > MAX_DATABASE_PASSWORD_LENGTH) {
      reject(DATABASE_PASSWORD_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateDatabasePassword").reject("password", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
