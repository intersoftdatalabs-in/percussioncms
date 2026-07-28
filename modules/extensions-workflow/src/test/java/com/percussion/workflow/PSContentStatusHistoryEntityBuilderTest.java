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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.cms.IPSConstants;
import com.percussion.services.system.data.PSContentStatusHistory;
import java.sql.Date;
import java.util.Calendar;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSContentStatusHistoryEntityBuilder#build(int, int, int, String, String,
 * int, String, String, IPSContentStatusContext, IPSStatesContext, IPSTransitionsContext)}.
 *
 * <p>This helper is the single source of truth for the field-by-field mapping used by both the
 * deprecated {@code PSContentStatusHistoryContext} write constructor and the migrated
 * {@code PSExitUpdateHistory} write path (#1561 Phase 2). Keeping the mapping in one place
 * prevents the two write paths from drifting apart and lets the mapping be unit-tested without a
 * Spring context.
 *
 * <p>The tests deliberately avoid the Spring context: they do not exercise
 * {@code IPSSystemService#saveContentStatusHistory} (that needs a full integration test, tracked
 * in {@code docs/ai-generated/migrations/workflow-orm/00-inventory.md}). They focus on the field
 * mapping, branch behaviour (transition present vs. null, content published vs. not), and
 * validation.
 */
public class PSContentStatusHistoryEntityBuilderTest {

  private static final int WORKFLOW_ID = 7;
  private static final int CONTENT_ID = 1042;
  private static final String SESSION_ID = "sess-abc";
  private static final String ACTOR = "Aaron";
  private static final int BASE_REVISION = 3;
  private static final String ROLE_NAME = "Editor;Author";
  private static final String TRANSITION_COMMENT = "looks good";
  private static final String TITLE = "My Article";
  private static final int CONTENT_STATE_ID = 5;
  private static final String CHECKOUT_USER = null; // not checked out
  private static final String LAST_MODIFIER = "Beth";
  private static final String STATE_NAME = "Review";

  /**
   * Builds a fully populated entity via the helper and asserts every field matches the inputs.
   * Covers the regular transition branch ({@code transitionContext != null}).
   */
  @Test
  void populatesEveryFieldFromInputs_regularTransition() {
    Date lastModified = yesterday();
    Date eventTimeFloor = nowMinusOneMs();

    IPSContentStatusContext csc = mockContentStatusContext(TITLE, CONTENT_STATE_ID, CHECKOUT_USER, LAST_MODIFIER, lastModified);
    IPSStatesContext sc = mockStatesContext(true, STATE_NAME);
    IPSTransitionsContext tc = mockTransitionsContext(11, "Approve");

    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            csc,
            sc,
            tc);

    assertEquals(-1L, entity.getId(), "non-positive id should request auto-allocation");
    assertEquals(WORKFLOW_ID, entity.getWorkflowId());
    assertEquals(CONTENT_ID, entity.getContentId());
    assertEquals(SESSION_ID, entity.getSessionId());
    assertEquals(ACTOR, entity.getActor());
    assertEquals(BASE_REVISION, entity.getRevision());
    assertEquals(ROLE_NAME, entity.getRoleName());
    assertEquals(TRANSITION_COMMENT, entity.getTransitionComment());
    assertEquals("Y", entity.getIsValidValue());
    assertEquals(CONTENT_STATE_ID, entity.getStateId());
    assertEquals(STATE_NAME, entity.getStateName());
    assertEquals(TITLE, entity.getTitle());
    assertEquals(CHECKOUT_USER, entity.getCheckoutUserName());
    assertEquals(LAST_MODIFIER, entity.getLastModifierName());
    assertEquals(lastModified, entity.getLastModifiedDate());
    assertNotNull(entity.getEventTime(), "EVENTTIME must be populated even when not supplied");
    assertTrue(
        !entity.getEventTime().before(eventTimeFloor),
        "EVENTTIME must be no earlier than ~now (got " + entity.getEventTime() + ")");
    assertEquals(11, entity.getTransitionId());
    assertEquals("Approve", entity.getTransitionLabel());
  }

  /**
   * Passing a positive id should preserve it (callers may want to upsert an existing row).
   */
  @Test
  void positiveIdIsPreserved() {
    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            9999,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, CHECKOUT_USER, LAST_MODIFIER, yesterday()),
            mockStatesContext(true, STATE_NAME),
            mockTransitionsContext(11, "Approve"));

    assertEquals(9999L, entity.getId());
  }

  /**
   * A zero id is also treated as "auto-allocate" — only strictly positive ids are preserved.
   */
  @Test
  void zeroIdIsTreatedAsAutoAllocate() {
    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            0,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, CHECKOUT_USER, LAST_MODIFIER, yesterday()),
            mockStatesContext(true, STATE_NAME),
            mockTransitionsContext(11, "Approve"));

    assertEquals(-1L, entity.getId());
  }

  /**
   * When {@code transitionContext} is {@code null} and {@code contentCheckedOutUserName} is
   * {@code null} the row should be marked {@code TRANSITIONID_CHECKINOUT} with label
   * {@code "CheckIn"}.
   */
  @Test
  void checkInBranch_nullTransitionAndNullCheckout() {
    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, null, LAST_MODIFIER, yesterday()),
            mockStatesContext(false, STATE_NAME),
            null);

    assertEquals(IPSConstants.TRANSITIONID_CHECKINOUT, entity.getTransitionId());
    assertEquals("CheckIn", entity.getTransitionLabel());
    assertEquals("N", entity.getIsValidValue());
  }

  /**
   * When {@code transitionContext} is {@code null} but a checkout user is set the row should be
   * labelled {@code "CheckOut"} (still {@code TRANSITIONID_CHECKINOUT}).
   */
  @Test
  void checkOutBranch_nullTransitionButCheckoutUserPresent() {
    String checkOutUser = "Carla";

    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, checkOutUser, LAST_MODIFIER, yesterday()),
            mockStatesContext(false, STATE_NAME),
            null);

    assertEquals(IPSConstants.TRANSITIONID_CHECKINOUT, entity.getTransitionId());
    assertEquals("CheckOut", entity.getTransitionLabel());
    assertEquals(checkOutUser, entity.getCheckoutUserName());
  }

  /**
   * A null content status context must be rejected up front — the resulting entity would
   * otherwise have null fields and {@code saveContentStatusHistory} would persist nonsense.
   */
  @Test
  void nullContentStatusContextIsRejected() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                PSContentStatusHistoryEntityBuilder.build(
                    -1,
                    WORKFLOW_ID,
                    CONTENT_ID,
                    SESSION_ID,
                    ACTOR,
                    BASE_REVISION,
                    ROLE_NAME,
                    TRANSITION_COMMENT,
                    null,
                    mockStatesContext(true, STATE_NAME),
                    mockTransitionsContext(11, "Approve")));
    assertTrue(ex.getMessage().contains("contentStatusContext"));
  }

  /**
   * A null states context must be rejected up front — without it we cannot derive the
   * {@code STATENAME} or the {@code VALID} flag.
   */
  @Test
  void nullStatesContextIsRejected() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                PSContentStatusHistoryEntityBuilder.build(
                    -1,
                    WORKFLOW_ID,
                    CONTENT_ID,
                    SESSION_ID,
                    ACTOR,
                    BASE_REVISION,
                    ROLE_NAME,
                    TRANSITION_COMMENT,
                    mockContentStatusContext(TITLE, CONTENT_STATE_ID, CHECKOUT_USER, LAST_MODIFIER, yesterday()),
                    null,
                    mockTransitionsContext(11, "Approve")));
    assertTrue(ex.getMessage().contains("statesContext"));
  }

  /**
   * Sanity: the helper sets {@code VALID} to {@code "N"} when the state reports
   * {@code getIsValid() == false}. This is the unpublish / archive path; without it
   * {@code updateLastPublicRevision} would mistakenly advance the public revision.
   */
  @Test
  void validFlagReflectsStatesContext() {
    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, CHECKOUT_USER, LAST_MODIFIER, yesterday()),
            mockStatesContext(false, "Archive"),
            mockTransitionsContext(11, "Approve"));

    assertEquals("N", entity.getIsValidValue());
    assertEquals("Archive", entity.getStateName());
  }

  /**
   * Sanity: the helper stores {@code CHECKOUTUSERNAME} verbatim (including the {@code null} case)
   * so the legacy {@link PSContentStatusHistoryContext} interface getters keep returning the
   * same values after the Phase 2 migration.
   */
  @Test
  void checkoutUserNameIsStoredVerbatim() {
    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, "Carla", LAST_MODIFIER, yesterday()),
            mockStatesContext(true, STATE_NAME),
            mockTransitionsContext(11, "Approve"));

    assertEquals("Carla", entity.getCheckoutUserName());

    // and the null case
    PSContentStatusHistory entityNoCheckout =
        PSContentStatusHistoryEntityBuilder.build(
            -1,
            WORKFLOW_ID,
            CONTENT_ID,
            SESSION_ID,
            ACTOR,
            BASE_REVISION,
            ROLE_NAME,
            TRANSITION_COMMENT,
            mockContentStatusContext(TITLE, CONTENT_STATE_ID, null, LAST_MODIFIER, yesterday()),
            mockStatesContext(true, STATE_NAME),
            mockTransitionsContext(11, "Approve"));

    assertNull(entityNoCheckout.getCheckoutUserName());
  }

  // --- helpers ------------------------------------------------------------

  private static IPSContentStatusContext mockContentStatusContext(
      String title,
      int contentStateId,
      String checkoutUser,
      String lastModifier,
      Date lastModified) {
    IPSContentStatusContext csc = org.mockito.Mockito.mock(IPSContentStatusContext.class);
    when(csc.getTitle()).thenReturn(title);
    when(csc.getContentStateID()).thenReturn(contentStateId);
    when(csc.getContentCheckedOutUserName()).thenReturn(checkoutUser);
    when(csc.getContentLastModifierName()).thenReturn(lastModifier);
    when(csc.getContentLastModifiedDate()).thenReturn(lastModified);
    return csc;
  }

  private static IPSStatesContext mockStatesContext(boolean isValid, String stateName) {
    IPSStatesContext sc = org.mockito.Mockito.mock(IPSStatesContext.class);
    when(sc.getIsValid()).thenReturn(isValid);
    when(sc.getStateName()).thenReturn(stateName);
    return sc;
  }

  private static IPSTransitionsContext mockTransitionsContext(int transitionId, String label) {
    IPSTransitionsContext tc = org.mockito.Mockito.mock(IPSTransitionsContext.class);
    when(tc.getTransitionID()).thenReturn(transitionId);
    when(tc.getTransitionLabel()).thenReturn(label);
    return tc;
  }

  private static Date yesterday() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -1);
    return new Date(c.getTimeInMillis());
  }

  private static Date nowMinusOneMs() {
    return new Date(System.currentTimeMillis() - 1);
  }
}