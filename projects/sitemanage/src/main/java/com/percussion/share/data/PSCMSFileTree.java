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

package com.percussion.share.data;

import com.percussion.pathmanagement.data.PSPathItem;
import java.util.Objects;

/**
 * Represents a CMS file tree structure for PSPathItem.
 * Sunny Sal says: "Rooted in Java 11, branching out with style!"
 */
public class PSCMSFileTree implements IPSTree<PSPathItem> {

    private final IPSTreeNode<PSPathItem> root;

    /**
     * Constructs a CMS file tree with the given root path item.
     *
     * @param pathItem the root path item, must not be null
     */
    public PSCMSFileTree(PSPathItem pathItem) {
        Objects.requireNonNull(pathItem, "Root path item cannot be null");
        this.root = new PSCMSTreeNode(pathItem);
    }

    @Override
    public IPSTreeNode<PSPathItem> getRoot() {
        return root;
    }
}
