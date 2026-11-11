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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Connection-only update for a database delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries exactly {@code server}, {@code userid}, and {@code password} replaces
 * the host, user id, and password in one save. Port, database/sid, driver, name, and the default
 * flag are not sent and stay stored. A blank host, blank user id, blank password, the masked
 * placeholder ({@code passwordEntry}) that GET responses carry, or an overlong value does not
 * write. Any driver that is not a database driver is rejected before properties change. The
 * stored secret is never read into an error message.
 *
 * <p>The {@code info} password value is the base64-decoded secret (product norm): {@code
 * PSPubServerService#updateDatabaseConnectionOnly} runs {@code convertPasswordFromBase64} before
 * this apply, mirroring the password-only path.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSDatabaseDeliveryConnectionUpdate {

  private PSDatabaseDeliveryConnectionUpdate() {}

  /**
   * True when the PUT body is only the database connection triple (host, user id, password, in
   * any order). A named server, a type, a description, a default flag, or any other property
   * (including a host-only, user-only, or password-only body) stays off this path.
   */
  public static boolean isConnectionOnly(PSPublishServerInfo info) {
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
    if (properties == null || properties.size() != 3) {
      return false;
    }
    Set<String> keys = new HashSet<>();
    for (PSPublishServerProperty property : properties) {
      if (property == null || property.getKey() == null) {
        return false;
      }
      keys.add(property.getKey().trim().toUpperCase());
    }
    return keys.size() == 3
        && keys.contains(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME.toUpperCase())
        && keys.contains(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY.toUpperCase())
        && keys.contains(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY.toUpperCase());
  }

  /**
   * Replace {@code server}, {@code userid}, and {@code password} on a database server. Does not
   * add or clear any other property. Throws before writing when the driver is not a database
   * driver, the host or user id is blank, the password is blank or the masked placeholder, or
   * any of the three values is longer than 255 characters.
   *
   * <p>The publish-server wire layer ({@code PSPublishServerProperty#setValue}) trims
   * surrounding whitespace, so padded values are stored trimmed — the same norm as the
   * full-server edit. A blank (after trim) host or user id is rejected and leaves the stored
   * values untouched.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !PSDatabaseDeliveryHostUpdate.isDatabaseDeliveryDriver(driver)) {
      reject(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_DRIVER);
      return;
    }
    String rawHost =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME);
    String host = rawHost == null ? "" : rawHost.trim();
    if (host.isEmpty()) {
      reject(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_REQUIRED);
      return;
    }
    if (host.length() > PSDatabaseDeliveryHostUpdate.MAX_DATABASE_HOST_LENGTH) {
      reject(PSDatabaseDeliveryHostUpdate.DATABASE_HOST_TOO_LONG);
      return;
    }
    String rawUser =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY);
    String userId = rawUser == null ? "" : rawUser.trim();
    if (userId.isEmpty()) {
      reject(PSDatabaseDeliveryUserUpdate.DATABASE_USER_REQUIRED);
      return;
    }
    if (userId.length() > PSDatabaseDeliveryUserUpdate.MAX_DATABASE_USER_LENGTH) {
      reject(PSDatabaseDeliveryUserUpdate.DATABASE_USER_TOO_LONG);
      return;
    }
    String password = validatedPassword(info);
    server.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, host);
    server.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, userId);
    server.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, password);
  }

  /**
   * Return the decoded secret from the body. Blank, masked-placeholder, and overlong values
   * throw before anything writes.
   */
  private static String validatedPassword(PSPublishServerInfo info)
      throws PSValidationException {
    String candidate =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY);
    if (StringUtils.isBlank(candidate)
        || PSPubServerService.PASSWORD_ENTRY.equals(candidate)) {
      reject(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED);
      throw new IllegalStateException(
          PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_REQUIRED);
    }
    if (candidate.length() > PSDatabaseDeliveryPasswordUpdate.MAX_DATABASE_PASSWORD_LENGTH) {
      reject(PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_TOO_LONG);
      throw new IllegalStateException(
          PSDatabaseDeliveryPasswordUpdate.DATABASE_PASSWORD_TOO_LONG);
    }
    return candidate;
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateDatabaseConnection").reject("connection", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
