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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
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
import org.w3c.dom.Element;

/** Copy a content type to a new name, including design fields (#4893). */
@Tag("UnitTest")
class ContentTypeAdaptorCopyTest {

  private IPSContentDesignWs designWs;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor =
        new ContentTypeAdaptor(
            designWs, itemDefManager, mock(IPSSystemDesignWs.class), () -> true);
    when(designWs.findContentTypes("*")).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void copy_createsDistinctTypeIncludingSourceFields() throws Exception {
    PSItemDefinition source =
        ContentTypeAdaptor.parseDesignXml(
            ContentTypeAdaptorImportTest.sampleImportXml("srcType", "Source"));
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, source.getTypeId());
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("srcType");
    when(designWs.findContentTypes("srcType")).thenReturn(List.of(sum));
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(source));
    PSItemDefinition dest = stubCreated("copyType", 9101);
    when(designWs.createContentTypes(eq(List.of("copyType")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(dest));

    ContentTypeDetail out = adaptor.copyContentType(null, "srcType", "copyType");

    assertEquals("copyType", out.getName());
    ArgumentCaptor<Element> applied = ArgumentCaptor.forClass(Element.class);
    verify(dest).fromXml(applied.capture(), any(), any());
    String appliedXml = com.percussion.xml.PSXmlDocumentBuilder.toString(applied.getValue());
    assertTrue(appliedXml.contains("sys_title"), appliedXml);
    assertTrue(appliedXml.contains("copyType"), appliedXml);
    verify(designWs, never()).saveContentTypes(eq(List.of(source)), anyBoolean(), any(), any());
  }

  @Test
  void copy_duplicateNameIs409() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("copyType");
    when(designWs.findContentTypes("*")).thenReturn(List.of(existing));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.copyContentType(null, "srcType", "copyType"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentTypes(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void copy_illegalNameIs400() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.copyContentType(null, "srcType", "bad name"));
  }

  @Test
  void copy_folderSystemTypeIsClearError() throws Exception {
    PSItemDefinition folder = mock(PSItemDefinition.class);
    when(folder.getName()).thenReturn("Folder");
    when(folder.getTypeId()).thenReturn(101);
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 101L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("Folder");
    when(designWs.findContentTypes("Folder")).thenReturn(List.of(sum));
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(folder));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.copyContentType(null, "Folder", "FolderCopy"));
    assertTrue(ex.getMessage().contains("cannot be copied"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void copy_unknownSourceReturnsNull() {
    when(designWs.findContentTypes("missing")).thenReturn(Collections.emptyList());
    assertEquals(null, adaptor.copyContentType(null, "missing", "copyType"));
  }

  private PSItemDefinition stubCreated(String name, int typeId) throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(name);
    when(def.getLabel()).thenReturn(name);
    when(def.getDescription()).thenReturn("");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("psx_ce" + name);
    when(def.getEditorUrl()).thenReturn("../psx_ce" + name + "/" + name + ".html");
    when(def.getTypeId()).thenReturn(typeId);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(mock(PSContentEditor.class));
    doNothing().when(def).fromXml(any(), any(), any());
    doAnswer(
            inv -> {
              when(def.getName()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setName(any());
    doAnswer(
            inv -> {
              when(def.getTypeId()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setTypeId(anyInt());
    when(itemDefManager.getItemDef(eq(name), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    return def;
  }
}
