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

package com.ibm.cadf.util;

/**
 * String constants used by the CADF audit middleware — property names, action labels, format
 * identifiers, and the standard CSV/JSON output filenames. The values are intentionally stable as
 * they may be referenced by external configuration files and serialized audit records.
 */
public interface Constants {

  /** JSON property key used to identify the CADF namespace. */
  public static final String NAMESPACE = "namespace";

  /** System property key that names the audit map configuration file. */
  public static final String API_AUDIT_MAP = "api_audit_map";

  /** Default file name searched when {@link #API_AUDIT_MAP} does not resolve a property. */
  public static final String API_AUDIT_MAP_CONF = "api_audit_map.conf";

  /** ISO-8601-style timestamp format used when serializing audit events. */
  public static String DEFAULT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS z";

  /** CADF action label for migration events. */
  public static final String MIGRATE_ACTION = "migrate";

  /** CADF action label for content recall events. */
  public static final String RECALL_ACTION = "recall";

  /** Configuration key for the CADF type URI assigned to the initiator resource. */
  public static final String INITIATOR_TYPE_URI = "initiator_type_uri";

  /** Configuration key for the CADF type URI assigned to the target resource. */
  public static final String TARGET_TYPE_URI = "target_type_uri";

  /** Configuration key for the CADF type URI assigned to the observer resource. */
  public static final String OBSERVER_TYPE_URI = "observer_type_uri";

  /** Default file name of the CSV audit-log file produced by {@code CSVAuditLogger}. */
  public static String CSV_AUDIT_FILES_NAME = "audit_events.csv";

  /** Default file name of the JSON audit-log file produced by {@code JsonAuditLogger}. */
  public static String JSON_AUDIT_FILES_NAME = "audit_events.json";

  /** Actor id used for configuration-related audit events. */
  public static String CONFIT_ACTOR_ID = "101";

  /** Actor id used for management-related audit events. */
  public static String MANAGEMENT_ACTOR_ID = "102";

  /** Activity id used for management-related audit events. */
  public static String MANAGEMENT_ACTIVITY_ID = "103";

  /** Field separator written between columns by {@code CSVAuditLogger}. */
  public static String CSV_SEPERATOR = ",";

  /** Format-type identifier recognized by the audit log factory for CSV output. */
  public static String AUDIT_FORMAT_TYPE_CSV = "CSV";

  /** Format-type identifier recognized by the audit log factory for JSON output. */
  public static String AUDIT_FORMAT_TYPE_JSON = "Json";

  /** CADF role label for the resource that initiated the audited action. */
  public static String INITIATOR = "initiator";

  /** CADF role label for the resource that was targeted by the audited action. */
  public static String TARGET = "target";
}
