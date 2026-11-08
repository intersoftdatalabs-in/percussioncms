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
 * Folder-only update for an SFTP delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code folder} must not clear host, port, user id, passphrase, or
 * private key. A blank folder clears the stored folder because the field is optional. Any driver
 * other than SFTP (including FTP, FTPS, Local, database drivers, and S3) is rejected before
 * properties change. Secrets are not read or included in the error. The folder value is a remote
 * SFTP path and is stored as trimmed text; it is not rewritten with the local filesystem
 * separator.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSSftpDeliveryFolderUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_SFTP_FOLDER_LENGTH = 255;

  public static final String SFTP_FOLDER_TOO_LONG = "SFTP folder must be 255 characters or fewer.";

  public static final String SFTP_FOLDER_DRIVER =
      "SFTP folder can be set only on an SFTP delivery server.";

  /** Defensive: a null body is not a blank folder and must not clear the stored folder. */
  public static final String SFTP_FOLDER_MISSING = "SFTP folder was not provided.";

  private PSSftpDeliveryFolderUpdate() {}

  /**
   * True when the PUT body is only the SFTP folder. A named server, a type, a description, a
   * default flag, or any other property (including a host-only, port-only, user-only,
   * passphrase-only, or private-key-only body) stays off this path. A blank folder value is still
   * folder-only.
   */
  public static boolean isFolderOnly(PSPublishServerInfo info) {
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
    return only != null && IPSPubServerDao.PUBLISH_FOLDER_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code folder} on an SFTP server. A blank value stores an empty folder and does not
   * remove host, port, user id, passphrase, or private key. Throws before writing when the driver
   * is not SFTP or the folder is longer than {@link #MAX_SFTP_FOLDER_LENGTH}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !"SFTP".equalsIgnoreCase(driver)) {
      reject(SFTP_FOLDER_DRIVER);
      return;
    }
    if (info == null) {
      reject(SFTP_FOLDER_MISSING);
      return;
    }
    String raw = info.findProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY);
    String folder = raw == null ? "" : raw.trim();
    if (folder.length() > MAX_SFTP_FOLDER_LENGTH) {
      reject(SFTP_FOLDER_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, folder);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateSftpFolder").reject("folder", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
