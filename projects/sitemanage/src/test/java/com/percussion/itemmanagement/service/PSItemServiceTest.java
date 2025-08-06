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

package com.percussion.itemmanagement.service;

import static java.util.Arrays.asList;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.itemmanagement.service.IPSItemService.PSUserItemTypeEnum;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.user.data.PSUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(IntegrationTest.class)
public class PSItemServiceTest extends PSServletTestCase
{
    private PSSiteDataServletTestCaseFixture fixture;

    @Override
    public void setUp() throws Exception
    {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
    }

    public void testCopyFolder() throws Exception
    {
        String asset1Id = null;
        String asset2Id = null;
        var folderIds = new ArrayList<IPSGuid>();

        try
        {
            // Create a folder
            var folders = contentWs.addFolderTree(PSAssetPathItemService.ASSET_ROOT + "/Test");
            var folderPath = folders.get(0).getFolderPath();
            var folderId = folders.get(0).getGuid();
            folderIds.add(folderId);

            // Create an asset in folder
            var asset1 = createAsset("asset1", folderPath);
            asset1Id = asset1.getId();

            // Create a sub-folder
            folders = contentWs.addFolderTree(PSAssetPathItemService.ASSET_ROOT + "/Test/TestSub");
            var subFolderPath = folders.get(0).getFolderPath();

            // Create an asset in sub-folder
            var asset2 = createAsset("asset2", subFolderPath);
            asset2Id = asset2.getId();

            // Copy folder
            var assetMap = itemService.copyFolder(folderPath, PSAssetPathItemService.ASSET_ROOT, "TestCopy");

            var copyFolders = contentWs.loadFolders(new String[]{PSAssetPathItemService.ASSET_ROOT + "/TestCopy"});
            assertTrue(!copyFolders.isEmpty());
            var copyFolderId = copyFolders.get(0).getGuid();
            folderIds.add(copyFolderId);
            assertFolders(folderId, copyFolderId);

            assertEquals(2, assetMap.size());
            assertTrue(assetMap.containsKey(asset1Id));
            assertTrue(assetMap.containsKey(asset2Id));

            // Create another folder
            folders = contentWs.addFolderTree(PSAssetPathItemService.ASSET_ROOT + "/TestCopy2");
            var copy2FolderId = folders.get(0).getGuid();
            folderIds.add(copy2FolderId);

            // Copy folder to existing folder
            assetMap = itemService.copyFolder(folderPath, PSAssetPathItemService.ASSET_ROOT, "TestCopy2");

            copyFolders = contentWs.loadFolders(new String[]{PSAssetPathItemService.ASSET_ROOT + "/TestCopy2"});
            assertTrue(!copyFolders.isEmpty());

            assertFolders(folderId, copy2FolderId);

            assertEquals(2, assetMap.size());
            assertTrue(assetMap.containsKey(asset1Id));
            assertTrue(assetMap.containsKey(asset2Id));
        }
        finally
        {
            // Wait before deleting (to allow for search index queue processing)
            Thread.sleep(1000);

            if (asset1Id != null)
            {
                assetService.delete(asset1Id);
            }

            if (asset2Id != null)
            {
                assetService.delete(asset2Id);
            }

            if (!folderIds.isEmpty())
            {
                contentWs.deleteFolders(folderIds, false);
            }
        }
    }

    public void testMyPages() throws Exception
    {
        myPagesAddRemoveTest();
        myPagesItemDeleteTest();
        myPagesDeleteUserTest();
        myPagesGetTest();
    }

    /**
     * Test add and remove from my pages.
     */
    private void myPagesAddRemoveTest() throws Exception
    {
        var page1 = fixture.createPage("myPagesAddRemoveTest1");
        var id1 = page1.getId();
        assertFalse(itemService.isMyPage(id1));
        itemService.addToMyPages(id1);
        assertTrue(itemService.isMyPage(id1));
        itemService.removeFromMyPages(id1);
        assertFalse(itemService.isMyPage(id1));

        // Add the page to multiple users and make sure data is not getting overwritten
        var count = itemService.getUserItems(id1).size();
        itemService.addToMyPages(id1);
        itemService.addUserItem("Editor", idMapper.getContentId(id1), PSUserItemTypeEnum.FAVORITE_PAGE);
        assertTrue(itemService.getUserItems(idMapper.getContentId(id1)).size() == count + 2);
    }

    /**
     * Test my pages functionality when an item is deleted.
     */
    private void myPagesItemDeleteTest() throws Exception
    {
        var page1 = fixture.createPage("myPagesItemDeleteTest1");
        var page2 = fixture.createPage("myPagesItemDeleteTest2");
        var id1 = page1.getId();
        var id2 = page2.getId();
        itemService.addToMyPages(id1);
        itemService.addToMyPages(id2);

        pageService.delete(id1);
        assertFalse(itemService.isMyPage(id1));
        assertTrue(itemService.isMyPage(id2));
    }

