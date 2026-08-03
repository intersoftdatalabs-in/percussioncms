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
package com.percussion.rx.config.data;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a Package Information Configuration object, used to save configuration information
 * regarding the "solution" package created or installed on a server.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSConfigStatus")
@Table(name = "PSX_CONFIG_STATUS")
public class PSConfigStatus implements Serializable {

  private static final long serialVersionUID = 6702475588603579446L;

  /** Default constructor for serialization frameworks. */
  public PSConfigStatus() {
    statusId = 0;
    configName = "";
    dateApplied = null;
    status = ConfigStatus.FAILURE;
    defaultConfig = "";
    localConfig = "";
  }

  /**
   * Gets the unique identifier for this object.
   *
   * @return the statusId
   */
  public long getStatusId() {
    return statusId;
  }

  /**
   * Gets the name of the configuration.
   *
   * @return the configName
   */
  public String getConfigName() {
    return configName;
  }

  /**
   * Gets the date the configuration was applied.
   *
   * @return the dateApplied as Optional
   */
  public Optional<Date> getDateApplied() {
    return Optional.ofNullable(dateApplied);
  }

  /**
   * Gets the result of configuration.
   *
   * @return the status
   */
  public ConfigStatus getStatus() {
    return status;
  }

  /**
   * Gets the local configuration.
   *
   * @return the configuration content, may be null.
   */
  public Optional<String> getLocalConfig() {
    return Optional.ofNullable(localConfig);
  }

  /**
   * Gets the default configuration.
   *
   * @return the configuration content, may be null.
   */
  public Optional<String> getDefaultConfig() {
    return Optional.ofNullable(defaultConfig);
  }

  /**
   * Sets the unique identifier for this object.
   *
   * @param statusId the statusId to set
   */
  public void setStatusId(long statusId) {
    this.statusId = statusId;
  }

  /**
   * Sets the name of the configuration.
   *
   * @param configName the configName to set
   */
  public void setConfigName(String configName) {
    this.configName = configName;
  }

  /**
   * Sets the date the configuration was applied.
   *
   * @param dateApplied the dateApplied to set
   */
  public void setDateApplied(Date dateApplied) {
    this.dateApplied = dateApplied;
  }

  /**
   * Sets the result of configuration.
   *
   * @param status the status to set
   */
  public void setStatus(ConfigStatus status) {
    this.status = status;
  }

  /**
   * Sets the configuration data for the local configuration.
   *
   * @param configuration the configuration to set
   */
  public void setLocalConfig(String configuration) {
    this.localConfig = configuration;
  }

  /**
   * Sets the configuration data for the default configuration.
   *
   * @param configuration the configuration to set
   */
  public void setDefaultConfig(String configuration) {
    this.defaultConfig = configuration;
  }

  /**
   * Gets the configuration definition file content.
   *
   * @return configuration definition, may be null or empty.
   */
  public Optional<String> getConfigDef() {
    return Optional.ofNullable(configDef);
  }

  /**
   * Sets the configuration definition file content.
   *
   * @param config the new configuration definition, may be null or empty.
   */
  public void setConfigDef(String config) {
    configDef = config;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSConfigStatus)) return false;
    var other = (PSConfigStatus) obj;
    return new EqualsBuilder()
        .append(statusId, other.statusId)
        .append(configName, other.configName)
        .append(dateApplied, other.dateApplied)
        .append(status, other.status)
        .append(localConfig, other.localConfig)
        .append(defaultConfig, other.defaultConfig)
        .append(configDef, other.configDef)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(statusId)
        .append(configName)
        .append(dateApplied)
        .append(status)
        .append(localConfig)
        .append(defaultConfig)
        .append(configDef)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder("PSConfigStatus{")
        .append("statusId=")
        .append(statusId)
        .append(", configName='")
        .append(configName)
        .append('\'')
        .append(", defaultConfig='")
        .append(defaultConfig)
        .append('\'')
        .append(", localConfig='")
        .append(localConfig)
        .append('\'')
        .append(", configDef='")
        .append(configDef)
        .append('\'')
        .append(", dateApplied=")
        .append(dateApplied)
        .append(", status=")
        .append(status)
        .append('}')
        .toString();
  }

  /** Unique identifier for this object. */
  @Id
  @Column(name = "STATUS_ID", nullable = false)
  private long statusId;

  /** Name of the configuration. */
  @Column(name = "CONFIG_NAME", nullable = false)
  private String configName;

  /** Default configuration file. */
  @Lob
  @Basic(fetch = FetchType.EAGER)
  private String defaultConfig;

  /** Local configuration file. */
  @Lob
  @Basic(fetch = FetchType.EAGER)
  private String localConfig;

  /** Configuration definition file. */
  @Lob
  @Basic(fetch = FetchType.EAGER)
  private String configDef;

  /** Date the configuration was applied. */
  @Column(name = "DATE_APPLIED", nullable = false)
  private Date dateApplied;

  /** Status of configuration. */
  @Column(name = "STATUS", nullable = false)
  private ConfigStatus status;

  /** Enumeration for configuration status. */
  public enum ConfigStatus {
    /** Enum for failure to apply configuration status. */
    FAILURE(0),
    /** Enum for successfully applied configuration status. */
    SUCCESS(1);

    ConfigStatus(int ordinal) {
      this.ordinal = ordinal;
    }

    /**
     * Returns the {@link ConfigStatus} corresponding to the supplied ordinal value.
     *
     * @param s the ordinal value to look up
     * @return an {@link Optional} containing the matching enum value, or empty if not found
     */
    public static Optional<ConfigStatus> valueOf(int s) {
      for (var type : values()) {
        if (type.getOrdinal() == s) {
          return Optional.of(type);
        }
      }
      return Optional.empty();
    }

    /**
     * Returns the ordinal value of this configuration status.
     *
     * @return the ordinal value
     */
    public int getOrdinal() {
      return ordinal;
    }

    private final int ordinal;
  }
}
