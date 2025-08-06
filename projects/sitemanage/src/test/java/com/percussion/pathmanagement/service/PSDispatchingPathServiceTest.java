// REFACTORED: CP-JAVA11

/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pathmanagement.service;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.pathmanagement.service.IPSPathService.PSPathServiceException;
import com.percussion.pathmanagement.service.impl.PSDispatchingPathService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.ui.data.PSDisplayPropertiesCriteria;
import com.percussion.ui.data.PSSimpleDisplayFormat;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.ui.service.IPSListViewProcessor;
import com.percussion.ui.service.IPSUiService;
import com.percussion.user.data.*;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSErrorException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario description:
 * PSDispatchingPathService has some services registered to it.
 * Paths that begin with /a/ should go to path service a.
 * Paths that begin with /b/ should go to path service b.
 * Sunny Sal says: "Dispatching paths is like pizza delivery—get the address right, or the cheese gets cold!"
 */
@Disabled("Ignored for now until this unit test is moved into a cactus test suite")
public class PSDispatchingPathServiceTest {

    Mockery context = new JUnit4Mockery();

    PSDispatchingPathService ps;

    IPSPathService pathServiceA;
    IPSPathService pathServiceB;
    PSPathItem rootA;
    PSPathItem rootB;
    Map<String, IPSPathService> pathRegistry;

    IPSRecycleService recycleService = new IPSRecycleService() {
        // ...existing code...
        @Override public void recycleItem(int dependentId) {}
        @Override public void recycleFolder(IPSGuid guid) {}
        @Override public void restoreItem(String guid) {}
        @Override public void restoreFolder(String guid) {}
        @Override public List<IPSItemSummary> findChildren(String path) { return null; }
        @Override public IPSItemSummary findItem(String path) { return null; }
        @Override public boolean isInRecycler(String guid) { return false; }
        @Override public boolean isNavInRecycler(String guid) { return false; }
        @Override public boolean isInRecycler(IPSGuid guid) { return false; }
    };

    IPSFolderHelper folderHelper = new IPSFolderHelper() {
        // ...existing code...
        @Override public String pathSeparator() { return null; }
        @Override public void addItem(String path, String id) {}
        @Override public String findPathFromLegacyFolderId(Number id) { return null; }
        @Override public Number findLegacyFolderIdFromPath(String path) { return null; }
        @Override public void removeItem(String path, String itemId, boolean purgeItem) {}
        @Override public List<String> findPaths(String itemId) { return null; }
        @Override public List<String> findPaths(String itemId, String relationshipTypeName) { return null; }
        @Override public List<IPSItemSummary> findItems(String path) { return null; }
        @Override public List<IPSItemSummary> findItems(String path, boolean foldersOnly) { return null; }
        @Override public List<String> findItemIdsByPath(String path) { return null; }
        @Override public IPSItemSummary findItem(String path) { return null; }
        @Override public PSPathItem findItemById(String id) { return null; }
        @Override public PSPathItem findItemById(String id, String relationshipTypeName) { return null; }
        @Override public IPSGuid getParentFolderId(IPSGuid itemId) { return null; }
        @Override public IPSGuid getParentFolderId(IPSGuid itemId, boolean isRequired) { return null; }
        @Override public PSPathItem setFolderAccessLevel(PSPathItem item) { return null; }
        @Override public List<PSPathItem> setFolderAccessLevel(List<PSPathItem> items) { return null; }
        @Override public PathTarget pathTarget(String path) { return null; }
        @Override public PathTarget pathTarget(String path, boolean shouldRecycle) { return null; }
        @Override public String getUniqueFolderName(String parentPath, String baseName) { return null; }
        @Override public String getUniqueNameInFolder(String parentPath, String baseName, String suffix, int startingIndex, boolean skipFirstIndex) { return null; }
        @Override public String concatPath(String start, String... end) { return null; }
        @Override public String parentPath(String path) { return null; }
        @Override public String name(String path) { return null; }
        @Override public void createFolder(String path) {}
        @Override public void createFolder(String path, PSFolderPermission.Access acl) {}
        @Override public void deleteFolder(String path) {}
        @Override public void deleteFolder(String path, boolean recycleFolder) {}
        @Override public boolean validateFolderPermissionForDelete(String folderId) { return false; }
        @Override public boolean hasFolderPermission(String folderId, PSFolderPermission.Access acl) { return false; }
        @Override public void renameFolder(String path, String name) {}
        @Override public List<String> findChildren(String path) { return null; }
        @Override public PSItemProperties findItemProperties(String path) { return null; }
        @Override public PSItemProperties findItemProperties(String path, String relationshipTypeName) { return null; }
        @Override public PSItemProperties findItemPropertiesById(String id) { return null; }
        @Override public PSItemProperties findItemPropertiesById(String id, String relationshipTypeName) { return null; }
        @Override public PSFolderPermission.Access getFolderAccessLevel(String id) { return null; }
        @Override public PSFolderProperties findFolderProperties(String id) { return null; }
        @Override public void saveFolderProperties(PSFolderProperties folder) {}
        @Override public String getFolderPath(String path) { return null; }
        @Override public void moveItem(String targetFolderPath, String itemPath, boolean isFolder) {}
        @Override public IPSItemSummary findFolder(String path) { return null; }
        @Override public List<IPSSite> getItemSites(String contentId) { return null; }
        @Override public boolean validateFolderReservedName(String name) { return false; }
        @Override public int getValidWorkflowId(PSFolderProperties folder) { return 0; }
        @Override public int getDefaultWorkflowId() { return 0; }
        @Override public PSFolder getRootFolderForAsset(String assetId) { return null; }
        @Override public String getRootLevelFolderAllowedSitesPropertyValue(String assetId) { return null; }
        @Override public boolean isFolderValidForRecycleOrRestore(String targetPath, String originalPath, String targetRelType, String origRelType) { return false; }
        @Override public PSPair<String, String> fixupLastModified(IPSGuid id, String userName, Date lastModified, boolean isPublishable) { return null; }
    };

