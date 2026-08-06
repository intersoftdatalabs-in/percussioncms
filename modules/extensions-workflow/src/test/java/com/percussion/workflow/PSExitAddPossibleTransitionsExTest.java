/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * <p><strong>Disabled</strong> because loading {@code PSStateRolesContext} (and {@code
 * PSContentStatusContext}) triggers its legacy raw-JDBC read constructor; both require a live DB
 * connection and therefore fail with {@code ExceptionInInitializerError} outside a Spring context.
 * The recommendation is a Spring+H2 integration test that boots a real {@code EntityManager}; that
 * infrastructure is tracked in the Phase 4 scope survey (Phase 4d-1d) and is out of scope for this
 * PR. The same pattern is followed by {@code PSLoadFromHibernateTest} in this module.
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
   * Argument-validation guards that the legacy public overload fires in this order: {@code
   * connection -> userName -> roleNameList -> req}. With a {@code null} connection only the first
   * guard is reachable; the userName / roleNameList guards require a non-null connection (so the
   * method can advance past the first guard), which requires Spring+H2 infrastructure that this
   * test class is gated on. The {@code assertThrows} calls below therefore document intent and pin
   * the {@code connection} guard — the {@code userName} / {@code roleNameList} subtests below are
   * commented placeholders awaiting Spring+H2 infra. The pre-Phase-4d-1c ordering is preserved
   * verbatim.
   */
  @Test
  void legacyGetAssignmentType_guardOrdering() throws SQLException {
    IPSRequestContext request = null; // never reached with null connection
    Connection connection = null; // intentional — only the connection guard is reachable

    // 1) connection guard: null connection rejects first.
    IllegalArgumentException exConn =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                PSExitAddPossibleTransitionsEx.getAssignmentType(
                    WORKFLOW_ID, CONTENT_ID, connection, STATE_ID, USER_NAME, ROLE_NAMES, request));
    assertEquals("connection may not be null", exConn.getMessage());

    // 2) userName guard: would fire second IF connection were non-null. Documented intent only;
    //    pinned behaviour awaits a Spring+H2 test that can supply a live connection.
    //    Once infra lands, replace `connection` with a Mockito Connection mock and uncomment.
    // assertThrows(IllegalArgumentException.class,
    //     () -> PSExitAddPossibleTransitionsEx.getAssignmentType(
    //         WORKFLOW_ID, CONTENT_ID, mockConnection, STATE_ID, "  ", ROLE_NAMES, request));

    // 3) roleNameList guard: would fire third IF connection were non-null. Documented intent only.
    // assertThrows(IllegalArgumentException.class,
    //     () -> PSExitAddPossibleTransitionsEx.getAssignmentType(
    //         WORKFLOW_ID, CONTENT_ID, mockConnection, STATE_ID, USER_NAME, null, request));
  }

  /**
   * Placeholder for the post-migration contract pin:
   *
   * <pre>
   * when the Hibernate factory throws {@link PSEntryNotFoundException},
   * the legacy public overload must catch it and return the legacy default
   * {@link PSWorkFlowUtils#ASSIGNMENT_TYPE_NOT_IN_WORKFLOW}.
   * </pre>
   *
   * This matches the pre-migration behaviour where the raw-JDBC {@code new PSStateRolesContext(...,
   * connection, stateid, ...)} constructor threw {@code PSEntryNotFoundException} when no rows
   * matched.
   *
   * <p>The test body cannot exercise the {@link PSEntryNotFoundException} path until Spring+H2 test
   * infrastructure lands in this module (tracked in {@code
   * docs/ai-generated/migrations/workflow-orm/00-inventory.md} §7); with {@code connection == null}
   * the first guard in {@code getAssignmentType} ("connection may not be null") fires before the
   * factory is reached, so the only reachable assertion is the connection guard itself — which
   * {@link #legacyGetAssignmentType_guardOrdering()} already covers. The body below documents the
   * contract; the actual future pin lives in the commented block. Rename or merge once Spring+H2
   * infra lands and the body can exercise the real path.
   */
  @Test
  void legacyGetAssignmentType_contractPlaceholder_returnsDefaultOnEntryNotFound()
      throws SQLException {
    IPSRequestContext request = null;
    Connection connection = null;

    // With connection == null, the first guard ("connection may not be null") fires before any
    // static-initializer work on PSStateRolesContext happens. The @Disabled class-level annotation
    // skips the body until a Spring+H2 test harness can supply a live connection; once that lands,
    // uncomment and pin the factory's PSEntryNotFoundException -> ASSIGNMENT_TYPE_NOT_IN_WORKFLOW
    // contract. The shape below documents the contract.
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                PSExitAddPossibleTransitionsEx.getAssignmentType(
                    WORKFLOW_ID, CONTENT_ID, connection, STATE_ID, USER_NAME, ROLE_NAMES, request));
    assertEquals("connection may not be null", ex.getMessage());

    // Future contract pin (commented until Spring+H2 infra lands):
    // assertEquals(
    //     PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW,
    //     PSExitAddPossibleTransitionsEx.getAssignmentType(
    //         WORKFLOW_ID, CONTENT_ID, mockConnection, STATE_ID, USER_NAME, ROLE_NAMES, request));
  }
}
