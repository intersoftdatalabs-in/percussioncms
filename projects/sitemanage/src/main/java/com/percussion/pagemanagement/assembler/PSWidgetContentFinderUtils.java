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

package com.percussion.pagemanagement.assembler;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.assembler.impl.finder.PSRelationshipWidgetContentFinder;
import com.percussion.pagemanagement.assembler.impl.finder.PSRelationshipWidgetContentFinder.WidgetCriteria;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.service.IPSWidgetService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Helper class used for retrieving page/template asset relationships.
 * It uses the same API to retrieve and sort relationships as the content finder does.
 * The content finder is used during assemble page/template.
 *
 * @author YuBingChen
 */
public class PSWidgetContentFinderUtils {

    /**
     * Retrieves associated relationships where the specified page/template is the owner
     * and the dependents are local and shared assets.
     *
     * @param id the ID of the specified page or template, not {@code null} or empty.
     * @return the list of relationships, sorted by "sort-rank" property, never {@code null}, but may be empty.
     */
    public static List<PSRelationship> getLocalSharedAssetRelationships(String id) {
        return getAssetRelationships(id, null);
    }

    /**
     * This is the same as {@link #getLocalSharedAssetRelationships(String)},
     * except the dependents are shared assets only.
     *
     * @param id the ID of the specified page or template, not {@code null} or empty.
     * @return the list of relationships, sorted by "sort-rank" property, never {@code null}, but may be empty.
     */
    public static List<PSRelationship> getSharedAssetRelationships(String id) {
        return getAssetRelationships(id, PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);
    }

    /**
     * Except the dependents are local assets only.
     *
     * @param id the ID of the specified page or template, not {@code null} or empty.
     * @return the list of relationships, sorted by "sort-rank" property, never {@code null}, but may be empty.
     */
    public static List<PSRelationship> getLocalAssetRelationships(String id) {
        return getAssetRelationships(id, PSRelationshipConfig.TYPE_LOCAL_CONTENT);
    }

    /**
     * Retrieves associated relationships where the specified page/template is the owner
     * and the dependents are local and shared assets. In addition, the relationship category
     * is active-assembly.
     *
     * @param id the ID of the specified page or template, not {@code null}.
     * @param relationshipName the name of the returned relationship. It may be {@code null}
     *                         if the dependent can be either shared & local assets.
     * @return the list of relationships, sorted by "sort-rank" property, never {@code null}, but may be empty.
     */
    private static List<PSRelationship> getAssetRelationships(String id, String relationshipName) {
        notEmpty(id, "id may not be empty");
        var guid = getIdMapper().getItemGuid(id);

        var rels = getFinder().findRelationshipByOwner(guid);
        if (relationshipName == null) {
            return rels;
        }

        var sortSet = new HashSet<PSRelationship>();
        for (var r : rels) {
            var name = r.getConfig().getName();
            if (name.equals(relationshipName) && (r.getOwner().getId() != guid.getUUID())) {
                sortSet.add(r);
            }
        }
        return new ArrayList<>(sortSet);
    }

