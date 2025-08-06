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
package com.percussion.pagemanagement.data;

/**
 * Visitor for walking a {@link PSRegionNode} tree.
 * The {@link #getStartRegionNodeVisitor()} visits regions when they are entered.
 * The {@link #getEndRegionNodeVisitor()} visits regions when they are exited.
 *
 * @see IPSRegionNodeVisitor
 * @see PSAbstractRegionNodeTreeVisitor
 * @author adamgent
 */
public interface IPSRegionNodeTreeVisitor {

    /**
     * Gets the visitor for entering a region node.
     * @return the visitor, never {@code null}
     */
    IPSRegionNodeVisitor getStartRegionNodeVisitor();

    /**
     * Gets the visitor for exiting a region node.
     * @return the visitor, never {@code null}
     */
    IPSRegionNodeVisitor getEndRegionNodeVisitor();
}
