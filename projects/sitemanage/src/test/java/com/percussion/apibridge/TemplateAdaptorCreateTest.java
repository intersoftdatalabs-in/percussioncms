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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyErrors;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class TemplateAdaptorCreateTest {

  private static PSAssemblyTemplate mockTemplate(String name) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    PSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 99L);
    when(template.getGUID()).thenReturn(guid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn(name);
    when(template.getAssembler()).thenReturn(TemplateAdaptor.DEFAULT_CREATE_ASSEMBLER);
    when(template.getBindings()).thenReturn(new ArrayList<>());
    when(template.getSlots()).thenReturn(new HashSet<>());
    when(template.getTemplate()).thenReturn("");
    return template;
  }

  @Test
  void validateCreateName_requiresLetterStart() {
    assertEquals("site.html.snippet", TemplateAdaptor.validateCreateName(" site.html.snippet "));
    assertThrows(IllegalArgumentException.class, () -> TemplateAdaptor.validateCreateName(""));
    assertThrows(IllegalArgumentException.class, () -> TemplateAdaptor.validateCreateName("has space"));
    assertThrows(IllegalArgumentException.class, () -> TemplateAdaptor.validateCreateName("1bad"));
  }

  @Test
  void createTemplate_savesHtmlFirstDefault() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate created = mockTemplate("site.html.snippet");
    when(asm.createTemplate()).thenReturn(created);
    when(asm.findTemplateByName("site.html.snippet"))
        .thenThrow(new PSAssemblyException(IPSAssemblyErrors.TEMPLATE_MISSING, "site.html.snippet"))
        .thenReturn(created);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setName("site.html.snippet");

    TemplateDetail out = adaptor.createTemplate(null, body);

    verify(created).setName("site.html.snippet");
    verify(created).setAssembler(TemplateAdaptor.DEFAULT_CREATE_ASSEMBLER);
    verify(asm).saveTemplate(created);
    assertEquals("site.html.snippet", out.getName());
  }

  @Test
  void createTemplate_rejectsDuplicateName() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate existing = mockTemplate("site.html.snippet");
    when(asm.findTemplateByName("site.html.snippet")).thenReturn(existing);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setName("site.html.snippet");

    assertThrows(IllegalArgumentException.class, () -> adaptor.createTemplate(null, body));
    verify(asm, never()).createTemplate();
    verify(asm, never()).saveTemplate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createTemplate_rejectsBlankBody() {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertThrows(IllegalArgumentException.class, () -> adaptor.createTemplate(null, null));
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.createTemplate(null, new TemplateDetail()));
    verify(asm, never()).createTemplate();
  }

  @Test
  void createTemplate_usesProvidedAssembler() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate created = mockTemplate("md.note");
    when(asm.createTemplate()).thenReturn(created);
    when(asm.findTemplateByName("md.note"))
        .thenThrow(new PSAssemblyException(IPSAssemblyErrors.TEMPLATE_MISSING, "md.note"))
        .thenReturn(created);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setName("md.note");
    body.setAssembler("Java/global/percussion/assembly/markdownAssembler");

    adaptor.createTemplate(null, body);

    verify(created).setAssembler("Java/global/percussion/assembly/markdownAssembler");
    verify(asm).saveTemplate(created);
  }

  @Test
  void createTemplate_lookupFailureIsNotTreatedAsAvailable() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    when(asm.findTemplateByName("site.html.snippet"))
        .thenThrow(new PSAssemblyException(IPSAssemblyErrors.UNKNOWN_ERROR, "db down"));

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setName("site.html.snippet");

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createTemplate(null, body));
    assertEquals("Failed to look up template name: site.html.snippet", ex.getMessage());
    verify(asm, never()).createTemplate();
    verify(asm, never()).saveTemplate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createTemplate_saveRaceMapsDuplicateTo400() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate created = mockTemplate("site.html.snippet");
    PSAssemblyTemplate existing = mockTemplate("site.html.snippet");
    when(asm.createTemplate()).thenReturn(created);
    when(asm.findTemplateByName("site.html.snippet"))
        .thenThrow(new PSAssemblyException(IPSAssemblyErrors.TEMPLATE_MISSING, "site.html.snippet"))
        .thenReturn(existing);
    org.mockito.Mockito.doThrow(
            new PSAssemblyException(IPSAssemblyErrors.UNKNOWN_CRUD_ERROR, "constraint"))
        .when(asm)
        .saveTemplate(created);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setName("site.html.snippet");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createTemplate(null, body));
    assertEquals("template name already exists: site.html.snippet", ex.getMessage());
  }
}
