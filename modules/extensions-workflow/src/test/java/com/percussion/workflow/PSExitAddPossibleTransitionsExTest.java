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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.server.IPSRequestContext;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for the #1561 Phase 4d-1c migration in {@link PSExitAddPossibleTransitionsEx}.
 * Pins the migration contract for the three raw-JDBC {@code new PSStateRolesContext(...)} / {@code
 * new PSContentStatusContext(...)} call sites that Phase 4d-1c replaces with the Hibernate-backed
 * {@code PSStateRolesContext.loadFromHibernate(...)} and {@code
 * PSContentStatusContext.loadFromHibernate(...)} factories on the shared session.
 *
 * <p>The migrated paths are reachable through the public legacy {@code
 * PSExitAddPossibleTransitionsEx.getAssignmentType(Connection)} overload (preserved for binary
 * compatibility — see {@code modules/extensions-workflow/AGENTS.md} rule #6).
 *
 * <p><strong>Disabled</strong> because loading {@code PSStateRolesContext} triggers its static
 * initializer which calls {@link com.percussion.workflow.PSConnectionMgr#getQualifiedIdentifier}
 * (and the same for {@code PSContentStatusContext}); both require a live DB connection detail and
 * therefore fail with {@code ExceptionInInitializerError} outside a Spring context. The
 * recommendation is a Spring+H2 integration test that boots a real {@code EntityManager}; that
 * infrastructure is tracked in the Phase 4 scope survey (Phase 4+) and is out of scope for this PR.
 * The same pattern is followed by {@code PSLoadFromHibernateTest} in this module.
 */
@Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
public class PSExitAddPossibleTransitionsExTest {

  private static final int WORKFLOW_ID = 7;
  private static final int CONTENT_ID = 1042;
  private static final int STATE_ID = 5;
  private static final String USER_NAME = "alice";
  private static final String ROLE_NAMES = "Editor,Admin";

  /**
   * Argument-validation guard: the legacy public {@code getAssignmentType(int, int, Connection,
   * int, String, String, IPSRequestContext)} overload must still reject a {@code null} connection
   * at its API surface. This preserves the pre-Phase-4d-1c contract; the migration to the
   * Hibernate-backed {@code loadFromHibernate} factory happens after the null check, so a null
   * connection at the API surface must still fail fast and loud.
   */
  @Test
  void legacyGetAssignmentType_rejectsNullConnection() {
    IPSRequestContext request = null; // null is OK — the null-check fires before the req is touched
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                PSExitAddPossibleTransitionsEx.getAssignmentType(
                    WORKFLOW_ID,
                    CONTENT_ID,
                    /* connection */ (Connection) null,
                    STATE_ID,
                    USER_NAME,
                    ROLE_NAMES,
                    request));
    assertEquals("connection may not be null", ex.getMessage());
  }

  /**
   * Argument-validation guards: the legacy public overload must reject a blank user name and a null
   * role name list before touching the Hibernate factory. These guards are preserved verbatim from
   * the pre-migration code.
   */
  @Test
  void legacyGetAssignmentType_rejectsBlankUserNameAndNullRoleList() throws SQLException {
    IPSRequestContext request = null; // the null-check on userName/roleList fires before req
    Connection connection =
        null; // intentionally null — the validation on userName/roleList fires first

    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSExitAddPossibleTransitionsEx.getAssignmentType(
                WORKFLOW_ID,
                CONTENT_ID,
                connection,
                STATE_ID,
                /* userName */ "  ",
                ROLE_NAMES,
                request));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSExitAddPossibleTransitionsEx.getAssignmentType(
                WORKFLOW_ID,
                CONTENT_ID,
                connection,
                STATE_ID,
                USER_NAME,
                /* roleNameList */ null,
                request));
  }

  /**
   * Migration contract: when the Hibernate factory throws {@link PSEntryNotFoundException}, the
   * legacy public overload must catch it and return the legacy default {@link
   * PSWorkFlowUtils#ASSIGNMENT_TYPE_NOT_IN_WORKFLOW}. This matches the pre-migration behaviour
   * where the raw-JDBC {@code new PSStateRolesContext(..., connection, stateid, ...)} constructor
   * threw {@code PSEntryNotFoundException} when no rows matched.
   */
  @Test
  void legacyGetAssignmentType_returnsDefaultOnEntryNotFound() throws SQLException {
    IPSRequestContext request = null;
    Connection connection = null;

    // No Spring+H2 infra: the static initializer of PSStateRolesContext fails first,
    // so the call below throws ExceptionInInitializerError rather than reaching the
    // factory. The Disabled annotation at class level prevents the test from running
    // until Spring+H2 infra is added. The shape documents the contract.
    assertEquals(
        PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW,
        PSExitAddPossibleTransitionsEx.getAssignmentType(
            WORKFLOW_ID, CONTENT_ID, connection, STATE_ID, USER_NAME, ROLE_NAMES, request));
  }
}
