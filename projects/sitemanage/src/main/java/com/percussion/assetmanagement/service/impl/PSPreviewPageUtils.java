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
// REFACTORED: CP-JAVA11

package com.percussion.assetmanagement.service.impl;

import static com.percussion.pagemanagement.assembler.PSWidgetContentFinderUtils.getLocalSharedAssetRelationships;
import static com.percussion.pagemanagement.assembler.PSWidgetContentFinderUtils.getMatchRelationship;
import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getEmptyWidgetRegions;
import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.assetmanagement.data.PSOrphanedAssetSummary;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.share.service.IPSIdMapper;

import java.util.*;

/**
 * Utility class for previewing page/template asset relationships.
 */
public class PSPreviewPageUtils {

    /**
     * Gets template widgets not occupied with template assets.
     *
     * @param template the template, must not be {@code null}.
     * @return set of empty template widgets, never {@code null}.
     */
    public static Set<PSWidgetItem> getEmptyTemplateWidgets(PSTemplate template) {
        notNull(template);
        var emptyTemplateWidgets = new HashSet<PSWidgetItem>();
        var templateAssets = getLocalSharedAssetRelationships(template.getId());
        var mapSlotIdToRelationship = getSlotIdToRelationshipMap(templateAssets);
        for (var templateWidget : template.getWidgets()) {
            if (!mapSlotIdToRelationship.containsKey(templateWidget.getId())) {
                emptyTemplateWidgets.add(templateWidget);
            }
        }
        return emptyTemplateWidgets;
    }

    /**
     * Gets page widgets placed in a page region, and that region is valid in the template.
     * If the template has widgets in a given region, that region is not included.
     *
     * @param page the page, must not be {@code null}.
     * @param template the template, must not be {@code null}.
     * @return set of page widgets, never {@code null}.
     */
    public static Set<PSWidgetItem> getPageWidgets(PSPage page, PSTemplate template) {
        notNull(page);
        notNull(template);
        var pageWidgets = new HashSet<PSWidgetItem>();
        if (template.getRegionTree() != null) {
            var emptyLeafRegions = getEmptyWidgetRegions(template.getRegionTree());
            var pageRegionWidgetsMap = page.getRegionBranches().getRegionWidgetsMap();
            for (var validTemplateRegion : emptyLeafRegions) {
                if (pageRegionWidgetsMap.containsKey(validTemplateRegion.getRegionId())) {
                    pageWidgets.addAll(pageRegionWidgetsMap.get(validTemplateRegion.getRegionId()));
                }
            }
        }
        return pageWidgets;
    }

    /**
     * Retrieves relationships that are unused assets for the given page.
     * Only retrieves:
     * - page assets that do not have matching widgets in the page's template
     * - page assets that have been overwritten by template assets
     *
     * @param page the page.
     * @param template the template used for the page, must not be {@code null}.
     * @return collection of unused asset relationships, never {@code null}.
     */
    public static Collection<PSRelationship> getOrphanedPageAssets(PSPage page, PSTemplate template) {
        return getPageAssets(page, template, null);
    }

    /**
     * Retrieves relationships that are used page assets for a given page.
     *
     * @param page the page.
     * @param template the template used for the page, must not be {@code null}.
     * @return map of widget ID to used page asset, never {@code null}.
     */
    public static Map<String, PSRelationship> getUsedPageAssets(PSPage page, PSTemplate template) {
        var widgetToAsset = new HashMap<String, PSRelationship>();
        getPageAssets(page, template, widgetToAsset);
        return widgetToAsset;
    }

    /**
     * Gets the used or orphaned page assets for the supplied page and related template.
     *
     * @param page the page.
     * @param template the template used for the page, must not be {@code null}.
     * @param widgetToAsset map to collect used page assets, may be {@code null}.
     * @return orphaned page assets, or {@code null} if only collecting used assets.
     */
    private static Collection<PSRelationship> getPageAssets(PSPage page, PSTemplate template, Map<String, PSRelationship> widgetToAsset) {
        notNull(page);
        notNull(template);
        var pageAssets = getLocalSharedAssetRelationships(page.getId());
        Collection<PSRelationship> orphanAssets = null;
        if (widgetToAsset == null) {
            orphanAssets = new ArrayList<>(pageAssets);
        }
        var widgets = getPageWidgets(page, template);
        widgets.addAll(getEmptyTemplateWidgets(template));
        for (var widget : widgets) {
            var matchingRelationship = getMatchRelationship(pageAssets, widget);
            if (matchingRelationship != null) {
                if (widgetToAsset != null)
                    widgetToAsset.put(widget.getId(), matchingRelationship);
                else
                    orphanAssets.remove(matchingRelationship);
            }
        }
        return orphanAssets;
    }

    /**
     * Retrieves the list of orphan assets for a given page, as {@link PSOrphanedAssetSummary}.
     *
     * @param page the page.
     * @param template the template used for the page, must not be {@code null}.
     * @return set of unused asset summaries, never {@code null}.
     */
    public static Set<PSOrphanedAssetSummary> getOrphanedAssetsSummaries(PSPage page, PSTemplate template) {
        notNull(page);
        notNull(template);
        var unusedAssets = new HashSet<PSOrphanedAssetSummary>();
        var orphanAssets = getOrphanedPageAssets(page, template);
        for (var relationship : orphanAssets) {
            var slotId = relationship.getProperties().get(PSRelationshipConfig.PDU_SLOTID);
            var dependantId = getIdMapper().getString(relationship.getDependent());
            var widgetName = relationship.getProperties().get(PSRelationshipConfig.PDU_WIDGET_NAME);
            unusedAssets.add(new PSOrphanedAssetSummary(dependantId, slotId, widgetName, relationship.getId()));
        }
        return unusedAssets;
    }

    private static Map<String, PSRelationship> getSlotIdToRelationshipMap(Collection<PSRelationship> templateAssets) {
        var map = new HashMap<String, PSRelationship>();
        for (var relationship : templateAssets) {
            map.put(relationship.getProperty(PSRelationshipConfig.PDU_SLOTID), relationship);
        }
        return map;
    }

    private static IPSIdMapper getIdMapper() {
        if (idMapper == null) {
            idMapper = (IPSIdMapper) getWebApplicationContext().getBean("sys_idMapper");
        }
        return idMapper;
    }

    private static IPSIdMapper idMapper;
}
