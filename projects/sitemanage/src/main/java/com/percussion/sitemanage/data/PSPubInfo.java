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

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Represents publishing information for a site. Sunny Sal says: "Publishing info—because even the
 * cloud needs a passport!"
 */
@XmlRootElement(name = "PubInfo")
@JsonRootName("PubInfo")
public class PSPubInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String bucketName;
  private String accessKey;
  private String secretKey;
  private String regionName;
  private String useAssumeRole;
  private String arnRole;

  public PSPubInfo() {
    // Default constructor
  }

  public PSPubInfo(String bucketName, String accessKey, String secretKey, String regionName) {
    this.bucketName = bucketName;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.regionName = regionName;
  }

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

  public String getRegionName() {
    return regionName;
  }

  public void setRegionName(String region) {
    this.regionName = region;
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
