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

/** Region-only Amazon S3 delivery-server update. Other properties must not change. */
class PSAmazonS3DeliveryRegionUpdateTest {

  private static final String ACCESS_KEY = "AKIATESTACCESS";
  private static final String SECRET_KEY = "secret-s3-key";

  @Test
  void regionOnlyBodyIsOnlyRegion() {
    assertTrue(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(regionOnly("us-east-1")));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(regionOnly("us-east-1")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(regionOnly("us-east-1")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(regionOnly("us-east-1")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(regionOnly("us-east-1")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(regionOnly("us-east-1")));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(regionOnly("us-east-1")));
  }

  @Test
  void namedBucketOrSecretsAreNotRegionOnly() {
    PSPublishServerInfo named = regionOnly("us-east-1");
    named.setServerName("s3-prod");
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(named));

    PSPublishServerInfo bucketOnly = new PSPublishServerInfo();
    bucketOnly.setProperties(
        List.of(property(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "new-bucket")));
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(bucketOnly));

    PSPublishServerInfo withSecret = regionOnly("us-east-1");
    List<PSPublishServerProperty> props = new ArrayList<>(withSecret.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, SECRET_KEY));
    withSecret.setProperties(props);
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(withSecret));

    PSPublishServerInfo withAccess = regionOnly("us-east-1");
    List<PSPublishServerProperty> accessProps = new ArrayList<>(withAccess.getProperties());
    accessProps.add(property(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY));
    withAccess.setProperties(accessProps);
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(withAccess));
    assertFalse(PSAmazonS3DeliveryRegionUpdate.isRegionOnly(null));
  }

  @Test
  void applyReplacesRegionAndLeavesBucketSecretsNameDriverAndDefault() throws Exception {
    PSPubServer server = s3Server("us-west-2");
    PSAmazonS3DeliveryRegionUpdate.apply(server, regionOnly("  us-east-1  "));
    assertEquals("us-east-1", value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals("old-bucket", value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
    assertEquals(SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
    assertEquals("AMAZONS3", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("s3-prod", server.getName());
    assertEquals(5, server.getProperties().size());
  }

  @Test
  void blankOrOverlongRegionDoesNotWrite() {
    PSPubServer blank = s3Server("us-west-2");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryRegionUpdate.apply(blank, regionOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSAmazonS3DeliveryRegionUpdate.S3_REGION_REQUIRED));
    assertFalse(blankError.getMessage().contains(SECRET_KEY));
    assertFalse(blankError.getMessage().contains(ACCESS_KEY));
    assertUnchanged(blank, "us-west-2");

    PSPubServer overlong = s3Server("us-west-2");
    String tooLong = "r".repeat(PSAmazonS3DeliveryRegionUpdate.MAX_S3_REGION_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryRegionUpdate.apply(overlong, regionOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSAmazonS3DeliveryRegionUpdate.S3_REGION_TOO_LONG));
    assertFalse(longError.getMessage().contains(SECRET_KEY));
    assertUnchanged(overlong, "us-west-2");
  }

  @Test
  void localFtpAndDatabaseDriversDoNotChangeRegion() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryRegionUpdate.apply(local, regionOnly("us-east-1")));
    assertTrue(localError.getMessage().contains(PSAmazonS3DeliveryRegionUpdate.S3_REGION_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryRegionUpdate.apply(ftp, regionOnly("us-east-1")));
    assertTrue(ftpError.getMessage().contains(PSAmazonS3DeliveryRegionUpdate.S3_REGION_DRIVER));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION).isEmpty());

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    oracle.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, "SYSTEM");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryRegionUpdate.apply(oracle, regionOnly("us-east-1")));
    assertTrue(oracleError.getMessage().contains(PSAmazonS3DeliveryRegionUpdate.S3_REGION_DRIVER));
    assertEquals("SYSTEM", value(oracle, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
  }

  @Test
  void s3DriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer mixed = s3Server("us-west-2");
    mixed.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "amazons3");
    PSAmazonS3DeliveryRegionUpdate.apply(mixed, regionOnly("eu-west-1"));
    assertEquals("eu-west-1", value(mixed, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals("old-bucket", value(mixed, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));

    PSPubServer shortName = s3Server("us-west-2");
    shortName.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "S3");
    PSAmazonS3DeliveryRegionUpdate.apply(shortName, regionOnly("ap-south-1"));
    assertEquals("ap-south-1", value(shortName, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals(SECRET_KEY, value(shortName, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
  }

  private static void assertUnchanged(PSPubServer server, String region) {
    assertEquals(region, value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals("old-bucket", value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
    assertEquals(SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
  }

  private static PSPubServer s3Server(String region) {
    PSPubServer server = new PSPubServer();
    server.setName("s3-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "AMAZONS3");
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "old-bucket");
    server.addProperty(IPSPubServerDao.PUBLISH_EC2_REGION, region);
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY);
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, SECRET_KEY);
    return server;
  }

  private static PSPublishServerInfo regionOnly(String region) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_EC2_REGION, region)));
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
