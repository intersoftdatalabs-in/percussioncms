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
package com.percussion.services.sitemgr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

/** Lookup by context id must query the scheme table (#5109). */
class PSSiteManagerFindSchemesByContextIdTest {

  @Test
  void findSchemesByContextIdQueriesMatchingSchemes() throws Exception {
    IPSGuid contextId = new PSGuid(PSTypeEnum.CONTEXT, 42L);
    EntityManager entityManager = mock(EntityManager.class);
    Session session = mock(Session.class);
    @SuppressWarnings("unchecked")
    Query<IPSLocationScheme> query = mock(Query.class);
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.createQuery(
            "from PSLocationScheme where contextId = :ctxId", IPSLocationScheme.class))
        .thenReturn(query);
    when(query.setParameter("ctxId", contextId.longValue())).thenReturn(query);
    when(query.list()).thenReturn(List.of(scheme));

    PSSiteManager manager = new PSSiteManager();
    Field entityManagerField = PSSiteManager.class.getDeclaredField("entityManager");
    entityManagerField.setAccessible(true);
    entityManagerField.set(manager, entityManager);

    assertEquals(List.of(scheme), manager.findSchemesByContextId(contextId));
    verify(query).setParameter("ctxId", contextId.longValue());
  }

  @Test
  void findSchemesByContextIdRejectsNull() {
    PSSiteManager manager = new PSSiteManager();
    NullPointerException thrown =
        assertThrows(NullPointerException.class, () -> manager.findSchemesByContextId(null));
    assertTrue(thrown.getMessage().contains("contextid"));
  }
}
