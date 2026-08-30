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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.rest.contenttypes.ContentTypeExport;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * CD-14 content-type export: Admin-only, IPSContentDesignWs load without stealing locks, XML
 * includes the type name.
 */
@Tag("UnitTest")
class ContentTypeAdaptorExportTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  private ContentTypeAdaptor allowAdmin() {
    return new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
  }

  /** Production no-arg Admin gate ({@code adminChecker == null} → {@code isCurrentUserAdmin}). */
  private ContentTypeAdaptor productionAdminGate() {
    return new ContentTypeAdaptor(
        designWs, itemDefManager, systemDesign, (BooleanSupplier) null);
  }

  private static void injectUserService(ContentTypeAdaptor adaptor, IPSUserService users)
      throws Exception {
    Field field = ContentTypeAdaptor.class.getDeclaredField("userService");
    field.setAccessible(true);
    field.set(adaptor, users);
  }

  private static PSCurrentUser namedUser(String name) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    return user;
  }

  private static PSItemDefinition stubExportDef(String name) {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(name);
    when(def.toXml(any(Document.class)))
        .thenAnswer(
            inv -> {
              Document doc = inv.getArgument(0);
              Element root = doc.createElement("ItemDefData");
              root.setAttribute("name", name);
              Element n = doc.createElement("name");
              n.appendChild(doc.createTextNode(name));
              root.appendChild(n);
              return root;
            });
    return def;
  }

  @Test
  void exportByName_returnsXmlContainingName_withoutLock() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("percPage");
    when(sum.getLabel()).thenReturn("Page");
    PSItemDefinition def = stubExportDef("percPage");
    when(designWs.findContentTypes("percPage")).thenReturn(List.of(sum));
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));

    ContentTypeExport out = allowAdmin().exportContentType(null, "percPage");
    assertNotNull(out);
    assertEquals("percPage", out.getName());
    assertTrue(out.getXml().contains("percPage"), out.getXml());
    verify(designWs).loadContentTypes(anyList(), eq(false), eq(false), any(), any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), eq(true), any(), any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void exportByNumericId_loadsReadOnly() throws Exception {
    PSItemDefinition def = stubExportDef("percPage");
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));

    ContentTypeExport out = allowAdmin().exportContentType(null, "311");
    assertNotNull(out);
    assertEquals("percPage", out.getName());
    assertTrue(out.getXml().contains("percPage"), out.getXml());
    verify(designWs, never()).findContentTypes(any());
    verify(designWs, atLeastOnce())
        .loadContentTypes(anyList(), eq(false), eq(false), any(), any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), eq(true), any(), any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void exportUnknown_returnsNull() throws Exception {
    when(designWs.findContentTypes("missing")).thenReturn(Collections.emptyList());
    assertNull(allowAdmin().exportContentType(null, "missing"));
    verify(designWs, never()).loadContentTypes(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void exportBlank_returnsNullWithoutDesignWs() throws Exception {
    assertNull(allowAdmin().exportContentType(null, "  "));
    assertNull(allowAdmin().exportContentType(null, null));
    verify(designWs, never()).findContentTypes(any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void exportForbiddenWhenNotAdmin() throws Exception {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.exportContentType(null, "percPage"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).findContentTypes(any());
    verify(designWs, never()).loadContentTypes(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void exportForbiddenWhenProductionAdminGateDenies() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("editor"));
    when(users.isAdminUser("editor")).thenReturn(false);
    ContentTypeAdaptor denied = productionAdminGate();
    injectUserService(denied, users);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.exportContentType(null, "percPage"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).findContentTypes(any());
  }

  @Test
  void exportAllowedWhenProductionAdminGateAllows() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("percPage");
    PSItemDefinition def = stubExportDef("percPage");
    when(designWs.findContentTypes("percPage")).thenReturn(List.of(sum));
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));

    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenReturn(namedUser("admin1"));
    when(users.isAdminUser("admin1")).thenReturn(true);
    ContentTypeAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);

    ContentTypeExport out = adaptor.exportContentType(null, "percPage");
    assertNotNull(out);
    assertEquals("percPage", out.getName());
    assertTrue(out.getXml().contains("percPage"), out.getXml());
  }

  @Test
  void isCurrentUserAdmin_falseWhenGetCurrentUserThrows() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    when(users.getCurrentUser()).thenThrow(new PSDataServiceException("boom"));
    ContentTypeAdaptor adaptor = productionAdminGate();
    injectUserService(adaptor, users);
    assertEquals(false, adaptor.isCurrentUserAdmin());
  }

  @Test
  void toDesignXml_nullDefThrows() {
    assertThrows(IllegalArgumentException.class, () -> ContentTypeAdaptor.toDesignXml(null));
  }
}
