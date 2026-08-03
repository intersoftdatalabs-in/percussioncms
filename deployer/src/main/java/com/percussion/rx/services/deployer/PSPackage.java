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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Objects;

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

  /**
   * Returns the package status.
   *
   * @return the package status, may be <code>null</code>.
   */
  public String getPackageStatus() {
    return packageStatus;
  }

  /**
   * Sets the package status.
   *
   * @param status the package status, may be <code>null</code>.
   */
  public void setPackageStatus(String status) {
    this.packageStatus = status;
  }

  /**
   * Returns the config status.
   *
   * @return the config status, may be <code>null</code>.
   */
  public String getConfigStatus() {
    return configStatus;
  }

  /**
   * Sets the config status.
   *
   * @param status the config status, may be <code>null</code>.
   */
  public void setConfigStatus(String status) {
    this.configStatus = status;
  }

  /**
   * Returns the package name.
   *
   * @return the package name, may be <code>null</code>.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the package name.
   *
   * @param name the package name, may be <code>null</code>.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the publisher.
   *
   * @return the publisher, may be <code>null</code>.
   */
  public String getPublisher() {
    return publisher;
  }

  /**
   * Sets the publisher.
   *
   * @param publisher the publisher, may be <code>null</code>.
   */
  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  /**
   * Returns the version.
   *
   * @return the version, may be <code>null</code>.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version, may be <code>null</code>.
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns the description.
   *
   * @return the description, may be <code>null</code>.
   */
  public String getDesc() {
    return desc;
  }

  /**
   * Sets the description.
   *
   * @param desc the description, may be <code>null</code>.
   */
  public void setDesc(String desc) {
    this.desc = desc;
  }

  /**
   * Returns the install date.
   *
   * @return the install date, may be <code>null</code>.
   */
  public Date getInstalldate() {
    return installdate;
  }

  /**
   * Sets the install date.
   *
   * @param installdate the install date, may be <code>null</code>.
   */
  public void setInstalldate(Date installdate) {
    this.installdate = installdate;
  }

  /**
   * Returns the installer.
   *
   * @return the installer, may be <code>null</code>.
   */
  public String getInstaller() {
    return installer;
  }

  /**
   * Sets the installer.
   *
   * @param installer the installer, may be <code>null</code>.
   */
  public void setInstaller(String installer) {
    this.installer = installer;
  }

  /**
   * Returns the category.
   *
   * @return the category, may be <code>null</code>.
   */
  public String getCategory() {
    return category;
  }

  /**
   * Sets the category.
   *
   * @param category the category, may be <code>null</code>.
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Returns the lock status.
   *
   * @return the lock status, may be <code>null</code>.
   */
  public String getLockStatus() {
    return lockStatus;
  }

  /**
   * Sets the lock status.
   *
   * @param lockStatus the lock status, may be <code>null</code>.
   */
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
