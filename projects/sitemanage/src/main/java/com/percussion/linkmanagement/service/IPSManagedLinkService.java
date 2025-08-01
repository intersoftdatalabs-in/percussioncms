// REFACTORED: CP-JAVA11
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
package com.percussion.linkmanagement.service;

import com.percussion.cms.IPSConstants;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import org.jsoup.nodes.Element;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to manage links to CMS pages and resources.
 * <p>
 * Sunny Sal says: "Link management so smooth, even your URLs will want to be managed!"
 *
 * @author JaySeletz
 */
public interface IPSManagedLinkService {

    String PERC_MANAGED_OLD_ATTR = "perc-managed";
    String PERC_LINKID_OLD_ATTR = "perc-linkId";
    String PERC_MANAGED_ATTR = "data-perc-managed";
    String PERC_LINKID_ATTR = "data-perc-linkId";
    String PERC_OLD_IMAGE_SLIDER_CONFIG_ATTR = "config";
    String PERC_OLD_IMAGE_SLIDER_IMAGEPATH_ATTR = "imagePath";
    String TRUE_VAL = "true";
    String HREF_ATTR = "href";
    String SRC_ATTR = "src";
    String PERC_MANAGED_LINK_SELECTOR = "a[perc-managed]";
    String PERC_MANAGED_LINK_IMG_SELECTOR = "img[perc-managed]";

    // JSON Payload constants
    String PERC_CONFIG = "percJSONConfig";
    String PERC_IMAGEPATH = "percImagePath";
    String PERC_IMAGEPATH_LINKID = "percImagePathLinkId";
    String PERC_FILEPATH = "percFilePath";
    String PERC_FILEPATH_LINKID = "percFilePathLinkId";
    String PERC_PAGEPATH = "percPagePath";
    String PERC_PAGEPATH_LINKID = "percPagePathLinkId";

    String SERVER_PROPERTY_AUTO_MANAGE_LOCAL_PATHS = IPSConstants.SERVER_PROP_MANAGELINKS;
    String A_HREF = "a[href]";
    String IMG_SRC = "img[src]";
    String LEGACY_INLINETYPE = "inlinetype";

    /**
     * Attempt to manage any links found in the supplied source.
     *
     * @param parentId The id of the asset containing the supplied source, not null or empty.
     * @param source   The source html to check for links, not null.
     * @return The updated html, never null.
     */
    String manageLinks(String parentId, String source);

    /**
     * Remove managed attributes from any links found in the supplied source.
     *
     * @param source The source html to check for links, not null.
     * @return The updated html, never null.
     */
    String unmanageImageLinks(String source);

    /**
     * Attempts to cleanup any managed links referencing content ids that no longer exist.
     * Fixes database for old code that left these around.
     */
    void cleanupOrphanedLinks();

    /**
     * Update the href of any managed links found in the supplied source based on the supplied link context.
     *
     * @param linkContext The link context to use, may be null to use edit path for the link.
     * @param source      The source html to update, not null.
     * @param parentId    The parent id for the links.
     * @return The updated html, never null.
     */
    String renderLinks(PSRenderLinkContext linkContext, String source, Integer parentId);

    /**
     * Update the href of any managed links found in the supplied source based on the supplied link context.
     *
     * @param linkContext The link context to use, may be null to use edit path for the link.
     * @param source      The source html to update, not null.
     * @param isStaging   if true then staging filter is used otherwise public filter is used to filter the links
     * @param parentId    The parent id for the links.
     * @return The updated html, never null.
     */
    String renderLinks(PSRenderLinkContext linkContext, String source, Boolean isStaging, Integer parentId);

    /**
     * Update the path of any managed links found in the supplied JSON string based on the supplied link context.
     *
     * @param linkContext The link context to use, may be null to use edit path for the link.
     * @param source      The source JSON payload to update, not null.
     * @param isStaging   if true then staging filter is used otherwise public filter is used to filter the links
     * @return The updated html, never null.
     */
    String renderLinksInJSON(PSRenderLinkContext linkContext, String source, Boolean isStaging);

    /**
     * Manage links for a new item, where the parent id is not yet known. Once the item has been persisted and
     * the id is known, {@link #updateNewItemLinks(String)} must be called to update the link with the correct id.
     * Both calls must be made from the same thread. This call clears any stored new link data for the current thread.
     *
     * @param source The source html to check for links, not null.
     * @return The updated html, never null.
     */
    String manageNewItemLinks(String source);

    /**
     * Updates the parent id for new managed links created by the previous call to {@link #manageNewItemLinks(String)} on the same
     * thread, clears the new link data for the thread. Noop if no new link data is present for the thread.
     *
     * @param parentId The new parent id, not null or empty.
     */
    void updateNewItemLinks(String parentId);

