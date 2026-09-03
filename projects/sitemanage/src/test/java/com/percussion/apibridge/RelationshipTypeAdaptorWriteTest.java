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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.rest.relationshiptypes.RelationshipType;
import com.percussion.rest.relationshiptypes.RelationshipTypeProperty;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SY-03 Admin POST create / PUT update / DELETE user relationship types via {@code
 * IPSSystemDesignWs}. Admin only; system types are 409.
 */
@Tag("UnitTest")
class RelationshipTypeAdaptorWriteTest {

  private IPSSystemDesignWs designWs;
  private RelationshipTypeAdaptor adaptor;
  private final Map<String, PSRelationshipConfig> store = new HashMap<>();
  private final AtomicInteger nextId = new AtomicInteger(100);

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");

    designWs = mock(IPSSystemDesignWs.class);
    adaptor = new RelationshipTypeAdaptor(designWs, () -> true);

    when(designWs.findRelationshipTypes(any(), isNull()))
        .thenAnswer(
            inv -> {
              String name = inv.getArgument(0);
              List<IPSCatalogSummary> out = new ArrayList<>();
              for (PSRelationshipConfig cfg : store.values()) {
                if (cfg == null) {
                  continue;
                }
                if (name == null
                    || name.isBlank()
                    || name.equalsIgnoreCase(cfg.getName())) {
                  out.add(toSummary(cfg));
                }
              }
              return out;
            });

