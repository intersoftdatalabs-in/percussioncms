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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.rest.contentexplorer.folders.AddFolderRequest;
import com.percussion.rest.contentexplorer.folders.AddFolderTreeRequest;
import com.percussion.rest.contentexplorer.folders.FolderChildrenRequest;
import com.percussion.rest.contentexplorer.folders.RxFolder;
import com.percussion.rest.contentexplorer.folders.RxFolderChildList;
import com.percussion.rest.contentexplorer.folders.RxFolderProperty;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ContentExplorerFolderAdaptorTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSIdMapper idMapper;

  private ContentExplorerFolderAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void init() {
    adaptor = new ContentExplorerFolderAdaptor(contentWs, idMapper, idMapper::getGuid);
  }

  // ---- path normalize ----

  @Test
  void normalizeKeepsDoubleSlashFolders() {
    assertEquals("//Folders/a/b", ContentExplorerFolderAdaptor.normalizeRxPath("//Folders/a/b"));
  }

  @Test
  void normalizePromotesSingleSlashFolders() {
    assertEquals("//Folders/a", ContentExplorerFolderAdaptor.normalizeRxPath("/Folders/a"));
  }

  @Test
  void normalizePromotesSites() {
    assertEquals("//Sites/MySite", ContentExplorerFolderAdaptor.normalizeRxPath("/Sites/MySite"));
    assertEquals("//Sites/MySite", ContentExplorerFolderAdaptor.normalizeRxPath("Sites/MySite"));
  }

  @Test
  void normalizeRoot() {
    assertEquals("/", ContentExplorerFolderAdaptor.normalizeRxPath("/"));
  }

  @Test
  void normalizeBlankIsNull() {
    assertNull(ContentExplorerFolderAdaptor.normalizeRxPath("  "));
    assertNull(ContentExplorerFolderAdaptor.normalizeRxPath(null));
  }

  @Test
  void normalizeStripsTrailingSlash() {
    assertEquals("//Folders", ContentExplorerFolderAdaptor.normalizeRxPath("//Folders/"));
  }

  @Test
  void normalizeBackslashAndDrive() {
    assertEquals(
        "//Folders/a", ContentExplorerFolderAdaptor.normalizeRxPath("C:\\Folders\\a"));
  }


  @Test
  void normalizeCollapsesDuplicateSlashes() {
    assertEquals("//Folders/a/b", ContentExplorerFolderAdaptor.normalizeRxPath("//Folders//a///b"));
    assertEquals("//Folders/a", ContentExplorerFolderAdaptor.normalizeRxPath("/Folders//a//"));
  }

  @Test
  void normalizeStripsLongTrailingSlashRunWithoutRegex() {
    // Adversarial trailing-slash run must remain linear (CodeQL #1977 / java/polynomial-redos).
    String path = "//Folders/a" + "/".repeat(5000);
    long start = System.nanoTime();
    assertEquals("//Folders/a", ContentExplorerFolderAdaptor.normalizeRxPath(path));
    long ms = (System.nanoTime() - start) / 1_000_000L;
    assertTrue(ms < 1000L, "normalizeRxPath took " + ms + "ms on long trailing slash run");
  }

  // ---- load ----

  @Test
  void loadByPathMapsFolder() throws Exception {
    PSFolder folder = new PSFolder("Assets", 100, -1, 0, "desc");
    folder.setGuid(new PSLegacyGuid(100, 1));
    folder.setFolderPath("//Folders/$System$/Assets");
    when(contentWs.loadFolders(new String[] {"//Folders/$System$/Assets"}))
        .thenReturn(List.of(folder));
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("100");

    RxFolder out = adaptor.loadByPath(base, "/Folders/$System$/Assets");

    assertNotNull(out);
    assertEquals("Assets", out.getName());
    assertEquals("100", out.getId());
    assertEquals("//Folders/$System$/Assets", out.getPath());
  }

  @Test
  void loadByPathMissingReturnsNull() throws Exception {
    when(contentWs.loadFolders(any(String[].class))).thenThrow(new PSErrorResultsException());
    assertNull(adaptor.loadByPath(base, "//Folders/missing"));
  }

  @Test
  void loadByIdUsesNumericContentId() throws Exception {
    PSFolder folder = new PSFolder("X", 55, -1, 0, "");
    folder.setGuid(new PSLegacyGuid(55, 1));
    when(contentWs.loadFolder(any(IPSGuid.class), eq(true))).thenReturn(folder);
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("55");

    RxFolder out = adaptor.loadById(base, "55");
    assertEquals("X", out.getName());

    ArgumentCaptor<IPSGuid> cap = ArgumentCaptor.forClass(IPSGuid.class);
    verify(contentWs).loadFolder(cap.capture(), eq(true));
    assertTrue(cap.getValue() instanceof PSLegacyGuid);
    assertEquals(55, ((PSLegacyGuid) cap.getValue()).getContentId());
  }

  @Test
  void loadByIdRejectsUnsafeChars() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.loadById(base, "a b"));
  }

  // ---- children ----

  @Test
  void findChildrenByPathDelegates() throws Exception {
    PSItemSummary child = new PSItemSummary(200, "childFolder");
    when(contentWs.findFolderChildren(eq("//Folders"), eq(false))).thenReturn(List.of(child));
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("200");

    RxFolderChildList out = adaptor.findChildrenByPath(base, "/Folders");
    assertEquals(1, out.getChildren().size());
    assertEquals("childFolder", out.getChildren().get(0).getName());
    assertEquals("//Folders", out.getParentPath());
  }

  // ---- add ----

  @Test
  void addFolderRequiresName() {
    AddFolderRequest req = new AddFolderRequest();
    req.setParentPath("//Folders");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addFolder(base, req));
    assertTrue(ex.getMessage().contains("name"));
  }

  @Test
  void addFolderCallsWs() throws Exception {
    AddFolderRequest req = new AddFolderRequest();
    req.setName("NewFolder");
    req.setParentPath("/Folders");
    PSFolder created = new PSFolder("NewFolder", 300, -1, 0, "");
    created.setGuid(new PSLegacyGuid(300, 1));
    when(contentWs.addFolder(eq("NewFolder"), eq("//Folders"), eq(true))).thenReturn(created);
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("300");

    RxFolder out = adaptor.addFolder(base, req);
    assertEquals("NewFolder", out.getName());
  }

  @Test
  void addFolderTreeRequiresPath() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.addFolderTree(base, new AddFolderTreeRequest()));
  }

  // ---- save ----

  @Test
  void saveFolderAppliesNameAndProps() throws Exception {
    PSFolder existing = new PSFolder("Old", 10, -1, 0, "d");
    existing.setGuid(new PSLegacyGuid(10, 1));
    when(contentWs.loadFolder(any(IPSGuid.class), eq(true))).thenReturn(existing);
    when(contentWs.saveFolder(any(PSFolder.class))).thenAnswer(inv -> inv.getArgument(0));
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("10");

    RxFolder body = new RxFolder();
    body.setName("NewName");
    body.setDescription("nd");
    body.setProperties(List.of(new RxFolderProperty("sys_pubFileName", "pub", "")));

    RxFolder out = adaptor.saveFolder(base, "10", body);
    assertEquals("NewName", out.getName());
    verify(contentWs).saveFolder(any(PSFolder.class));
  }

  // ---- multi-child ----

  @Test
  void moveChildrenByPath() throws Exception {
    FolderChildrenRequest req = new FolderChildrenRequest();
    req.setSourcePath("//Folders/a");
    req.setTargetPath("//Folders/b");
    req.setChildIds(List.of("5"));

    adaptor.moveChildren(base, req);

    verify(contentWs)
        .moveFolderChildren(eq("//Folders/a"), eq("//Folders/b"), anyList());
  }

  @Test
  void addChildrenRequiresParent() {
    FolderChildrenRequest req = new FolderChildrenRequest();
    req.setChildIds(List.of("1"));
    assertThrows(IllegalArgumentException.class, () -> adaptor.addChildren(base, req));
  }

  @Test
  void removeChildrenWithPurge() throws Exception {
    FolderChildrenRequest req = new FolderChildrenRequest();
    req.setParentPath("//Folders/a");
    req.setChildIds(List.of("9"));
    req.setPurgeItems(true);

    adaptor.removeChildren(base, req);

    verify(contentWs)
        .removeFolderChildren(eq("//Folders/a"), anyList(), eq(true));
  }

  @Test
  void deleteFolderCallsWs() throws Exception {
    adaptor.deleteFolder(base, "42", false);
    verify(contentWs).deleteFolders(anyList(), eq(false), eq(true));
  }
}
