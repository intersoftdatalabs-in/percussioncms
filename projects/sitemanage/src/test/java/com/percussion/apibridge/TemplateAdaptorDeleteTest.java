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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
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
class TemplateAdaptorDeleteTest {

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
  void deleteTemplate_deletesByName() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate existing = mockTemplate("site.html.snippet");
    when(asm.findTemplateByName("site.html.snippet")).thenReturn(existing);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertTrue(adaptor.deleteTemplate(null, "site.html.snippet"));
    verify(asm).deleteTemplate(existing.getGUID());
  }

  @Test
  void deleteTemplate_returnsFalseWhenMissing() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    when(asm.findTemplateByName("missing"))
        .thenThrow(
            new PSAssemblyException(AssemblyErrorCodes.TEMPLATE_MISSING.numericCode(), "missing"));

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertFalse(adaptor.deleteTemplate(null, "missing"));
    verify(asm, never()).deleteTemplate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void deleteTemplate_nullGuidIsCorruptionNotNotFound() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate corrupt = mock(PSAssemblyTemplate.class);
    when(corrupt.getGUID()).thenReturn(null);
    when(corrupt.getName()).thenReturn("corrupt.html");
    when(asm.findTemplateByName("corrupt.html")).thenReturn(corrupt);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> adaptor.deleteTemplate(null, "corrupt.html"));
    assertTrue(ex.getMessage().contains("no GUID"));
    verify(asm, never()).deleteTemplate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void deleteTemplate_returnsFalseWhenConcurrentDelete() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate existing = mockTemplate("site.html.snippet");
    var guid = existing.getGUID();
    when(asm.findTemplateByName("site.html.snippet")).thenReturn(existing);
    org.mockito.Mockito.doThrow(
            new PSAssemblyException(
                AssemblyErrorCodes.TEMPLATE_MISSING.numericCode(), "site.html.snippet"))
        .when(asm)
        .deleteTemplate(guid);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertFalse(adaptor.deleteTemplate(null, "site.html.snippet"));
    verify(asm).deleteTemplate(guid);
  }

  @Test
  void deleteTemplate_rejectsBlankId() {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteTemplate(null, "  "));
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteTemplate(null, null));
  }

  @Test
  void deleteTemplate_wrapsAssemblyFailure() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate existing = mockTemplate("site.html.snippet");
    when(asm.findTemplateByName("site.html.snippet")).thenReturn(existing);
    org.mockito.Mockito.doThrow(
            new PSAssemblyException(
                AssemblyErrorCodes.UNKNOWN_CRUD_ERROR.numericCode(), "constraint"))
        .when(asm)
        .deleteTemplate(org.mockito.ArgumentMatchers.any());

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> adaptor.deleteTemplate(null, "site.html.snippet"));
    assertTrue(ex.getMessage().contains("Failed to delete template"));
  }
}
