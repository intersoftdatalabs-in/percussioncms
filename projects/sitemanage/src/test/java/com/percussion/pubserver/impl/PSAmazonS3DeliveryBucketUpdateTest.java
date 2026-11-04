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

/** Bucket-only Amazon S3 delivery-server update. Other properties must not change. */
class PSAmazonS3DeliveryBucketUpdateTest {

  private static final String ACCESS_KEY = "AKIATESTACCESS";
  private static final String SECRET_KEY = "secret-s3-key";

  @Test
  void bucketOnlyBodyIsOnlyBucket() {
    assertTrue(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(bucketOnly("new-bucket")));
    assertFalse(PSDatabaseDeliverySchemaUpdate.isSchemaOnly(bucketOnly("new-bucket")));
    assertFalse(PSDatabaseDeliveryHostUpdate.isHostOnly(bucketOnly("new-bucket")));
    assertFalse(PSDatabaseDeliveryOwnerUpdate.isOwnerOnly(bucketOnly("new-bucket")));
    assertFalse(PSFtpDeliveryHostUpdate.isHostOnly(bucketOnly("new-bucket")));
    assertFalse(PSLocalDeliveryFolderUpdate.isFolderOnly(bucketOnly("new-bucket")));
  }

  @Test
  void namedRegionOrSecretsAreNotBucketOnly() {
    PSPublishServerInfo named = bucketOnly("new-bucket");
    named.setServerName("s3-prod");
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(named));

