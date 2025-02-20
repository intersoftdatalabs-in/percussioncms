/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.IPSItemSummary.Category;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSItemSummaryFactoryService;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import com.percussion.util.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.thread.PSThreadUtils;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;


@PSSiteManageBean("itemSummaryService")
public class PSItemSummaryService implements IPSItemSummaryFactoryService, IPSDataItemSummaryService
{
    private static final String ICON_BASE_PATH = "/Rhythmyx/";

    private static final String FOLDER_ICON_PATH = ICON_BASE_PATH + "sys_resources/images/finderFolder.png";
    private static final String FOLDER_NAVON_ICON_PATH = ICON_BASE_PATH + "sys_resources/images/finderFolderNavigation.png";
    private static final String PAGE_ICON_PATH = ICON_BASE_PATH + "sys_resources/images/finderPage.png";
    private static final String LANDING_PAGE_ICON_PATH = ICON_BASE_PATH + "sys_resources/images/finderLandingPage.png";
    
    private final IPSContentWs contentWs;
    private final PSItemDefManager itemDefManager;
    private final IPSIdMapper idMapper;
    private final IPSManagedNavService navService;
    
    private static final String ASSET_ROOT = PSAssetPathItemService.ASSET_ROOT;

    private static final String FOLDER_RELATE_TYPE = PSRelationshipConfig.TYPE_FOLDER_CONTENT;
    private static final String RECYCLED_TYPE = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
    private static final String RECYCLING_ROOT = PSRecycleService.RECYCLING_ROOT;

    /**
     * Folders created by the system.
     */
    private static final String[] SYSTEM_FOLDERS = new String[]
    {ASSET_ROOT, ASSET_ROOT + "/forms", ASSET_ROOT + "/uploads", ASSET_ROOT + "/uploads/files", ASSET_ROOT + "/uploads/images",ASSET_ROOT + "/calendars",ASSET_ROOT + "/polls"};

    @Autowired
    public PSItemSummaryService(IPSContentWs contentWs, PSItemDefManager itemDefManager, IPSIdMapper idMapper,
            IPSManagedNavService navService)
    {
        super();
        this.contentWs = contentWs;
        this.itemDefManager = itemDefManager;
        this.idMapper = idMapper;
        this.navService = navService;
    }

    @Override
    public String pathToId(String path) throws DataServiceNotFoundException {
        String pathToId;
        if(path==null){
            throw new IllegalArgumentException("path may not be null or empty");
        }
        if(path.contains(RECYCLING_ROOT)){
            //CMS-8526 : The id was being returned null as for recycle bin the relationship type should be RecycledContent and others FolderContent.
            //Null value led to other display properties not getting populated thus the relevant data in column was not showing up in list view of Recycle bin.
            pathToId = pathToId(path, RECYCLED_TYPE);
        }else{
            pathToId = pathToId(path, FOLDER_RELATE_TYPE);
        }
        return pathToId;
    }

    @Override
    public String pathToId(String path, String relationshipTypeName) throws DataServiceNotFoundException {
        notEmpty(path, "Path cannot be null or empty");
        try
        {
            String normalizedPath = StringUtils.removeEnd(path, "/");
            if(log.isTraceEnabled())
                log.trace("Getting id for path: {} normalized: {}",path , normalizedPath);
            IPSGuid guid = contentWs.getIdByPath(normalizedPath, relationshipTypeName);
            String id = guid == null ? null : idMapper.getString(guid);
            if(log.isTraceEnabled())
                log.trace(format("Converted path: {0} to id {1}", normalizedPath, id));
            return id;
        }
        catch (PSErrorException e)
        {
            throw new DataServiceNotFoundException("Failed to convert path: " + path, e);
        }

    }

