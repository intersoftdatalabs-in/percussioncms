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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.data.PSContentTraffic;
import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.data.impl.PSAnalyticsQueryResult;
import com.percussion.assetmanagement.data.PSAbstractAssetRequest.AssetType;
import com.percussion.assetmanagement.data.PSBinaryAssetRequest;
import com.percussion.assetmanagement.data.PSExtractedAssetRequest;
import com.percussion.assetmanagement.data.PSOrphanedAssetSummary;
import com.percussion.assetmanagement.data.PSUnusedAssetSummary;
import com.percussion.integritymanagement.data.PSIntegrityTaskProperty;
import com.percussion.itemmanagement.data.PSComment;
import com.percussion.pagemanagement.dao.impl.PSResourceDefinitionUniqueId;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService.PSResourceDefinitionInvalidIdException;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSExternalUser;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserProviderType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for DTO constructors after this-escape mitigation (issue #2980): direct field
 * seeds preserve validation and defaults without overridable setter calls during construction.
 */
class PSThisEscapeDtoConstructorTest {

  @Test
  void contentActivityCtorsSeedFieldsAndValidateName() {
    var a = new PSContentActivity("Pages", 1, 2, 3, 4, 5);
    assertEquals("Pages", a.getName());
    assertEquals(1, a.getPublishedItems());
    assertEquals(5, a.getArchivedItems());

    var b = new PSContentActivity("SiteA", "Folder", 9, 8, 7, 6, 5);
    assertEquals("SiteA", b.getSiteName());
    assertEquals("Folder", b.getName());

    var c = new PSContentActivity("SiteA", "/Sites/A", "Home", 1, 0, 0, 0, 0);
    assertEquals("/Sites/A", c.getPath());
    assertEquals("Home", c.getName());

    assertThrows(
        IllegalArgumentException.class, () -> new PSContentActivity("  ", 0, 0, 0, 0, 0));
  }

  @Test
  void contentTrafficCtorSeedsSite() {
    var t = new PSContentTraffic("mysite");
    assertEquals("mysite", t.getSite().orElseThrow());
  }

  @Test
  void integrityTaskPropertyCtorValidatesName() {
    var p = new PSIntegrityTaskProperty("key", "val");
    assertEquals("key", p.getName());
    assertEquals("val", p.getValue());
    assertThrows(IllegalArgumentException.class, () -> new PSIntegrityTaskProperty("", "x"));
  }

  @Test
  void itemCommentCtorSeedsFields() {
    var when = new Date(1_700_000_000_000L);
    var c = new PSComment("hello", "admin", "checkin", when);
    assertEquals("hello", c.getComment());
    assertEquals("admin", c.getCommenter());
    assertEquals("checkin", c.getCommentType());
    assertEquals(when, c.getCommentDate());
  }

  @Test
  void renderLinkCtorsSeedUrlDefaults() {
    var empty = new PSInlineRenderLink();
    assertEquals("", empty.getUrl());
    assertEquals("", empty.getThumbUrl());
    assertEquals("", empty.getTitle());
    assertEquals("", empty.getStateClass());

    var link = new PSRenderLink("https://example.test/a", null);
    assertEquals("https://example.test/a", link.getUrl());
    assertNull(link.getResourceDefinition());
  }

  @Test
  void binaryAndExtractedAssetRequestCtorsValidate() {
    var bytes = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
    var bin =
        new PSBinaryAssetRequest(
            "/Assets/uploads", AssetType.FILE, "note.txt", "text/plain", bytes);
    assertEquals("/Assets/uploads", bin.getFolderPath());
    assertEquals(AssetType.FILE, bin.getType());
    assertEquals("note.txt", bin.getFileName());
    assertEquals("text/plain", bin.getFileType());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSBinaryAssetRequest(
                " ", AssetType.FILE, "a", "b", new ByteArrayInputStream(new byte[0])));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSBinaryAssetRequest(
                "/p", AssetType.HTML, "a", "b", new ByteArrayInputStream(new byte[0])));

    var ext =
        new PSExtractedAssetRequest(
            "/Assets/html",
            AssetType.HTML,
            "page.html",
            new ByteArrayInputStream(new byte[0]),
            "div.main",
            true);
    assertEquals(AssetType.HTML, ext.getType());
    assertEquals("div.main", ext.getSelector());
    assertTrue(ext.shouldIncludeOuterHtml());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSExtractedAssetRequest(
                "/p",
                AssetType.FILE,
                "a",
                new ByteArrayInputStream(new byte[0]),
                "sel",
                false));
  }

  @Test
  void orphanedAndUnusedAssetSummaryCtors() {
    var orphan = new PSOrphanedAssetSummary("//1-101", "slot-1", "richText", 42);
    assertEquals("//1-101", orphan.getId());
    assertEquals("slot-1", orphan.getSlotId().orElseThrow());
    assertEquals("richText", orphan.getWidgetName().orElseThrow());
    assertEquals(42, orphan.getRelationshipId());

    var base = new PSDataItemSummary();
    base.setId("//2-202");
    base.setName("widget.png");
    base.setLabel("Image");
    base.setType("percImageAsset");
    base.setFolderPaths(List.of("/Assets/uploads"));
    base.setRevisionable(true);

    var unused = new PSUnusedAssetSummary(base);
    assertEquals("//2-202", unused.getId());
    assertEquals("widget.png", unused.getName());
    assertEquals("Image", unused.getLabel());
    assertEquals("percImageAsset", unused.getType());
    assertEquals(List.of("/Assets/uploads"), unused.getFolderPaths());
    assertTrue(unused.isRevisionable());
  }

  @Test
  void pathItemAndSiteSummaryDefaults() {
    var path = new PSPathItem();
    assertEquals(List.of(), path.getFolderPaths());

    var site = new PSSiteSummary();
    assertEquals(PSDataItemSummary.TYPE_SITE, site.getType());
  }

  @Test
  void userCtorsSeedFromSource() {
    var external = new PSExternalUser("ldap-user");
    assertEquals("ldap-user", external.getName());

    var src = new PSUser();
    src.setName("editor");
    src.setPassword("secret");
    src.setEmail("e@example.test");
    src.setProviderType(PSUserProviderType.INTERNAL);
    src.setRoles(List.of("Editor", "Author"));

    var current = new PSCurrentUser(src);
    assertEquals("editor", current.getName());
    assertEquals("secret", current.getPassword());
    assertEquals("e@example.test", current.getEmail());
    assertEquals(PSUserProviderType.INTERNAL, current.getProviderType());
    assertEquals(List.of("Editor", "Author"), current.getRoles());
  }

  @Test
  void analyticsConfigAndQueryResultCtors() {
    var cfg =
        new PSAnalyticsProviderConfig(
            "uid", "pwd", true, Map.of("account", "123", "profile", "p1"));
    assertEquals("uid", cfg.getUserid());
    assertEquals("pwd", cfg.getPassword());
    assertTrue(cfg.isEncrypted());
    assertEquals("123", cfg.getExtraParamsMap().get("account"));
    assertEquals("p1", cfg.getExtraParamsMap().get("profile"));

    var row = new PSAnalyticsQueryResult(Map.of("sessions", 12, "source", "web"));
    assertEquals(12, row.getInt("sessions"));
    assertEquals("web", row.getString("source"));
  }

  @Test
  void resourceDefinitionUniqueIdInit() throws PSResourceDefinitionInvalidIdException {
    var id = new PSResourceDefinitionUniqueId("sys_resources.jquery");
    assertEquals("sys_resources", id.getGroupId());
    assertEquals("jquery", id.getLocalId());
    assertEquals("sys_resources.jquery", id.getUniqueId());

    var id2 = new PSResourceDefinitionUniqueId("g", "local");
    assertEquals("g.local", id2.getUniqueId());

    assertThrows(
        PSResourceDefinitionInvalidIdException.class, () -> new PSResourceDefinitionUniqueId("bad"));
    assertThrows(
        PSResourceDefinitionInvalidIdException.class,
        () -> new PSResourceDefinitionUniqueId("a.b.c"));
  }

  @Test
  void pathItemIsNotFolderByDefault() {
    var path = new PSPathItem();
    assertFalse(path.isFolder());
  }
}
