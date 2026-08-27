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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * CD-01 POST create persists via {@code createContentTypes} then {@code saveContentTypes}
 * (Workbench Finish). Admin only; unique name; no spaces.
 */
@Tag("UnitTest")
class ContentTypeAdaptorCreateTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    when(designWs.findContentTypes("*")).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSItemDefinition def = stubCreatedDefinition("percNewType", 9001);
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));

    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    body.setLabel("New Type");
    body.setDescription("created via REST");

    ContentTypeDetail out = adaptor.createContentType(null, body);

    assertEquals("percNewType", out.getName());
    assertEquals("New Type", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    verify(designWs).createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(def).setLabel("New Type");
    verify(def).setDescription("created via REST");
    verify(def).setEnabled(true);
  }

  @Test
  void create_omittedEnabled_defaultsTrue() throws Exception {
    PSItemDefinition def = stubCreatedDefinition("percNewType", 9001);
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");

    ContentTypeDetail out = adaptor.createContentType(null, body);

    verify(def).setEnabled(true);
    assertEquals(Boolean.TRUE, out.getEnabled());
  }

  @Test
  void create_explicitEnabledFalse_isHonored() throws Exception {
    PSItemDefinition def = stubCreatedDefinition("percNewType", 9001);
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    body.setEnabled(false);

    ContentTypeDetail out = adaptor.createContentType(null, body);

    verify(def).setEnabled(false);
    assertEquals(Boolean.FALSE, out.getEnabled());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException(
                "The name 'percNewType' for type 'NODEDEF' already exists."));
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createContentType(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicateFromPsError_is409() throws Exception {
    PSErrorException pe =
        new PSErrorException(
            11, "The name 'percNewType' for type 'NODEDEF' already exists.", "stack");
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenThrow(pe);
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createContentType(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createContentType(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("percPage");
    when(designWs.findContentTypes("*")).thenReturn(List.of(existing));

    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percpage");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createContentType(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createContentType(null, null));
    ContentTypeDetail blank = new ContentTypeDetail();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createContentType(null, blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() throws Exception {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createContentType(null, body));
    assertTrue(ex.getMessage().contains("spaces"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void create_wildcardName_throwsBeforeDesignWs() throws Exception {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("perc*");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createContentType(null, body));
    assertTrue(ex.getMessage().contains("wildcard"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() throws Exception {
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createContentType(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void create_emptyDesignWs_throwsIllegalState() throws Exception {
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenReturn(Collections.emptyList());
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createContentType(null, body));
    assertTrue(ex.getMessage().contains("createContentTypes returned empty"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_designWsError_throwsIllegalState() throws Exception {
    when(designWs.createContentTypes(eq(List.of("percNewType")), eq("test-session"), eq("Admin")))
        .thenThrow(new PSErrorException());
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createContentType(null, body));
    assertTrue(ex.getMessage().contains("Failed to create content type"));
  }

  private PSItemDefinition stubCreatedDefinition(String name, int typeId) throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(name);
    when(def.getLabel()).thenReturn(name);
    when(def.getDescription()).thenReturn("");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_" + name);
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_" + name + "/" + name + ".html");
    when(def.getTypeId()).thenReturn(typeId);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(null);
    doAnswer(
            inv -> {
              when(def.getLabel()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setLabel(any());
    doAnswer(
            inv -> {
              when(def.getDescription()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setDescription(any());
    doAnswer(
            inv -> {
              when(def.isEnabled()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setEnabled(anyBoolean());
    when(itemDefManager.getItemDef(eq(name), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    return def;
  }
}
