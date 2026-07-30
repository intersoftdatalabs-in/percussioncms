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

package com.ibm.cadf.auditlogger;

import com.ibm.cadf.auditlogger.csv.CSVAuditLogger;
import com.ibm.cadf.auditlogger.json.JsonAuditLogger;
import com.ibm.cadf.util.Constants;

/**
 * Static factory that returns the {@link AuditLogger} singleton appropriate for a given output
 * format. Supported format identifiers are {@link Constants#AUDIT_FORMAT_TYPE_CSV} and {@link
 * Constants#AUDIT_FORMAT_TYPE_JSON}; any other (or {@code null}) value falls back to the CSV
 * logger.
 */
public class AuditLoggerFactory {

  /** Default no-argument constructor for {@link AuditLoggerFactory}. */
  public AuditLoggerFactory() {
    // Default constructor for AuditLoggerFactory.
  }

  /**
   * Returns the singleton {@link AuditLogger} for the requested output format. The default file
   * format is CSV; known JSON requests return the JSON logger; any other value falls back to CSV.
   *
   * @param auditorType the audit format identifier (e.g., {@link Constants#AUDIT_FORMAT_TYPE_CSV}
   *     or {@link Constants#AUDIT_FORMAT_TYPE_JSON}); may be {@code null} or unrecognized, in which
   *     case CSV is used.
   * @return the shared audit logger for the resolved format, never {@code null}.
   */
  public static AuditLogger getAuditLogger(String auditorType) {

    if (auditorType.equals(Constants.AUDIT_FORMAT_TYPE_CSV)) {
      return CSVAuditLogger.getInstance();
    } else if (auditorType.equals(Constants.AUDIT_FORMAT_TYPE_JSON)) {
      return JsonAuditLogger.getInstance();
    }
    return CSVAuditLogger.getInstance();
  }
}
