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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import java.sql.Date;
import java.util.Calendar;
import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link PSComponentSummaryAdapter}.
 *
 * <p>The adapter wraps a Hibernate-managed {@link PSComponentSummary} (the
 * {@code CONTENTSTATUS} row) so it can satisfy the legacy
 * {@link IPSContentStatusContext} interface that
 * {@link PSContentStatusHistoryEntityBuilder} still consumes. These tests pin the
 * adapter's field-mapping behaviour, including the fallback values for fields
 * {@link PSComponentSummary} does not expose (e.g. {@code CONTENTCREATEDBY},
 * {@code ISREVISIONLOCKED}, {@code NEVERAGED}, {@code REMINDERDATE}) so a future
 * change to {@link PSComponentSummary} can't silently break callers.
 */
public class PSComponentSummaryAdapterTest {

  private static final int CONTENT_ID = 1042;
  private static final int WORKFLOW_ID = 7;
  private static final int STATE_ID = 5;
  private static final String TITLE = "My Article";
  private static final String CHECKOUT_USER = "Carla";
  private static final String LAST_MODIFIER = "Beth";

  /**
   * Happy-path mapping: every getter returns the corresponding value from the
   * wrapped summary. Date fields are narrowed from {@link java.util.Date} to
   * {@link java.sql.Date} via {@link PSComponentSummaryAdapter}'s private
   * helper.
   */
  @Test
  void happyPath_delegatesAndNarrowsDates() {
    java.util.Date lastModified = nowMinusOneDay();
    long lastModifiedMillis = lastModified.getTime();
    java.util.Date contentCreatedDate = nowMinusTwoDays();
    long contentCreatedMillis = contentCreatedDate.getTime();
    PSComponentSummary summary =
        mockSummary(
            /* checkout */ CHECKOUT_USER,
            /* lastModifier */ LAST_MODIFIER,
            /* lastModified */ lastModified,
            /* contentCreatedDate */ contentCreatedDate);

    PSComponentSummaryAdapter adapter = new PSComponentSummaryAdapter(summary);

    assertEquals(CONTENT_ID, adapter.getContentID());
    assertEquals(WORKFLOW_ID, adapter.getWorkflowID());
    assertEquals(STATE_ID, adapter.getContentStateID());
    assertEquals(TITLE, adapter.getTitle());
    assertEquals(CHECKOUT_USER, adapter.getContentCheckedOutUserName());
    assertEquals(LAST_MODIFIER, adapter.getContentLastModifierName());
    assertEquals(lastModifiedMillis, adapter.getContentLastModifiedDate().getTime());
    assertNotNull(adapter.getContentCreatedDate());
    assertEquals(contentCreatedMillis, adapter.getContentCreatedDate().getTime());
  }

  /**
   * Date narrowing: a null {@code lastModifiedDate} on the wrapped summary must
   * come out as null on the adapter side, not a synthetic default. Same for
   * {@code contentCreatedDate}.
   */
  @Test
  void nullDatesAreNullNotSyntheticDefaults() {
    PSComponentSummary summary =
        mockSummary(CHECKOUT_USER, LAST_MODIFIER, /* lastModified */ null, /* contentCreatedDate */ null);

    PSComponentSummaryAdapter adapter = new PSComponentSummaryAdapter(summary);

    assertNull(adapter.getContentLastModifiedDate());
    assertNull(adapter.getContentCreatedDate());
  }

  /**
   * Fields {@link PSComponentSummary} does not expose (or that legacy callers
   * tolerate as empty / false / null) fall back to documented defaults. These
   * are observable by callers of the legacy interface, so pinning them in a
   * test is what protects against silent regressions.
   */
  @Test
  void fallbackValues_matchLegacyContract() {
    PSComponentSummary summary = mockSummary(null, "", null, null);

    PSComponentSummaryAdapter adapter = new PSComponentSummaryAdapter(summary);

    // Legacy PSContentStatusContext used PSWorkFlowUtils.trimmedOrEmptyString on
    // CONTENTCREATEDBY which returns "" for null. The adapter preserves that.
    assertEquals("", adapter.getContentCreatedBy());

    // PSComponentSummary has no revision-lock surface; legacy cursor also
    // returned false. The adapter returns false to match.
    assertFalse(adapter.isRevisionLocked());

    // Aging / never-aged markers are not exposed by PSComponentSummary; the
    // legacy cursor did not set them either. The adapter returns false.
    assertFalse(adapter.neverAged());

    // Reminder date was never populated by the legacy cursor (column was
    // NULL by default). The adapter returns null to preserve that.
    assertNull(adapter.getReminderDate());

    // Next-aging-date and transition id fall through to the wrapped summary's
    // own defaults (null / 0).
    assertNull(adapter.getNextAgingDate());
    assertEquals(0, adapter.getNextAgingTransition());
    assertNull(adapter.getRepeatedAgingTransitionStartDate());
    assertNull(adapter.getLastTransitionDate());
  }

