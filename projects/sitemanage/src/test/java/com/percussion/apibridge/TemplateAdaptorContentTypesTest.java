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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.contentmgr.data.PSContentTemplateDesc;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class TemplateAdaptorContentTypesTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSAssemblyDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private IPSContentDesignWs contentDesign;
  private TemplateAdaptor adaptor;
  private IPSGuid templateGuid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    asm = mock(IPSAssemblyService.class);
    contentWs = mock(IPSContentWs.class);
    designWs = mock(IPSAssemblyDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    contentDesign = mock(IPSContentDesignWs.class);
    adaptor =
        new TemplateAdaptor(asm, contentWs, designWs, systemDesign, contentDesign, () -> true);
    templateGuid = new PSGuid(PSTypeEnum.TEMPLATE, 42L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  private PSAssemblyTemplate mockTemplate(String name) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    when(template.getGUID()).thenReturn(templateGuid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn("Page");
    when(template.getAssembler()).thenReturn("Java/global/percussion/assembly/htmlAssembler");
    when(template.getBindings()).thenReturn(new ArrayList<>());
    when(template.getSlots()).thenReturn(new HashSet<>());
    when(template.getTemplate()).thenReturn("#header");
    return template;
  }

  private IPSCatalogSummary mockCtSummary(long uuid, String name, String label) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    IPSGuid g = new PSGuid(PSTypeEnum.NODEDEF, uuid);
    when(sum.getGUID()).thenReturn(g);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(label);
    return sum;
  }

  private PSContentTemplateDesc desc(long ctUuid, IPSGuid tpl) {
    PSContentTemplateDesc d = new PSContentTemplateDesc();
    d.setContentTypeId(new PSGuid(PSTypeEnum.NODEDEF, ctUuid));
    d.setTemplateId(tpl);
    return d;
  }

  private void stubLookup(String name, PSAssemblyTemplate template) throws Exception {
    when(asm.findTemplateByName(name)).thenReturn(template);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(templateGuid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findAssemblyTemplates(
            eq(name), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(sum));
    when(designWs.loadAssemblyTemplates(anyList(), eq(true), anyBoolean(), any(), any()))
        .thenReturn(List.of(new PSAssemblyTemplateWs(template, Collections.emptyMap())));
    PSObjectSummary held = new PSObjectSummary(templateGuid, name);
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  @Test
  void getTemplate_listsAssociatedContentTypesWithNameAndGuid() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    when(asm.findTemplateByName("perc.page")).thenReturn(template);
    IPSCatalogSummary page = mockCtSummary(311L, "percPage", "Page");
    when(contentDesign.findContentTypes(null)).thenReturn(List.of(page));
    when(contentDesign.loadAssociatedTemplates(isNull(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(desc(311L, templateGuid)));

    TemplateDetail out = adaptor.getTemplate(null, "perc.page");

    assertNotNull(out);
    assertNotNull(out.getAssociatedContentTypes());
    assertEquals(1, out.getAssociatedContentTypes().size());
    NamedObjectRef ref = out.getAssociatedContentTypes().get(0);
    assertEquals("percPage", ref.getName());
    assertEquals("Page", ref.getLabel());
    assertNotNull(ref.getGuid());
    assertEquals(311, ref.getGuid().getUuid());
    assertTrue(out.getDesignGaps() == null || out.getDesignGaps().isEmpty());
  }

  @Test
  void updateTemplate_replacesAssociationsWithHeldLock() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    stubLookup("perc.page", template);
    IPSCatalogSummary page = mockCtSummary(311L, "percPage", "Page");
    IPSCatalogSummary image = mockCtSummary(312L, "percImageAsset", "Image Asset");
    when(contentDesign.findContentTypes(null)).thenReturn(List.of(page, image));
    when(contentDesign.loadAssociatedTemplates(isNull(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(desc(311L, templateGuid)));
    when(contentDesign.loadAssociatedTemplates(any(IPSGuid.class), eq(true), eq(true), any(), any()))
        .thenReturn(List.of(desc(311L, templateGuid)));

    NamedObjectRef want = new NamedObjectRef();
    want.setName("percImageAsset");
    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of(want));

    TemplateDetail out = adaptor.updateTemplate(null, "perc.page", body);

    assertNotNull(out);
    verify(contentDesign, times(2))
        .saveAssociatedTemplates(any(IPSGuid.class), anyList(), eq(true), any(), any());
    verify(asm).saveTemplate(template);
  }

  @Test
  void updateTemplate_emptyListClearsAssociations() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    stubLookup("perc.page", template);
    IPSCatalogSummary page = mockCtSummary(311L, "percPage", "Page");
    when(contentDesign.findContentTypes(null)).thenReturn(List.of(page));
    when(contentDesign.loadAssociatedTemplates(isNull(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(desc(311L, templateGuid)));
    when(contentDesign.loadAssociatedTemplates(any(IPSGuid.class), eq(true), eq(true), any(), any()))
        .thenReturn(List.of(desc(311L, templateGuid)));

    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of());

    adaptor.updateTemplate(null, "perc.page", body);

    verify(contentDesign)
        .saveAssociatedTemplates(any(IPSGuid.class), eq(List.of()), eq(true), any(), any());
  }

  @Test
  void updateTemplate_omitPreservesAssociations() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    stubLookup("perc.page", template);

    TemplateDetail body = new TemplateDetail();
    body.setLabel("Renamed");

    adaptor.updateTemplate(null, "perc.page", body);

    verify(template).setLabel("Renamed");
    verify(contentDesign, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateTemplate_unknownContentType_is400() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    stubLookup("perc.page", template);
    when(contentDesign.findContentTypes(null)).thenReturn(List.of());
    when(contentDesign.loadAssociatedTemplates(isNull(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());

    NamedObjectRef want = new NamedObjectRef();
    want.setName("noSuchType");
    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of(want));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateTemplate(null, "perc.page", body));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    verify(contentDesign, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateTemplate_guidUnknownContentType_is400() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    stubLookup("perc.page", template);
    when(contentDesign.findContentTypes(null)).thenReturn(List.of());
    when(contentDesign.loadAssociatedTemplates(isNull(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());

    NamedObjectRef want = new NamedObjectRef();
    Guid g = new Guid();
    g.setStringValue("0-2-99999");
    want.setGuid(g);
    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of(want));

    assertThrows(
        IllegalArgumentException.class, () -> adaptor.updateTemplate(null, "perc.page", body));
    verify(contentDesign, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateTemplate_unlocked_is409() throws Exception {
    PSAssemblyTemplate template = mockTemplate("perc.page");
    when(asm.findTemplateByName("perc.page")).thenReturn(template);
    PSObjectSummary unlocked = new PSObjectSummary(templateGuid, "perc.page");
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(unlocked));

    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateTemplate(null, "perc.page", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(contentDesign, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateTemplate_notAdmin_is403() {
    TemplateAdaptor denied =
        new TemplateAdaptor(asm, contentWs, designWs, systemDesign, contentDesign, () -> false);
    TemplateDetail body = new TemplateDetail();
    body.setAssociatedContentTypes(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.updateTemplate(null, "perc.page", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void getTemplate_missingTemplate_returnsNull() throws Exception {
    when(asm.findTemplateByName("missing")).thenReturn(null);
    assertEquals(null, adaptor.getTemplate(null, "missing"));
  }
}
