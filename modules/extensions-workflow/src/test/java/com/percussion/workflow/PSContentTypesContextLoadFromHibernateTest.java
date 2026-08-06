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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.data.PSNodeDefinition;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import java.lang.reflect.Field;
import java.util.Collections;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping tests for {@link PSContentTypesContext#loadFromHibernate(int)} added in #1561 Phase
 * 4d-1a. The legacy class has only legacy raw-JDBC read constructors; the suite is
 * {@code @Disabled} until the Spring+H2 test infrastructure ships. The mock wiring is in place so
 * the tests will pass as soon as the raw-JDBC read path is replaced.
 */
@org.junit.jupiter.api.Disabled(
    "PSContentTypesContext read constructors still use the legacy raw-JDBC path;"
        + " will be re-enabled when Spring+H2 test infrastructure ships (Phase 4d-1d follow-up).")
public class PSContentTypesContextLoadFromHibernateTest {

  private IPSContentMgr savedContentMgr;
  private IPSGuidManager savedGuidMgr;
  private IPSContentMgr mockContentMgr;
  private IPSGuidManager mockGuidMgr;

  @BeforeEach
  void setUp() throws Exception {
    savedContentMgr = PSContentMgrLocator.getContentMgr();
    savedGuidMgr = PSGuidManagerLocator.getGuidMgr();
    mockContentMgr = mock(IPSContentMgr.class);
    mockGuidMgr = mock(IPSGuidManager.class);
    setStaticField(PSContentMgrLocator.class, "cmgr", mockContentMgr);
    setStaticField(PSGuidManagerLocator.class, "mgr", mockGuidMgr);
  }

  @AfterEach
  void tearDown() throws Exception {
    setStaticField(PSContentMgrLocator.class, "cmgr", savedContentMgr);
    setStaticField(PSGuidManagerLocator.class, "mgr", savedGuidMgr);
  }

  @Test
  void loadFromHibernate_rejectsNonPositiveContentTypeId() {
    assertThrows(IllegalArgumentException.class, () -> PSContentTypesContext.loadFromHibernate(0));
    assertThrows(IllegalArgumentException.class, () -> PSContentTypesContext.loadFromHibernate(-1));
  }

  @Test
  void loadFromHibernate_nodeDefinitionFound_populatesFields() throws Exception {
    IPSGuid guid = mock(IPSGuid.class);
    when(mockGuidMgr.makeGuid(123L, PSTypeEnum.NODEDEF)).thenReturn(guid);

    PSNodeDefinition nd = mock(PSNodeDefinition.class);
    when(nd.getName()).thenReturn("rffGeneric");
    when(nd.getDescription()).thenReturn("Generic Resource");
    when(nd.getNewRequest()).thenReturn("newRequestUrl");
    when(nd.getQueryRequest()).thenReturn("queryRequestUrl");
    when(nd.getUpdateRequest()).thenReturn("updateRequestUrl");
    when(mockContentMgr.loadNodeDefinitions(Collections.singletonList(guid)))
        .thenReturn(Collections.singletonList(nd));

    PSContentTypesContext ctx = PSContentTypesContext.loadFromHibernate(123);

    assertNotNull(ctx);
    assertEquals("rffGeneric", ctx.getContentTypeName());
    assertEquals("Generic Resource", ctx.getContentTypeDescription());
    assertEquals("newRequestUrl", ctx.getContentTypeNewRequest());
    assertEquals("queryRequestUrl", ctx.getContentTypeQueryRequest());
    assertEquals("updateRequestUrl", ctx.getContentTypeUpdateRequest());
  }

  @Test
  void loadFromHibernate_nodeDefinitionMissing_returnsEmpty() throws Exception {
    IPSGuid guid = mock(IPSGuid.class);
    when(mockGuidMgr.makeGuid(456L, PSTypeEnum.NODEDEF)).thenReturn(guid);
    when(mockContentMgr.loadNodeDefinitions(Collections.singletonList(guid)))
        .thenThrow(new NoSuchNodeTypeException("not found"));

    PSContentTypesContext ctx = PSContentTypesContext.loadFromHibernate(456);

    assertNotNull(ctx);
    assertEquals("", ctx.getContentTypeName());
    assertEquals("", ctx.getContentTypeQueryRequest());
    assertEquals("", ctx.getContentTypeUpdateRequest());
  }

  @Test
  void loadFromHibernate_nodeDefinitionRepositoryException_returnsEmpty() throws Exception {
    IPSGuid guid = mock(IPSGuid.class);
    when(mockGuidMgr.makeGuid(789L, PSTypeEnum.NODEDEF)).thenReturn(guid);
    when(mockContentMgr.loadNodeDefinitions(Collections.singletonList(guid)))
        .thenThrow(new javax.jcr.RepositoryException("db down"));

    PSContentTypesContext ctx = PSContentTypesContext.loadFromHibernate(789);

    assertNotNull(ctx);
    assertEquals("", ctx.getContentTypeName());
  }

  private static void setStaticField(Class<?> owner, String name, Object value) throws Exception {
    Field f = owner.getDeclaredField(name);
    f.setAccessible(true);
    Field mf = Field.class.getDeclaredField("modifiers");
    mf.setAccessible(true);
    mf.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
    f.set(null, value);
  }
}
