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
package com.percussion.services.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.workflow.impl.PSWorkflowService;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link PSWorkflowService#updateWorkflowVersion(IPSGuid)} must run the bulk UPDATE through JPA
 * {@link EntityManager#createQuery(String)} + {@code executeUpdate()}, not Hibernate {@link
 * Session#createMutationQuery(String)}.
 */
@Tag("UnitTest")
class PSWorkflowServiceUpdateVersionTest {

  private PSWorkflowService service;
  private Session session;
  private EntityManager entityManager;
  private IPSCacheAccess cache;

  @BeforeEach
  void setUp() {
    cache = mock(IPSCacheAccess.class);
    service = new PSWorkflowService(cache, mock(IPSGuidManager.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  @Test
  @DisplayName("updateWorkflowVersion uses EntityManager.createQuery executeUpdate")
  void updateWorkflowVersionUsesJpaCreateQuery() {
    IPSGuid id = mock(IPSGuid.class);
    when(id.getUUID()).thenReturn(42);

    @SuppressWarnings("unchecked")
    Query<Integer> versionQuery = mock(Query.class);
    when(session.createQuery(eq(PSWorkflowService.WORKFLOW_VERSION_HQL), eq(Integer.class)))
        .thenReturn(versionQuery);
    when(versionQuery.setParameter("id", 42L)).thenReturn(versionQuery);
    when(versionQuery.list()).thenReturn(Collections.singletonList(3));

    jakarta.persistence.Query update = mock(jakarta.persistence.Query.class);
    when(entityManager.createQuery(PSWorkflowService.UPDATE_WORKFLOW_VERSION_HQL)).thenReturn(update);
    when(update.setParameter("version", 4)).thenReturn(update);
    when(update.setParameter("id", 42L)).thenReturn(update);

    service.updateWorkflowVersion(id);

    verify(entityManager).createQuery(PSWorkflowService.UPDATE_WORKFLOW_VERSION_HQL);
    verify(update).setParameter("version", 4);
    verify(update).setParameter("id", 42L);
    verify(update).executeUpdate();
    verify(session, never()).createMutationQuery(anyString());
    verify(cache).evict(id, "workflow");
  }
}
