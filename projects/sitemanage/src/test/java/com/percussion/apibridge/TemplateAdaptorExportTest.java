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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.templates.TemplateExport;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * AS-08 template export: Admin-only, IPSAssemblyDesignWs load without stealing locks, XML includes
 * the template name.
 */
@Tag("UnitTest")
class TemplateAdaptorExportTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSAssemblyDesignWs designWs;

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    asm = mock(IPSAssemblyService.class);
    contentWs = mock(IPSContentWs.class);
    designWs = mock(IPSAssemblyDesignWs.class);
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfo.resetRequestInfo();
  }

  private TemplateAdaptor allowAdmin() {
    return new TemplateAdaptor(asm, contentWs, designWs, () -> true);
  }

  /**
   * Production no-arg Admin gate ({@code adminChecker == null} → {@link
   * TemplateAdaptor#isCurrentUserAdmin()}).
   */
  private TemplateAdaptor productionAdminGate() {
    return new TemplateAdaptor(asm, contentWs, designWs, null);
  }

  private static void injectUserService(TemplateAdaptor adaptor, IPSUserService users)
      throws Exception {
    Field field = TemplateAdaptor.class.getDeclaredField("userService");
    field.setAccessible(true);
    field.set(adaptor, users);
  }

  private static PSCurrentUser namedUser(String name) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    return user;
  }

  private static PSAssemblyTemplateWs namedTemplate(String name, IPSGuid guid, String xml)
      throws Exception {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    when(template.getName()).thenReturn(name);
    when(template.getGUID()).thenReturn(guid);
    when(template.toXML()).thenReturn(xml);
    return new PSAssemblyTemplateWs(template, Map.of());
  }

  @Test
  void exportByName_returnsXmlContainingName_withoutLock() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 602L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("perc.page");
    when(sum.getLabel()).thenReturn("Page");
    String xml = "<assembly-template><name>perc.page</name></assembly-template>";
    PSAssemblyTemplateWs loaded = namedTemplate("perc.page", guid, xml);
    when(designWs.findAssemblyTemplates(eq("perc.page"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(sum));
    when(designWs.loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loaded));

    TemplateExport out = allowAdmin().exportTemplate(null, "perc.page");
    assertNotNull(out);
    assertEquals("perc.page", out.getName());
    assertTrue(out.getXml().contains("perc.page"));
    verify(designWs).loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any());
    verify(designWs, never()).loadAssemblyTemplates(anyList(), eq(true), eq(true), any(), any());
    verify(designWs, never()).loadAssemblyTemplates(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void exportByNumericId_loadsReadOnly() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 602L);
    String xml = "<assembly-template><name>perc.page</name></assembly-template>";
    PSAssemblyTemplateWs loaded = namedTemplate("perc.page", guid, xml);
    when(designWs.loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loaded));

    TemplateExport out = allowAdmin().exportTemplate(null, "602");
    assertNotNull(out);
    assertEquals("perc.page", out.getName());
    assertTrue(out.getXml().contains("perc.page"));
    verify(designWs, never())
        .findAssemblyTemplates(any(), any(), any(), any(), any(), any(), any());
    verify(designWs).loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void exportUnknown_returnsNull() throws Exception {
    when(designWs.findAssemblyTemplates(eq("missing"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of());
    assertNull(allowAdmin().exportTemplate(null, "missing"));
    verify(designWs, never()).loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void exportBlank_returnsNullWithoutDesignWs() {
    assertNull(allowAdmin().exportTemplate(null, "  "));
    assertNull(allowAdmin().exportTemplate(null, null));
    verify(designWs, never())
        .findAssemblyTemplates(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void exportForbiddenWhenNotAdmin() throws Exception {
    TemplateAdaptor denied = new TemplateAdaptor(asm, contentWs, designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.exportTemplate(null, "perc.page"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never())
        .findAssemblyTemplates(any(), any(), any(), any(), any(), any(), any());
    verify(designWs, never()).loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void isCurrentUserAdmin_trueForAdminUser() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("admin1"));
    when(users.isAdminUser("admin1")).thenReturn(true);
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertTrue(adaptor.isCurrentUserAdmin());
  }

  @Test
  void isCurrentUserAdmin_falseForNonAdmin() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("editor"));
    when(users.isAdminUser("editor")).thenReturn(false);
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertFalse(adaptor.isCurrentUserAdmin());
  }

  @Test
  void isCurrentUserAdmin_falseWhenCurrentUserNull() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(null);
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertFalse(adaptor.isCurrentUserAdmin());
  }

  @Test
  void isCurrentUserAdmin_falseWhenNameBlank() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("  "));
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertFalse(adaptor.isCurrentUserAdmin());
  }

  @Test
  void isCurrentUserAdmin_falseWhenUserServiceMissing() {
    TemplateAdaptor adaptor = productionAdminGate();
    assertFalse(adaptor.isCurrentUserAdmin());
  }

  @Test
  void isCurrentUserAdmin_falseWhenGetCurrentUserThrows() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenThrow(new PSDataServiceException("boom"));
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertFalse(adaptor.isCurrentUserAdmin());
  }

  @Test
  void exportForbiddenWhenProductionAdminGateDenies() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("editor"));
    when(users.isAdminUser("editor")).thenReturn(false);
    TemplateAdaptor denied = productionAdminGate();
    injectUserService(denied, users);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.exportTemplate(null, "perc.page"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never())
        .findAssemblyTemplates(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void exportAllowedWhenProductionAdminGateAllows() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 602L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("perc.page");
    String xml = "<assembly-template><name>perc.page</name></assembly-template>";
    PSAssemblyTemplateWs loaded = namedTemplate("perc.page", guid, xml);
    when(designWs.findAssemblyTemplates(
            eq("perc.page"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(sum));
    when(designWs.loadAssemblyTemplates(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loaded));

    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("admin1"));
    when(users.isAdminUser("admin1")).thenReturn(true);
    TemplateAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);

    TemplateExport out = adaptor.exportTemplate(null, "perc.page");
    assertNotNull(out);
    assertEquals("perc.page", out.getName());
  }
}
