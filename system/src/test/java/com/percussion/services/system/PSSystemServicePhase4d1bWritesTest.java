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
package com.percussion.services.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.system.impl.PSSystemService;
import com.percussion.services.workflow.data.PSContentAdhocUser;
import com.percussion.services.workflow.data.PSContentApproval;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mockito-only tests for the #1561 Phase 4d-1b write methods on {@link IPSSystemService}.
 * Verifies the JPQL is well-formed and that the parameters are forwarded correctly.
 *
 * <p>The companion behavioral test for the {@code buildLegacyColumnMap} hot-fix #2
 * in {@link PSContentStatusContext#commit()} lives in
 * {@code com.percussion.workflow.PSContentStatusContextCommitTest} (same package,
 * so it pins the 15-column map directly).</p>
 */
public class PSSystemServicePhase4d1bWritesTest {

  private PSSystemService service;
  private Session session;
  private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    service =
        new PSSystemService(
            mock(com.percussion.utils.jdbc.IPSDatasourceManager.class),
            mock(IPSGuidManager.class),
            mock(com.percussion.services.workflow.IPSWorkflowService.class),
            mock(com.percussion.services.legacy.IPSCmsObjectMgr.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  // --- updateContentStatusState --------------------------------------------

  @Test
  void updateContentStatusState_rejectsNonPositiveContentId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.updateContentStatusState(0, 1, "", 1, 1, 1, false, null, null, 0, null, null, null, null, null));
  }

  @Test
  void updateContentStatusState_rejectsNonPositiveStateId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.updateContentStatusState(1, 0, "", 1, 1, 1, false, null, null, 0, null, null, null, null, null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateContentStatusState_returnsRowsUpdated() {
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(1);
    // Stub the SessionFactory → Cache chain that updateContentStatusState touches when
    // updated > 0, so the cache.evictEntityData call doesn't NPE.
    org.hibernate.SessionFactory mockFactory = mock(org.hibernate.SessionFactory.class);
    org.hibernate.Cache mockCache = mock(org.hibernate.Cache.class);
    when(mockFactory.getCache()).thenReturn(mockCache);
    when(session.getSessionFactory()).thenReturn(mockFactory);

    int updated =
        service.updateContentStatusState(
            7, 11, "alice", 5, 6, 7, true, new java.util.Date(), new java.util.Date(),
            3, new java.util.Date(), new java.util.Date(), new java.util.Date(),
            new java.util.Date(), new java.util.Date());
    assertEquals(1, updated,
        "Phase 4d-1b hot-fix: updateContentStatusState must return the rows-updated"
            + " count so PSContentStatusContext.commit() can fire PSItemSummaryCache");
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateContentStatusState_returnsZeroWhenNoRowMatches() {
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(0);
    // No SessionFactory stub — updated = 0 must short-circuit before the cache.evictEntityData call.

    int updated =
        service.updateContentStatusState(
            7, 11, "alice", 5, 6, 7, true, new java.util.Date(), new java.util.Date(),
            3, new java.util.Date(), new java.util.Date(), new java.util.Date(),
            new java.util.Date(), new java.util.Date());
    assertEquals(0, updated);
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateContentStatusState_jpqlAndParameters() {
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(1);
    // Stub the SessionFactory → Cache chain that updateContentStatusState touches when
    // updated > 0, so the cache.evictEntityData call doesn't NPE.
    org.hibernate.SessionFactory mockFactory = mock(org.hibernate.SessionFactory.class);
    org.hibernate.Cache mockCache = mock(org.hibernate.Cache.class);
    when(mockFactory.getCache()).thenReturn(mockCache);
    when(session.getSessionFactory()).thenReturn(mockFactory);

    service.updateContentStatusState(
        7, 11, "alice", 5, 6, 7, true, new java.util.Date(), new java.util.Date(),
        3, new java.util.Date(), new java.util.Date(), new java.util.Date(), new java.util.Date(),
        new java.util.Date());

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture());
    String q = jpql.getValue();
    // All 14 columns must be set in the JPQL update.
    assertContains(q, "m_contentStateId = :stateId");
    assertContains(q, "m_checkoutUserName = :checkOutUserName");
    assertContains(q, "m_currRevision = :currentRevision");
    assertContains(q, "m_editRevision = :editRevision");
    assertContains(q, "m_tipRevision = :tipRevision");
    assertContains(q, "m_revisionLock = :revisionLock");
    assertContains(q, "m_lastTransitionDate = :lastTransitionDate");
    assertContains(q, "m_stateEnteredDate = :stateEnteredDate");
    assertContains(q, "m_nextAgingTransition = :nextAgingTransition");
    assertContains(q, "m_nextAgingDate = :nextAgingDate");
    assertContains(q, "m_contentStartDate = :startDate");
    assertContains(q, "m_contentExpiryDate = :expiryDate");
    assertContains(q, "m_reminderDate = :reminderDate");
    assertContains(q, "m_repeatedAgingTransStartDate = :repeatedAgingStartDate");
    assertContains(q, "where m_contentId = :contentId");

    verify(mockQuery).setParameter("stateId", 11);
    verify(mockQuery).setParameter("contentId", 7);
    verify(mockQuery).setParameter("revisionLock", 'Y');
  }

  private static void assertContains(String haystack, String needle) {
    if (!haystack.contains(needle)) {
      throw new AssertionError("Expected JPQL to contain '" + needle + "': " + haystack);
    }
  }

  // --- saveContentAdhocUsers ------------------------------------------------

  @Test
  void saveContentAdhocUsers_rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> service.saveContentAdhocUsers(null));
  }

  @Test
  void saveContentAdhocUsers_emptyList_noOp() {
    service.saveContentAdhocUsers(Collections.emptyList());
    // No exception, no session interaction expected.
  }

  @Test
  @SuppressWarnings("unchecked")
  void saveContentAdhocUsers_mergesEachRow() {
    PSContentAdhocUser u1 = new PSContentAdhocUser();
    u1.setContentId(7);
    u1.setUser("alice");
    u1.setRoleId(11);
    u1.setAdhocType(2);
    PSContentAdhocUser u2 = new PSContentAdhocUser();
    u2.setContentId(7);
    u2.setUser("bob");
    u2.setRoleId(13);
    u2.setAdhocType(2);
    service.saveContentAdhocUsers(Arrays.asList(u1, u2));
    verify(session).merge(u1);
    verify(session).merge(u2);
  }

  // --- deleteContentAdhocUsers ----------------------------------------------

  @Test
  void deleteContentAdhocUsers_rejectsNonPositiveContentId() {
    assertThrows(IllegalArgumentException.class, () -> service.deleteContentAdhocUsers(0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void deleteContentAdhocUsers_jpqlAndParameters() {
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(2);

    int n = service.deleteContentAdhocUsers(7);

    assertEquals(2, n);
    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture());
    assertContains(jpql.getValue(), "delete from PSContentAdhocUser");
    assertContains(jpql.getValue(), "where contentId = :cid");
    verify(mockQuery).setParameter("cid", 7);
  }

  // --- saveContentApproval -------------------------------------------------

  @Test
  void saveContentApproval_rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> service.saveContentApproval(null));
  }

  @Test
  void saveContentApproval_mergesRow() {
    PSContentApproval approval = new PSContentApproval(7, 11, "alice", 4, 13, 5);
    service.saveContentApproval(approval);
    verify(session).merge(approval);
  }

  // --- deleteContentApprovals ---------------------------------------------

  @Test
  void deleteContentApprovals_rejectsNonPositiveArgs() {
    assertThrows(
        IllegalArgumentException.class, () -> service.deleteContentApprovals(0, 1, 1, 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.deleteContentApprovals(1, 0, 1, 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.deleteContentApprovals(1, 1, 0, 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.deleteContentApprovals(1, 1, 1, 0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void deleteContentApprovals_jpqlAndParameters() {
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(3);

    int n = service.deleteContentApprovals(7, 4, 5, 13);

    assertEquals(3, n);
    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture());
    String q = jpql.getValue();
    assertContains(q, "delete from PSContentApproval");
    assertContains(q, "where contentId = :cid and workflowId = :wf");
    assertContains(q, "and transitionId = :tid and stateId = :sid");
    verify(mockQuery).setParameter("cid", 7);
    verify(mockQuery).setParameter("wf", 4);
    verify(mockQuery).setParameter("tid", 5);
    verify(mockQuery).setParameter("sid", 13);
  }

  // --- deleteContentApprovals(int contentId) — Phase 4d-1b hot-fix -------------

  @Test
  void deleteContentApprovalsByContentId_rejectsNonPositiveContentId() {
    assertThrows(IllegalArgumentException.class, () -> service.deleteContentApprovals(0));
    assertThrows(
        IllegalArgumentException.class, () -> service.deleteContentApprovals(-1));
  }

  @Test
  @SuppressWarnings("unchecked")
  void deleteContentApprovalsByContentId_jpqlIsContentIdOnly() {
    // PR #1589 review thread databaseId 3670307327 / 3670307331: the transition-
    // completion path must delete by contentId only (legacy semantics) — not the
    // narrower 4-tuple filter. This test pins the JPQL string so the regression
    // cannot re-emerge.
    org.hibernate.query.Query<Integer> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.executeUpdate()).thenReturn(5);

    int n = service.deleteContentApprovals(7);

    assertEquals(5, n);
    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture());
    String q = jpql.getValue();
    assertContains(q, "delete from PSContentApproval");
    assertContains(q, "where contentId = :cid");
    // Anti-regression: must NOT filter by workflowId/transitionId/stateId.
    if (q.contains("workflowId") || q.contains("transitionId") || q.contains("stateId")) {
      throw new AssertionError(
          "deleteContentApprovals(int) JPQL must be contentId-only, got: " + q);
    }
    verify(mockQuery).setParameter("cid", 7);
  }
}
