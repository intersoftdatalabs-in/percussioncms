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
package com.percussion.pagemanagement.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.parser.IPSRegionParser.IPSRegionParserRegionFactory;

/**
 * An Abstract Syntax Tree of Regions parsed from an unexpanded HTML template.
 * Groups top-level {@link PSAbstractRegion} objects by id and keeps track of all child regions.
 *
 * @param <REGION> Region type.
 * @param <CODE> Code type.
 * @author adamgent, Sunny Sal
 */
public class PSParsedRegionTree<REGION extends PSAbstractRegion, CODE extends PSRegionCode> {

    private static final String ROOT_NODE_ID = "percRoot";
    private final REGION rootNode;
    private final Map<String, REGION> regions = new HashMap<>();

    /**
     * Constructs a parsed region tree with the given region factory.
     * @param regionFactory the factory, never null.
     */
    public PSParsedRegionTree(IPSRegionParserRegionFactory<REGION, CODE> regionFactory) {
        this.rootNode = regionFactory.createRootRegion();
        this.rootNode.setRegionId(ROOT_NODE_ID);
    }

    /**
     * Gets the root node of the region tree.
     * @return never null.
     */
    public REGION getRootNode() {
        return rootNode;
    }

    /**
     * Gets the region id to region map.
     * @return never null, unmodifiable.
     */
    public Map<String, REGION> getRegions() {
        return Collections.unmodifiableMap(regions);
    }

    /**
     * Internal use: gets the mutable region map.
     * @return the mutable map.
     */
    Map<String, REGION> getMutableRegions() {
        return regions;
    }
}
