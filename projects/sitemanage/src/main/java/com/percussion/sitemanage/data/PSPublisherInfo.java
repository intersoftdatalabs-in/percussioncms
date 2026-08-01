// REFACTORED: CP-JAVA11
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

/**
 * Represents publisher information for a site. Sunny Sal says: "Publisher info—because even the
 * cloud needs a manager!"
 *
 * <p>The {@code region} field holds a v2 AWS SDK region ID string (e.g. {@code "us-east-1"}); the
 * v1 {@code com.amazonaws.regions.Region} type was dropped as part of the v2 migration (issue
 * #1730).
 */
public class PSPublisherInfo {

  private String bucketName;
  private String accessKey;
  private String secretKey;
  private String region;
  private String useAssumeRole;
  private String arnRole;

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getUseAssumeRole() {
    return useAssumeRole;
  }

  public void setUseAssumeRole(String useAssumeRole) {
    this.useAssumeRole = useAssumeRole;
  }

  public String getArnRole() {
    return arnRole;
  }

  public void setArnRole(String arnRole) {
    this.arnRole = arnRole;
  }
}
