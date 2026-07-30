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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import java.io.Serializable;

/**
 * Common ancestor for the CADF resource/event model types. Encapsulates the namespace constants and
 * the two canonical enums (event type and reporter role) referenced throughout the model.
 * Subclasses contribute the actual attribute set plus the {@link #isValid()} contract.
 */
public abstract class CADFType implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Default no-argument constructor for {@link CADFType}. */
  public CADFType() {}

  /** Prefix applied to every CADF identifier (resource, type, action, etc.). */
  public static final String CADF_SCHEMA_1_0_0 = "cadf:";

  /** Schema URL for CADF version 1.0.0, embedded in serialized audit events. */
  public static final String CADF_VERSION_1_0_0 = "http://schemas.dmtf.org/cloud/audit/1.0/";

  // Valid cadf:Event record "types"
  /** Enumerates the {@code typeURI} values recognized for CADF events. */
  public enum EVENTTYPE {
    /** A standard user/system-originated action — the default event category. */
    EVENTTYPE_ACTIVITY("activity"),
    /** A revocation event (e.g., permission or credential revocation). */
    EVENTTYPE_REVOKE("revoke"),
    /** An event emitted by a passive observer for monitoring purposes. */
    EVENTTYPE_MONITOR("monitor"),
    /** A control event such as configuration changes. */
    EVENTTYPE_CONTROL("control");

    /** The CADF wire-string value associated with this event type. */
    public String value;

    private EVENTTYPE(String value) {
      this.value = value;
    }
  }

  /**
   * Indicates whether the supplied string matches one of the canonical {@link EVENTTYPE} values.
   *
   * @param value the type identifier to test, may be {@code null}.
   * @return {@code true} when {@code value} matches a known event type, {@code false} otherwise.
   */
  public static boolean isValidEventType(String value) {
    for (EVENTTYPE event : EVENTTYPE.values()) {
      if (event.name().equals(value)) {
        return true;
      }
    }
    return false;
  }

  // Valid cadf:Event record "Reporter" roles

  /** Enumerates the CADF reporter roles that may originate an event. */
  public enum REPORTER_ROLES {
    /** The reporter merely observed the action and recorded it. */
    REPORTER_ROLE_OBSERVER("observer"),
    /** The reporter caused the action and recorded it. */
    REPORTER_ROLE_MODIFIER("modifier"),
    /** The reporter is forwarding an event originating from another source. */
    REPORTER_ROLE_RELAY("relay");

    /** The CADF wire-string value associated with this reporter role. */
    String value;

    private REPORTER_ROLES(String value) {
      this.value = value;
    }
  }

  /**
   * Indicates whether the supplied string matches one of the canonical {@link REPORTER_ROLES}
   * values.
   *
   * @param value the role identifier to test, may be {@code null}.
   * @return {@code true} when {@code value} matches a known reporter role, {@code false} otherwise.
   */
  public static boolean isValidReporterRoles(String value) {
    for (REPORTER_ROLES event : REPORTER_ROLES.values()) {
      if (event.value.equals(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates that all required attributes are populated.
   *
   * @return {@code true} when the type is fully populated, {@code false} otherwise.
   * @throws CADFException when a structural problem prevents validation from completing.
   */
  // TODO : validate method should be modified to return error message with details of missing
  // mandatory fields.
  // Validation to ensure all required attributes are set.
  public abstract boolean isValid() throws CADFException;
}
