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
 * Private-key-only update for an SFTP delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code privateKey} must not clear host, port, user id, folder,
 * driver, name, the default flag, or the stored passphrase ({@code password}, which doubles as
 * the key passphrase for SFTP auth). A blank key does not write. Any driver other than SFTP
 * (including FTP, FTPS, Local, database drivers, and S3) is rejected before properties change.
 * The stored key is not read or included in the error.
 *
 * <p>The wire value is base64-decoded by {@code PSPubServerService#updateSftpPrivateKeyOnly}
 * before this apply runs (product norm for secrets, mirroring the FTP-password-only path), so
 * {@code apply} validates the decoded key. Like the full-server edit ({@code
 * PSPubServerService#setPasswordProperty}), the key is stored as-is (not encrypted): encrypting
 * it only on this path would break the full-edit GET round trip, which echoes the stored value.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSSftpDeliveryPrivateKeyUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_SFTP_PRIVATE_KEY_LENGTH = 255;

  public static final String SFTP_PRIVATE_KEY_REQUIRED = "SFTP private key is required.";

  public static final String SFTP_PRIVATE_KEY_TOO_LONG =
      "SFTP private key must be 255 characters or fewer.";

  public static final String SFTP_PRIVATE_KEY_DRIVER =
      "SFTP private key can be set only on an SFTP delivery server.";

  private PSSftpDeliveryPrivateKeyUpdate() {}

  /**
   * True when the PUT body is only the SFTP {@code privateKey} property. A named server, a type,
   * a description, a default flag, or any other property (including host-only, port-only,
   * user-only, folder-only, or password-only bodies) stays on the full or field-only update path.
   */
  public static boolean isPrivateKeyOnly(PSPublishServerInfo info) {
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
        && IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code privateKey} on an SFTP server. Does not add or clear any other property,
   * including the stored passphrase. Throws before writing when the driver is not SFTP or the
   * key is blank (after trim) or longer than {@link #MAX_SFTP_PRIVATE_KEY_LENGTH}. The value
   * written here is the decoded key; the caller decodes it before {@code savePubServer}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"SFTP".equalsIgnoreCase(driver)) {
      reject(SFTP_PRIVATE_KEY_DRIVER);
      return;
    }
    String raw =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY);
    String privateKey = raw == null ? "" : raw.trim();
    if (privateKey.isEmpty()) {
      reject(SFTP_PRIVATE_KEY_REQUIRED);
      return;
    }
    if (privateKey.length() > MAX_SFTP_PRIVATE_KEY_LENGTH) {
      reject(SFTP_PRIVATE_KEY_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, privateKey);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateSftpPrivateKey")
        .reject(IPSPubServerDao.PUBLISH_PRIVATE_KEY_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
