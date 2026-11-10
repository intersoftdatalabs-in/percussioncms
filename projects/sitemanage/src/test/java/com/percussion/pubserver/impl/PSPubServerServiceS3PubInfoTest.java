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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.data.PSPubServerProperty;
import com.percussion.sitemanage.data.PSPubInfo;
import com.percussion.sitemanage.service.IPSSiteDataService.PublishType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behavior preservation for the {@code getS3PubInfo} complexity extraction (issue #5915). The
 * extracted helpers must decide exactly as the previous inline branches did.
 */
@ExtendWith(MockitoExtension.class)
class PSPubServerServiceS3PubInfoTest {

  @Test
  void s3PublishTypesMatchCaseInsensitively() {
    assertTrue(
        PSPubServerService.isS3PublishType(PublishType.amazon_s3.toString()));
    assertTrue(
        PSPubServerService.isS3PublishType(PublishType.amazon_s3_only.toString()));
    assertTrue(PSPubServerService.isS3PublishType("AMAZON_S3"));
    assertFalse(PSPubServerService.isS3PublishType(PublishType.ftp.toString()));
    assertFalse(PSPubServerService.isS3PublishType(null));
  }

  @Test
  void nonDefaultServerMatchesOnS3Driver() {
    assertTrue(
        PSPubServerService.matchesS3PubServer(
            PublishType.amazon_s3.toString(), PublishType.ftp.toString()));
    assertFalse(
        PSPubServerService.matchesS3PubServer(
            PublishType.ftp.toString(), PublishType.ftp.toString()));
  }

  @Test
  void nonDefaultServerMatchesWhenDefaultIsS3Only() {
    // Preserves the original loop predicate, whose amazon_s3_only leg tests the default
    // server publish type rather than the candidate driver.
    assertTrue(
        PSPubServerService.matchesS3PubServer(
            PublishType.ftp.toString(), PublishType.amazon_s3_only.toString()));
  }

  @Test
  void toS3PubInfoCopiesConfig() {
    PSPubServerService.S3PubConfig config = new PSPubServerService.S3PubConfig();
    config.bucket = "bucket";
    config.accessKey = "key";
    config.securityKey = "secret";
    config.region = "us-east-1";
    config.useAssumeRole = "true";
    config.arnRole = "arn";

    PSPubInfo info = PSPubServerService.toS3PubInfo(config);

    assertEquals("bucket", info.getBucketName());
    assertEquals("key", info.getAccessKey());
    assertEquals("secret", info.getSecretKey());
    assertEquals("us-east-1", info.getRegionName());
    assertEquals("true", info.getUseAssumeRole());
    assertEquals("arn", info.getArnRole());
  }

  @Test
  void defaultUseAssumeRoleIsFalse() {
    assertEquals("false", new PSPubServerService.S3PubConfig().useAssumeRole);
  }

  @Test
  void propertyValueReadsValueOrNull() {
    IPSPubServer pubServer = mock(IPSPubServer.class);
    PSPubServerProperty prop = new PSPubServerProperty();
    prop.setName("bucket");
    prop.setValue("my-bucket");
    when(pubServer.getProperty("bucket")).thenReturn(Optional.of(prop));
    when(pubServer.getProperty("missing")).thenReturn(Optional.empty());

    assertEquals(
        "my-bucket", PSPubServerService.getPubServerPropertyValue(pubServer, "bucket"));
    assertNull(PSPubServerService.getPubServerPropertyValue(pubServer, "missing"));
  }
}
