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
package com.percussion.pagemanagement.data;

/**
 * Abstract base for region node tree visitors.
 * Provides default start/end region node visitor implementations.
 * Subclasses should implement visitStart/visitEnd for region and region code.
 */
public abstract class PSAbstractRegionNodeTreeVisitor implements IPSRegionNodeTreeVisitor {

    private final IPSRegionNodeVisitor startRegionNodeVisitor = new IPSRegionNodeVisitor() {
        @Override
        public void visit(PSRegionCode regionCode) {
            visitStart(regionCode);
        }

        @Override
        public void visit(PSRegion region) {
            visitStart(region);
        }
    };

    private final IPSRegionNodeVisitor endRegionNodeVisitor = new IPSRegionNodeVisitor() {
        @Override
        public void visit(PSRegionCode regionCode) {
            visitEnd(regionCode);
        }

        @Override
        public void visit(PSRegion region) {
            visitEnd(region);
        }
    };

    @Override
    public IPSRegionNodeVisitor getStartRegionNodeVisitor() {
        return startRegionNodeVisitor;
    }

    @Override
    public IPSRegionNodeVisitor getEndRegionNodeVisitor() {
        return endRegionNodeVisitor;
    }

    /**
     * Called when ending a region code node.
     * @param regionCode the region code node, never {@code null}
     */
    protected abstract void visitEnd(PSRegionCode regionCode);

    /**
     * Called when ending a region node.
     * @param region the region node, never {@code null}
     */
    protected abstract void visitEnd(PSRegion region);

    /**
     * Called when starting a region code node.
     * @param regionCode the region code node, never {@code null}
     */
    protected abstract void visitStart(PSRegionCode regionCode);

    /**
     * Called when starting a region node.
     * @param region the region node, never {@code null}
     */
    protected abstract void visitStart(PSRegion region);
}
