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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.data.IPSItemSummary.Category;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins every {@code getCategory} dispatch branch so the cognitive-complexity
 * extraction for #6961 cannot silently change categorization.
 */
class PSItemSummaryServiceCategoryTest {

  private static final String REL = "FolderContent";
  private static final String ASSET_PARENT = "//Folders/$System$/Assets";

  @Test
  void landingPageWinsOverEveryOtherSignal() throws Exception {
    var ctx = folderContext("CorporateInvestments", List.of(ASSET_PARENT));
    when(ctx.navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString()))
        .thenReturn(mock(IPSGuid.class));

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, true, REL);

    assertEquals(Category.LANDING_PAGE, out.getCategory());
  }

  @Test
  void externalLinkNavTypeMapsToExternalSectionFolder() throws Exception {
    var ctx = folderContext("Section", List.of("//Sites/Corp"));
    navOn(ctx, "externallink");

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.EXTERNAL_SECTION_FOLDER, out.getCategory());
  }

  @Test
  void sectionNavTypeMapsToSectionFolder() throws Exception {
    var ctx = folderContext("Section", List.of("//Sites/Corp"));
    navOn(ctx, "section");

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.SECTION_FOLDER, out.getCategory());
  }

  @Test
  void pageContentTypeMapsToPage() throws Exception {
    var ctx = itemContext("Home", "percPage", ObjectTypeEnum.ITEM, List.of("//Sites/Corp"));

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.PAGE, out.getCategory());
  }

  @Test
  void folderUnderAssetRootMapsToSystem() throws Exception {
    var ctx = folderContext("uploads", List.of(ASSET_PARENT));

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.SYSTEM, out.getCategory());
  }

  @Test
  void pathlessFolderMapsToFolderNeverSystem() throws Exception {
    var ctx = folderContext("Recycling", List.of());

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.FOLDER, out.getCategory());
  }

  @Test
  void plainItemMapsToAsset() throws Exception {
    var ctx = itemContext("logo", "percFileAsset", ObjectTypeEnum.ITEM, List.of(ASSET_PARENT));

    var out = new PSDataItemSummary();
    ctx.sut.convert(ctx.item, out, false, REL);

    assertEquals(Category.ASSET, out.getCategory());
  }

  /** Folder item with the given parent paths and no nav association. */
  private static CategoryContext folderContext(String name, List<String> parentPaths)
      throws Exception {
    return itemContext(name, "Folder", ObjectTypeEnum.FOLDER, parentPaths);
  }

  private static CategoryContext itemContext(
      String name, String contentType, ObjectTypeEnum objectType, List<String> parentPaths)
      throws Exception {
    var contentWs = mock(IPSContentWs.class);
    var itemDefManager = mock(PSItemDefManager.class);
    var idMapper = mock(IPSIdMapper.class);
    var navService = mock(IPSManagedNavService.class);
    var sut = new PSItemSummaryService(contentWs, itemDefManager, idMapper, navService);

    var guid = mock(PSLegacyGuid.class);
    when(idMapper.getString(guid)).thenReturn("id-" + name);
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("id-" + name);

    var item = mock(PSItemSummary.class);
    when(item.getGUID()).thenReturn(guid);
    when(item.getName()).thenReturn(name);
    when(item.getObjectType()).thenReturn(objectType);
    when(item.getContentTypeName()).thenReturn(contentType);
    when(item.isRevisionLock()).thenReturn(false);

    when(itemDefManager.contentTypeNameToId(contentType)).thenReturn(101L);
    when(itemDefManager.contentTypeIdToLabel(101L)).thenReturn(contentType);
    // Covers the ASSET icon fallback (getIconFromSystem) for non-folder items.
    var locator = mock(PSLocator.class);
    when(idMapper.getLocator(anyString())).thenReturn(locator);
    when(itemDefManager.getContentTypeIconPaths(anyList()))
        .thenReturn(Map.of(locator, "../sys_resources/images/finderFile.png"));
    when(contentWs.findFolderPaths(guid, REL)).thenReturn(parentPaths.toArray(new String[0]));
    when(navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString()))
        .thenReturn(null);

    var ctx = new CategoryContext();
    ctx.sut = sut;
    ctx.contentWs = contentWs;
    ctx.itemDefManager = itemDefManager;
    ctx.navService = navService;
    ctx.item = item;
    return ctx;
  }

  /** Associates the context folder with a navon of the given section type. */
  private static void navOn(CategoryContext ctx, String navType) throws Exception {
    var navGuid = mock(IPSGuid.class);
    when(ctx.navService.findNavigationIdFromFolder(any(IPSGuid.class), anyString()))
        .thenReturn(navGuid);
    var navSum = mock(PSItemSummary.class);
    when(navSum.getContentTypeId()).thenReturn(101);
    when(ctx.contentWs.findItems(anyList(), anyBoolean()))
        .thenReturn(Collections.singletonList(navSum));
    when(ctx.itemDefManager.contentTypeIdToName(101L)).thenReturn("rffNavon");
    when(ctx.navService.getNavonProperties(any(IPSGuid.class), anyList()))
        .thenReturn(Map.of(IPSManagedNavService.NAVON_FIELD_TYPE, navType));
  }

  private static class CategoryContext {
    PSItemSummaryService sut;
    IPSContentWs contentWs;
    PSItemDefManager itemDefManager;
    IPSManagedNavService navService;
    PSItemSummary item;
  }
}