    /**
     * Gets a list of relationships (from the given source relationships) that matches one of the supplied widgets.
     * The matched relationships are the ones that will be used during (page) rendering.
     *
     * @param srcRels the source relationships, never {@code null}.
     * @param widgets the widget instances (that may be used on a page/template to render the source relationships/assets). Not {@code null}.
     * @return the matching relationships, never {@code null}, but may be empty.
     */
    public static Collection<PSRelationship> getMatchRelationships(Collection<PSRelationship> srcRels, Collection<PSWidgetItem> widgets) {
        var result = new ArrayList<PSRelationship>();
        for (var w : widgets) {
            var r = getMatchRelationship(srcRels, w);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Gets the relationship (from the source relationships) that matches the given widget.
     *
     * @param srcRels the source relationships, assumed not {@code null}.
     * @param widget the widget instance, assumed not {@code null}.
     * @return the matching relationship. It may be {@code null} if cannot find one.
     */
    public static PSRelationship getMatchRelationship(Collection<PSRelationship> srcRels, PSWidgetItem widget) {
        try {
            var wi = new PSWidgetInstance();
            wi.setItem(widget);
            var widgetDefId = widget.getDefinitionId();
            var widgetDef = getWidgetService().load(widgetDefId);
            wi.setDefinition(widgetDef);
            var criteria = new WidgetCriteria(wi);

            var rels = new TreeSet<PSRelationship>(new RelationshipOrder(criteria));
            for (var r : srcRels) {
                if (getFinder().isMatchRelationship(r, criteria, null)) {
                    if (StringUtils.isNotBlank(widget.getId())
                            && StringUtils.isNotBlank(r.getProperty("sys_slotid"))
                            && !r.getProperty("sys_slotid").equals(widget.getId())) {
                        var relationships = new PSRelationshipSet();
                        r.setProperty("sys_slotid", widget.getId());
                        relationships.add(r);
                        try {
                            PSRelationshipProcessor.getInstance().save(relationships);
                        } catch (PSCmsException e) {
                            log.error("Error saving relationship when matching content relationships.", e);
                        }
                    }
                    rels.add(r);
                }
            }
            if (rels.isEmpty()) {
                return null;
            }
            return rels.first();
        } catch (PSDataServiceException | PSNotFoundException e) {
            log.error("Error getting Widget Definition for: {} Error: {}", widget.getDefinitionId(), e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return null;
    }

    private static IPSIdMapper getIdMapper() {
        if (idMapper == null) {
            idMapper = (IPSIdMapper) getWebApplicationContext().getBean("sys_idMapper");
        }
        return idMapper;
    }

    private static IPSIdMapper idMapper = null;

    private static IPSWidgetService getWidgetService() {
        if (widgetService == null) {
            widgetService = (IPSWidgetService) getWebApplicationContext().getBean("widgetService");
        }
        return widgetService;
    }

    private static IPSWidgetService widgetService = null;

    /**
     * Comparator to order widget/slot relationships.
     * Note, this comparator must be compatible or behave the same as the
     * comparator defined in {@link PSRelationshipWidgetContentFinder}.
     */
    private static class RelationshipOrder implements Comparator<PSRelationship> {
        private final WidgetCriteria criteria;

        public RelationshipOrder(WidgetCriteria widget) {
            this.criteria = widget;
        }

        /**
         * Compare widget/slot items for ordering.
         *
         * @param r1 page/asset relationship one, never {@code null}
         * @param r2 page/asset relationship two, never {@code null}
         * @return positive number for increasing order, negative for decreasing
         *         order, zero for no change
         */
        @Override
        public int compare(PSRelationship r1, PSRelationship r2) {
            notNull(r1);
            notNull(r2);

            if (isBlank(criteria.getWidgetName())) {
                return compareUnnamed(r1, r2);
            }

            var wname1 = r1.getProperty(PSRelationshipConfig.PDU_WIDGET_NAME);
            var wname2 = r2.getProperty(PSRelationshipConfig.PDU_WIDGET_NAME);
            if (isBlank(wname1) && isNotBlank(wname2)) {
                return 1;
            }
            if (isNotBlank(wname1) && isBlank(wname2)) {
                return -1;
            }
            return compareUnnamed(r1, r2);
        }

        /**
         * Compare items as unnamed items.
         * @param r1 widget/slot item one, never {@code null}
         * @param r2 widget/slot item two, never {@code null}
         * @return positive number for increasing order, negative for decreasing
         *         order, zero for no change
         */
        private int compareUnnamed(PSRelationship r1, PSRelationship r2) {
            var sortRank1 = getSortRank(r1);
            var sortRank2 = getSortRank(r2);

            if (sortRank1 != sortRank2) {
                return sortRank1 - sortRank2;
            }

            // If this comparator returns zero, a set based on this comparator
            // will treat the two slot items as equal (and only store one of them).
            // Therefore, if by some chance the sort ranks are the same, compare
            // the items using their relationship ids (if set) or their item ids.
            var id1 = r1.getGuid();
            var id2 = r2.getGuid();
            return Long.compare(id1.longValue(), id2.longValue());
        }

        private int getSortRank(PSRelationship rel) {
            var sort = rel.getProperty(PSRelationshipConfig.PDU_SORTRANK);
            if (isBlank(sort)) {
                return 0;
            }
            try {
                return Integer.parseInt(sort);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
    }

    private static PSRelationshipWidgetContentFinder getFinder() {
        if (ms_finder == null) {
            ms_finder = (PSRelationshipWidgetContentFinder) PSPageUtils.getWidgetContentFinder(null);
        }
        return ms_finder;
    }

    private static PSRelationshipWidgetContentFinder ms_finder = null;

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(PSWidgetContentFinderUtils.class);
}
