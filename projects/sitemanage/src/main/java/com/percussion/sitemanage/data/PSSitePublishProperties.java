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

import com.percussion.share.data.PSAbstractPersistantObject;
import com.percussion.sitemanage.service.IPSSiteDataService.PublishType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Contains the publishing information for a site.
 *
 * @author radharanisonnathi
 */
@XmlRootElement(name = "SitePublishProperties")
public class PSSitePublishProperties extends PSAbstractPersistantObject {
  private static final long serialVersionUID = 1L;

  private String id;
  private String deliveryRootPath;
  private String ftpServerName;
  private String ftpPassword;
  private String privateKey;
  private Integer ftpServerPort;
  private String ftpUserName;
  private String siteName;
  private PublishType publishType;
  private Boolean secure;

  @XmlElement
  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getDeliveryRootPath() {
    return Optional.ofNullable(deliveryRootPath);
  }

  public void setDeliveryRootPath(String deliveryRootPath) {
    this.deliveryRootPath = deliveryRootPath;
  }

  public Optional<String> getFtpServerName() {
    return Optional.ofNullable(ftpServerName);
  }

  public void setFtpServerName(String ftpServerName) {
    this.ftpServerName = ftpServerName;
  }

  public Optional<String> getFtpPassword() {
    return Optional.ofNullable(ftpPassword);
  }

  public void setFtpPassword(String ftpPassword) {
    this.ftpPassword = ftpPassword;
  }

  public Optional<String> getPrivateKey() {
    return Optional.ofNullable(privateKey);
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public Optional<Integer> getFtpServerPort() {
    return Optional.ofNullable(ftpServerPort);
  }

  public void setFtpServerPort(Integer ftpServerPort) {
    this.ftpServerPort = ftpServerPort;
  }

  public Optional<String> getFtpUserName() {
    return Optional.ofNullable(ftpUserName);
  }

  public void setFtpUserName(String ftpUserName) {
    this.ftpUserName = ftpUserName;
  }

  public Optional<String> getSiteName() {
    return Optional.ofNullable(siteName);
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public Optional<PublishType> getPublishType() {
    return Optional.ofNullable(publishType);
  }

  public void setPublishType(PublishType publishType) {
    this.publishType = publishType;
  }

  public Optional<Boolean> getSecure() {
    return Optional.ofNullable(secure);
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }
}
