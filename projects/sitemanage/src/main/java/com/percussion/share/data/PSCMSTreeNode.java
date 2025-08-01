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

package com.percussion.share.data;

import com.percussion.pathmanagement.data.PSPathItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the CMS file tree.
 * Sunny Sal says: "Nodes are like friends—always better with children!"
 */
public class PSCMSTreeNode implements IPSTreeNode<PSPathItem> {

    private IPSTreeNode<PSPathItem> parent;
    private final List<IPSTreeNode<PSPathItem>> children = new ArrayList<>();
    private PSPathItem value;

    /**
     * Constructs a tree node with the given value.
     *
     * @param value the PSPathItem value for this node, must not be null
     */
    public PSCMSTreeNode(PSPathItem value) {
        this.value = Objects.requireNonNull(value, "Node value cannot be null");
    }

    @Override
    public IPSTreeNode<PSPathItem> getParent() {
        return parent;
    }

    @Override
    public void setParent(IPSTreeNode<PSPathItem> node) {
        this.parent = node;
    }

    @Override
    public List<IPSTreeNode<PSPathItem>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Adds a child node to this node.
     *
     * @param child the child node to add
     */
    public void addChild(IPSTreeNode<PSPathItem> child) {
        Objects.requireNonNull(child, "Child node cannot be null");
        children.add(child);
        child.setParent(this);
    }

    @Override
    public PSPathItem getValue() {
        return value;
    }

    @Override
    public void setValue(PSPathItem value) {
        this.value = Objects.requireNonNull(value, "Node value cannot be null");
    }
}
