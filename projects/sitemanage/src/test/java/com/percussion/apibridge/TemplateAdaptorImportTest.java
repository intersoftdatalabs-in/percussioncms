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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * AS-08 POST import of Workbench-equivalent {@code assembly-template} design XML via {@code
 * IPSAssemblyDesignWs}. Admin only; 409 on name collision; does not steal locks.
 */
@Tag("UnitTest")
class TemplateAdaptorImportTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSAssemblyDesignWs designWs;
  private TemplateAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    asm = mock(IPSAssemblyService.class);
    contentWs = mock(IPSContentWs.class);
    designWs = mock(IPSAssemblyDesignWs.class);
    adaptor = new TemplateAdaptor(asm, contentWs, designWs, () -> true);
    when(designWs.findAssemblyTemplates(any(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void import_createsViaDesignWsAndRoundTripsName() throws Exception {
    String xml = sampleImportXml("imported.one", "Imported One");
    PSAssemblyTemplate created = newCreatedTemplate(2001L, "imported.one");
    PSAssemblyTemplateWs createdWs = new PSAssemblyTemplateWs(created, Map.of());
    when(designWs.createAssemblyTemplates(eq(List.of("imported.one")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(createdWs));
    stubReload("imported.one", created.getGUID(), createdWs);

    TemplateDetail out = adaptor.importTemplate(null, xml);

    assertEquals("imported.one", out.getName());
    assertEquals("Imported One", out.getLabel());
    assertEquals("Java/global/percussion/assembly/htmlAssembler", out.getAssembler());
    assertEquals("#set($x=1)$x", out.getTemplateSource());
    verify(designWs)
        .createAssemblyTemplates(eq(List.of("imported.one")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSAssemblyTemplateWs>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveAssemblyTemplates(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    IPSAssemblyTemplate persisted = saved.getValue().get(0).getTemplate();
    assertEquals("imported.one", persisted.getName());
    assertEquals(2001L, persisted.getGUID().getUUID());
    verify(designWs)
        .loadAssemblyTemplates(eq(List.of(created.getGUID())), eq(false), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void import_nonAdmin_is403() throws Exception {
    adaptor = new TemplateAdaptor(asm, contentWs, designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importTemplate(null, sampleImportXml("imported.one", "Imported One")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createAssemblyTemplates(anyList(), any(), any());
    verify(designWs, never()).saveAssemblyTemplates(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void import_invalidXml_is400() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.importTemplate(null, "<not-a-template"));
    assertTrue(ex.getMessage().contains("invalid assembly-template XML"));
    verify(designWs, never()).createAssemblyTemplates(anyList(), any(), any());
  }

  @Test
  void import_blankXml_is400() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.importTemplate(null, "  "));
    assertEquals("assembly-template XML is required", ex.getMessage());
  }

  @Test
  void import_jsonBody_is400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.importTemplate(null, "{\"name\":\"x\"}"));
    assertEquals("expected assembly-template XML", ex.getMessage());
  }

  @Test
  void parseDesignXml_roundTripsNameFromWorkbenchXml() throws Exception {
    String xml = sampleImportXml("imported.one", "Imported One");
    PSAssemblyTemplate parsed = TemplateAdaptor.parseDesignXml(xml);
    assertEquals("imported.one", parsed.getName());
    assertEquals("Imported One", parsed.getLabel());
  }

  @Test
  void import_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("imported.one");
    when(designWs.findAssemblyTemplates(
            eq("imported.one"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(existing));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importTemplate(null, sampleImportXml("imported.one", "Imported One")));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createAssemblyTemplates(anyList(), any(), any());
    verify(designWs, never()).saveAssemblyTemplates(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void import_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createAssemblyTemplates(eq(List.of("imported.one")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException(
                "The name 'imported.one' for type 'TEMPLATE' already exists."));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importTemplate(null, sampleImportXml("imported.one", "Imported One")));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveAssemblyTemplates(anyList(), anyBoolean(), any(), any());
  }

  private void stubReload(String name, IPSGuid guid, PSAssemblyTemplateWs ws)
      throws Exception {
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn(name);
    when(summary.getGUID()).thenReturn(guid);
    when(designWs.findAssemblyTemplates(
            eq(name), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of())
        .thenReturn(List.of(summary));
    when(designWs.loadAssemblyTemplates(eq(List.of(guid)), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(ws));
  }

  private static PSAssemblyTemplate newCreatedTemplate(long uuid, String name) {
    PSAssemblyTemplate t = new PSAssemblyTemplate();
    t.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, uuid));
    t.setName(name);
    t.setLabel(name);
    return t;
  }

  private static String sampleImportXml(String name, String label) throws Exception {
    PSAssemblyTemplate t = new PSAssemblyTemplate();
    t.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 557L));
    t.setName(name);
    t.setLabel(label);
    t.setAssembler("Java/global/percussion/assembly/htmlAssembler");
    t.setAssemblyUrl("../assembler/render");
    t.setCharset("UTF-8");
    t.setDescription("");
    t.setMimeType("text/html");
    t.setActiveAssemblyType(IPSAssemblyTemplate.AAType.Normal);
    t.setOutputFormat(IPSAssemblyTemplate.OutputFormat.Snippet);
    t.setPublishWhen(IPSAssemblyTemplate.PublishWhen.Default);
    t.setTemplateType(IPSAssemblyTemplate.TemplateType.Shared);
    t.setGlobalTemplateUsage(IPSAssemblyTemplate.GlobalTemplateUsage.None);
    t.setTemplate("#set($x=1)$x");
    t.setLocationPrefix("");
    t.setLocationSuffix("");
    t.setStyleSheetPath("");
    t.setBindings(List.of(new PSTemplateBinding(1, "$sys.item", "$sys.item")));
    return t.toXML();
  }
}
