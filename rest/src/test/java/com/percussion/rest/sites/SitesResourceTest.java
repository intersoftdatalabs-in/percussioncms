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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
public class SitesResourceTest {

  private ISiteAdaptor adaptor;
  private SitesResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @TempDir Path tempDir;

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
    assertEquals("git-filesystem", out.getVirtual().getSourceKind());
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
    assertEquals("git-filesystem", out.getSourceKind());
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
  public void getVirtualPropertiesRoundTripsCsvFilesystem() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("csv-filesystem");
    v.setRootPath("C:/csv-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("CsvHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("CsvHelp");
    assertEquals("csv-filesystem", out.getSourceKind());
    assertEquals("C:/csv-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("CsvHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsCsvFilesystem() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("csv-filesystem");
    body.setRootPath("C:/csv-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("csv-filesystem");
    saved.setRootPath("C:/csv-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("CsvHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("CsvHelp", body);
    assertEquals("csv-filesystem", out.getSourceKind());
    assertEquals("C:/csv-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("CsvHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsSqlDatabase() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("sql-database");
    v.setRootPath("C:/sql-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("SqlHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("SqlHelp");
    assertEquals("sql-database", out.getSourceKind());
    assertEquals("C:/sql-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("SqlHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsSqlDatabase() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-database");
    body.setRootPath("C:/sql-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("sql-database");
    saved.setRootPath("C:/sql-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("SqlHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("SqlHelp", body);
    assertEquals("sql-database", out.getSourceKind());
    assertEquals("C:/sql-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("SqlHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsHttpJson() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("http-json");
    v.setRootPath("C:/http-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("HttpHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("HttpHelp");
    assertEquals("http-json", out.getSourceKind());
    assertEquals("C:/http-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("HttpHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsHttpJson() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("http-json");
    body.setRootPath("C:/http-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("http-json");
    saved.setRootPath("C:/http-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("HttpHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("HttpHelp", body);
    assertEquals("http-json", out.getSourceKind());
    assertEquals("C:/http-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("HttpHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsObjectStorage() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("object-storage");
    v.setRootPath("C:/object-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("ObjectHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("ObjectHelp");
    assertEquals("object-storage", out.getSourceKind());
    assertEquals("C:/object-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("ObjectHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsObjectStorage() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("C:/object-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("object-storage");
    saved.setRootPath("C:/object-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("ObjectHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("ObjectHelp", body);
    assertEquals("object-storage", out.getSourceKind());
    assertEquals("C:/object-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("ObjectHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsRssAtom() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("rss-atom");
    v.setRootPath("C:/rss-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("RssHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("RssHelp");
    assertEquals("rss-atom", out.getSourceKind());
    assertEquals("C:/rss-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("RssHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsRssAtom() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("C:/rss-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("rss-atom");
    saved.setRootPath("C:/rss-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("RssHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("RssHelp", body);
    assertEquals("rss-atom", out.getSourceKind());
    assertEquals("C:/rss-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("RssHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsIcalendar() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("icalendar");
    v.setRootPath("C:/cal-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("CalHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("CalHelp");
    assertEquals("icalendar", out.getSourceKind());
    assertEquals("C:/cal-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("CalHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsIcalendar() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("icalendar");
    body.setRootPath("C:/cal-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("icalendar");
    saved.setRootPath("C:/cal-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("CalHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("CalHelp", body);
    assertEquals("icalendar", out.getSourceKind());
    assertEquals("C:/cal-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("CalHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsSitemapXml() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("sitemap-xml");
    v.setRootPath("C:/sitemap-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("SitemapHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("SitemapHelp");
    assertEquals("sitemap-xml", out.getSourceKind());
    assertEquals("C:/sitemap-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("SitemapHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsSitemapXml() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sitemap-xml");
    body.setRootPath("C:/sitemap-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("sitemap-xml");
    saved.setRootPath("C:/sitemap-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("SitemapHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("SitemapHelp", body);
    assertEquals("sitemap-xml", out.getSourceKind());
    assertEquals("C:/sitemap-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("SitemapHelp", body);
  }

  @Test
  public void updateVirtualPropertiesSitemapXmlRemoteUrlPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sitemap-xml");
    body.setRootPath("C:/sitemap-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");
    when(adaptor.updateVirtualSiteProperties(eq("SitemapHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for sitemap-xml", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("SitemapHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    assertFalse(String.valueOf(ex.getMessage()).contains("user:secret"));
    verify(adaptor).updateVirtualSiteProperties("SitemapHelp", body);
  }

  @Test
  public void updateVirtualPropertiesSitemapXmlCloudRootPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sitemap-xml");
    body.setRootPath("https://example.com/sitemap.xml");
    when(adaptor.updateVirtualSiteProperties(eq("SitemapHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for sitemap-xml must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("SitemapHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).updateVirtualSiteProperties("SitemapHelp", body);
  }

  @Test
  public void updateVirtualPropertiesSitemapXmlCredentialsPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sitemap-xml");
    body.setRootPath("C:/sitemap-docs");
    when(adaptor.updateVirtualSiteProperties(eq("SitemapHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for sitemap-xml (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("SitemapHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).updateVirtualSiteProperties("SitemapHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsRobotsTxt() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("robots-txt");
    v.setRootPath("C:/robots-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("RobotsHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("RobotsHelp");
    assertEquals("robots-txt", out.getSourceKind());
    assertEquals("C:/robots-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("RobotsHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsRobotsTxt() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("robots-txt");
    body.setRootPath("C:/robots-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("robots-txt");
    saved.setRootPath("C:/robots-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("RobotsHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("RobotsHelp", body);
    assertEquals("robots-txt", out.getSourceKind());
    assertEquals("C:/robots-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("RobotsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesRobotsTxtRemoteUrlPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("robots-txt");
    body.setRootPath("C:/robots-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");
    when(adaptor.updateVirtualSiteProperties(eq("RobotsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for robots-txt", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("RobotsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    assertFalse(String.valueOf(ex.getMessage()).contains("user:secret"));
    verify(adaptor).updateVirtualSiteProperties("RobotsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesRobotsTxtCloudRootPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("robots-txt");
    body.setRootPath("https://example.com/robots.txt");
    when(adaptor.updateVirtualSiteProperties(eq("RobotsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for robots-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("RobotsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).updateVirtualSiteProperties("RobotsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesRobotsTxtCredentialsPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("robots-txt");
    body.setRootPath("C:/robots-docs");
    when(adaptor.updateVirtualSiteProperties(eq("RobotsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for robots-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("RobotsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).updateVirtualSiteProperties("RobotsHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsLlmsTxt() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("llms-txt");
    v.setRootPath("C:/llms-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("LlmsHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("LlmsHelp");
    assertEquals("llms-txt", out.getSourceKind());
    assertEquals("C:/llms-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("LlmsHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsLlmsTxt() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("llms-txt");
    body.setRootPath("C:/llms-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("llms-txt");
    saved.setRootPath("C:/llms-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("LlmsHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("LlmsHelp", body);
    assertEquals("llms-txt", out.getSourceKind());
    assertEquals("C:/llms-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("LlmsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesLlmsTxtRemoteUrlPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("llms-txt");
    body.setRootPath("C:/llms-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");
    when(adaptor.updateVirtualSiteProperties(eq("LlmsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for llms-txt", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("LlmsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    assertFalse(String.valueOf(ex.getMessage()).contains("user:secret"));
    verify(adaptor).updateVirtualSiteProperties("LlmsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesLlmsTxtCloudRootPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("llms-txt");
    body.setRootPath("https://example.com/llms.txt");
    when(adaptor.updateVirtualSiteProperties(eq("LlmsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for llms-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("LlmsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).updateVirtualSiteProperties("LlmsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesLlmsTxtCredentialsPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("llms-txt");
    body.setRootPath("C:/llms-docs");
    when(adaptor.updateVirtualSiteProperties(eq("LlmsHelp"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for llms-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateVirtualProperties("LlmsHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).updateVirtualSiteProperties("LlmsHelp", body);
  }

  @Test
  public void updateVirtualPropertiesUnknownKindPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-adapter");
    body.setRootPath("C:/docs");
    when(adaptor.updateVirtualSiteProperties(eq("Help"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "Unsupported virtual.sourceKind", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateVirtualProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
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
    assertEquals("C:/tmp/out", out.getOutputPath());
    verify(adaptor).buildVirtualSite("Help", req);
  }

  @Test
  public void buildVirtualSiteCsvFilesystemFixtureDelegates() throws Exception {
    Path csvRoot = tempDir.resolve("csv-site");
    Files.createDirectories(csvRoot.resolve("8.2"));
    Files.writeString(
        csvRoot.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path,order\ncsv-home,CSV Home,Hello from CSV.,index.md,1\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("csv-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("CsvHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("CsvHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("CsvHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(csvRoot.resolve("8.2").resolve("pages.csv")));
    verify(adaptor).buildVirtualSite("CsvHelp", req);
  }

  @Test
  public void buildVirtualSiteSqlDatabaseFixtureDelegates() throws Exception {
    Path sqlRoot = tempDir.resolve("sql-site");
    Files.createDirectories(sqlRoot);
    Files.writeString(
        sqlRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:h2:mem:vsql_rest_fixture;DB_CLOSE_DELAY=-1
          user: sa
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("sql-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("SqlHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("SqlHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("SqlHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(sqlRoot.resolve("_config.yaml")));
    verify(adaptor).buildVirtualSite("SqlHelp", req);
  }

  @Test
  public void buildVirtualSiteHttpJsonFixtureDelegates() throws Exception {
    Path httpRoot = tempDir.resolve("http-site");
    Files.createDirectories(httpRoot);
    Files.writeString(
        httpRoot.resolve("_config.yaml"),
        """
        site:
          title: HTTP Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        http:
          file: pages.json
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        httpRoot.resolve("pages.json"),
        """
        {"pages":[{"id":"http-home","path":"index.html","title":"HTTP Home","body":"Hello from JSON."}]}
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("http-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("HttpHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("HttpHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("HttpHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(httpRoot.resolve("pages.json")));
    assertTrue(Files.isRegularFile(httpRoot.resolve("_config.yaml")));
    verify(adaptor).buildVirtualSite("HttpHelp", req);
  }

  @Test
  public void buildVirtualSiteObjectStorageFixtureDelegates() throws Exception {
    Path objectRoot = tempDir.resolve("object-site");
    Files.createDirectories(objectRoot.resolve("8.2"));
    Files.writeString(
        objectRoot.resolve("_config.yaml"),
        """
        site:
          title: Object Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        objectRoot.resolve("8.2").resolve("index.md"),
        """
        ---
        id: home
        title: Object Home
        ---
        Hello from objects.
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("object-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("ObjectHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("ObjectHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("ObjectHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(objectRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(objectRoot.resolve("8.2").resolve("index.md")));
    verify(adaptor).buildVirtualSite("ObjectHelp", req);
  }

  @Test
  public void buildVirtualSiteRssAtomFixtureDelegates() throws Exception {
    Path rssRoot = tempDir.resolve("rss-site");
    Files.createDirectories(rssRoot);
    Files.writeString(
        rssRoot.resolve("_config.yaml"),
        """
        site:
          title: RSS Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        rss:
          file: feed.xml
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        rssRoot.resolve("feed.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Sample</title>
            <item>
              <guid>home</guid>
              <title>RSS Home</title>
              <description>Hello from RSS.</description>
            </item>
          </channel>
        </rss>
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("rss-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("RssHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("RssHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("RssHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(rssRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(rssRoot.resolve("feed.xml")));
    verify(adaptor).buildVirtualSite("RssHelp", req);
  }

  @Test
  public void buildVirtualSiteIcalendarFixtureDelegates() throws Exception {
    Path calRoot = tempDir.resolve("cal-site");
    Files.createDirectories(calRoot);
    Files.writeString(
        calRoot.resolve("_config.yaml"),
        """
        site:
          title: Cal Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        icalendar:
          file: calendar.ics
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        calRoot.resolve("calendar.ics"),
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Percussion//Test//EN
        BEGIN:VEVENT
        UID:index
        DTSTART:20260828T100000Z
        SUMMARY:Cal Home
        DESCRIPTION:Hello from iCalendar.
        END:VEVENT
        END:VCALENDAR
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("cal-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("CalHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("CalHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("CalHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(calRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(calRoot.resolve("calendar.ics")));
    verify(adaptor).buildVirtualSite("CalHelp", req);
  }

  @Test
  public void buildVirtualSiteSitemapXmlFixtureDelegates() throws Exception {
    Path smRoot = tempDir.resolve("sm-site");
    Files.createDirectories(smRoot.resolve("pages"));
    Files.writeString(
        smRoot.resolve("_config.yaml"),
        """
        site:
          title: Sitemap Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sitemap:
          file: sitemap.xml
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("pages").resolve("home.md"),
        "Hello from sitemap.",
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>pages/home.md</loc>
          </url>
        </urlset>
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("sm-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("SitemapHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("SitemapHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("SitemapHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getPagesWritten().intValue() > 0);
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(smRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(smRoot.resolve("sitemap.xml")));
    verify(adaptor).buildVirtualSite("SitemapHelp", req);
  }

  /**
   * PUT sitemap-xml, write sitemap.xml via Path/Files, POST build, edit the current file, POST
   * build again. Resource delegates twice; pagesWritten and HTML change. Leftover remoteUrl /
   * credentials / cloud rootPath stay 400 in sibling tests.
   */
  @Test
  public void buildVirtualSiteSitemapXmlSecondBuildAfterCurrentFileEditDelegates()
      throws Exception {
    Path smRoot = tempDir.resolve("sm-rebuild-site");
    Files.createDirectories(smRoot.resolve("pages"));
    Files.writeString(
        smRoot.resolve("_config.yaml"),
        """
        site:
          title: Sitemap Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sitemap:
          file: sitemap.xml
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("pages").resolve("home.md"),
        "unique-token-AAA",
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>pages/home.md</loc>
            <lastmod>2020-01-01</lastmod>
          </url>
        </urlset>
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("sm-rebuild-out");
    Files.createDirectories(out);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sitemap-xml");
    body.setRootPath(smRoot.toAbsolutePath().toString());
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("sitemap-xml");
    saved.setRootPath(smRoot.toAbsolutePath().toString());
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("SitemapHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties put = resource.updateVirtualProperties("SitemapHelp", body);
    assertEquals("sitemap-xml", put.getSourceKind());
    verify(adaptor).updateVirtualSiteProperties("SitemapHelp", body);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    when(adaptor.buildVirtualSite(eq("SitemapHelp"), same(req)))
        .thenAnswer(
            invocation -> {
              Path htmlDir = out.resolve("8.2");
              Files.createDirectories(htmlDir);
              Path html = htmlDir.resolve("home.html");
              String md =
                  Files.readString(
                      smRoot.resolve("pages").resolve("home.md"), StandardCharsets.UTF_8);
              String sitemap =
                  Files.readString(smRoot.resolve("sitemap.xml"), StandardCharsets.UTF_8);
              Files.writeString(html, "<html>" + md + sitemap + "</html>", StandardCharsets.UTF_8);
              int pages = sitemap.contains("pages/second.md") ? 2 : 1;
              if (pages == 2) {
                Files.writeString(
                    htmlDir.resolve("second.html"),
                    "<html>unique-token-SECOND</html>",
                    StandardCharsets.UTF_8);
              }
              VirtualSiteBuildResult built = new VirtualSiteBuildResult();
              built.setSiteName("SitemapHelp");
              built.setPagesWritten(pages);
              built.setLinkProblemCount(0);
              built.setHasLinkProblems(false);
              built.setOutputPath(out.toAbsolutePath().toString());
              return built;
            });

    VirtualSiteBuildResult first = resource.buildVirtualSite("SitemapHelp", req);
    assertTrue(first.getPagesWritten().intValue() > 0);
    assertEquals(1, first.getPagesWritten().intValue());
    Path html = out.resolve("8.2").resolve("home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);
    assertTrue(firstHtml.contains("2020-01-01"), firstHtml);
    assertFalse(Files.isRegularFile(out.resolve("8.2").resolve("second.html")));

    Files.writeString(
        smRoot.resolve("pages").resolve("home.md"),
        "unique-token-BBB",
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("pages").resolve("second.md"),
        "unique-token-SECOND",
        StandardCharsets.UTF_8);
    Files.writeString(
        smRoot.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>pages/home.md</loc>
            <lastmod>2026-09-02</lastmod>
          </url>
          <url>
            <loc>pages/second.md</loc>
          </url>
        </urlset>
        """,
        StandardCharsets.UTF_8);

    VirtualSiteBuildResult second = resource.buildVirtualSite("SitemapHelp", req);
    assertTrue(second.getPagesWritten().intValue() > 0);
    assertEquals(2, second.getPagesWritten().intValue());
    assertNotEquals(first.getPagesWritten(), second.getPagesWritten());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertTrue(secondHtml.contains("2026-09-02"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
    Path extra = out.resolve("8.2").resolve("second.html");
    assertTrue(Files.isRegularFile(extra), "missing " + extra);
    assertTrue(
        Files.readString(extra, StandardCharsets.UTF_8).contains("unique-token-SECOND"));
    verify(adaptor, times(2)).buildVirtualSite("SitemapHelp", req);
  }

  @Test
  public void buildVirtualSiteSitemapXmlRemoteUrlPropagates400() {
    when(adaptor.buildVirtualSite(eq("SitemapHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for sitemap-xml", Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("SitemapHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).buildVirtualSite("SitemapHelp", null);
  }

  @Test
  public void buildVirtualSiteSitemapXmlCloudRootPathPropagates400() {
    when(adaptor.buildVirtualSite(eq("SitemapHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for sitemap-xml must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("SitemapHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).buildVirtualSite("SitemapHelp", null);
  }

  @Test
  public void buildVirtualSiteSitemapXmlCredentialsPropagates400() {
    when(adaptor.buildVirtualSite(eq("SitemapHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for sitemap-xml (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("SitemapHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).buildVirtualSite("SitemapHelp", null);
  }

  @Test
  public void buildVirtualSiteRobotsTxtFixtureDelegates() throws Exception {
    Path rbRoot = tempDir.resolve("rb-site");
    Files.createDirectories(rbRoot);
    Files.writeString(
        rbRoot.resolve("_config.yaml"),
        """
        site:
          title: Robots Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        robots:
          file: robots.txt
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        rbRoot.resolve("robots.txt"),
        """
        User-agent: *
        Disallow: /Hello-from-robots
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("rb-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("RobotsHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("RobotsHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("RobotsHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getPagesWritten().intValue() > 0);
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(rbRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(rbRoot.resolve("robots.txt")));
    verify(adaptor).buildVirtualSite("RobotsHelp", req);
  }

  @Test
  public void buildVirtualSiteRobotsTxtRemoteUrlPropagates400() {
    when(adaptor.buildVirtualSite(eq("RobotsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for robots-txt", Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("RobotsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).buildVirtualSite("RobotsHelp", null);
  }

  @Test
  public void buildVirtualSiteRobotsTxtCloudRootPathPropagates400() {
    when(adaptor.buildVirtualSite(eq("RobotsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for robots-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("RobotsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).buildVirtualSite("RobotsHelp", null);
  }

  @Test
  public void buildVirtualSiteRobotsTxtCredentialsPropagates400() {
    when(adaptor.buildVirtualSite(eq("RobotsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for robots-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("RobotsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).buildVirtualSite("RobotsHelp", null);
  }

  @Test
  public void buildVirtualSiteLlmsTxtFixtureDelegates() throws Exception {
    Path llmsRoot = tempDir.resolve("llms-site");
    Files.createDirectories(llmsRoot);
    Files.writeString(
        llmsRoot.resolve("_config.yaml"),
        """
        site:
          title: Llms Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        llms:
          file: llms.txt
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        llmsRoot.resolve("llms.txt"),
        """
        # Llms Docs
        - [Quickstart](docs/quickstart.md): Hello-from-llms
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("llms-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("LlmsHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("LlmsHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("LlmsHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getPagesWritten().intValue() > 0);
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(llmsRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(llmsRoot.resolve("llms.txt")));
    verify(adaptor).buildVirtualSite("LlmsHelp", req);
  }

  @Test
  public void buildVirtualSiteLlmsTxtRemoteUrlPropagates400() {
    when(adaptor.buildVirtualSite(eq("LlmsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for llms-txt", Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("LlmsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).buildVirtualSite("LlmsHelp", null);
  }

  @Test
  public void buildVirtualSiteLlmsTxtCloudRootPathPropagates400() {
    when(adaptor.buildVirtualSite(eq("LlmsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for llms-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("LlmsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).buildVirtualSite("LlmsHelp", null);
  }

  @Test
  public void buildVirtualSiteLlmsTxtCredentialsPropagates400() {
    when(adaptor.buildVirtualSite(eq("LlmsHelp"), any()))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for llms-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("LlmsHelp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).buildVirtualSite("LlmsHelp", null);
  }

  @Test
  public void buildVirtualSiteUnknownKindPropagates400() {
    when(adaptor.buildVirtualSite(eq("Help"), any()))
        .thenThrow(
            new WebApplicationException(
                "Unsupported virtual.sourceKind", Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor).buildVirtualSite("Help", null);
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
    assertEquals("8.2/index.html", out.getHomePath());
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
  public void previewStatusDelegatesSqlDatabase() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("SqlHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("SqlHelp");
  }

  @Test
  public void previewStatusSqlDatabaseMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("SqlHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("SqlHelp");
  }

  @Test
  public void previewFileDelegatesSqlDatabaseHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">SQL Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("SqlHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("SqlHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/SqlHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("SQL Home"), text);
    verify(adaptor).previewVirtualSiteFile("SqlHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesHttpJson() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("HttpHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("HttpHelp");
  }

  @Test
  public void previewStatusHttpJsonMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("HttpHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("HttpHelp");
  }

  @Test
  public void previewFileDelegatesHttpJsonHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">HTTP Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("HttpHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("HttpHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/HttpHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("HTTP Home"), text);
    verify(adaptor).previewVirtualSiteFile("HttpHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesObjectStorage() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("ObjectHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("ObjectHelp");
  }

  @Test
  public void previewStatusObjectStorageMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("ObjectHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("ObjectHelp");
  }

  @Test
  public void previewFileDelegatesObjectStorageHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Object Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("ObjectHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/ObjectHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("Object Home"), text);
    verify(adaptor).previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesRssAtom() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("RssHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RssHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("RssHelp");
  }

  @Test
  public void previewStatusRssAtomMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("RssHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RssHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("RssHelp");
  }

  @Test
  public void previewFileDelegatesRssAtomHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">RSS Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("RssHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("RssHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/RssHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("RSS Home"), text);
    verify(adaptor).previewVirtualSiteFile("RssHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesIcalendar() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("CalHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("CalHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("CalHelp");
  }

  @Test
  public void previewStatusIcalendarMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("CalHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("CalHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("CalHelp");
  }

  @Test
  public void previewFileDelegatesIcalendarHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Cal Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("CalHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("CalHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/CalHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("Cal Home"), text);
    verify(adaptor).previewVirtualSiteFile("CalHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesSitemapXml() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("SitemapHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SitemapHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("SitemapHelp");
  }

  @Test
  public void previewStatusSitemapXmlMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("SitemapHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SitemapHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("SitemapHelp");
  }

  @Test
  public void previewFileDelegatesSitemapXmlHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Sitemap Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("SitemapHelp"), eq("8.2/index.html")))
        .thenReturn(
            new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("SitemapHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("/services/sites/SitemapHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("Sitemap Home"), text);
    verify(adaptor).previewVirtualSiteFile("SitemapHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusSitemapXmlLeftoverRemoteUrl400() {
    when(adaptor.getVirtualSitePreviewStatus("SitemapHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for sitemap-xml",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getVirtualSitePreviewStatus("SitemapHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  public void previewStatusDelegatesRobotsTxt() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/star-1.html");
    when(adaptor.getVirtualSitePreviewStatus("RobotsHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RobotsHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/star-1.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("RobotsHelp");
  }

  @Test
  public void previewStatusRobotsTxtMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("RobotsHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RobotsHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("RobotsHelp");
  }

  @Test
  public void previewFileDelegatesRobotsTxtHtml() {
    byte[] html = "<a href=\"/8.2/star-1.html\">Robots Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("RobotsHelp"), eq("8.2/star-1.html")))
        .thenReturn(
            new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/star-1.html", html));

    Response out = resource.previewVirtualSiteFile("RobotsHelp", "8.2/star-1.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("/services/sites/RobotsHelp/virtual/preview/8.2/star-1.html"), text);
    assertTrue(text.contains("Robots Home"), text);
    verify(adaptor).previewVirtualSiteFile("RobotsHelp", "8.2/star-1.html");
  }

  @Test
  public void previewStatusRobotsTxtLeftoverRemoteUrl400() {
    when(adaptor.getVirtualSitePreviewStatus("RobotsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for robots-txt",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getVirtualSitePreviewStatus("RobotsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  public void previewStatusDelegatesLlmsTxt() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/Quickstart-1.html");
    when(adaptor.getVirtualSitePreviewStatus("LlmsHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("LlmsHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/Quickstart-1.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("LlmsHelp");
  }

  @Test
  public void previewStatusLlmsTxtMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("LlmsHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("LlmsHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("LlmsHelp");
  }

  @Test
  public void previewFileDelegatesLlmsTxtHtml() {
    byte[] html = "<a href=\"/8.2/Quickstart-1.html\">Quickstart</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("LlmsHelp"), eq("8.2/Quickstart-1.html")))
        .thenReturn(
            new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/Quickstart-1.html", html));

    Response out = resource.previewVirtualSiteFile("LlmsHelp", "8.2/Quickstart-1.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("/services/sites/LlmsHelp/virtual/preview/8.2/Quickstart-1.html"), text);
    assertTrue(text.contains("Quickstart"), text);
    verify(adaptor).previewVirtualSiteFile("LlmsHelp", "8.2/Quickstart-1.html");
  }

  @Test
  public void previewStatusLlmsTxtLeftoverRemoteUrl400() {
    when(adaptor.getVirtualSitePreviewStatus("LlmsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for llms-txt",
                Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getVirtualSitePreviewStatus("LlmsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  public void buildVirtualSiteBlankName400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.buildVirtualSite(" ", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).buildVirtualSite(any(), any());
  }

  @Test
  public void publishVirtualSiteDelegates() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("Help");
    published.setPagesWritten(3);
    published.setFilesCopied(5);
    published.setPublishPath("C:/pub/help");
    when(adaptor.publishVirtualSite("Help")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("Help");
    assertEquals(3, out.getPagesWritten().intValue());
    assertEquals(5, out.getFilesCopied().intValue());
    assertEquals("C:/pub/help", out.getPublishPath());
    verify(adaptor).publishVirtualSite("Help");
  }

  @Test
  public void publishVirtualSiteDelegatesSqlDatabase() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("SqlHelp");
    published.setSiteKey("sql-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("sql-pub").toString());
    when(adaptor.publishVirtualSite("SqlHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("SqlHelp");
    assertEquals("SqlHelp", out.getSiteName());
    assertEquals("sql-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("SqlHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesHttpJson() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("HttpHelp");
    published.setSiteKey("http-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("http-pub").toString());
    when(adaptor.publishVirtualSite("HttpHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("HttpHelp");
    assertEquals("HttpHelp", out.getSiteName());
    assertEquals("http-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("HttpHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesObjectStorage() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("ObjHelp");
    published.setSiteKey("obj-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("obj-pub").toString());
    when(adaptor.publishVirtualSite("ObjHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("ObjHelp");
    assertEquals("ObjHelp", out.getSiteName());
    assertEquals("obj-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("ObjHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesRssAtom() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("RssHelp");
    published.setSiteKey("rss-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("rss-pub").toString());
    when(adaptor.publishVirtualSite("RssHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("RssHelp");
    assertEquals("RssHelp", out.getSiteName());
    assertEquals("rss-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("RssHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesIcalendar() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("CalHelp");
    published.setSiteKey("cal-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("cal-pub").toString());
    when(adaptor.publishVirtualSite("CalHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("CalHelp");
    assertEquals("CalHelp", out.getSiteName());
    assertEquals("cal-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("CalHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesSitemapXml() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("SitemapHelp");
    published.setSiteKey("sitemap-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("sm-pub").toString());
    when(adaptor.publishVirtualSite("SitemapHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("SitemapHelp");
    assertEquals("SitemapHelp", out.getSiteName());
    assertEquals("sitemap-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("SitemapHelp");
  }

  @Test
  public void publishVirtualSiteSitemapXmlRemoteUrlPropagates400() {
    when(adaptor.publishVirtualSite("SitemapHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for sitemap-xml", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("SitemapHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).publishVirtualSite("SitemapHelp");
  }

  @Test
  public void publishVirtualSiteSitemapXmlCloudRootPropagates400() {
    when(adaptor.publishVirtualSite("SitemapHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for sitemap-xml must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("SitemapHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).publishVirtualSite("SitemapHelp");
  }

  @Test
  public void publishVirtualSiteSitemapXmlCredentialsPropagates400() {
    when(adaptor.publishVirtualSite("SitemapHelp"))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for sitemap-xml (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("SitemapHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).publishVirtualSite("SitemapHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesRobotsTxt() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("RobotsHelp");
    published.setSiteKey("rb-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("rb-pub").toString());
    when(adaptor.publishVirtualSite("RobotsHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("RobotsHelp");
    assertEquals("RobotsHelp", out.getSiteName());
    assertEquals("rb-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("RobotsHelp");
  }

  @Test
  public void publishVirtualSiteRobotsTxtRemoteUrlPropagates400() {
    when(adaptor.publishVirtualSite("RobotsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for robots-txt", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("RobotsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).publishVirtualSite("RobotsHelp");
  }

  @Test
  public void publishVirtualSiteRobotsTxtCloudRootPropagates400() {
    when(adaptor.publishVirtualSite("RobotsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for robots-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("RobotsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).publishVirtualSite("RobotsHelp");
  }

  @Test
  public void publishVirtualSiteRobotsTxtCredentialsPropagates400() {
    when(adaptor.publishVirtualSite("RobotsHelp"))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for robots-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("RobotsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).publishVirtualSite("RobotsHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesLlmsTxt() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("LlmsHelp");
    published.setSiteKey("llms-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("llms-pub").toString());
    when(adaptor.publishVirtualSite("LlmsHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("LlmsHelp");
    assertEquals("LlmsHelp", out.getSiteName());
    assertEquals("llms-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("LlmsHelp");
  }

  @Test
  public void publishVirtualSiteLlmsTxtRemoteUrlPropagates400() {
    when(adaptor.publishVirtualSite("LlmsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.remoteUrl is not supported for llms-txt", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("LlmsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(adaptor).publishVirtualSite("LlmsHelp");
  }

  @Test
  public void publishVirtualSiteLlmsTxtCloudRootPropagates400() {
    when(adaptor.publishVirtualSite("LlmsHelp"))
        .thenThrow(
            new WebApplicationException(
                "virtual.rootPath for llms-txt must be a local filesystem path (NIO Path). Cloud URLs are rejected.",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("LlmsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(adaptor).publishVirtualSite("LlmsHelp");
  }

  @Test
  public void publishVirtualSiteLlmsTxtCredentialsPropagates400() {
    when(adaptor.publishVirtualSite("LlmsHelp"))
        .thenThrow(
            new WebApplicationException(
                "Credential property is not allowed for llms-txt (no AWS/IAM/secrets on this envelope).",
                Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.publishVirtualSite("LlmsHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(adaptor).publishVirtualSite("LlmsHelp");
  }

  @Test
  public void publishVirtualSiteBlankName400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.publishVirtualSite(" "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).publishVirtualSite(any());
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
    String putVirtualBlock = text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual\")"));
    assertTrue(
        putVirtualBlock.contains("http-json"),
        "updateVirtualProperties OpenAPI description must mention http-json");
    assertTrue(
        putVirtualBlock.contains("rss-atom"),
        "updateVirtualProperties OpenAPI description must mention rss-atom persist");
    assertTrue(
        putVirtualBlock.contains("icalendar"),
        "updateVirtualProperties OpenAPI description must mention icalendar persist");
    assertTrue(
        putVirtualBlock.contains("sitemap-xml"),
        "updateVirtualProperties OpenAPI description must mention sitemap-xml persist");
    assertTrue(
        putVirtualBlock.contains("robots-txt"),
        "updateVirtualProperties OpenAPI description must mention robots-txt persist");
    assertTrue(
        putVirtualBlock.contains("llms-txt"),
        "updateVirtualProperties OpenAPI description must mention llms-txt persist");
    assertTrue(
        putVirtualBlock.contains("no CalDAV"),
        "updateVirtualProperties OpenAPI description must mention icalendar local fixture only");
    assertTrue(
        putVirtualBlock.contains("no live crawl"),
        "updateVirtualProperties OpenAPI description must mention sitemap-xml local fixture only");
    assertTrue(
        putVirtualBlock.contains("robots.txt fixture"),
        "updateVirtualProperties OpenAPI description must mention robots-txt local fixture only");
    assertTrue(
        putVirtualBlock.contains("llms.txt fixture"),
        "updateVirtualProperties OpenAPI description must mention llms-txt local fixture only");
    assertTrue(
        putVirtualBlock.contains("local/loopback"),
        "updateVirtualProperties OpenAPI description must mention rss-atom local/loopback only");
    assertTrue(
        text.contains(
            "return requireAdaptor().buildVirtualSite(nameOrId, request); // codeql[java/xss]"),
        "buildVirtualSite return sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains("csv-filesystem"),
        "buildVirtualSite OpenAPI description must mention csv-filesystem");
    assertTrue(
        text.contains("sql-database"),
        "buildVirtualSite OpenAPI description must mention sql-database");
    String buildBlock = text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/build\")"));
    assertTrue(
        buildBlock.contains("http-json"),
        "buildVirtualSite OpenAPI description must mention http-json");
    assertTrue(
        buildBlock.contains("object-storage"),
        "buildVirtualSite OpenAPI description must mention object-storage");
    assertTrue(
        buildBlock.contains("rss-atom"),
        "buildVirtualSite OpenAPI description must mention rss-atom");
    assertTrue(
        buildBlock.contains("icalendar"),
        "buildVirtualSite OpenAPI description must mention icalendar");
    assertTrue(
        buildBlock.contains("sitemap-xml"),
        "buildVirtualSite OpenAPI description must mention sitemap-xml");
    assertTrue(
        buildBlock.contains("robots-txt"),
        "buildVirtualSite OpenAPI description must mention robots-txt");
    assertTrue(
        buildBlock.contains("llms-txt"),
        "buildVirtualSite OpenAPI description must mention llms-txt");
    assertTrue(
        buildBlock.contains("llms.txt") || buildBlock.contains("no live HTTP fetch"),
        "buildVirtualSite OpenAPI description must mention local llms.txt fixture");
    assertTrue(
        buildBlock.contains("sitemap.xml") || buildBlock.contains("no live crawl"),
        "buildVirtualSite OpenAPI description must mention local sitemap.xml fixture");
    assertTrue(
        buildBlock.contains("current file")
            && (buildBlock.contains("no JVM") || buildBlock.contains("Jetty")),
        "buildVirtualSite OpenAPI description must mention sitemap-xml rebuild from current file");
    assertTrue(
        buildBlock.contains("calendar.ics") || buildBlock.contains("no CalDAV"),
        "buildVirtualSite OpenAPI description must mention local icalendar fixture");
    assertTrue(
        buildBlock.contains("feed.xml") || buildBlock.contains("loopback"),
        "buildVirtualSite OpenAPI description must mention local RSS/Atom fixture or loopback");
    assertTrue(
        buildBlock.contains("jdbc:h2:mem:"),
        "buildVirtualSite OpenAPI description must mention in-memory H2 jdbc:h2:mem:");
    String publishBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/publish\")"));
    assertTrue(
        publishBlock.contains("csv-filesystem"),
        "publishVirtualSite OpenAPI description must mention csv-filesystem");
    assertTrue(
        publishBlock.contains("sql-database"),
        "publishVirtualSite OpenAPI description must mention sql-database");
    assertTrue(
        publishBlock.contains("http-json"),
        "publishVirtualSite OpenAPI description must mention http-json");
    assertTrue(
        publishBlock.contains("object-storage"),
        "publishVirtualSite OpenAPI description must mention object-storage");
    assertTrue(
        publishBlock.contains("rss-atom"),
        "publishVirtualSite OpenAPI description must mention rss-atom");
    assertTrue(
        publishBlock.contains("icalendar"),
        "publishVirtualSite OpenAPI description must mention icalendar");
    assertTrue(
        publishBlock.contains("sitemap-xml"),
        "publishVirtualSite OpenAPI description must mention sitemap-xml");
    assertTrue(
        publishBlock.contains("robots-txt"),
        "publishVirtualSite OpenAPI description must mention robots-txt");
    assertTrue(
        publishBlock.contains("robots.txt") || publishBlock.contains("no live crawl"),
        "publishVirtualSite OpenAPI description must mention robots-txt local fixture only");
    assertTrue(
        publishBlock.contains("llms-txt"),
        "publishVirtualSite OpenAPI description must mention llms-txt");
    assertTrue(
        publishBlock.contains("llms.txt") || publishBlock.contains("no live HTTP fetch"),
        "publishVirtualSite OpenAPI description must mention llms-txt local fixture only");
    assertTrue(
        publishBlock.contains("no live crawl"),
        "publishVirtualSite OpenAPI description must mention sitemap-xml local fixture only");
    assertTrue(
        publishBlock.contains("calendar.ics") || publishBlock.contains("no CalDAV"),
        "publishVirtualSite OpenAPI description must mention local icalendar fixture");
    assertTrue(
        publishBlock.contains("feed.xml") || publishBlock.contains("loopback"),
        "publishVirtualSite OpenAPI description must mention local RSS/Atom fixture or loopback");
    assertTrue(
        publishBlock.contains("jdbc:h2:mem:"),
        "publishVirtualSite OpenAPI description must mention in-memory H2 jdbc:h2:mem:");
    assertTrue(
        text.contains(
            "return requireAdaptor().getVirtualSitePreviewStatus(nameOrId); // codeql[java/xss]"),
        "getVirtualSitePreviewStatus return sink must carry same-line codeql[java/xss]");
    String previewStatusBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/preview\")"));
    String previewFileBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/preview/{relPath:.*}\")"));
    assertTrue(
        previewStatusBlock.contains("csv-filesystem"),
        "getVirtualSitePreviewStatus OpenAPI description must mention csv-filesystem");
    assertTrue(
        previewStatusBlock.contains("sql-database"),
        "getVirtualSitePreviewStatus OpenAPI description must mention sql-database");
    assertTrue(
        previewStatusBlock.contains("http-json"),
        "getVirtualSitePreviewStatus OpenAPI description must mention http-json");
    assertTrue(
        previewStatusBlock.contains("object-storage"),
        "getVirtualSitePreviewStatus OpenAPI description must mention object-storage");
    assertTrue(
        previewStatusBlock.contains("rss-atom"),
        "getVirtualSitePreviewStatus OpenAPI description must mention rss-atom");
    assertTrue(
        previewStatusBlock.contains("icalendar"),
        "getVirtualSitePreviewStatus OpenAPI description must mention icalendar");
    assertTrue(
        previewStatusBlock.contains("sitemap-xml"),
        "getVirtualSitePreviewStatus OpenAPI description must mention sitemap-xml");
    assertTrue(
        previewStatusBlock.contains("robots-txt"),
        "getVirtualSitePreviewStatus OpenAPI description must mention robots-txt");
    assertTrue(
        previewStatusBlock.contains("llms-txt"),
        "getVirtualSitePreviewStatus OpenAPI description must mention llms-txt");
    assertTrue(
        previewStatusBlock.contains("no live crawl")
            || previewStatusBlock.contains("last-build local HTML"),
        "getVirtualSitePreviewStatus OpenAPI description must mention sitemap-xml last-build local HTML");
    assertTrue(
        previewFileBlock.contains("csv-filesystem"),
        "previewVirtualSiteFile OpenAPI description must mention csv-filesystem");
    assertTrue(
        previewFileBlock.contains("sql-database"),
        "previewVirtualSiteFile OpenAPI description must mention sql-database");
    assertTrue(
        previewFileBlock.contains("http-json"),
        "previewVirtualSiteFile OpenAPI description must mention http-json");
    assertTrue(
        previewFileBlock.contains("object-storage"),
        "previewVirtualSiteFile OpenAPI description must mention object-storage");
    assertTrue(
        previewFileBlock.contains("rss-atom"),
        "previewVirtualSiteFile OpenAPI description must mention rss-atom");
    assertTrue(
        previewFileBlock.contains("icalendar"),
        "previewVirtualSiteFile OpenAPI description must mention icalendar");
    assertTrue(
        previewFileBlock.contains("sitemap-xml"),
        "previewVirtualSiteFile OpenAPI description must mention sitemap-xml");
    assertTrue(
        previewFileBlock.contains("robots-txt"),
        "previewVirtualSiteFile OpenAPI description must mention robots-txt");
    assertTrue(
        previewFileBlock.contains("llms-txt"),
        "previewVirtualSiteFile OpenAPI description must mention llms-txt");
    assertTrue(
        previewFileBlock.contains("no live crawl")
            || previewFileBlock.contains("last-build local HTML"),
        "previewVirtualSiteFile OpenAPI description must mention sitemap-xml last-build local HTML");
    assertTrue(
        previewFileBlock.contains("20 MB"),
        "previewVirtualSiteFile OpenAPI description must mention the 20 MB size cap");
    assertTrue(
        text.contains(".build(); // codeql[java/xss]"),
        "previewVirtualSiteFile Response.ok sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains("return requireAdaptor().publishVirtualSite(nameOrId); // codeql[java/xss]"),
        "publishVirtualSite return sink must carry same-line codeql[java/xss]");
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
