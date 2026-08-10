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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class TemplateAdaptorUpdateTest {

  private static PSAssemblyTemplate mockTemplate(String name, String assembler) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    PSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 42L);
    when(template.getGUID()).thenReturn(guid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn("Base");
    when(template.getAssembler()).thenReturn(assembler);
    when(template.getBindings()).thenReturn(new ArrayList<>());
    when(template.getSlots()).thenReturn(new HashSet<>());
    when(template.getTemplate()).thenReturn("#header");
    return template;
  }

  @Test
  void updateTemplate_setsAssemblerWhenProvided() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    when(asm.findTemplateByName("site.base")).thenReturn(template);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setAssembler("Java/global/percussion/assembly/htmlAssembler");

    TemplateDetail out = adaptor.updateTemplate(null, "site.base", body);

    verify(template).setAssembler("Java/global/percussion/assembly/htmlAssembler");
    verify(asm).saveTemplate(template);
    assertEquals("site.base", out.getName());
  }

  @Test
  void updateTemplate_rejectsBlankAssembler() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    when(asm.findTemplateByName("site.base")).thenReturn(template);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setAssembler("   ");

    assertThrows(
        IllegalArgumentException.class, () -> adaptor.updateTemplate(null, "site.base", body));
    verify(template, never()).setAssembler(anyString());
    verify(asm, never()).saveTemplate(template);
  }

  @Test
  void updateTemplate_leavesAssemblerWhenNull() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate template =
        mockTemplate("site.base", "Java/global/percussion/assembly/velocityAssembler");
    when(asm.findTemplateByName("site.base")).thenReturn(template);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail body = new TemplateDetail();
    body.setLabel("Renamed");

    adaptor.updateTemplate(null, "site.base", body);

    verify(template).setLabel("Renamed");
    verify(template, never()).setAssembler(anyString());
    verify(asm).saveTemplate(template);
  }
}