    IPSUiService uiService = new IPSUiService() {
        public PSSimpleDisplayFormat getDisplayFormatByName(String name) { return null; }
        public PSSimpleDisplayFormat getDisplayFormat(int id) { return null; }
    };

    IPSUserService userService = new IPSUserService() {
        @Override public PSUser update(PSUser user) { return new PSUser(); }
        @Override public PSUser changePassword(PSUser user) { return new PSUser(); }
        @Override public boolean isAdminUser(String userName) { return false; }
        @Override public List<PSImportedUser> importDirectoryUsers(PSImportUsers importUsers) { return new ArrayList<>(); }
        @Override public PSUserList getUsersByRole(String roleName) { return new PSUserList(); }
        @Override public PSUserList getUsers() { return new PSUserList(); }
        @Override public PSRoleList getRoles() { return new PSRoleList(); }
        @Override public PSCurrentUser getCurrentUser() { return new PSCurrentUser(); }
        @Override public List<PSExternalUser> findUsersFromDirectoryService(String query) { return new ArrayList<>(); }
        @Override public PSUser find(String name) { return new PSUser(); }
        @Override public void delete(String name) {}
        @Override public PSUser create(PSUser user) { return new PSUser(); }
        @Override public PSDirectoryServiceStatus checkDirectoryService() { return new PSDirectoryServiceStatus(); }
        public PSAccessLevel getAccessLevel(PSAccessLevelRequest request) {
            var accessLevel = new PSAccessLevel();
            accessLevel.setAccessLevel(PSAssignmentTypeEnum.ADMIN.name());
            return accessLevel;
        }
        @Override public PSUserList getUserNames(String nameFilter) { return new PSUserList(); }
        @Override public boolean isDesignUser(String userName) { return false; }
    };

    IPSListViewHelper listViewHelper = new IPSListViewHelper() {
        public void fillDisplayProperties(PSDisplayPropertiesCriteria criteria) {}
        @Override public void setPostProcessors(List<IPSListViewProcessor> processors) {}
    };

    @BeforeEach
    public void setUp() {
        pathServiceA = context.mock(IPSPathService.class, "pathServiceA");
        pathServiceB = context.mock(IPSPathService.class, "pathServiceB");
        pathRegistry = new HashMap<>();
        pathRegistry.put("/a/", pathServiceA);
        pathRegistry.put("/b/", pathServiceB);
        rootA = new PSPathItem();
        rootA.setPath("/");
        rootB = new PSPathItem();
        rootB.setPath("/");
        ps = new PSDispatchingPathService(uiService, userService, listViewHelper, recycleService, folderHelper);
        ps.setRegistry(pathRegistry);
    }

