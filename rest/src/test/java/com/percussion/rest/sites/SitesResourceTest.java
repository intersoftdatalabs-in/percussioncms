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
package com.percussion.rest.sites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SitesResourceTest {

  private ISiteAdaptor adaptor;
  private SitesResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @BeforeEach
  public void setUp() {
    previousLog = SitesResource.log;
    mockLog = mock(Logger.class);
    SitesResource.log = mockLog;

    adaptor = mock(ISiteAdaptor.class);
    resource = new SitesResource(adaptor);
  }

  @AfterEach
  public void restoreLog() {
    SitesResource.log = previousLog;
  }

  @Test
  public void listSitesSuccess() {
    SiteList list = new SiteList();
    Site s = new Site();
    s.setName("Help");
    list.add(s);
    when(adaptor.findAllSites()).thenReturn(list);

    SiteList out = resource.listSites();
    assertEquals(1, out.size());
    assertEquals("Help", out.get(0).getName());
    verify(adaptor).findAllSites();
  }

  @Test
  public void listSitesNullBecomesEmpty() {
    when(adaptor.findAllSites()).thenReturn(null);
    assertTrue(resource.listSites().isEmpty());
  }

  @Test
  public void getSiteByName() {
    Site s = new Site();
    s.setName("Help");
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("git-filesystem");
    v.setRootPath("C:/docs");
    v.setVirtual(true);
    s.setVirtual(v);
    when(adaptor.findByName("Help")).thenReturn(s);

    Site out = resource.getSite("Help");
    assertEquals("Help", out.getName());
    assertTrue(out.getVirtual() != null);
    assertEquals("git-filesystem", out.getVirtual().getSourceKind().orElse(null));
    verify(adaptor).findByName("Help");
    verify(adaptor, never()).findByGuid(any());
  }

  @Test
  public void getSiteFallsBackToGuid() {
    Site s = new Site();
    s.setName("Help");
    when(adaptor.findByName("0-1-301")).thenReturn(null);
    when(adaptor.findByGuid("0-1-301")).thenReturn(s);

    Site out = resource.getSite("0-1-301");
    assertEquals("Help", out.getName());
    verify(adaptor).findByGuid("0-1-301");
  }

  @Test
  public void getSiteNotFound404() {
    when(adaptor.findByName("missing")).thenReturn(null);
    when(adaptor.findByGuid("missing")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSite("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getVirtualPropertiesDelegates() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("git-filesystem");
    when(adaptor.getVirtualSiteProperties("Help")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("Help");
    assertEquals("git-filesystem", out.getSourceKind().orElse(null));
    verify(adaptor).getVirtualSiteProperties("Help");
  }

  @Test
  public void updateVirtualPropertiesDelegates() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRootPath("C:/docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("git-filesystem");
    saved.setRootPath("C:/docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("Help"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("Help", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("Help", body);
  }

  @Test
  public void updateVirtualPropertiesNullBody400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateVirtualProperties("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateVirtualSiteProperties(any(), any());
  }

  @Test
  public void buildVirtualSiteDelegates() {
    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("Help");
    built.setPagesWritten(3);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath("C:/tmp/out");
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot("C:/tmp/out");
    when(adaptor.buildVirtualSite(eq("Help"), same(req))).thenReturn(built);

    VirtualSiteBuildResult out = resource.buildVirtualSite("Help", req);
    assertEquals(3, out.getPagesWritten().intValue());
    assertEquals("C:/tmp/out", out.getOutputPath().orElse(null));
    verify(adaptor).buildVirtualSite("Help", req);
  }

  @Test
  public void buildVirtualSiteAllowsNullBody() {
    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setPagesWritten(1);
    when(adaptor.buildVirtualSite(eq("Help"), any())).thenReturn(built);

    VirtualSiteBuildResult out = resource.buildVirtualSite("Help", null);
    assertEquals(1, out.getPagesWritten().intValue());
    verify(adaptor).buildVirtualSite("Help", null);
  }

  @Test
  public void previewStatusDelegates() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("Help")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("Help");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath().orElse(null));
    verify(adaptor).getVirtualSitePreviewStatus("Help");
  }

  @Test
  public void previewStatusBlankName400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getVirtualSitePreviewStatus(" "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).getVirtualSitePreviewStatus(any());
  }

  @Test
  public void previewFileDelegatesAndRewritesHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("Help"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("Help", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/Help/virtual/preview/8.2/index.html"), text);
    verify(adaptor).previewVirtualSiteFile("Help", "8.2/index.html");
  }

  @Test
  public void previewFileBlankName400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.previewVirtualSiteFile(" ", "index.html"));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).previewVirtualSiteFile(any(), any());
  }

  @Test
  public void buildVirtualSiteBlankName400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.buildVirtualSite(" ", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).buildVirtualSite(any(), any());
  }

  /**
   * CodeQL #1949: Jackson/JAXB JSON/XML DTO return must keep same-line {@code // codeql[java/xss]}
   * on the updateVirtual sink (not HTML body construction).
   */
  @Test
  public void updateVirtualReturnSinkHasCodeqlXssAnnotation() throws Exception {
    Path src =
        Path.of("src/main/java/com/percussion/rest/sites/SitesResource.java");
    assertTrue(Files.isRegularFile(src), "SitesResource source must exist for annotation guard");
    String text = Files.readString(src, StandardCharsets.UTF_8);
    assertTrue(
        text.contains(
            "return requireAdaptor().updateVirtualSiteProperties(nameOrId, props); // codeql[java/xss]"),
        "updateVirtualSiteProperties return sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains(
            "return requireAdaptor().buildVirtualSite(nameOrId, request); // codeql[java/xss]"),
        "buildVirtualSite return sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains(
            "return requireAdaptor().getVirtualSitePreviewStatus(nameOrId); // codeql[java/xss]"),
        "getVirtualSitePreviewStatus return sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains(".build(); // codeql[java/xss]"),
        "previewVirtualSiteFile Response.ok sink must carry same-line codeql[java/xss]");
  }

  @Test
  public void blankNameOrId400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSite("  "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503() {
    SitesResource bare = new SitesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listSites);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void listSitesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.findAllSites()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listSites());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }
}
