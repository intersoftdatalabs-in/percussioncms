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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerProperty;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.share.service.exception.PSValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Secret-key-only Amazon S3 delivery-server update. Other properties must not change and the
 * stored secret key must never leak into an error message.
 */
class PSAmazonS3DeliverySecretKeyUpdateTest {

  private static final String ACCESS_KEY = "AKIATESTACCESS";
  private static final String SECRET_KEY = "secret-s3-key";
  private static final String NEW_SECRET_KEY = "new-secret-s3-key";

  @Test
  void secretKeyOnlyBodyIsOnlySecretKey() {
    assertTrue(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSAmazonS3DeliveryAccessKeyUpdate.isAccessKeyOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(secretKeyOnly(NEW_SECRET_KEY)));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(secretKeyOnly(NEW_SECRET_KEY)));

    PSPublishServerInfo upper = new PSPublishServerInfo();
    upper.setProperties(List.of(property("SecurityKey", NEW_SECRET_KEY)));
    assertTrue(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(upper));
  }

  @Test
  void namedRegionBucketOrAccessKeyAreNotSecretKeyOnly() {
    PSPublishServerInfo named = secretKeyOnly(NEW_SECRET_KEY);
    named.setServerName("s3-prod");
    assertFalse(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(named));

    PSPublishServerInfo regionOnly = new PSPublishServerInfo();
    regionOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_EC2_REGION, "us-east-1")));
    assertFalse(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(regionOnly));

    PSPublishServerInfo bucketOnly = new PSPublishServerInfo();
    bucketOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "new-bucket")));
    assertFalse(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(bucketOnly));

    PSPublishServerInfo withAccessKey = secretKeyOnly(NEW_SECRET_KEY);
    List<PSPublishServerProperty> props = new ArrayList<>(withAccessKey.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY));
    withAccessKey.setProperties(props);
    assertFalse(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(withAccessKey));
    assertFalse(PSAmazonS3DeliverySecretKeyUpdate.isSecretKeyOnly(null));
  }

  @Test
  void applyReplacesSecretKeyAndLeavesBucketRegionAccessKeyNameDriver() throws Exception {
    PSPubServer server = s3Server();
    PSAmazonS3DeliverySecretKeyUpdate.apply(server, secretKeyOnly("  " + NEW_SECRET_KEY + "  "));
    assertEquals(NEW_SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
    assertEquals("old-bucket", value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals("us-west-2", value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
    assertEquals("AMAZONS3", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("s3-prod", server.getName());
    assertEquals(5, server.getProperties().size());
  }

  @Test
  void blankOrOverlongSecretKeyDoesNotWrite() {
    PSPubServer blank = s3Server();
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliverySecretKeyUpdate.apply(blank, secretKeyOnly("   ")));
    assertTrue(
        blankError.getMessage().contains(PSAmazonS3DeliverySecretKeyUpdate.S3_SECRET_KEY_REQUIRED));
    assertFalse(blankError.getMessage().contains(SECRET_KEY));
    assertFalse(blankError.getMessage().contains(ACCESS_KEY));
    assertUnchanged(blank);

    PSPubServer overlong = s3Server();
    String tooLong = "k".repeat(PSAmazonS3DeliverySecretKeyUpdate.MAX_S3_SECRET_KEY_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliverySecretKeyUpdate.apply(overlong, secretKeyOnly(tooLong)));
    assertTrue(
        longError
            .getMessage()
            .contains(PSAmazonS3DeliverySecretKeyUpdate.S3_SECRET_KEY_TOO_LONG));
    assertFalse(longError.getMessage().contains(SECRET_KEY));
    assertFalse(longError.getMessage().contains(tooLong));
    assertUnchanged(overlong);
  }

  @Test
  void localFtpAndDatabaseDriversDoNotChangeSecretKey() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliverySecretKeyUpdate.apply(local, secretKeyOnly(NEW_SECRET_KEY)));
    assertTrue(
        localError.getMessage().contains(PSAmazonS3DeliverySecretKeyUpdate.S3_SECRET_KEY_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliverySecretKeyUpdate.apply(ftp, secretKeyOnly(NEW_SECRET_KEY)));
    assertTrue(
        ftpError.getMessage().contains(PSAmazonS3DeliverySecretKeyUpdate.S3_SECRET_KEY_DRIVER));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY).isEmpty());

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    oracle.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, "SYSTEM");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliverySecretKeyUpdate.apply(oracle, secretKeyOnly(NEW_SECRET_KEY)));
    assertTrue(
        oracleError.getMessage().contains(PSAmazonS3DeliverySecretKeyUpdate.S3_SECRET_KEY_DRIVER));
    assertEquals("SYSTEM", value(oracle, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
  }

  @Test
  void s3DriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer shortName = s3Server();
    shortName.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "S3");
    PSAmazonS3DeliverySecretKeyUpdate.apply(shortName, secretKeyOnly(NEW_SECRET_KEY));
    assertEquals(NEW_SECRET_KEY, value(shortName, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
    assertEquals("old-bucket", value(shortName, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals(ACCESS_KEY, value(shortName, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server) {
    assertEquals(SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
    assertEquals("old-bucket", value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals("us-west-2", value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
  }

  private static PSPubServer s3Server() {
    PSPubServer server = new PSPubServer();
    server.setName("s3-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "AMAZONS3");
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "old-bucket");
    server.addProperty(IPSPubServerDao.PUBLISH_EC2_REGION, "us-west-2");
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY);
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, SECRET_KEY);
    return server;
  }

  private static PSPublishServerInfo secretKeyOnly(String secretKey) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, secretKey)));
    return info;
  }

  private static PSPublishServerProperty property(String key, String value) {
    PSPublishServerProperty property = new PSPublishServerProperty();
    property.setKey(key);
    property.setValue(value);
    return property;
  }

  private static String value(PSPubServer server, String key) {
    return server.getPropertyValue(key).orElse("");
  }
}
