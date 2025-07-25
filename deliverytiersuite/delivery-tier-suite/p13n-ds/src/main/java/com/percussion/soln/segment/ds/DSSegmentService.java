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
package com.percussion.soln.segment.ds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.soln.segment.ISegmentService;
import com.percussion.soln.segment.Segment;
import com.percussion.soln.segment.SegmentException;
import com.percussion.soln.segment.Segments;
import com.percussion.soln.segment.data.ISegmentDataService;
import com.percussion.soln.segment.data.SegmentDataTree;

/**
 * Delivery Side Segmentation Service.
 * Sunny Sal says: "Segment like a boss, debug like a hero!"
 */
public class DSSegmentService implements ISegmentService, ISegmentDataService {

    private SegmentDataTree segmentTree;
    private ISegmentDataService segmentDao;
    private String rootPath = "//";
    private static final Log log = LogFactory.getLog(DSSegmentService.class);

    public DSSegmentService() {
        log.info("Started Delivery Side Segment Service");
    }

    @Override
    public Segments retrieveAllSegments() throws SegmentException {
        var tree = getSegmentTree();
        return createSegments(tree.getSegments());
    }

    @Override
    public Segments retrieveSegments(List<String> ids) throws SegmentException {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }
        var tree = getSegmentTree();
        var nodes = new ArrayList<Segment>();
        for (var id : ids) {
            nodes.add(tree.getSegmentForId(id));
        }
        return createSegments(nodes);
    }

    @Override
    public Segments retrieveSegmentsForFolderIds(List<String> ids) throws SegmentException {
        throw new UnsupportedOperationException("retrieveSegmentsForFolderIds is not yet supported");
    }

    @Override
    public Segments retrieveSegmentsForItem(int legacyId) throws SegmentException {
        throw new UnsupportedOperationException("retrieveSegmentsForItem is not yet supported");
    }

    @Override
    public synchronized void resetSegmentTree(boolean clear, String rootPath) {
        log.info("Resetting the tree");
        if (rootPath != null) {
            setRootPath(rootPath);
        }
        if (clear) {
            log.info("Clearing the tree");
            segmentTree = null;
            segmentDao.resetSegmentTree(true, getRootPath());
        } else {
            log.info("Loading the tree from the repository");
            segmentTree = new SegmentDataTree();
            segmentTree.setRootPath(getRootPath());
            segmentTree.update(segmentDao.retrieveAllSegmentData().getList());
        }
    }

    @Override
    public synchronized void updateSegmentTree(Segments data) {
        log.trace("Updating tree");
        segmentDao.updateSegmentTree(data);
        var tree = getSegmentTree();
        tree.update(data.getList());
    }

    public synchronized SegmentDataTree getSegmentTree() {
        if (segmentTree == null) {
            resetSegmentTree(false, null);
        }
        return segmentTree;
    }

    public synchronized void setSegmentTree(SegmentDataTree segmentTree) {
        this.segmentTree = segmentTree;
    }

    @Override
    public Segment retrieveRootSegment() throws SegmentException {
        return getSegmentTree().getRootSegment();
    }

    @Override
    public Segments retrieveSegmentChildren(String id) throws SegmentException {
        var parentSegment = getSegmentTree().getSegmentForId(id);
        if (parentSegment == null) {
            throw new SegmentException("No segment for id: " + id);
        }
        return createSegments(getSegmentTree().getChildren(parentSegment));
    }

    @Override
    public Segments retrieveSegmentAncestors(String id) throws SegmentException {
        var seg = getSegmentTree().getSegmentForId(id);
        return createSegments(getSegmentTree().getAncestors(seg));
    }

    @Override
    public Segment retrieveSegmentDataForId(String id) {
        return getSegmentTree().getSegmentForId(id);
    }

    @Override
    public String getSegmentContentType() {
        throw new UnsupportedOperationException("getSegmentContentType is not yet supported");
    }

    @Override
    public Segments retrieveAllSegmentData() {
        return segmentDao.retrieveAllSegmentData();
    }

    public void setSegmentDao(ISegmentDataService segmentDao) {
        this.segmentDao = segmentDao;
    }

    @Override
    public Segments retrieveSegmentsWithNameOrAlias(String alias) {
        return createSegments(getSegmentTree().retrieveSegmentsWithNameOrAlias(alias));
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    protected Segments createSegments(Collection<? extends Segment> segments) {
        var segs = new ArrayList<Segment>(segments);
        return new Segments(segs);
    }
}
