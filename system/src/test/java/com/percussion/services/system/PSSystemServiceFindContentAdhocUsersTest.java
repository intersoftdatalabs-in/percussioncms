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
package com.percussion.services.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.system.impl.PSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSContentAdhocUser;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mockito-only tests for {@link PSSystemService#findContentAdhocUsers(int)} added for #1561 Phase
 * 4b. Verifies the JPQL is well-formed and that the {@code contentId} parameter is forwarded
 * correctly. Behavioural assertions about the result set are out of scope here — see {@code
 * com.percussion.workflow.PSLoadFromHibernateTest} for the mapping tests.
 */
public class PSSystemServiceFindContentAdhocUsersTest {

  private PSSystemService service;
  private Session session;
  private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    service =
        new PSSystemService(
            mock(IPSDatasourceManager.class),
            mock(IPSGuidManager.class),
            mock(IPSWorkflowService.class),
            mock(IPSCmsObjectMgr.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  @Test
  void rejectsNonPositiveContentId() {
    assertThrows(IllegalArgumentException.class, () -> service.findContentAdhocUsers(0));
    assertThrows(IllegalArgumentException.class, () -> service.findContentAdhocUsers(-1));
  }

  @Test
  @SuppressWarnings("unchecked")
  void usesContentIdParameter() {
    org.hibernate.query.Query<PSContentAdhocUser> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSContentAdhocUser.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("cid"), any(Integer.class))).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    service.findContentAdhocUsers(471);

    verify(mockQuery).setParameter("cid", 471);
  }

  @Test
  @SuppressWarnings("unchecked")
  void jpqlContainsContentIdPredicate() {
    org.hibernate.query.Query<PSContentAdhocUser> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSContentAdhocUser.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("cid"), any(Integer.class))).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    service.findContentAdhocUsers(471);

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSContentAdhocUser.class));
    String sql = jpql.getValue();
    assertNotNull(sql);
    assertTrue(sql.contains("contentId = :cid"), "JPQL must filter by contentId, got: " + sql);
    assertEquals(
        "from PSContentAdhocUser where contentId = :cid",
        sql.trim(),
        "JPQL must match the entity property name 'contentId' "
            + "(no proprietary 'adhocUserId' identifier — see PR #1578 review)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyResultIsForwarded() {
    org.hibernate.query.Query<PSContentAdhocUser> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSContentAdhocUser.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("cid"), any(Integer.class))).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    List<PSContentAdhocUser> result = service.findContentAdhocUsers(471);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
