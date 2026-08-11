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
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.monitor.process.PSSearchIndexProcessMonitor;
import com.percussion.monitor.process.PSWorkflowAssignmentProcessMonitor;
import com.percussion.pagemanagement.assembler.impl.PSProxyAssemblyTemplate;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.sitemanage.servlet.PSServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for service/listener this-escape mitigations (issue #2999): path root field
 * seeds, final leaf types, proxy template seeds, and servlet request wrapper construction.
 */
@Tag("UnitTest")
class PSThisEscapeServiceListenerTest {

  @Test
  void sitePathItemServiceCtorSeedsRootNameWithoutSetRootName() {
    var service =
        new PSSitePathItemService(
            null, null, null, null, null, null, null, null, null, null, null, null);
    assertEquals("Sites", service.getRootName());
    service.setRootName("Other");
    assertEquals("Other", service.getRootName());
  }

  @Test
  void proxyAssemblyTemplateCtorSeedsFields() {
    var wrapped = new PSAssemblyTemplate();
    wrapped.setTemplate("tpl-body");
    wrapped.setAssembler("asm");
    wrapped.setName("MyTemplate");
    var binding = new PSTemplateBinding();
    wrapped.setBindings(new Vector<>(List.of(binding)));

    var proxy = new PSProxyAssemblyTemplate(wrapped);
    assertEquals("tpl-body", proxy.getTemplate());
    assertEquals("asm", proxy.getAssembler());
    assertEquals("MyTemplate", proxy.getName());
    assertEquals(1, proxy.getBindings().size());
    assertTrue(Modifier.isFinal(PSProxyAssemblyTemplate.class.getModifiers()));
  }

  @Test
  void servletRequestWrapperCopiesParameters() {
    var request = mock(HttpServletRequest.class);
    when(request.getParameterMap()).thenReturn(Map.of("a", new String[] {"1", "2"}));
    var wrapper = new PSServletRequestWrapper(request);
    assertEquals("1", wrapper.getParameter("a"));
    assertEquals(2, wrapper.getParameterValues("a").length);
    assertTrue(Modifier.isFinal(PSServletRequestWrapper.class.getModifiers()));
  }

  @Test
  void serviceListenerLeafTypesAreFinal() {
    assertTrue(Modifier.isFinal(PSSearchIndexProcessMonitor.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSWorkflowAssignmentProcessMonitor.class.getModifiers()));
    assertFalse(Modifier.isFinal(PSSitePathItemService.class.getModifiers()));
  }
}
