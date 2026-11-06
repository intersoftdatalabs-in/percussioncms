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
 * Access-key-only update for an Amazon S3 delivery server on the existing publish-server PUT.
 *
 * <p>A body that carries only {@code accesskey} must not clear bucket, region, secret key, name,
 * driver, or default flag. Blank and overlong keys do not write. FTP, Local, and database drivers
 * are rejected before properties change. The stored secret key is not read or included in the
 * error.
 *
 * <p>The wire value is base64-decoded by {@code PSPubServerService#updateS3AccessKeyOnly} before
 * this apply runs (product norm for secrets, mirroring the FTP password-only path), so {@code
 * apply} validates the decoded key. The decoded key is stored encrypted with {@code PSEncryptor}
 * by the service after apply — never in plain text — mirroring the full-edit path for {@code
 * encryptableProperties}.
 *
 * <p>{@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE} is VARCHAR(255).
 */
public final class PSAmazonS3DeliveryAccessKeyUpdate {

  /** Matches {@code PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE}. */
  public static final int MAX_S3_ACCESS_KEY_LENGTH = 255;

  public static final String S3_ACCESS_KEY_REQUIRED = "S3 access key is required.";

  public static final String S3_ACCESS_KEY_TOO_LONG =
      "S3 access key must be 255 characters or fewer.";

  public static final String S3_ACCESS_KEY_DRIVER =
      "S3 access key can be set only on an Amazon S3 delivery server.";

  private PSAmazonS3DeliveryAccessKeyUpdate() {}

  /**
   * True when the PUT body is only the S3 {@code accesskey} property. A named server, a type, a
   * description, a default flag, or any other property (including {@code bucketlocation}, {@code
   * region}, and {@code securitykey}) stays on the full or field-only update path.
   */
  public static boolean isAccessKeyOnly(PSPublishServerInfo info) {
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
        && IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY.equalsIgnoreCase(only.getKey());
  }

  /**
   * Replace {@code accesskey} on an Amazon S3 server. Does not add or clear any other property.
   * Throws before writing when the driver is not S3 or the key is blank or longer than {@link
   * #MAX_S3_ACCESS_KEY_LENGTH}. The value written here is the decoded key; the caller encrypts it
   * before {@code savePubServer}.
   */
  public static void apply(PSPubServer server, PSPublishServerInfo info)
      throws PSValidationException {
    String driver =
        server == null
            ? ""
            : server.getPropertyValue(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY).orElse("");
    if (server == null || !PSAmazonS3DeliveryBucketUpdate.isS3Driver(driver)) {
      reject(S3_ACCESS_KEY_DRIVER);
      return;
    }
    String raw =
        info == null ? null : info.findProperty(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY);
    String accessKey = raw == null ? "" : raw.trim();
    if (accessKey.isEmpty()) {
      reject(S3_ACCESS_KEY_REQUIRED);
      return;
    }
    if (accessKey.length() > MAX_S3_ACCESS_KEY_LENGTH) {
      reject(S3_ACCESS_KEY_TOO_LONG);
      return;
    }
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, accessKey);
  }

  private static void reject(String message) throws PSValidationException {
    validateParameters("updateS3AccessKey")
        .reject(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, message)
        .throwIfInvalid();
    throw new IllegalStateException(message);
  }
}
