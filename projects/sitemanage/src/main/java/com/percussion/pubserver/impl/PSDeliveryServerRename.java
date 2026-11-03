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
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Name-only update for an existing delivery server on the publish-server PUT.
 *
 * <p>A body that carries only {@code serverName} must not replace properties, change the driver,
 * type, or default flag. Blank, overlong, and invalid names do not write. Uniqueness is checked
 * by the service against other servers on the same site.
 *
 * <p>{@code PSX_PUBSERVER.NAME} is VARCHAR(50). Allowed characters match the full update:
 * {@code a-z}, {@code 0-9}, {@code .}, {@code _}, and {@code -}.
 */
public final class PSDeliveryServerRename {

  /** Matches {@code PSX_PUBSERVER.NAME} and {@code PSPubServerService} name length. */
  public static final int MAX_NAME_LENGTH = 50;

  public static final String NAME_REQUIRED = "Name is a required field.";

  public static final String NAME_TOO_LONG =
      "Server name cannot have more than " + MAX_NAME_LENGTH + " characters.";

  public static final String NAME_INVALID =
      "Invalid character in server name. Characters allowed are: a-z, 0-9, ., _ and -.";

  private static final Pattern NAME_PATTERN = Pattern.compile("[\\w.-]+");

  private PSDeliveryServerRename() {}

  /**
   * True when the PUT body is only a server name. A type, description, default flag, or any
   * property stays on the full (or field-only) update path. A blank name is still name-only so
   * the full replace does not run.
   */
  public static boolean isNameOnly(PSPublishServerInfo info) {
    if (info == null || info.getServerName() == null) {
      return false;
    }
    if (isNotBlank(info.getType()) || isNotBlank(info.getServerType())) {
      return false;
    }
    if (info.getDescription() != null || info.getIsDefault() != null) {
      return false;
    }
    List<PSPublishServerProperty> properties = info.getProperties();
    return properties == null || properties.isEmpty();
  }

  /**
   * Trimmed name that may be stored. Throws before writing when blank, longer than {@link
   * #MAX_NAME_LENGTH}, or not matching the allowed character set.
   */
  public static String normalizedName(PSPublishServerInfo info) throws PSValidationException {
    String raw = info == null ? null : info.getServerName();
    String name = raw == null ? "" : raw.trim();
    if (name.isEmpty()) {
      reject(NAME_REQUIRED);
    }
    if (name.length() > MAX_NAME_LENGTH) {
      reject(NAME_TOO_LONG);
    }
    if (!NAME_PATTERN.matcher(name).matches()) {
      reject(NAME_INVALID);
    }
    return name;
  }

  /**
   * Replace only the stored name. Properties, type, and default flag are not read from {@code
   * info}. Call {@link #normalizedName} first.
   */
  public static void apply(PSPubServer server, String name) {
    if (server == null) {
      return;
    }
    server.setName(name);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateDeliveryServerName").reject("serverName", message).throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
