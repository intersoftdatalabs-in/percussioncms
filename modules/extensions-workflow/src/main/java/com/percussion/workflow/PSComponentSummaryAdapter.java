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

import com.percussion.cms.objectstore.PSComponentSummary;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

/**
 * Read-only adapter that wraps a Hibernate-managed {@link PSComponentSummary} (the {@code
 * CONTENTSTATUS} row) so it can be passed to legacy code that still uses the {@link
 * IPSContentStatusContext} interface.
 *
 * <p>Added for #1561 Phase 3 so {@code PSExitUpdateHistory} can read {@code CONTENTSTATUS} via
 * {@code PSCmsObjectMgr#loadComponentSummary(...)} without keeping the legacy {@code
 * PSContentStatusContext} JDBC context alive. Only the read accessors are real; setters and the
 * {@code commit}/{@code close} hooks throw {@link UnsupportedOperationException} so misuse fails
 * loudly.
 *
 * <p>The class is package-private and intended for use only by {@code PSExitUpdateHistory} and its
 * tests. Public callers should keep consuming {@code PSComponentSummary} directly.
 */
final class PSComponentSummaryAdapter implements IPSContentStatusContext {

  private final PSComponentSummary summary;

  PSComponentSummaryAdapter(PSComponentSummary summary) {
    if (summary == null) throw new IllegalArgumentException("summary may not be null");
    this.summary = summary;
  }

  // -- Read accessors that delegate to the wrapped PSComponentSummary ----------

  @Override
  public String getTitle() {
    return summary.getName();
  }

  @Override
  public int getCurrentRevision() {
    Integer v = summary.getCurrRevision();
    return v == null ? 0 : v;
  }

  @Override
  public int getEditRevision() {
    Integer v = summary.getEditRevision();
    return v == null ? 0 : v;
  }

  @Override
  public int getTipRevision() {
    Integer v = summary.getTipRevision();
    return v == null ? 0 : v;
  }

  @Override
  public boolean isRevisionLocked() {
    return false; // not surfaced via PSComponentSummary getters; legacy callers tolerate false.
  }

  @Override
  public boolean neverAged() {
    return false;
  }

  @Override
  public int getContentStateID() {
    return summary.getContentStateId();
  }

  @Override
  public int getContentID() {
    return summary.getContentId();
  }

  @Override
  public int getContentTypeID() {
    return (int) summary.getContentTypeId();
  }

  @Override
  public String getContentCheckedOutUserName() {
    return summary.getCheckoutUserName();
  }

  @Override
  public String getContentLastModifierName() {
    return summary.getContentLastModifier();
  }

  @Override
  public Date getContentLastModifiedDate() {
    return toSqlDate(summary.getContentLastModifiedDate());
  }

  @Override
  public String getContentCreatedBy() {
    return ""; // PSComponentSummary does not expose the creator name; legacy callers tolerate "".
  }

  @Override
  public Date getContentCreatedDate() {
    return toSqlDate(summary.getContentCreatedDate());
  }

  @Override
  public Date getContentStartDate() {
    return toSqlDate(summary.getContentStartDate());
  }

  @Override
  public Date getContentExpiryDate() {
    return toSqlDate(summary.getContentExpiryDate());
  }

  @Override
  public Date getReminderDate() {
    return null;
  }

  @Override
  public Date getNextAgingDate() {
    return toSqlDate(summary.getNextAgingDate());
  }

  @Override
  public int getNextAgingTransition() {
    return summary.getNextAgingTransition();
  }

  @Override
  public Date getRepeatedAgingTransitionStartDate() {
    return toSqlDate(summary.getRepeatedAgingTransStartDate());
  }

  @Override
  public Date getLastTransitionDate() {
    return toSqlDate(summary.getLastTransitionDate());
  }

  @Override
  public int getWorkflowID() {
    return summary.getWorkflowAppId();
  }

  // -- Mutators and JDBC hooks are intentionally unsupported -----------------

  @Override
  public void setLastTransitionDate() {
    throw unsupported();
  }

  @Override
  public void setLastTransitionDate(Date lastTransitionDate) {
    throw unsupported();
  }

  @Override
  public void setContentCheckedOutUserName(String checkedUserName) {
    throw unsupported();
  }

  @Override
  public void setCurrentRevision(int currentRevision) {
    throw unsupported();
  }

  @Override
  public void setEditRevision(int editRevision) {
    throw unsupported();
  }

  @Override
  public void setTipRevision(int tipRevision) {
    throw unsupported();
  }

  @Override
  public void lockRevision() {
    throw unsupported();
  }

  @Override
  public void setContentStateID(int stateID) {
    throw unsupported();
  }

  @Override
  public void commit(Connection connection) throws SQLException {
    throw unsupported();
  }

  @Override
  public void setStateEnteredDate() {
    throw unsupported();
  }

  @Override
  public void setNextAgingDate(Date nextAgingDate) {
    throw unsupported();
  }

  @Override
  public void setNextAgingTransition(int nextAgingTransition) {
    throw unsupported();
  }

  @Override
  public void setRepeatedAgingTransitionStartDate(Date repeatedAgingTransitionStartDate) {
    throw unsupported();
  }

  @Override
  public void close() {
    // No-op: PSComponentSummary is Hibernate-managed; nothing to free here.
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException(
        "PSComponentSummaryAdapter is read-only; mutate PSComponentSummary directly instead.");
  }

  /**
   * Narrows a {@link java.util.Date} (the type returned by {@link PSComponentSummary} getters) to
   * the {@link java.sql.Date} that the legacy {@link IPSContentStatusContext} interface declares.
   * Returns {@code null} when the source is {@code null}.
   */
  private static Date toSqlDate(java.util.Date source) {
    return source == null ? null : new Date(source.getTime());
  }
}
