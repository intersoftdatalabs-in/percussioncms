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
package com.ibm.cadf;

import static java.util.Arrays.asList;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.List;

/**
 * Catalog of permissible values for CADF event {@code action}, {@code outcome}, and {@code
 * resource} fields. Each taxonomy is a list of known strings; matching is performed with a prefix
 * check so callers can supply scoped variants (e.g., {@code authenticate/login}) that begin with
 * one of the canonical entries.
 */
public class CADFTaxonomy {

  /** Default no-argument constructor for {@link CADFTaxonomy}. */
  public CADFTaxonomy() {}

  /** Sentinel returned for any value that cannot be classified. */
  public static String UNKNOWN = "unknown";

  // Commonly used (valid) Event.action values from Nova
  /** CADF action label for resource creation. */
  public static String ACTION_CREATE = "create";

  /** CADF action label for reading a resource. */
  public static String ACTION_READ = "read";

  /** CADF action label for updating a resource. */
  public static String ACTION_UPDATE = "update";

  /** CADF action label for deleting a resource. */
  public static String ACTION_DELETE = "delete";

  /** CADF action label for backing up a resource. */
  public static String ACTION_BACKUP = "backup";

  /** CADF action label for restoring a resource from backup. */
  public static String ACTION_RESTORE = "restore";

  // OpenStack specific, Profile or change CADF spec. to add this action
  /**
   * CADF action label for listing resources (OpenStack-specific variant of {@link #ACTION_READ}).
   */
  public static String ACTION_LIST = "read/list";

  List<String> ACTION_TAXONOMY =
      asList(
          "backup",
          "capture",
          ACTION_CREATE,
          "configure",
          ACTION_READ,
          ACTION_LIST,
          ACTION_UPDATE,
          ACTION_DELETE,
          "monitor",
          "start",
          "stop",
          "deploy",
          "undeploy",
          "enable",
          "disable",
          "send",
          "receive",
          "authenticate",
          "authenticate/login",
          "revoke",
          "renew",
          "restore",
          "evaluate",
          "allow",
          "deny",
          "notify",
          UNKNOWN);

  /**
   * Indicates whether the supplied string is a valid (or scoped-prefix valid) CADF action label.
   *
   * @param value the action label to test, may be {@code null}.
   * @return {@code true} when {@code value} starts with any entry in {@link #ACTION_TAXONOMY}.
   */
  public boolean isValidAction(String value) {
    return findElementStartsWith(ACTION_TAXONOMY, value);
  }

  // Valid Event.outcome values
  /** Enumerates the CADF {@code outcome} values permitted for emitted events. */
  public enum OUTCOME {
    /** The audited action completed without an observable error. */
    SUCCESS("success"),
    /** The audited action did not complete as intended. */
    FAILURE("failure"),
    /** The audited action is still in flight. */
    PENDING("pending"),
    /** Default outcome used when neither success nor failure has been determined. */
    UNKNOWN("unknown");

    /** The CADF wire-string value associated with this outcome. */
    public String value;

    private OUTCOME(String value) {
      this.value = value;
    }
  }

  /** CADF outcome label for successful actions; equivalent to {@link OUTCOME#SUCCESS}. */
  public static String OUTCOME_SUCCESS = "success";

  /** CADF outcome label for failed actions; equivalent to {@link OUTCOME#FAILURE}. */
  public static String OUTCOME_FAILURE = "failure";

  /** CADF outcome label for in-progress actions; equivalent to {@link OUTCOME#PENDING}. */
  public static String OUTCOME_PENDING = "pending";

  List<String> OUTCOME_TAXONOMY =
      asList(OUTCOME_SUCCESS, OUTCOME_FAILURE, OUTCOME_PENDING, UNKNOWN);

  /**
   * Indicates whether the supplied string matches a CADF outcome (or a scope-prefixed variant).
   *
   * @param value the outcome label to test, may be {@code null}.
   * @return {@code true} when {@code value} starts with any entry in {@link #OUTCOME_TAXONOMY}.
   */
  public boolean isValidOutcome(String value) {
    return findElementStartsWith(OUTCOME_TAXONOMY, value);
  }

  /** Resource type URI for the security service. */
  public static String SERVICE_SECURITY = "service/security";

  /** Resource type URI for an individual user account under the security service. */
  public static String ACCOUNT_USER = "service/security/account/user";

  /** Resource type URI for the CADF audit filter. */
  public static String CADF_AUDIT_FILTER = "service/security/audit/filter";

  List<String> RESOURCE_TAXONOMY =
      asList(
          "storage",
          "storage/node",
          "storage/volume",
          "storage/memory",
          "storage/container",
          "storage/directory",
          "storage/database",
          "storage/queue",
          "compute",
          "compute/node",
          "compute/cpu",
          "compute/machine",
          "compute/process",
          "compute/thread",
          "network",
          "network/node",
          "network/node/host",
          "network/connection",
          "network/domain",
          "network/cluster",
          "service",
          "service/oss",
          "service/bss",
          "service/bss/metering",
          "service/composition",
          "service/compute",
          "service/database",
          SERVICE_SECURITY,
          "service/security/account",
          ACCOUNT_USER,
          CADF_AUDIT_FILTER,
          "service/storage",
          "service/storage/block",
          "service/storage/image",
          "service/storage/object",
          "service/network",
          "data",
          "data/message",
          "data/workload",
          "data/workload/app",
          "data/workload/service",
          "data/workload/task",
          "data/workload/job",
          "data/file",
          "data/file/catalog",
          "data/file/log",
          "data/template",
          "data/package",
          "data/image",
          "data/module",
          "data/config",
          "data/directory",
          "data/database",
          "data/security",
          "data/security/account",
          "data/security/credential",
          "data/security/group",
          "data/security/identity",
          "data/security/key",
          "data/security/license",
          "data/security/policy",
          "data/security/profile",
          "data/security/role",
          "data/security/service",
          "data/security/account/user",
          "data/security/account/user/privilege",
          "data/database/alias",
          "data/database/catalog",
          "data/database/constraints",
          "data/database/index",
          "data/database/instance",
          "data/database/key",
          "data/database/routine",
          "data/database/schema",
          "data/database/sequence",
          "data/database/table",
          "data/database/trigger",
          "data/database/view",
          UNKNOWN);

  /**
   * Indicates whether the supplied string is a valid CADF resource type URI (or scope-prefixed
   * variant).
   *
   * @param value the resource URI to test, may be {@code null}.
   * @return {@code true} when {@code value} starts with any entry in {@link #RESOURCE_TAXONOMY}.
   */
  public boolean isValidResource(String value) {
    return findElementStartsWith(RESOURCE_TAXONOMY, value);
  }

  /**
   * Returns {@code true} when {@code value} is the prefix of any string in {@code list}.
   *
   * @param list the candidate strings, never {@code null}.
   * @param value the prefix to look up, may be {@code null}.
   * @return {@code true} when at least one entry in {@code list} starts with {@code value}.
   */
  public boolean findElementStartsWith(List<String> list, final String value) {
    boolean startsWithValue =
        Iterables.any(
            list,
            new Predicate<String>() {
              public boolean apply(String input) {
                return input.startsWith(value);
              }
            });

    return startsWithValue;
  }
}