    /**
     * Updates the managed links contained in the specified assets. The assets were created during copy a site.
     *
     * @param assetIds     the IDs of the assets that may have managed links, not null or empty.
     * @param origSiteRoot the root path of the original site, not blank.
     * @param copySiteRoot the root path of the copied site, not blank.
     * @param assetMap     the shared asset IDs that are created during copy above (original) site,
     *                     not null, may be empty.
     */
    void updateCopyAssetsLinks(Collection<String> assetIds, String origSiteRoot, String copySiteRoot, Map<String, String> assetMap);

    /**
     * Finds the dependent and returns it. If the supplied element has perc-linkid attribute, loads the managed link with that id and
     * returns the dependent, if the load fails or link id doesn't exist, finds the dependent based on the href attribute value.
     * If not found then returns -1.
     *
     * @param elem May be null, if null returns -1 for dependent.
     * @return int id of the dependent or -1 if not found.
     */
    int getDependent(Element elem);

    /**
     * Finds the dependent by linkId and path.
     *
     * @param linkId the link id
     * @param path   the path
     * @return int id of the dependent or -1 if not found.
     */
    int getDependent(String linkId, String path);

    /**
     * Returns a list of managed links for the given parent ids.
     *
     * @param parentIds collection of parent ids
     * @return list of managed link ids
     */
    List<String> getManagedLinks(Collection<String> parentIds);

    /**
     * Initialize the thread local storage of new item links. To be called before managing one or more links for a new
     * owner, after which {@link #updateNewItemLinks(String)} must be called to set the newly generated owner id on the
     * collected new item links.
     */
    void initNewItemLinks();

    /**
     * Create a managed link for the supplied owner and path.
     *
     * @param ownerId The parent item content id, may be null or empty, in which case a new owner item is assumed.
     *                Note that {@link #initNewItemLinks()} should have been called once per owner item save prior to calling this method for new owner items.
     * @param path    The path to the dependent item, if null or empty, no managed link is created.
     * @param linkId  The current link Id, may be null or empty. If supplied, if no matching managed link is located,
     *                the item is re-managed and a new link id is returned.
     * @return The new link Id, or null if no dependent item could be located.
     */
    String manageItemPath(String ownerId, String path, String linkId);

    /**
     * Get the updated path to the dependent item identified by the supplied link id based on the supplied link context.
     *
     * @param linkContext The link context to use, may be null to use edit path for the link.
     * @param linkId      The link id to use, not null.
     * @return The updated path, not null.
     */
    String renderItemPath(PSRenderLinkContext linkContext, String linkId);

    /**
     * Get the updated path to the dependent item identified by the supplied link id based on the supplied link context.
     *
     * @param linkContext The link context to use, may be null to use edit path for the link.
     * @param linkId      The link id to use, not null.
     * @param isStaging   if true then staging filter is used otherwise public filter is used to filter the links
     * @return The updated path, not null.
     */
    String renderItemPath(PSRenderLinkContext linkContext, String linkId, Boolean isStaging);

    /**
     * Checks a server property called {@link #SERVER_PROPERTY_AUTO_MANAGE_LOCAL_PATHS} is available with a value of true.
     *
     * @return true or false based on the property.
     */
    boolean doManageAll();

    /**
     * Update the src attribute of the supplied image link element with the correct rendering based on the supplied context.
     * If the link id of the element is not found, or doesn't resolve to a dependent asset, the src resolves to "#".
     *
     * @param linkContext The context to use, may be null.
     * @param link        The element to update, assumed not null.
     * @return true if the link was successfully rendered for the given context, false if not.
     */
    boolean renderImageLink(PSRenderLinkContext linkContext, Element link);

    /**
     * Update the href attribute of the supplied link element with the correct rendering based on the supplied context.
     * If the link id of the element is not found, or doesn't resolve to a dependent page or asset, the href resolves to "#".
     *
     * @param linkContext The context to use, may be null.
     * @param link        The element to update, assumed not null.
     * @return true if the link was successfully rendered for the given context, false if not.
     */
    boolean renderLink(PSRenderLinkContext linkContext, Element link);

    /**
     * Returns a list of PSManagedLink objects that contain links to the given child.
     *
     * @param contentId the child content id
     * @return list of PSManagedLink objects
     */
    List<PSManagedLink> findLinksByChildId(int contentId);

    /**
     * Given an anchor element returns the link id if it is a managed link.
     *
     * @param link the anchor element
     * @return A long indicating the link id, -1 if managed but no link is present, 0 if not managed.
     */
    long getLinkId(Element link);
}