  /**
   * Integer-typed getters that the wrapped summary holds as boxed
   * {@link Integer} must come out as {@code int} with null-coerced zero. The
   * adapter does this with explicit null checks; this test pins the contract
   * for {@code CURRENTREVISION}, {@code EDITREVISION}, and {@code TIPREVISION}.
   */
  @Test
  void boxedIntegerFields_nullCoerceToZero() {
    PSComponentSummary summary = org.mockito.Mockito.mock(PSComponentSummary.class);
    when(summary.getContentId()).thenReturn(CONTENT_ID);
    when(summary.getWorkflowAppId()).thenReturn(WORKFLOW_ID);
    when(summary.getContentStateId()).thenReturn(STATE_ID);
    when(summary.getName()).thenReturn(TITLE);
    when(summary.getCheckoutUserName()).thenReturn("");
    when(summary.getContentLastModifier()).thenReturn("");
    when(summary.getCurrRevision()).thenReturn(null);
    when(summary.getEditRevision()).thenReturn(null);
    when(summary.getTipRevision()).thenReturn(null);

    PSComponentSummaryAdapter adapter = new PSComponentSummaryAdapter(summary);

    assertEquals(0, adapter.getCurrentRevision());
    assertEquals(0, adapter.getEditRevision());
    assertEquals(0, adapter.getTipRevision());
  }

  /**
   * Sanity: a content item with no checkout user (i.e. not checked out) must
   * surface as {@code null} on the adapter side. The legacy cursor pattern
   * distinguished between "checked out by Alice" and "not checked out" via
   * {@code null} vs {@code "Alice"}; the adapter must preserve that.
   */
  @Test
  void nullCheckoutUserIsNullNotEmpty() {
    PSComponentSummary summary = mockSummary(null, LAST_MODIFIER, nowMinusOneDay(), null);

    PSComponentSummaryAdapter adapter = new PSComponentSummaryAdapter(summary);

    assertNull(adapter.getContentCheckedOutUserName());
  }

  /**
   * The adapter is a defensive wrapper around a {@link PSComponentSummary}. A
   * null summary is a programmer error and must fail loudly, not NPE later.
   */
  @Test
  void nullSummaryIsRejected() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> new PSComponentSummaryAdapter(null));
    assertTrue(ex.getMessage().contains("summary"));
  }

  /**
   * Mutators on the adapter must throw {@link UnsupportedOperationException}
   * so misuse fails loudly. The adapter is read-only by design.
   */
  @Test
  void mutatorsThrowUnsupportedOperationException() {
    PSComponentSummaryAdapter adapter =
        new PSComponentSummaryAdapter(mockSummary("", "", null, null));

    assertThrows(UnsupportedOperationException.class, () -> adapter.setCurrentRevision(5));
    assertThrows(UnsupportedOperationException.class, () -> adapter.setContentCheckedOutUserName("x"));
    assertThrows(UnsupportedOperationException.class, () -> adapter.lockRevision());
    // close() is a no-op (Phase 3 migration): no JDBC resources to free.
    adapter.close(); // does not throw
  }

  // ---------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------

  /**
   * Builds a mocked {@link PSComponentSummary} with the most common getter
   * values populated. Returns a fresh mock per call so tests can also override
   * individual stubs.
   */
  private static PSComponentSummary mockSummary(
      String checkoutUser,
      String lastModifier,
      java.util.Date lastModified,
      java.util.Date contentCreatedDate) {
    PSComponentSummary summary = org.mockito.Mockito.mock(PSComponentSummary.class);
    when(summary.getContentId()).thenReturn(CONTENT_ID);
    when(summary.getWorkflowAppId()).thenReturn(WORKFLOW_ID);
    when(summary.getContentStateId()).thenReturn(STATE_ID);
    when(summary.getName()).thenReturn(TITLE);
    when(summary.getCheckoutUserName()).thenReturn(checkoutUser);
    when(summary.getContentLastModifier()).thenReturn(lastModifier);
    when(summary.getContentLastModifiedDate()).thenReturn(lastModified);
    when(summary.getContentCreatedDate()).thenReturn(contentCreatedDate);
    return summary;
  }

  private static java.util.Date nowMinusOneDay() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -1);
    return c.getTime();
  }

  private static java.util.Date nowMinusTwoDays() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -2);
    return c.getTime();
  }

  // ---------------------------------------------------------------------
  // sanity: confirm the legacy interface's date type so the test for date
  // narrowing documents what is being asserted.
  // ---------------------------------------------------------------------

  @SuppressWarnings("unused")
  private static Date unused_sqlDateSentinelToMakeTheImportNonRedundant() {
    return null; // import anchor
  }
}