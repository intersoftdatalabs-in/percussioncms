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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.templates.TemplateBindingSummary;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
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
class TemplateAdaptorUpdateTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSAssemblyDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private TemplateAdaptor adaptor;
  private IPSGuid guid;

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
    adaptor = new TemplateAdaptor(asm, contentWs, designWs, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.TEMPLATE, 42L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  private PSAssemblyTemplate mockTemplate(String name, String assembler) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    when(template.getGUID()).thenReturn(guid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn("Base");
    when(template.getAssembler()).thenReturn(assembler);
    when(template.getBindings()).thenReturn(new ArrayList<>());
    when(template.getSlots()).thenReturn(new HashSet<>());
    when(template.getTemplate()).thenReturn("#header");
    return template;
  }

  private void stubLookup(String name, PSAssemblyTemplate template) throws Exception {
    when(asm.findTemplateByName(name)).thenReturn(template);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findAssemblyTemplates(eq(name), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(sum));
    when(designWs.loadAssemblyTemplates(anyList(), eq(true), anyBoolean(), any(), any()))
        .thenReturn(List.of(new PSAssemblyTemplateWs(template, Collections.emptyMap())));
    PSObjectSummary held = new PSObjectSummary(guid, name);
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  @Test
  void updateTemplate_setsAssemblerWhenProvided() throws Exception {
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    stubLookup("site.base", template);

    TemplateDetail body = new TemplateDetail();
    body.setAssembler("Java/global/percussion/assembly/htmlAssembler");

    TemplateDetail out = adaptor.updateTemplate(null, "site.base", body);

    verify(template).setAssembler("Java/global/percussion/assembly/htmlAssembler");
    verify(asm).saveTemplate(template);
    assertEquals("site.base", out.getName());
  }

  @Test
  void updateTemplate_rejectsBlankAssembler() throws Exception {
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    stubLookup("site.base", template);

    TemplateDetail body = new TemplateDetail();
    body.setAssembler("   ");

    assertThrows(
        IllegalArgumentException.class, () -> adaptor.updateTemplate(null, "site.base", body));
    verify(template, never()).setAssembler(anyString());
    verify(asm, never()).saveTemplate(template);
  }

  @Test
  void updateTemplate_leavesAssemblerWhenNull() throws Exception {
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    stubLookup("site.base", template);

    TemplateDetail body = new TemplateDetail();
    body.setLabel("Renamed");

    adaptor.updateTemplate(null, "site.base", body);

    verify(template).setLabel("Renamed");
    verify(template, never()).setAssembler(anyString());
    verify(asm).saveTemplate(template);
  }

  @Test
  void updateTemplate_replacesBindingsViaAddRemove() throws Exception {
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    PSTemplateBinding old = new PSTemplateBinding(1, "$old", "0");
    when(template.getBindings()).thenReturn(new ArrayList<>(List.of(old)));
    stubLookup("site.base", template);

    TemplateBindingSummary row = new TemplateBindingSummary();
    row.setExecutionOrder(1);
    row.setVariable("$x");
    row.setExpression("1");
    TemplateDetail body = new TemplateDetail();
    body.setBindings(List.of(row));

    adaptor.updateTemplate(null, "site.base", body);

    verify(template).setBindings(any());
    verify(asm).saveTemplate(template);
  }

  @Test
  void updateTemplate_forbiddenWhenNotAdmin() throws Exception {
    TemplateAdaptor denied =
        new TemplateAdaptor(asm, contentWs, designWs, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.updateTemplate(null, "site.base", new TemplateDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }
}
