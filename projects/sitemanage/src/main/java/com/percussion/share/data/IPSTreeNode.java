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

import java.util.List;

/**
 * Defines a tree node for use with {@link IPSTree}.
 *
 * @param <T> the type of value stored in the node
 * @author natechadwick
 */
public interface IPSTreeNode<T> {

    /**
     * Gets the node's parent.
     *
     * @return the parent node, or null if this is the root
     */
    IPSTreeNode<T> getParent();

    /**
     * Sets the node's parent.
     *
     * @param node the parent node to set
     */
    void setParent(IPSTreeNode<T> node);

    /**
     * Gets the children of this node.
     *
     * @return a list of child nodes, never null
     */
    List<IPSTreeNode<T>> getChildren();

    /**
     * Gets the value stored in this node.
     *
     * @return the value
     */
    T getValue();

    /**
     * Sets the value stored in this node.
     *
     * @param value the value to set
     */
    void setValue(T value);
}
