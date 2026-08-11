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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
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

/**
 * Ensures getTemplate maps assembly template body into TemplateDetail.templateSource (#3039).
 */
@Tag("UnitTest")
class TemplateAdaptorGetTest {

  private static PSAssemblyTemplate mockTemplate(String name, String source) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    PSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 42L);
    when(template.getGUID()).thenReturn(guid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn("Label-" + name);
    when(template.getDescription()).thenReturn("desc");
    when(template.getAssembler()).thenReturn("Java/global/percussion/assembly/velocityAssembler");
    when(template.getBindings()).thenReturn(new ArrayList<>());
    when(template.getSlots()).thenReturn(new HashSet<>());
    when(template.getTemplate()).thenReturn(source);
    when(template.isVariant()).thenReturn(false);
    return template;
  }

  @Test
  void getTemplate_populatesNonEmptyTemplateSource() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    String body = "#header()\n$body\n#footer()\n";
    // Build template mock before stubbing asm (avoid nested when/thenReturn UnfinishedStubbing).
    PSAssemblyTemplate template = mockTemplate("site.base", body);
    when(asm.findTemplateByName("site.base")).thenReturn(template);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail out = adaptor.getTemplate(null, "site.base");

    assertNotNull(out);
    assertEquals("site.base", out.getName());
    assertEquals(body, out.getTemplateSource());
  }

  @Test
  void getTemplate_preservesEmptyTemplateSource() throws Exception {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    PSAssemblyTemplate template = mockTemplate("empty.tpl", "");
    when(asm.findTemplateByName("empty.tpl")).thenReturn(template);

    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    TemplateDetail out = adaptor.getTemplate(null, "empty.tpl");

    assertNotNull(out);
    assertEquals("", out.getTemplateSource());
  }

  @Test
  void getTemplate_nullWhenBlankId() {
    IPSAssemblyService asm = mock(IPSAssemblyService.class);
    IPSContentWs contentWs = mock(IPSContentWs.class);
    TemplateAdaptor adaptor = new TemplateAdaptor(asm, contentWs);
    assertNull(adaptor.getTemplate(null, "  "));
    assertNull(adaptor.getTemplate(null, null));
  }
}