    when(designWs.loadRelationshipTypes(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenAnswer(
            inv -> {
              List<IPSGuid> ids = inv.getArgument(0);
              List<PSRelationshipConfig> out = new ArrayList<>();
              for (IPSGuid id : ids) {
                for (PSRelationshipConfig cfg : store.values()) {
                  if (cfg.isAssinedId() && cfg.getGUID().equals(id)) {
                    out.add(cfg);
                    break;
                  }
                }
              }
              return out;
            });

    when(designWs.createRelationshipTypes(anyList(), anyList(), any(), any()))
        .thenAnswer(
            inv -> {
              List<String> names = inv.getArgument(0);
              List<String> categories = inv.getArgument(1);
              List<PSRelationshipConfig> created = new ArrayList<>();
              for (int i = 0; i < names.size(); i++) {
                PSRelationshipConfig cfg =
                    new PSRelationshipConfig(
                        names.get(i),
                        PSRelationshipConfig.RS_TYPE_USER,
                        categories.get(i));
                cfg.setId(nextId.getAndIncrement());
                store.put(cfg.getName().toLowerCase(), cfg);
                created.add(cfg);
              }
              return created;
            });

    doAnswer(
            inv -> {
              List<PSRelationshipConfig> configs = inv.getArgument(0);
              for (PSRelationshipConfig cfg : configs) {
                if (cfg != null && cfg.getName() != null) {
                  store.put(cfg.getName().toLowerCase(), cfg);
                }
              }
              return null;
            })
        .when(designWs)
        .saveRelationshipTypes(anyList(), anyBoolean(), any(), any());

    doAnswer(
            inv -> {
              List<IPSGuid> ids = inv.getArgument(0);
              store
                  .entrySet()
                  .removeIf(
                      e -> {
                        PSRelationshipConfig cfg = e.getValue();
                        if (cfg == null || !cfg.isAssinedId()) {
                          return false;
                        }
                        for (IPSGuid id : ids) {
                          if (cfg.getGUID().equals(id)) {
                            return true;
                          }
                        }
                        return false;
                      });
              return null;
            })
        .when(designWs)
        .deleteRelationshipTypes(anyList(), anyBoolean(), any(), any());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_persistsUserTypeAndRoundTrips() throws Exception {
    RelationshipType body = userBody("MyUserRel");
    body.setLabel("My User Rel");
    body.setDescription("created via REST");

    RelationshipType out = adaptor.createRelationshipType(body);

    assertEquals("MyUserRel", out.getName());
    assertEquals("My User Rel", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    assertTrue(out.isUserType());
    assertFalse(out.isSystemType());
    assertEquals(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY, out.getCategory());

    RelationshipType fetched = adaptor.findRelationshipType("MyUserRel");
    assertNotNull(fetched);
    assertEquals("My User Rel", fetched.getLabel());

    ArgumentCaptor<List> namesCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> catsCap = ArgumentCaptor.forClass(List.class);
    verify(designWs).createRelationshipTypes(namesCap.capture(), catsCap.capture(), any(), any());
    assertEquals(List.of("MyUserRel"), namesCap.getValue());
    assertEquals(
        List.of(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY), catsCap.getValue());
  }

  @Test
  void create_acceptsCategoryLabel() {
    RelationshipType body = userBody("MyUserRel");
    body.setCategory("Active Assembly");
    RelationshipType out = adaptor.createRelationshipType(body);
    assertEquals(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY, out.getCategory());
  }

  @Test
  void create_copyFromSystemCopiesMutableFields() {
    PSRelationshipConfig system =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    system.setId(1);
    system.setLabel("Active Assembly");
    system.setDescription("system desc");
    store.put(system.getName().toLowerCase(), system);

    RelationshipType body = new RelationshipType();
    body.setName("CopiedRel");
    body.setCopyFrom("ActiveAssembly");

    RelationshipType out = adaptor.createRelationshipType(body);

    assertEquals("CopiedRel", out.getName());
    assertTrue(out.isUserType());
    assertFalse(out.isSystemType());
    assertEquals("Active Assembly", out.getLabel());
    assertEquals("system desc", out.getDescription());
    assertEquals(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY, out.getCategory());
  }

  @Test
  void create_duplicateIs409() {
    seedUser("MyUserRel");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createRelationshipType(userBody("MyUserRel")));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).createRelationshipTypes(anyList(), anyList(), any(), any());
  }

  @Test
  void create_blankNameThrows400() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createRelationshipType(null));
    RelationshipType blank = new RelationshipType();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.createRelationshipType(blank));
    assertTrue(ex.getMessage().contains("name is required"));
  }

  @Test
  void create_missingCategoryThrows() {
    RelationshipType body = new RelationshipType();
    body.setName("MyUserRel");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.createRelationshipType(body));
    assertTrue(ex.getMessage().contains("category"));
  }

  @Test
  void create_nonAdminIs403() {
    RelationshipTypeAdaptor denied = new RelationshipTypeAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.createRelationshipType(userBody("MyUserRel")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void update_mutatesLabelDescriptionAndFlags() {
    seedUser("MyUserRel");
    RelationshipType body = new RelationshipType();
    body.setLabel("Updated Label");
    body.setDescription("updated desc");
    body.setAllowCloning(false);
    body.setUseOwnerRevision(true);
    body.setUseDependentRevision(false);
    body.setUserProperties(List.of(new RelationshipTypeProperty("custom", "v1")));

    RelationshipType out = adaptor.updateRelationshipType("MyUserRel", body);

    assertEquals("Updated Label", out.getLabel());
    assertEquals("updated desc", out.getDescription());
    assertFalse(out.isAllowCloning());
    RelationshipType fetched = adaptor.findRelationshipType("MyUserRel");
    assertEquals("Updated Label", fetched.getLabel());
    assertTrue(
        fetched.getUserProperties().stream()
            .anyMatch(p -> "custom".equals(p.getName()) && "v1".equals(p.getValue())));
  }

  @Test
  void update_systemTypeIs409() {
    PSRelationshipConfig system =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    system.setId(1);
    store.put(system.getName().toLowerCase(), system);

    RelationshipType body = new RelationshipType();
    body.setLabel("Nope");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateRelationshipType("ActiveAssembly", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveRelationshipTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_unknownReturnsNull() {
    RelationshipType body = new RelationshipType();
    body.setLabel("x");
    assertEquals(null, adaptor.updateRelationshipType("missing", body));
  }

  @Test
  void delete_removesUserType() {
    seedUser("MyUserRel");
    assertTrue(adaptor.deleteRelationshipType("MyUserRel"));
    assertEquals(null, adaptor.findRelationshipType("MyUserRel"));
  }

  @Test
  void delete_systemTypeIs409() {
    PSRelationshipConfig system =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    system.setId(1);
    store.put(system.getName().toLowerCase(), system);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteRelationshipType("ActiveAssembly"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteRelationshipTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_unknownReturnsFalse() {
    assertFalse(adaptor.deleteRelationshipType("missing"));
  }

  @Test
  void resolveCategoryCode_mapsLabelAndCode() {
    assertEquals(
        PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY,
        RelationshipTypeAdaptor.resolveCategoryCode("Active Assembly"));
    assertEquals(
        PSRelationshipConfig.CATEGORY_TRANSLATION,
        RelationshipTypeAdaptor.resolveCategoryCode("rs_translation"));
    assertEquals(null, RelationshipTypeAdaptor.resolveCategoryCode("nope"));
  }

  private void seedUser(String name) {
    PSRelationshipConfig cfg =
        new PSRelationshipConfig(
            name,
            PSRelationshipConfig.RS_TYPE_USER,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    cfg.setId(nextId.getAndIncrement());
    cfg.setLabel(name);
    store.put(cfg.getName().toLowerCase(), cfg);
  }

  private static RelationshipType userBody(String name) {
    RelationshipType t = new RelationshipType();
    t.setName(name);
    t.setCategory(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    return t;
  }

  private static IPSCatalogSummary toSummary(PSRelationshipConfig cfg) {
    return new PSObjectSummary(cfg.getGUID(), cfg.getName());
  }
}
