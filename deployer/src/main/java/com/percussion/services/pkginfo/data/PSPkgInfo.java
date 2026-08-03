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

package com.percussion.services.pkginfo.data;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a package information object, used to save information regarding the "solution"
 * package created or installed on a server. Sunny Sal says: "Package info—because every package has
 * a story!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPkgInfo")
@Table(name = "PSX_PKG_INFO")
public class PSPkgInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * The types of actions that can be taken with the associated package.
   *
   * <p>Implementation note - the enum names are stored in persistent storage, so they cannot be
   * changed.
   */
  public enum PackageAction {
    /** A package is installed or a descriptor is saved. */
    INSTALL_CREATE,

    /**
     * A package is uninstalled, meaning all the design objects in the package are removed from the
     * system (unless they have content dependencies.) If a descriptor is removed, the pkgInfo entry
     * is removed from persistent storage.
     */
    UNINSTALL
  }

  /**
   * The results of the actions taken with the associated package.
   *
   * <p>Implementation note - the enum names are stored in persistent storage, so they cannot be
   * changed.
   */
  public enum PackageActionStatus {
    /** The action completed with no errors. */
    SUCCESS,

    /** An exception occurred during action processing. */
    FAIL
  }

  /**
   * The different categories of packages that can exist on a system.
   *
   * <p>Implementation note - the enum names are stored in persistent storage, so they cannot be
   * changed.
   */
  public enum PackageType {
    /**
     * The entry identifies a package descriptor. There may or may not be an actual package. In any
     * case, this entry only describes the descriptor.
     */
    DESCRIPTOR,

    /** This entry identifies a package that has been installed or uninstalled on this server. */
    PACKAGE
  }

  /**
   * The different types of packages that can exist on a system.
   *
   * <p>Implementation note - the enum names are stored in persistent storage, so they cannot be
   * changed.
   */
  public enum PackageCategory {
    /** The entry identifies a system package. Packages of this type cannot be uninstalled. */
    SYSTEM,

    /** The entry identifies a user package. Packages of this type can be uninstalled. */
    USER
  }

  /** No-arg constructor required by JPA. */
  public PSPkgInfo() {
    guid = 0;
    descriptorName = "";
    descriptorGuid = 0;
    publisherName = "";
    publisherUrl = "";
    description = "";
    pkgVersion = "";
    shippedConfigDefinition = "";
    lastActionDate = new Date(0);
    origConfigDate = new Date(0);
    lastActionByUser = "";
    type = PackageType.DESCRIPTOR.name() + "_true";
    lastAction = PackageAction.INSTALL_CREATE.name();
    lastActionStatus = PackageActionStatus.FAIL.name();
    cmVersionMinimum = "";
    cmVersionMaximum = "";
    category = PackageCategory.USER.name();
  }

  /**
   * Gets the unique identifier for this object.
   *
   * @return The GUID for the package info object.
   */
  public IPSGuid getGuid() {
    return new PSGuid(PSTypeEnum.PACKAGE_INFO, guid);
  }

  /**
   * Gets the package descriptor name of this package.
   *
   * @return The package descriptor name.
   */
  public String getPackageDescriptorName() {
    return descriptorName;
  }

  /**
   * Gets the package descriptor's GUID.
   *
   * @return The GUID for the package's descriptor.
   */
  public IPSGuid getPackageDescriptorGuid() {
    return new PSGuid(PSTypeEnum.DEPLOYER_DESCRIPTOR_ID, descriptorGuid);
  }

  /**
   * Gets the name of the package's publisher.
   *
   * @return The name of the package publisher.
   */
  public String getPublisherName() {
    return publisherName;
  }

  /**
   * Gets the URL of the package publisher.
   *
   * @return The URL of the package publisher.
   */
  public String getPublisherUrl() {
    return publisherUrl;
  }

  /**
   * Gets the description of the package.
   *
   * @return The description of the package.
   */
  public String getPackageDescription() {
    return description;
  }

  /**
   * Gets the version of the package.
   *
   * @return The version of the package.
   */
  public String getPackageVersion() {
    return pkgVersion;
  }

  /**
   * Gets the configuration definition shipped with the package.
   *
   * @return The configuration definition of the package.
   */
  public String getShippedConfigDefinition() {
    return shippedConfigDefinition;
  }

  /**
   * Gets the installation date of the package.
   *
   * @return The installation date of the package.
   */
  public Date getLastActionDate() {
    return lastActionDate;
  }

  /**
   * Gets the original configuration date of the package.
   *
   * @return The original configuration date of the package.
   */
  public Date getOriginalConfigDate() {
    return origConfigDate;
  }

  /**
   * Gets the last action performed on the package.
   *
   * @return the last package action, never <code>null</code>.
   */
  public PackageAction getLastAction() {
    return PackageAction.valueOf(lastAction);
  }

  /**
   * Gets the status of the last action performed on the package.
   *
   * @return the last package action status, never <code>null</code>.
   */
  public PackageActionStatus getLastActionStatus() {
    return PackageActionStatus.valueOf(lastActionStatus);
  }

  /**
   * Gets the name of the installer of the package.
   *
   * @return The installer of the package.
   */
  public String getLastActionByUser() {
    return lastActionByUser;
  }

  /**
   * Returns true if the associated package is of the DESCRIPTOR type.
   *
   * @return true if the package is a descriptor, false otherwise.
   */
  public boolean isCreated() {
    return getTypeValue(true).equals(PackageType.DESCRIPTOR.name());
  }

  /**
   * Returns true if the package is editable.
   *
   * @return true if the package is editable, false otherwise.
   */
  public boolean isEditable() {
    return Boolean.valueOf(getTypeValue(false));
  }

  /**
   * Returns true if the associated package is of the SYSTEM category.
   *
   * @return true if the package is a system package, false otherwise.
   */
  public boolean isSystem() {
    return category.equals(PackageCategory.SYSTEM.name());
  }

  /**
   * Returns true if this package type is PACKAGE and the last action is INSTALL_CREATE and the last
   * action status is SUCCESS.
   *
   * @return true if the package is successfully installed, false otherwise.
   */
  public boolean isSuccessfullyInstalled() {
    return getTypeValue(true).equals(PackageType.PACKAGE.name())
        && lastAction.equals(PackageAction.INSTALL_CREATE.name())
        && lastActionStatus.equals(PackageActionStatus.SUCCESS.name());
  }

  /**
   * Gets the type of the package.
   *
   * @return The type of the package. Either DESCRIPTOR or PACKAGE.
   */
  public PackageType getType() {
    return PackageType.valueOf(getTypeValue(true));
  }

  /**
   * Gets the category of the package.
   *
   * @return The category of the package. Either SYSTEM or USER.
   */
  public PackageCategory getCategory() {
    return PackageCategory.valueOf(category);
  }

  /**
   * Gets the content management version minimum of this package.
   *
   * @return The content management version minimum of the package.
   */
  public String getCmVersionMinimum() {
    return cmVersionMinimum;
  }

  /**
   * Gets the content management version maximum of this package.
   *
   * @return The content management version maximum of the package.
   */
  public String getCmVersionMaximum() {
    return cmVersionMaximum;
  }

  // ------------------------------------------------------------------------------

  /**
   * Sets the package info GUID. See {@link #getGuid()}.
   *
   * @param theGuid The GUID, must not be null.
   */
  public void setGuid(IPSGuid theGuid) {
    if (theGuid == null) {
      throw new IllegalArgumentException("Guid may not be null");
    }
    guid = theGuid.longValue();
  }

  /**
   * Sets the package descriptor name of the package. This is base name without the extension. See
   * {@link #getPackageDescriptorName()}.
   *
   * @param thePackageDescriptorName The name of the package descriptor, must not be null.
   */
  public void setPackageDescriptorName(String thePackageDescriptorName) {
    if (thePackageDescriptorName == null) {
      throw new IllegalArgumentException("Package Name may not be null");
    }
    descriptorName = thePackageDescriptorName;
  }

  /**
   * Sets the package descriptor GUID. See {@link #getPackageDescriptorGuid()}.
   *
   * @param guid The GUID, must not be null.
   */
  public void setPackageDescriptorGuid(IPSGuid guid) {
    if (guid == null) {
      throw new IllegalArgumentException("Descriptor Guid may not be null");
    }
    descriptorGuid = guid.longValue();
  }

  /**
   * Sets the name of the package publisher. See {@link #getPublisherName()}.
   *
   * @param thePublisherName The name of the package's publisher, must not be null.
   */
  public void setPublisherName(String thePublisherName) {
    if (thePublisherName == null) {
      throw new IllegalArgumentException("Publisher Name may not be null");
    }
    publisherName = thePublisherName;
  }

  /**
   * Sets the URL of the package publisher. See {@link #getPublisherUrl()}.
   *
   * @param thePublisherUrl The URL of the package publisher, must not be null.
   */
  public void setPublisherUrl(String thePublisherUrl) {
    if (thePublisherUrl == null) {
      throw new IllegalArgumentException("Publisher URL may not be null");
    }
    publisherUrl = thePublisherUrl;
  }

  /**
   * Sets the description of the package. See {@link #getPackageDescription()}.
   *
   * @param thePackageDescription The description of the package, must not be null.
   */
  public void setPackageDescription(String thePackageDescription) {
    if (thePackageDescription == null) {
      throw new IllegalArgumentException("Package description may not be null");
    }
    description = thePackageDescription;
  }

  /**
   * Sets the version of the package. See {@link #getPackageVersion()}.
   *
   * @param thePackageVersion The version of the package, must not be null.
   */
  public void setPackageVersion(String thePackageVersion) {
    if (thePackageVersion == null) {
      throw new IllegalArgumentException("Package version may not be null");
    }
    pkgVersion = thePackageVersion;
  }

  /**
   * Sets the configuration definition to be shipped with the package. See {@link
   * #getShippedConfigDefinition()}.
   *
   * @param theShippedConfigDefinition The configuration definition to be shipped with the package,
   *     must not be null.
   */
  public void setShippedConfigDefinition(String theShippedConfigDefinition) {
    if (theShippedConfigDefinition == null) {
      throw new IllegalArgumentException("The Configuration Definition may not be null");
    }
    shippedConfigDefinition = theShippedConfigDefinition;
  }

  /**
   * Sets the date when the action was done. See {@link #getLastActionDate()}.
   *
   * @param actionDate The installation date of the package, must not be null.
   */
  public void setLastActionDate(Date actionDate) {
    if (actionDate == null) {
      throw new IllegalArgumentException("The Installation Date may not be null");
    }
    // eliminate milliseconds.
    var dateTime = actionDate.getTime();
    dateTime = dateTime - (dateTime % 1000);
    lastActionDate.setTime(dateTime);
  }

  /**
   * Sets the original configuration date of the package. See {@link #getOriginalConfigDate()}.
   *
   * @param theOriginalConfigurationDate The original configuration date of the package, must not be
   *     null.
   */
  public void setOriginalConfigDate(Date theOriginalConfigurationDate) {
    if (theOriginalConfigurationDate == null) {
      throw new IllegalArgumentException("The Original Configuration Date may not be null");
    }
    var dateTime = theOriginalConfigurationDate.getTime();
    dateTime = dateTime - (dateTime % 1000);
    origConfigDate.setTime(dateTime);
  }

  /**
   * Sets the last action performed on the package.
   *
   * @param action the last package action, may not be <code>null</code>.
   */
  public void setLastAction(PackageAction action) {
    lastAction = action.name();
  }

  /**
   * Sets the status of the last action performed on the package.
   *
   * @param status the last package action status, may not be <code>null</code>.
   */
  public void setLastActionStatus(PackageActionStatus status) {
    lastActionStatus = status.name();
  }

  /**
   * Sets the name of the user that performed the last action on this package. See {@link
   * #getLastActionByUser()}.
   *
   * @param user The installer of the package, must not be null.
   */
  public void setLastActionByUser(String user) {
    if (user == null) {
      throw new IllegalArgumentException("The Installer may not be null");
    }
    lastActionByUser = user;
  }

  /**
   * Sets the type of the package. See {@link #getType()}.
   *
   * @param thePackageType The type of the package, must not be null.
   */
  public void setType(PackageType thePackageType) {
    if (thePackageType == null) {
      throw new IllegalArgumentException("Package type may not be null");
    }
    updateType(true, thePackageType.name());
  }

  /**
   * Updates the type field with the type and editable values.
   *
   * @param isFirst type if true or editable if false
   * @param value String value to save
   */
  private void updateType(boolean isFirst, String value) {
    var items = StringUtils.split(type, "_");
    if (isFirst) {
      items[0] = value;
    } else {
      items[1] = value;
    }
    type = StringUtils.join(items, "_");
  }

  /**
   * Parses type field and gets type or editable value.
   *
   * @param isFirst type if true or editable if false
   * @return String value of field
   */
  private String getTypeValue(boolean isFirst) {
    var items = StringUtils.split(type, "_");
    if (isFirst) {
      return items[0];
    }
    if (items.length < 2) {
      return "false";
    }
    return items[1];
  }

  /**
   * Sets the type to Package and sets if editable or not.
   *
   * @param editmode true if editable, false otherwise.
   */
  public void setEditable(boolean editmode) {
    updateType(false, Boolean.toString(editmode));
  }

  /**
   * Sets the category of the package. See {@link #getCategory()}.
   *
   * @param thePackageCat The category of the package, must not be null.
   */
  public void setCategory(PackageCategory thePackageCat) {
    if (thePackageCat == null) {
      throw new IllegalArgumentException("Package category may not be null");
    }
    category = thePackageCat.name();
  }

  /**
   * Sets the content management version minimum of the package. See {@link #getCmVersionMinimum()}.
   *
   * @param theCmVersionMinimum The content management version minimum of the package, must not be
   *     null.
   */
  public void setCmVersionMinimum(String theCmVersionMinimum) {
    if (theCmVersionMinimum == null) {
      throw new IllegalArgumentException("Package Name may not be null");
    }
    cmVersionMinimum = theCmVersionMinimum;
  }

  /**
   * Sets the content management version maximum of the package. See {@link #getCmVersionMaximum()}.
   *
   * @param theCmVersionMaximum The content management version maximum of the package, must not be
   *     null.
   */
  public void setCmVersionMaximum(String theCmVersionMaximum) {
    if (theCmVersionMaximum == null) {
      throw new IllegalArgumentException("Package Name may not be null");
    }
    cmVersionMaximum = theCmVersionMaximum;
  }

  @Override
  public boolean equals(Object b) {
    if (!(b instanceof PSPkgInfo)) {
      return false;
    }
    var second = (PSPkgInfo) b;
    return new EqualsBuilder()
        .append(guid, second.guid)
        .append(descriptorName, second.descriptorName)
        .append(descriptorGuid, second.descriptorGuid)
        .append(publisherName, second.publisherName)
        .append(publisherUrl, second.publisherUrl)
        .append(description, second.description)
        .append(pkgVersion, second.pkgVersion)
        .append(shippedConfigDefinition, second.shippedConfigDefinition)
        .append(origConfigDate, second.origConfigDate)
        .append(lastAction, second.lastAction)
        .append(lastActionDate, second.lastActionDate)
        .append(lastActionByUser, second.lastActionByUser)
        .append(lastActionStatus, second.lastActionStatus)
        .append(type, second.type)
        .append(cmVersionMinimum, second.cmVersionMinimum)
        .append(cmVersionMaximum, second.cmVersionMaximum)
        .append(category, second.category)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(guid)
        .append(descriptorName)
        .append(descriptorGuid)
        .append(publisherName)
        .append(publisherUrl)
        .append(description)
        .append(pkgVersion)
        .append(shippedConfigDefinition)
        .append(origConfigDate)
        .append(lastAction)
        .append(lastActionDate)
        .append(lastActionByUser)
        .append(lastActionStatus)
        .append(type)
        .append(cmVersionMinimum)
        .append(cmVersionMaximum)
        .append(category)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "PSPkgInfo{"
        + "guid="
        + guid
        + ", descriptorName='"
        + descriptorName
        + '\''
        + ", descriptorGuid="
        + descriptorGuid
        + ", publisherName='"
        + publisherName
        + '\''
        + ", publisherUrl='"
        + publisherUrl
        + '\''
        + ", description='"
        + description
        + '\''
        + ", pkgVersion='"
        + pkgVersion
        + '\''
        + ", shippedConfigDefinition='"
        + shippedConfigDefinition
        + '\''
        + ", lastActionDate="
        + lastActionDate
        + ", origConfigDate="
        + origConfigDate
        + ", lastActionByUser='"
        + lastActionByUser
        + '\''
        + ", lastAction='"
        + lastAction
        + '\''
        + ", lastActionStatus='"
        + lastActionStatus
        + '\''
        + ", type='"
        + type
        + '\''
        + ", category='"
        + category
        + '\''
        + ", cmVersionMinimum='"
        + cmVersionMinimum
        + '\''
        + ", cmVersionMaximum='"
        + cmVersionMaximum
        + '\''
        + '}';
  }

  // ------------------------------------------------------------------------------

  /** Unique Identifier for this object. */
  @Id
  @Column(name = "GUID", nullable = false)
  private long guid;

  /** Package Descriptor Name - base name without extension. */
  @Column(name = "DESCRIPTOR_NAME", nullable = false)
  private String descriptorName;

  /** Unique Identifier for the Descriptor. */
  @Column(name = "DESCRIPTOR_GUID", nullable = false)
  private long descriptorGuid;

  /** The name of the package publisher. */
  @Column(name = "PUBLISHER_NAME", nullable = false)
  private String publisherName;

  /** The URL of the package publisher. */
  @Column(name = "PUBLISHER_URL", nullable = true)
  private String publisherUrl;

  /** Description of the package. */
  @Column(name = "DESCRIPTION", nullable = true)
  private String description;

  /** Version of the package. */
  @Column(name = "PKG_VERSION", nullable = false)
  private String pkgVersion;

  /** The configuration definition shipped with the package. */
  @Column(name = "SHIPPED_CONFIG_DEF", nullable = true)
  private String shippedConfigDefinition;

  /** Installation date. */
  @Column(name = "LAST_ACTION_DATE", nullable = false)
  private Date lastActionDate;

  /** Original configuration date. */
  @Column(name = "CONFIG_DATE_ORIG", nullable = false)
  private Date origConfigDate;

  /** Name of installer. */
  @Column(name = "LAST_ACTION_BY_USER", nullable = false)
  private String lastActionByUser;

  /** The last action performed on the package. */
  @Column(name = "LAST_ACTION", nullable = false)
  private String lastAction;

  /** The status of the last action performed on the package. */
  @Column(name = "LAST_ACTION_STATUS", nullable = false)
  private String lastActionStatus;

  /**
   * Type - field is split. First param type is "Package or Descriptor" Second is boolean if Package
   * is Editable
   */
  @Column(name = "TYPE", nullable = false)
  private String type;

  /** Category - "System" or "User". */
  @Column(name = "CATEGORY", nullable = false)
  private String category;

  /** Content Management Version Minimum - minimum version of Rx required for this package. */
  @Column(name = "CM_VERSION_MIN", nullable = true)
  private String cmVersionMinimum;

  /** Content Management Version Maximum - maximum version of Rx allowed for this package. */
  @Column(name = "CM_VERSION_MAX", nullable = true)
  private String cmVersionMaximum;
}