    /**
     * Test my pages get functionality.
     */
    private void myPagesGetTest() throws Exception
    {
        var origCount = itemService.getUserItems(PSWebserviceUtils.getUserName()).size();
        var page1 = fixture.createPage("myPagesGetTest1");
        var page2 = fixture.createPage("myPagesGetTest2");
        var id1 = page1.getId();
        var id2 = page2.getId();
        itemService.addToMyPages(id1);
        itemService.addToMyPages(id2);
        var newCount = itemService.getUserItems(PSWebserviceUtils.getUserName()).size();
        assertTrue(newCount == origCount + 2);
        itemService.addUserItem("Editor", idMapper.getContentId(id1), PSUserItemTypeEnum.FAVORITE_PAGE);
        newCount = itemService.getUserItems(PSWebserviceUtils.getUserName()).size();
        assertTrue(newCount == origCount + 2);
    }

    /**
     * Test my pages when a user is deleted.
     */
    private void myPagesDeleteUserTest() throws Exception
    {
        var testuser = "MyPagesTestUser";
        var myPagesUser = new PSUser();
        myPagesUser.setName(testuser);
        myPagesUser.setPassword("demo");
        myPagesUser.setRoles(Collections.singletonList(PSRoleService.ADMINISTRATOR_ROLE));
        userService.create(myPagesUser);
        var isUserDeleted = false;
        try
        {
            var page1 = fixture.createPage("myPagesDeleteUserTest1");
            var page2 = fixture.createPage("myPagesDeleteUserTest2");
            var id1 = page1.getId();
            var id2 = page2.getId();
            itemService.addUserItem(testuser, idMapper.getContentId(id1), PSUserItemTypeEnum.FAVORITE_PAGE);
            itemService.addUserItem(testuser, idMapper.getContentId(id2), PSUserItemTypeEnum.FAVORITE_PAGE);
            var count = itemService.getUserItems(testuser).size();
            assertTrue(count > 0);
            userService.delete(testuser);
            isUserDeleted = true;
            count = itemService.getUserItems(testuser).size();
            assertTrue(count == 0);
        }
        finally
        {
            if (!isUserDeleted)
                userService.delete(testuser);
        }
    }

    /**
     * Creates an asset.
     *
     * @param name   asset name, not null.
     * @param folder folder path, not null.
     * @return {@link PSAsset} representation of the asset item, never null.
     * @throws Exception if an error occurs saving the asset.
     */
    private PSAsset createAsset(String name, String folder) throws Exception
    {
        var asset = new PSAsset();
        asset.getFields().put("sys_title", name);
        asset.setType("percRawHtmlAsset");
        asset.getFields().put("html", "TestHTML");
        if (folder != null)
        {
            asset.setFolderPaths(asList(folder));
        }
        return assetService.save(asset);
    }

    private void assertFolders(IPSGuid id1, IPSGuid id2)
    {
        var items1 = contentWs.findFolderChildren(id1, false);
        var items2 = contentWs.findFolderChildren(id2, false);
        assertEquals(items1.size(), items2.size());

        for (var item1 : items1)
        {
            var match = false;
            var item1Name = item1.getName();
            var item1Guid = item1.getGUID();

            for (var item2 : items2)
            {
                var item2Name = item2.getName();
                var item2Guid = item2.getGUID();

                if (item2Name.equals(item1Name))
                {
                    PSFolder folder1 = null;
                    try
                    {
                        folder1 = contentWs.loadFolder(item1Guid);
                    }
                    catch (PSErrorException e)
                    {
                        // item1 is not a folder
                    }

                    if (folder1 != null)
                    {
                        try
                        {
                            contentWs.loadFolder(item2Guid);
                        }
                        catch (PSErrorException e)
                        {
                            fail(item2Name + " should be a folder");
                        }

                        assertFolders(item2Guid, item1Guid);
                    }

                    match = true;
                    break;
                }
            }

            assertTrue("match not found for " + item1Name, match);
        }
    }

    public IPSAssetService getAssetService()
    {
        return assetService;
    }

    public void setAssetService(IPSAssetService assetService)
    {
        this.assetService = assetService;
    }

    /**
     * @return the contentWs
     */
    public IPSContentWs getContentWs()
    {
        return contentWs;
    }

    /**
     * @param contentWs the contentWs to set
     */
    public void setContentWs(IPSContentWs contentWs)
    {
        this.contentWs = contentWs;
    }

    /**
     * @return the itemService
     */
    public IPSItemService getItemService()
    {
        return itemService;
    }

    /**
     * @param itemService the itemService to set
     */
    public void setItemService(IPSItemService itemService)
    {
        this.itemService = itemService;
    }

    public IPSPageService getPageService()
    {
        return pageService;
    }

    public void setPageService(IPSPageService pageService)
    {
        this.pageService = pageService;
    }

    public IPSUserService getUserService()
    {
        return userService;
    }

    public void setUserService(IPSUserService userService)
    {
        this.userService = userService;
    }

    public IPSIdMapper getIdMapper()
    {
        return idMapper;
    }

    public void setIdMapper(IPSIdMapper idMapper)
    {
        this.idMapper = idMapper;
    }

    private IPSAssetService assetService;
    private IPSContentWs contentWs;
    private IPSItemService itemService;
    private IPSPageService pageService;
    private IPSUserService userService;
    private IPSIdMapper idMapper;


}
