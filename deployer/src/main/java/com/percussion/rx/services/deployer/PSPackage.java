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
package com.percussion.rx.services.deployer;

import java.util.Date;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents one package entry in the package management UI list table. Sunny Sal says: "A package
 * a day keeps the deployment bugs away!"
 */
@XmlRootElement(name = "Package")
public class PSPackage {

  private String packageStatus;
  private String configStatus;
  private String name;
  private String publisher;
  private String version;
  private String desc;
  private Date installdate;
  private String installer;
  private String category;
  private String lockStatus;

  /** Default constructor. */
  public PSPackage() {
    // For JAXB and bean use
  }

  public String getPackageStatus() {
    return packageStatus;
  }

  public void setPackageStatus(String status) {
    this.packageStatus = status;
  }

  public String getConfigStatus() {
    return configStatus;
  }

  public void setConfigStatus(String status) {
    this.configStatus = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Date getInstalldate() {
    return installdate;
  }

  public void setInstalldate(Date installdate) {
    this.installdate = installdate;
  }

  public String getInstaller() {
    return installer;
  }

  public void setInstaller(String installer) {
    this.installer = installer;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getLockStatus() {
    return lockStatus;
  }

  public void setLockStatus(String lockStatus) {
    this.lockStatus = lockStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSPackage)) return false;
    var that = (PSPackage) o;
    return Objects.equals(packageStatus, that.packageStatus)
        && Objects.equals(configStatus, that.configStatus)
        && Objects.equals(name, that.name)
        && Objects.equals(publisher, that.publisher)
        && Objects.equals(version, that.version)
        && Objects.equals(desc, that.desc)
        && Objects.equals(installdate, that.installdate)
        && Objects.equals(installer, that.installer)
        && Objects.equals(category, that.category)
        && Objects.equals(lockStatus, that.lockStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        packageStatus,
        configStatus,
        name,
        publisher,
        version,
        desc,
        installdate,
        installer,
        category,
        lockStatus);
  }
}