    PSPublishServerInfo regionOnly = new PSPublishServerInfo();
    regionOnly.setProperties(List.of(property(IPSPubServerDao.PUBLISH_EC2_REGION, "us-east-1")));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(regionOnly));

    PSPublishServerInfo withSecret = bucketOnly("new-bucket");
    List<PSPublishServerProperty> props = new ArrayList<>(withSecret.getProperties());
    props.add(property(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, SECRET_KEY));
    withSecret.setProperties(props);
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(withSecret));

    PSPublishServerInfo withAccess = bucketOnly("new-bucket");
    List<PSPublishServerProperty> accessProps = new ArrayList<>(withAccess.getProperties());
    accessProps.add(property(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY));
    withAccess.setProperties(accessProps);
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(withAccess));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isBucketOnly(null));
  }

  @Test
  void applyReplacesBucketAndLeavesRegionSecretsNameDriverAndDefault() throws Exception {
    PSPubServer server = s3Server("old-bucket");
    PSAmazonS3DeliveryBucketUpdate.apply(server, bucketOnly("  new-bucket  "));
    assertEquals("new-bucket", value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals("us-west-2", value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
    assertEquals(SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
    assertEquals("AMAZONS3", value(server, IPSPubServerDao.PUBLISH_DRIVER_PROPERTY));
    assertEquals("s3-prod", server.getName());
    assertEquals(5, server.getProperties().size());
  }

  @Test
  void blankOrOverlongBucketDoesNotWrite() {
    PSPubServer blank = s3Server("old-bucket");
    PSValidationException blankError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryBucketUpdate.apply(blank, bucketOnly("   ")));
    assertTrue(blankError.getMessage().contains(PSAmazonS3DeliveryBucketUpdate.S3_BUCKET_REQUIRED));
    assertFalse(blankError.getMessage().contains(SECRET_KEY));
    assertFalse(blankError.getMessage().contains(ACCESS_KEY));
    assertUnchanged(blank, "old-bucket");

    PSPubServer overlong = s3Server("old-bucket");
    String tooLong = "b".repeat(PSAmazonS3DeliveryBucketUpdate.MAX_S3_BUCKET_LENGTH + 1);
    PSValidationException longError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryBucketUpdate.apply(overlong, bucketOnly(tooLong)));
    assertTrue(longError.getMessage().contains(PSAmazonS3DeliveryBucketUpdate.S3_BUCKET_TOO_LONG));
    assertFalse(longError.getMessage().contains(SECRET_KEY));
    assertUnchanged(overlong, "old-bucket");
  }

  @Test
  void localFtpAndDatabaseDriversDoNotChangeBucket() {
    PSPubServer local = new PSPubServer();
    local.setName("local-files");
    local.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    local.addProperty(IPSPubServerDao.PUBLISH_FOLDER_PROPERTY, "/kept/local");
    PSValidationException localError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryBucketUpdate.apply(local, bucketOnly("new-bucket")));
    assertTrue(localError.getMessage().contains(PSAmazonS3DeliveryBucketUpdate.S3_BUCKET_DRIVER));
    assertEquals("/kept/local", value(local, IPSPubServerDao.PUBLISH_FOLDER_PROPERTY));
    assertTrue(local.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY).isEmpty());

    PSPubServer ftp = new PSPubServer();
    ftp.setName("ftp-prod");
    ftp.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "FTP");
    ftp.addProperty(IPSPubServerDao.PUBLISH_SERVER_IP_PROPERTY, "old.example");
    PSValidationException ftpError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryBucketUpdate.apply(ftp, bucketOnly("new-bucket")));
    assertTrue(ftpError.getMessage().contains(PSAmazonS3DeliveryBucketUpdate.S3_BUCKET_DRIVER));
    assertTrue(ftp.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY).isEmpty());

    PSPubServer oracle = new PSPubServer();
    oracle.setName("ora-prod");
    oracle.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Oracle");
    oracle.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, "SYSTEM");
    PSValidationException oracleError =
        assertThrows(
            PSValidationException.class,
            () -> PSAmazonS3DeliveryBucketUpdate.apply(oracle, bucketOnly("new-bucket")));
    assertTrue(oracleError.getMessage().contains(PSAmazonS3DeliveryBucketUpdate.S3_BUCKET_DRIVER));
    assertEquals("SYSTEM", value(oracle, IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY));
  }

  @Test
  void s3DriverMatchIsCaseInsensitive() throws Exception {
    PSPubServer mixed = s3Server("old-bucket");
    mixed.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "amazons3");
    PSAmazonS3DeliveryBucketUpdate.apply(mixed, bucketOnly("alias-bucket"));
    assertEquals("alias-bucket", value(mixed, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals("us-west-2", value(mixed, IPSPubServerDao.PUBLISH_EC2_REGION));

    PSPubServer shortName = s3Server("old-bucket");
    shortName.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "S3");
    PSAmazonS3DeliveryBucketUpdate.apply(shortName, bucketOnly("s3-alias"));
    assertEquals("s3-alias", value(shortName, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals(SECRET_KEY, value(shortName, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
  }

  @Test
  void isS3DriverAcceptsAmazonS3AndS3() {
    assertTrue(PSAmazonS3DeliveryBucketUpdate.isS3Driver("AMAZONS3"));
    assertTrue(PSAmazonS3DeliveryBucketUpdate.isS3Driver(" amazons3 "));
    assertTrue(PSAmazonS3DeliveryBucketUpdate.isS3Driver("S3"));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isS3Driver("FTP"));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isS3Driver("Local"));
    assertFalse(PSAmazonS3DeliveryBucketUpdate.isS3Driver(null));
  }

  private static void assertUnchanged(PSPubServer server, String bucket) {
    assertEquals(bucket, value(server, IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY));
    assertEquals("us-west-2", value(server, IPSPubServerDao.PUBLISH_EC2_REGION));
    assertEquals(ACCESS_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY));
    assertEquals(SECRET_KEY, value(server, IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY));
  }

  private static PSPubServer s3Server(String bucket) {
    PSPubServer server = new PSPubServer();
    server.setName("s3-prod");
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "AMAZONS3");
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, bucket);
    server.addProperty(IPSPubServerDao.PUBLISH_EC2_REGION, "us-west-2");
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ACCESS_KEY);
    server.addProperty(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, SECRET_KEY);
    return server;
  }

  private static PSPublishServerInfo bucketOnly(String bucket) {
    PSPublishServerInfo info = new PSPublishServerInfo();
    info.setProperties(List.of(property(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, bucket)));
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
