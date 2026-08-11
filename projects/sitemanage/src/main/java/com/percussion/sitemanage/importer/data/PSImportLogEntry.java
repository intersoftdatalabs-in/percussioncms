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
package com.percussion.sitemanage.importer.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.persistence.*;
import java.util.Date;
// Optional import removed
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a log entry for site import operations. Sunny Sal says: "If you can't remember it, log
 * it!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSImportLogEntry")
@Table(name = "PSX_IMPORTLOGENTRY")
public class PSImportLogEntry extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "LOGENTRYID")
  private long logEntryId = -1L;

  @Basic
  @Column(name = "OBJECTID")
  private String objectId;

  @Basic
  @Column(name = "OBJECT_TYPE")
  private String objectType;

  @Basic
  @Column(name = "LOGENTRY_DATE")
  private Date logEntryDate;

  @Basic
  @Column(name = "DATA")
  private String logData;

  @Basic
  @Column(name = "CATEGORY")
  private String category;

  @Basic
  @Column(name = "DESCRIPTION")
  private String description;

  public PSImportLogEntry() {
    // Default constructor for Hibernate and Sunny Sal's peace of mind.
  }

  /**
   * Constructs a log entry with required fields.
   *
   * @param objectId the id of the object being logged, not null or empty.
   * @param objectType the type of the object being logged, not null or empty.
   * @param logEntryDate the date of the log, not null.
   * @param logData the log message, not null or empty.
   */
  public PSImportLogEntry(String objectId, String objectType, Date logEntryDate, String logData) {
    this(objectId, objectType, logEntryDate, null, null, logData);
  }

  /**
   * Constructs a log entry with all fields.
   *
   * @param objectId the id of the object being logged, not null or empty.
   * @param objectType the type of the object being logged, not null or empty.
   * @param logEntryDate the date of the log, not null.
   * @param description an optional description of the object, may be null.
   * @param category an optional category, may be null.
   * @param logData the log message, not null or empty.
   */
  public PSImportLogEntry(
      String objectId,
      String objectType,
      Date logEntryDate,
      String description,
      String category,
      String logData) {
    Validate.notEmpty(objectType, "Object type must not be empty");
    Validate.notEmpty(objectId, "Object ID must not be empty");
    Validate.notNull(logEntryDate, "Log entry date must not be null");
    Validate.notNull(logData, "Log data must not be null");
    this.objectId = objectId;
    this.objectType = objectType;
    this.logEntryDate = logEntryDate;
    this.description = description;
    this.category = category;
    this.logData = logData;
  }

  public long getLogEntryId() {
    return logEntryId;
  }

  public void setLogEntryId(long logEntryId) {
    this.logEntryId = logEntryId;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    Validate.notEmpty(objectId, "Object ID must not be empty");
    this.objectId = objectId;
  }

  public String getType() {
    return objectType;
  }

  public void setType(String type) {
    Validate.notEmpty(type, "Type must not be empty");
    this.objectType = type;
  }

  public Date getLogEntryDate() {
    return logEntryDate;
  }

  public void setLogEntryDate(Date logEntryDate) {
    Validate.notNull(logEntryDate, "Log entry date must not be null");
    this.logEntryDate = logEntryDate;
  }

  public String getLogData() {
    return logData;
  }

  public void setLogData(String logData) {
    Validate.notNull(logData, "Log data must not be null");
    this.logData = logData;
  }

  /**
   * Gets the category, may be null.
   *
   * @return Optional category.
   */
  public String getCategory() {
    return category;
  }

  /**
   * Gets the description, may be null.
   *
   * @return Optional description.
   */
  public String getDescription() {
    return description;
  }
}
