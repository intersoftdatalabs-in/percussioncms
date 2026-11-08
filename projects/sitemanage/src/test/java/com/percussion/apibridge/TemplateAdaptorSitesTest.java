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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.DesignGap;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Template detail Sites tab read path (#5742, parent #1690 §7.3): {@code associatedSites}
 * reverse lookup from site/template publishing associations. Read-only first; PUT ignores
 * sites (see {@code TemplateDetailJsonReaderTest}).
 */
@Tag("UnitTest")
class TemplateAdaptorSitesTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSSiteManager siteManager;
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
    siteManager = mock(IPSSiteManager.class);
    adaptor = new TemplateAdaptor(asm, contentWs, null, null, null, siteManager, () -> true);
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

  private Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> assoc(
      String siteName, long siteUuid, IPSGuid... templates) {
    Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> out = new HashMap<>();
    out.put(new PSPair<>(new PSGuid(PSTypeEnum.SITE, siteUuid), siteName), List.of(templates));
    return out;
  }

  private TemplateDetail get(String name) throws Exception {
    // Build the template mock before stubbing asm (avoid nested when/thenReturn
    // UnfinishedStubbing).
    PSAssemblyTemplate template = mockTemplate(name);
    when(asm.findTemplateByName(name)).thenReturn(template);
    return adaptor.getTemplate(null, name);
  }

  @Test
  void getTemplate_listsOnlySitesUsingTemplate() throws Exception {
    Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> all = new HashMap<>();
    all.putAll(assoc("EI", 7L, templateGuid));
    all.putAll(
        assoc(
            "Other",
            9L,
            new PSGuid(PSTypeEnum.TEMPLATE, 777L)));
    when(siteManager.findSiteTemplatesAssociations()).thenReturn(all);

    TemplateDetail out = get("site.base");

    assertNotNull(out);
    assertNotNull(out.getAssociatedSites());
    assertEquals(1, out.getAssociatedSites().size());
    assertEquals("EI", out.getAssociatedSites().get(0).getName());
    assertNotNull(out.getAssociatedSites().get(0).getGuid());
    assertTrue(out.getDesignGaps().isEmpty(), () -> out.getDesignGaps().toString());
  }

  @Test
  void getTemplate_emptySitesWhenNoSiteUsesTemplate() throws Exception {
    when(siteManager.findSiteTemplatesAssociations())
        .thenReturn(assoc("Other", 9L, new PSGuid(PSTypeEnum.TEMPLATE, 777L)));

    TemplateDetail out = get("site.base");

    assertNotNull(out);
    assertNotNull(out.getAssociatedSites());
    assertTrue(out.getAssociatedSites().isEmpty());
    assertTrue(out.getDesignGaps().isEmpty(), () -> out.getDesignGaps().toString());
  }

  @Test
  void getTemplate_siteLoadFailureAddsGap() throws Exception {
    when(siteManager.findSiteTemplatesAssociations())
        .thenThrow(new RuntimeException("site db down"));

    TemplateDetail out = get("site.base");

    assertNotNull(out);
    assertNotNull(out.getAssociatedSites());
    assertTrue(out.getAssociatedSites().isEmpty());
    List<String> codes =
        out.getDesignGaps().stream().map(DesignGap::getCode).collect(Collectors.toList());
    assertTrue(
        codes.contains(TemplateAdaptor.TPL_SITE_ASSOC_LOAD), () -> codes.toString());
  }

  @Test
  void getTemplate_nullSiteManagerYieldsEmptyWithoutGap() throws Exception {
    TemplateAdaptor noSites =
        new TemplateAdaptor(asm, contentWs, null, null, null, null, () -> true);
    PSAssemblyTemplate template = mockTemplate("site.base");
    when(asm.findTemplateByName("site.base")).thenReturn(template);

    TemplateDetail out = noSites.getTemplate(null, "site.base");

    assertNotNull(out);
    assertNotNull(out.getAssociatedSites());
    assertTrue(out.getAssociatedSites().isEmpty());
    assertTrue(out.getDesignGaps().isEmpty(), () -> out.getDesignGaps().toString());
  }
}