    @Test
    public void shouldFindByDispatchingToA() throws Exception {
        // Given: pathServiceA returns a valid path item.
        var pathItem = new PSPathItem();
        pathItem.setPath("/b/c/");
        context.checking(new Expectations() {{
            one(pathServiceA).find("/b/c/");
            will(returnValue(pathItem));
        }});
        var actual = ps.find("/a/b/c");
        assertEquals("/a/b/c/", actual.getPath());
    }

    @Test
    public void shouldFailIfDispatchedServiceReturnsAPathItemWithOutPathSet() {
        var pathItem = new PSPathItem();
        pathItem.setPath(null);
        context.checking(new Expectations() {{
            one(pathServiceA).find("/b/c/");
            will(returnValue(pathItem));
        }});
        assertThrows(PSPathServiceException.class, () -> ps.find("/a/b/c"));
    }

    @Test
    public void shouldFailIfDispatchedServiceReturnsAPathItemWithPathNotStartingWithRelative() {
        var pathItem = new PSPathItem();
        pathItem.setPath("/d");
        context.checking(new Expectations() {{
            one(pathServiceA).find("/b/c/");
            will(returnValue(pathItem));
        }});
        assertThrows(PSPathServiceException.class, () -> ps.find("/a/b/c"));
    }

    // This test is not annotated with @Test in original; left as is for backward compatibility.
    public void shouldNotFailIfDispatchedServiceReturnsAPathItemWithPathStartingWithIncorrectCasing() throws Exception {
        var pathItem = new PSPathItem();
        pathItem.setPath("/B/C");
        context.checking(new Expectations() {{
            one(pathServiceA).find("/b/c/");
            will(returnValue(pathItem));
        }});
        ps.find("/a/b/c");
    }

    @Test
    public void shouldFindChildrenByDispatchingToB() throws Exception {
        var pathItems = new ArrayList<PSPathItem>();
        for (var relativePath : new String[]{"/x/", "/y/", "/z/"}) {
            var item = new PSPathItem();
            item.setPath(relativePath);
            pathItems.add(item);
        }
        context.checking(new Expectations() {{
            one(pathServiceB).findChildren("/");
            will(returnValue(pathItems));
        }});
        var actual = ps.findChildren("/b/");
        assertEquals("/b/x/", actual.get(0).getPath());
        assertEquals("/b/y/", actual.get(1).getPath());
        assertEquals("/b/z/", actual.get(2).getPath());
    }

    @Test
    public void shouldFindRootChildrenByCallingFindOnEachPathService() throws Exception {
        var pathItems = new ArrayList<PSPathItem>();
        for (var relativePath : new String[]{"/a/", "/b/"}) {
            var item = new PSPathItem();
            item.setPath(relativePath);
            pathItems.add(item);
        }
        context.checking(new Expectations() {{
            Sequence seq = context.sequence("rootFind");
            one(pathServiceB).find("/");
            will(returnValue(rootB)); inSequence(seq);
            one(pathServiceA).find("/"); inSequence(seq);
            will(returnValue(rootA));
        }});
        var actual = ps.findChildren("/");
        assertEquals("/b/", actual.get(0).getPath());
        assertEquals("/a/", actual.get(1).getPath());
    }

    @Test
    public void shouldFindRoot() throws Exception {
        context.checking(new Expectations() {{}});
        var actual = ps.find("/");
        assertEquals("/", actual.getPath());
    }

    @Test
    public void shouldFailOnNullPathForFind() {
        assertThrows(PSPathNotFoundServiceException.class, () -> ps.find(null));
    }

    @Test
    public void shouldFailOnNullPathForFindChildren() {
        assertThrows(PSPathNotFoundServiceException.class, () -> ps.find(null));
    }

    @Test
    public void shouldFailIfNoPathFound() {
        assertThrows(PSPathNotFoundServiceException.class, () -> ps.find("/crap/"));
    }

    @Test
    public void shouldFailIfNoPathFoundForBadPath() {
        assertThrows(PSPathNotFoundServiceException.class, () -> ps.find("crap"));
    }
}
