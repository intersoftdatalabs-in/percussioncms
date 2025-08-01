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
package com.percussion.pagemanagement.assembler;

import com.percussion.pagemanagement.assembler.PSMergedRegion.PSMergedRegionOwner;
import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSRegionBranches;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionTreeUtils;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static com.percussion.pagemanagement.assembler.PSMergedRegion.PSMergedRegionOwner.PAGE;
import static com.percussion.pagemanagement.assembler.PSMergedRegion.PSMergedRegionOwner.TEMPLATE;
import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getChildRegions;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Represents the merging of {@link PSRegionTree}
 * with {@link PSRegionBranches} as one unified tree.
 * <p>
 * The root nodes of the page region branches will replace the
 * matching nodes of the tree branches if
 * {@link #chooseTemplateOrPageRegion(PSAbstractRegion, PSAbstractRegion, PSMergedRegion)} ==
 * {@link PSMergedRegionOwner#PAGE}.
 * <p>
 * Implementations may also choose whether or not to expand subregions through
 * {@link #isLeaf(PSMergedRegion)}.
 *
 * @author adamgent
 */
public abstract class PSAbstractMergedRegionTree {

    private Map<String, PSAbstractRegion> pageRegionMap;
    /**
     * Widget items overlay: template widgets with the page widgets overlayed.
     */
    private final Map<String, List<PSWidgetItem>> mergedWidgetItems = new HashMap<>();

    /**
     * The widgets associated to a page.
     * Will never be {@code null} after {@link #merge(PSRegionTree, PSRegionBranches)}
     * is called. May be empty.
     */
    protected Map<String, List<PSWidgetItem>> pageWidgetItems;

    /**
     * The widgets associated to a template.
     * Will never be {@code null} after {@link #merge(PSRegionTree, PSRegionBranches)}
     * is called. May be empty.
     */
    protected Map<String, List<PSWidgetItem>> templateWidgetItems;

    /**
     * Never {@code null} after merge.
     */
    private PSMergedRegion rootNode;
    /**
     * Never {@code null} after merge.
     */
    private PSRegionTree templateRegionTree;

    private final Map<String, PSMergedRegion> mergedRegionMap = new HashMap<>();
    /**
     * Regions that can be overridden by the page.
     */
    private final List<PSMergedRegion> overriddenRegions = new ArrayList<>();

    /**
     * Creates the merged region tree for the given tree and branches.
     * <em>This should only be run once as reuse of the object through this method is undefined.
     * It is recommended a new object should be used for different inputs.</em>
     * @param templateRegionTree never {@code null}.
     * @param pageRegionBranches never {@code null}.
     */
    public void merge(PSRegionTree templateRegionTree, PSRegionBranches pageRegionBranches) throws PSDataServiceException {
        notNull(templateRegionTree);
        notNull(pageRegionBranches);
        this.templateRegionTree = templateRegionTree;
        this.templateWidgetItems = this.templateRegionTree.getRegionWidgetsMap();
        this.pageWidgetItems = pageRegionBranches.getRegionWidgetsMap();

        createPageRegionMap(pageRegionBranches.getRegions());
        rootNode = mergeTree(this.templateRegionTree.getRootRegion(), null);
    }

    private void createPageRegionMap(Collection<? extends PSAbstractRegion> pageRegionBranches) {
        pageRegionMap = new HashMap<>();
        if (pageRegionBranches == null) {
            return;
        }
        for (var pr : pageRegionBranches) {
            pageRegionMap.put(pr.getRegionId(), pr);
        }
    }

    /**
     * Recursive method.
     * @param regionNode not {@code null}
     * @param parent may be {@code null}
     * @return not {@code null}
     */
    private PSMergedRegion mergeTree(PSAbstractRegion regionNode, PSMergedRegion parent) throws PSDataServiceException {
        notNull(regionNode);
        notNull(pageRegionMap);
        if (parent != null) {
            notNull(parent.getOwner());
        }
        notEmpty(regionNode.getRegionId());
        // TODO: validate region hasn't been merged yet.
        PSMergedRegionOwner owner = parent == null ? TEMPLATE : parent.getOwner();
        var pageRegionOverride = pageRegionMap.get(regionNode.getRegionId());
        PSAbstractRegion overriddenRegion = null;
        boolean pageIsTryingToOverride = pageRegionOverride != null && owner == TEMPLATE;

        if (pageIsTryingToOverride) {
            // Determine whether to use the page or template
            owner = chooseTemplateOrPageRegion(regionNode, pageRegionOverride, parent);
            if (owner == PAGE) {
                overriddenRegion = regionNode;
                regionNode = pageRegionOverride;
            } else if (owner == TEMPLATE) {
                overriddenRegion = pageRegionOverride;
            } else {
                throw new UnsupportedOperationException("Do not support: " + owner);
            }
        }

        var rvalue = createNode(regionNode, owner, overriddenRegion, parent);
        getMergedRegionMap().put(rvalue.getRegionId(), rvalue);

        if (owner == PAGE && rvalue.getOverriddenRegion() != null) {
            overriddenRegions.add(rvalue);
        }

        if (isLeaf(rvalue)) {
            if (log.isDebugEnabled()) {
                log.debug("Region is leaf: {}", rvalue);
            }
        } else {
            // Recurse on the subregions.
            var mergedSubRegions = getChildRegions(rvalue.getOriginalRegion()).stream()
                    .map(subRegion -> {
                        try {
                            return mergeTree(subRegion, rvalue);
                        } catch (PSDataServiceException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
            rvalue.setSubRegions(mergedSubRegions);
        }

        notNull(rvalue);
        return rvalue;
    }

    /**
     * Chooses between the template or page region for merging.
     * @param template never {@code null}.
     * @param page never {@code null}.
     * @param parent may be {@code null}.
     * @return never {@code null}.
     */
    protected abstract PSMergedRegionOwner chooseTemplateOrPageRegion(PSAbstractRegion template, PSAbstractRegion page, PSMergedRegion parent);

    protected boolean hasRegionChildren(PSAbstractRegion template) {
        return !PSRegionTreeUtils.getChildRegions(template).isEmpty();
    }

    protected boolean hasTemplateWidgets(PSAbstractRegion template) {
        return this.templateWidgetItems.get(template.getRegionId()) != null;
    }

    /**
     * Indicates whether or not merged region should have children.
     * The {@link PSMergedRegion#getOriginalRegion() original region}
     * may actually have subregions but implementations may choose to indicate not
     * to expand this region.
     * @param mr never {@code null}.
     * @return never {@code null}. If {@code true} will stop recursively merging subregions.
     */
    protected abstract boolean isLeaf(PSMergedRegion mr);

    /**
     * Gets the widget items for the merged region.
     * @param mr never {@code null}.
     * @return may be {@code null}.
     */
    protected abstract List<PSWidgetItem> getMergedWidgetItemsForRegion(PSMergedRegion mr);

    /**
     * Creates the node by loading the widget instances if needed.
     * @param region never {@code null}.
     * @param owner never {@code null}.
     * @param overriddenRegion may be {@code null}.
     * @param parent may be {@code null}.
     * @return never {@code null}.
     */
    protected PSMergedRegion createNode(PSAbstractRegion region,
                                        PSMergedRegionOwner owner,
                                        PSAbstractRegion overriddenRegion,
                                        PSMergedRegion parent) throws PSDataServiceException {
        var id = region.getRegionId();
        notNull(id);
        var mr = new PSMergedRegion(region);
        mr.setOwner(owner);
        mr.setOverriddenRegion(overriddenRegion);
        var widgetItems = getMergedWidgetItemsForRegion(mr);
        if (widgetItems != null) {
            mr.setWidgetInstances(loadWidgets(widgetItems));
            this.mergedWidgetItems.put(mr.getRegionId(), widgetItems);
        }
        return mr;
    }

    /**
     * Load the widget instances.
     * @param widgetItems never {@code null}.
     * @return never {@code null}.
     */
    protected abstract List<PSWidgetInstance> loadWidgets(List<PSWidgetItem> widgetItems) throws PSDataServiceException;

    /**
     * Retrieves the root merged node.
     * @return may be {@code null} if {@link #merge(PSRegionTree, PSRegionBranches)} has not been called.
     */
    public PSMergedRegion getRootNode() {
        return rootNode;
    }

    public Map<String, PSMergedRegion> getMergedRegionMap() {
        return mergedRegionMap;
    }

    public List<PSMergedRegion> getOverriddenRegions() {
        return overriddenRegions;
    }

    /**
     * Retrieves all merged regions that have widgets in them.
     * @return never {@code null}.
     */
    public Collection<PSMergedRegion> getWidgetRegions() {
        var regions = new ArrayList<PSMergedRegion>();
        for (var id : mergedWidgetItems.keySet()) {
            var r = mergedRegionMap.get(id);
            if (r != null) {
                regions.add(r);
            } else {
                log.error(format("Widgets associated to non-existent region. Widgets: {0}, RegionId: {1}", mergedWidgetItems.get(id), id));
            }
        }
        return regions;
    }

    /**
     * The log instance to use for this class, never {@code null}.
     */
    protected final Logger log = LogManager.getLogger(getClass());
}
