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
 * Passphrase-only update for an SFTP delivery server on the existing publish-server PUT.
 *
 * <p>An SFTP key passphrase is stored in the {@code password} property (the same key the
 * FTP-password-only and database-password-only paths use), so a passphrase-only body shares their
 * wire shape. The stored driver selects this SFTP apply inside {@code
 * PSPubServerService#updateDeliveryPasswordOnly}; any driver other than SFTP (including FTP,
 * FTPS, Local, database drivers, and S3) is rejected before properties change.
 *
 * <p>A body that carries only {@code password} for an SFTP server must not clear host, port, user
 * id, folder, driver, name, the default flag, or the stored private key ({@code privateKey}). A
 * blank passphrase does not write. The masked placeholder ({@code passwordEntry}) that GET
 * responses carry is never persisted as the stored secret. The stored secret is never read into
 * an error message.
 *
 * <p>The {@code info} value is the base64-decoded passphrase (product norm): {@code
 * PSPubServerService#updateDeliveryPasswordOnly} runs {@code convertPasswordFromBase64} before
 * this apply, mirroring the FTP-password-only path.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSSftpDeliveryPassphraseUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_SFTP_PASSPHRASE_LENGTH = 255;

  public static final String SFTP_PASSPHRASE_REQUIRED = "SFTP passphrase is required.";

  public static final String SFTP_PASSPHRASE_TOO_LONG =
      "SFTP passphrase must be 255 characters or fewer.";

  public static final String SFTP_PASSPHRASE_DRIVER =
      "SFTP passphrase can be set only on an SFTP delivery server.";

  private PSSftpDeliveryPassphraseUpdate() {}

  /**
   * True when the PUT body is only the SFTP passphrase ({@code password} property). A named
   * server, a type, a description, a default flag, or any other property (including a host-only,
   * port-only, username-only, folder-only, or private-key-only body) stays off this path. The
   * stored driver selects SFTP vs FTP vs database apply.
   */
  public static boolean isPassphraseOnly(PSPublishServerInfo info) {
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
   * Replace the passphrase ({@code password}) on an SFTP server. Does not add or clear any other
   * property, including the stored private key. Throws before writing when the driver is not
   * SFTP, the passphrase is blank, the masked placeholder is submitted, or the passphrase is
   * longer than {@link #MAX_SFTP_PASSPHRASE_LENGTH}.
   *
   * <p>The publish-server wire layer ({@code PSPublishServerProperty#setValue}) trims surrounding
   * whitespace, so a padded passphrase is stored trimmed — the same norm as the full-server edit.
   * A blank (empty after trim) passphrase is rejected and leaves the stored secret untouched.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"SFTP".equalsIgnoreCase(driver)) {
      reject(SFTP_PASSPHRASE_DRIVER);
      return;
    }
    String passphrase =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY);
    if (StringUtils.isBlank(passphrase)
        || PSPubServerService.PASSWORD_ENTRY.equals(passphrase)) {
      reject(SFTP_PASSPHRASE_REQUIRED);
      return;
    }
    if (passphrase.length() > MAX_SFTP_PASSPHRASE_LENGTH) {
      reject(SFTP_PASSPHRASE_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, passphrase);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateSftpPassphrase").reject("password", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
