/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;

/**
 * Default-flag-only update for an existing delivery server on the publish-server PUT.
 *
 * <p>A body that carries only {@code isDefault} must not rename the server, change the driver, type,
 * or properties. {@code isDefault} must be {@code true}; this path does not unmark a default.
 * A server that is already the site default is not a write.
 */
public final class PSDeliveryServerDefaultUpdate {

  public static final String DEFAULT_FLAG_REQUIRED =
      "Default flag must be true to mark a delivery server as default.";

  private PSDeliveryServerDefaultUpdate() {}

  /**
   * True when the PUT body is only the default flag. A name, type, description, or any property
   * stays on the full (or field-only) update path.
   */
  public static boolean isDefaultOnly(PSPublishServerInfo info) {
    if (info == null || info.getIsDefault() == null) {
      return false;
    }
    if (isNotBlank(info.getServerName())
        || isNotBlank(info.getType())
        || isNotBlank(info.getServerType())) {
      return false;
    }
    if (info.getDescription() != null) {
      return false;
    }
    List<PSPublishServerProperty> properties = info.getProperties();
    return properties == null || properties.isEmpty();
  }

  /**
   * Rejects a missing or false flag before any write. Call before {@link #alreadyDefault}.
   */
  public static void validate(PSPublishServerInfo info) throws PSValidationException {
    if (info == null || !Boolean.TRUE.equals(info.getIsDefault())) {
      reject(DEFAULT_FLAG_REQUIRED);
    }
  }

  /**
   * True when {@code server} is already the site default. That case must not call {@code
   * savePubServer}.
   */
  public static boolean alreadyDefault(PSPubServer server, PSPubServer currentDefault) {
    return server != null
        && currentDefault != null
        && currentDefault.getServerId() == server.getServerId();
  }

  /**
   * Store only the site default id. Does not change either server's publish type, properties, or
   * editions.
   */
  public static void apply(IPSSite site, PSPubServer server) {
    if (site == null || server == null) {
      throw new IllegalArgumentException("site and server are required");
    }
    site.setDefaultPubServer(server.getServerId());
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateDeliveryServerDefault")
        .reject("isDefault", message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