    @Override
    public PSDataItemSummary find(String id) throws DataServiceLoadException
    {
        notEmpty(id, "id");
        return find(defaultItemSummaryFactory, id, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    }

    @Override
    public PSDataItemSummary find(String id, String relationshipTypeName) throws DataServiceLoadException
    {
        notEmpty(id, "id");
        return find(defaultItemSummaryFactory, id, relationshipTypeName);
    }
    
    @Override
    public List<PSDataItemSummary> findChildFolders(String id) throws DataServiceLoadException {
        notEmpty(id, "id");
        try
        {
            IPSGuid guid = idMapper.getGuid(id);
            List<PSItemSummary> sums = contentWs.findChildFolders(guid);
           
            return convert(defaultItemSummaryFactory, sums, -1, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
        }
        catch (Exception e)
        {
            log.error("Failed to load: {}" ,id);
            throw new DataServiceLoadException(e);
        }
    }

    
    public List<PSDataItemSummary> findFolderChildren(String id) throws DataServiceLoadException
    {

        return findFolderChildren(defaultItemSummaryFactory, id);
    }
    
    public List<PSDataItemSummary> findAll() throws DataServiceLoadException, DataServiceNotFoundException
    {
        throw new UnsupportedOperationException("findAll is not yet supported");
    }



    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    public static final Logger log = LogManager.getLogger(PSItemSummaryService.class);


    
    private <F extends IPSItemSummary> List<F> convert(IPSCatalogItemFactory<F, String> factory,
            List<PSItemSummary> sums, int landingPageId, String relationshipTypeName) throws PSErrorException, PSInvalidContentTypeException, DataServiceLoadException {
        List<F> items = new ArrayList<>();
        for(PSItemSummary sum : sums) 
        {
            PSThreadUtils.checkForInterrupt();
            F item = factory.create("");
            boolean isLandingPage = ((PSLegacyGuid)sum.getGUID()).getContentId() == landingPageId;
            
            convert(sum, item, isLandingPage, relationshipTypeName);
            items.add(item);
        }
        return items;
    }

    /**
     * Determines if the specified item is a folder.
     * @param item the item in question, assumed not <code>null</code>.
     * @return <code>true</code> if the item is a folder; otherwise return <code>false</code>.
     */
    private boolean isFolder(PSItemSummary item)
    {
        return item.getObjectType().getOrdinal() == ObjectTypeEnum.FOLDER.getOrdinal();
    }
    
    /**
     * Determines if the specified item is a folder of a navigation (or section).
     * 
     * @param item the item in question, assumed not <code>null</code>.
     *
     * @param relationshipTypeName the type of relationship
     * @return <code>true</code> if the item is a navigation folder; otherwise
     * return <code>false</code>.
     */
    private String getNavFolderType(PSItemSummary item, String relationshipTypeName)
    {
        if (!isFolder(item))
            return null;
        String navType = null;
        IPSGuid childNavId = navService.findNavigationIdFromFolder(item.getGUID(), relationshipTypeName);
        if(childNavId != null)
        {
        	Map<String, String> map = navService.getNavonProperties(childNavId,Collections.singletonList(IPSManagedNavService.NAVON_FIELD_TYPE));
        	navType = map.get(IPSManagedNavService.NAVON_FIELD_TYPE);
        }
        return navType; 
    }
    
    protected <F extends IPSItemSummary> void convert(PSItemSummary itemSummary, F dataItemSummary,
            boolean isLandingPage, String relationshipTypeName) throws PSErrorException, PSInvalidContentTypeException
    {
        String id = idMapper.getString(itemSummary.getGUID());
        dataItemSummary.setId(id);
        dataItemSummary.setName(itemSummary.getName());

        String ctType = itemSummary.getContentTypeName();
        dataItemSummary.setType(ctType);

        long ctId = itemDefManager.contentTypeNameToId(ctType);
        dataItemSummary.setLabel(itemDefManager.contentTypeIdToLabel(ctId));

        List<String> parentPaths = asList(contentWs.findFolderPaths(itemSummary.getGUID(), relationshipTypeName));
        dataItemSummary.setFolderPaths(parentPaths);

        // Navons will always be of folder type.  This is because they are untouched when
        // an item is recycled.
        String navType = getNavFolderType(itemSummary, relationshipTypeName);
        Category cat = getCategory(itemSummary, navType, isLandingPage, parentPaths);
        dataItemSummary.setCategory(cat);

        boolean revisionLock = itemSummary.isRevisionLock();
        dataItemSummary.setRevisionable(revisionLock);

        String icon = getIcon(itemSummary, isLandingPage, navType);
        dataItemSummary.setIcon(icon);
    }

    /**
     * Gets the category for the given item.
     * 
     * @param itemSummary the item in question, assumed not <code>null</code>.
     * @param navType <code>true</code> if the category is for a
     * navigation folder.
     * @param isLandingPage <code>true</code> if the category is for a landing
     * page.
     * @param parentPaths the paths of the parent folders that contains the
     * item, it may be empty, but not <code>null</code>.
     * 
     * @return the category of the targeted item, never <code>null</code>.
     */
    private Category getCategory(PSItemSummary itemSummary, String navType, boolean isLandingPage,
            List<String> parentPaths)
    {
        String ctType = itemSummary.getContentTypeName();
        
        Category category = Category.ASSET;
        if (isLandingPage)
            category = Category.LANDING_PAGE;
        else if (navType != null)
        {
            if(navType.equals(PSSectionTypeEnum.externallink.name()))
            	category = Category.EXTERNAL_SECTION_FOLDER;
            else 
            	category = Category.SECTION_FOLDER;
        }
        else if (IPSPageService.PAGE_CONTENT_TYPE.equals(ctType))
            category = Category.PAGE;
        else if (isFolder(itemSummary))
        {
            String path = parentPaths.get(0) + "/" + itemSummary.getName();
            if (isSystemFolder(path))
                category = Category.SYSTEM;
            else
                category = Category.FOLDER;
        }
        
        return category;
    }

    /**
     * Determines if the specified folder is one of the folders created by the system.
     * 
     * @param folderPath the path of the folder in question, it may be <code>null</code> or empty.
     * 
     * @return <code>true</code> if the specified folder is one of the folders created by the system.
     */
    private boolean isSystemFolder(String folderPath)
    {
        for (String path : SYSTEM_FOLDERS)
        {
            if (path.equals(folderPath))
                return true;
        }
        return false;
    }
    
    /**
     * Gets the landing page from the specified item summaries.
     * 
     * @param sums the item summaries in question, assumed they are all under
     * the same folder, not <code>null</code>.
     * 
     * @return the ID of the landing page. It may be <code>-1</code> if there is
     * no landing page in the list.
     */
    private int getLandingPageId(List<PSItemSummary> sums)
    {
        List<Long> navonCType = navService.getNavonContentTypeIds();
        List<Long> navtreeCType = navService.getNavTreeContentTypeIds();
        PSItemSummary navItem = null;
        for (PSItemSummary sum : sums)
        {
            PSThreadUtils.checkForInterrupt();
            long type = sum.getContentTypeId();
            if (navonCType.contains(type) || navtreeCType.contains(type))
            {
                navItem = sum;
                break;
            }
        }
        if (navItem == null)
            return -1;
        
        PSLegacyGuid navId = (PSLegacyGuid) navItem.getGUID();
        IPSGuid pageGuid = navService.getLandingPageFromNavnode(navId);
        return (pageGuid == null) ? -1 : ((PSLegacyGuid)pageGuid).getContentId();
    }
    
    /**
     * Gets the icon path for the specified item. 
     * 
     * @param item the item in question, not <code>null</code>.
     * @param isLandingPage <code>true</code> if the item is a landing page;
     * otherwise it is <code>false</code>.
     * @param navType <code>true</code> if the item is a navigation folder.
     * 
     * @return the icon path, never blank.
     */
    private String getIcon(PSItemSummary item, boolean isLandingPage, String navType)
    {
        if (isLandingPage)
        {
            return LANDING_PAGE_ICON_PATH;
        }
        else if (navType != null)
        {
            return FOLDER_NAVON_ICON_PATH;
        }
        else if (item.getObjectType().getOrdinal() == ObjectTypeEnum.FOLDER.getOrdinal())
        {
            return FOLDER_ICON_PATH; 
        }
        
        // handle page
        String ctType = item.getContentTypeName();
        if (IPSPageService.PAGE_CONTENT_TYPE.equals(ctType))
        {
            return PAGE_ICON_PATH;
        }
        PSLegacyGuid id = ((PSLegacyGuid)item.getGUID());
        return getIcon(idMapper.getString(id));
    }
    
    public <F extends IPSItemSummary> List<F> findFolderChildren(
            IPSCatalogItemFactory<F, String> factory, String id)
            throws DataServiceLoadException
    {
        try
        {
            isTrue( ! StringUtils.startsWith(id, "//"), 
                    "findFolderChildren takes an id not a path. Use pathToId(path).");
            IPSGuid guid = idMapper.getGuid(id);
            List<PSItemSummary> sums = contentWs.findFolderChildren(guid, false);
            int landingPageId = getLandingPageId(sums);
            return convert(factory, sums, landingPageId, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
         
        }
        catch (Exception e)
        {
            String err = "Failed to load: " + id;
            log.error(err, e);
            throw new DataServiceLoadException(err,e);
        }

    }

    /**
     * Determines if the specified item is a landing page.
     * 
     * @param item the item in question, assumed not <code>null</code>.
     * 
     * @return <code>true</code> if the item is a landing page; otherwise
     * return <code>false</code>.
     */
    private boolean isLandingPage(PSItemSummary item, String relationshipTypeName)
    {
        if (!item.getContentTypeName().equals(IPSPageService.PAGE_CONTENT_TYPE))
            return false;
          
        return navService.isLandingPage(item.getGUID(), relationshipTypeName);
    }
    
    public <F extends IPSItemSummary> F find(
            IPSCatalogItemFactory<F, String> factory, String id)
            throws DataServiceLoadException
    {
        return find(factory, id, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    }

    public <F extends IPSItemSummary> F find(
            IPSCatalogItemFactory<F, String> factory, String id, String relationshipTypeName)
            throws DataServiceLoadException
    {
        try
        {
            IPSGuid guid = idMapper.getGuid(id);
            List<PSItemSummary> sums = contentWs.findItems(Collections.singletonList(guid), false);
            // get the landing page
            if (sums.isEmpty()) return null;

            PSItemSummary item = sums.get(0);
            int landingPageId = -1;
            if (isLandingPage(item, relationshipTypeName))
            {
                landingPageId = ((PSLegacyGuid) item.getGUID()).getContentId();
            }
            return convert(factory, sums, landingPageId, relationshipTypeName).get(0);
        }
        catch (Exception e)
        {
            String err = "Failed to load: " + id;
            log.error(err, e);
            throw new DataServiceLoadException(e);
        }
    }

    protected String getIcon(String id) {
        String path = getIconFromSystem(id);
        if (path != null)
            path = path.replaceFirst("^\\.\\./",ICON_BASE_PATH);
        return path;
    }
    
    protected String getIconFromSystem(String id) {
        PSLocator locator = idMapper.getLocator(id);
        Map<PSLocator, String> paths = itemDefManager.getContentTypeIconPaths(Collections.singletonList(locator));
        return paths.get(locator);
    }
    
    
    
    public <F extends IPSItemSummary> List<F> findAll(
            IPSCatalogItemFactory<F, String> factory)
            throws DataServiceLoadException, DataServiceNotFoundException
    {
        throw new UnsupportedOperationException("findAll is not yet supported");
    }

    
    public static class PSDataItemSummaryFactory implements IPSCatalogItemFactory<PSDataItemSummary, String>
    {

        public PSDataItemSummary create(String id) throws DataServiceLoadException
        {
            return new PSDataItemSummary();
        }
    }

    
    
    private final PSDataItemSummaryFactory defaultItemSummaryFactory = new PSDataItemSummaryFactory();



    
}
