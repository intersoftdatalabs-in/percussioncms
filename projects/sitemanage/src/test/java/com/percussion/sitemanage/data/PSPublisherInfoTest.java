/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.sitemanage.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Bean-level test for {@link PSPublisherInfo} after the v1 {@code Region} -> {@code String}
 * migration.
 */
public class PSPublisherInfoTest {

  @Test
  public void gettersAndSetters_roundTrip() {
    var info = new PSPublisherInfo();
    info.setBucketName("my-bucket");
    info.setAccessKey("AKIA");
    info.setSecretKey("secret");
    info.setRegion("eu-west-1");
    info.setUseAssumeRole("true");
    info.setArnRole("arn:aws:iam::123:role/x");

    assertEquals("my-bucket", info.getBucketName());
    assertEquals("AKIA", info.getAccessKey());
    assertEquals("secret", info.getSecretKey());
    assertEquals("eu-west-1", info.getRegion());
    assertEquals("true", info.getUseAssumeRole());
    assertEquals("arn:aws:iam::123:role/x", info.getArnRole());
  }

  @Test
  public void defaults_allNull() {
    var info = new PSPublisherInfo();
    assertNull(info.getBucketName());
    assertNull(info.getAccessKey());
    assertNull(info.getSecretKey());
    assertNull(info.getRegion());
    assertNull(info.getUseAssumeRole());
    assertNull(info.getArnRole());
  }

  @Test
  public void setRegion_acceptsNull() {
    var info = new PSPublisherInfo();
    info.setRegion("us-east-1");
    assertEquals("us-east-1", info.getRegion());
    info.setRegion(null);
    assertNull(info.getRegion());
  }
}
