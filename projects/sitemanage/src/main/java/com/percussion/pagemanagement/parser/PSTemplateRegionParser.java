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

import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.data.PSRegionTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Region parser for templates.
 * @author adamgent, Sunny Sal
 */
public class PSTemplateRegionParser extends PSRegionParserAdapter<PSRegion, PSRegionCode> {

    private final Map<String, PSRegion> regions;

    public PSTemplateRegionParser(Map<String, PSRegion> regions) {
        this.regions = regions == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(regions));
    }

    @Override
    public PSRegion createRegion(String regionId) {
        var region = regions.get(regionId);
        if (region != null) {
            region.setChildren(new ArrayList<>());
        } else {
            region = new PSRegion();
        }
        region.setRegionId(regionId);
        return region;
    }

    @Override
    public PSRegionCode createRegionCode() {
        return new PSRegionCode();
    }

    @Override
    public PSRegion createRootRegion() {
        return new PSRegion();
    }

    /**
     * Static utility for parsing HTML with region map.
     * @param regions region map, may be null.
     * @param html HTML string, never null.
     * @return parsed region tree.
     */
    public static PSParsedRegionTree<PSRegion, PSRegionCode> parse(Map<String, PSRegion> regions, String html) {
        var parser = new PSTemplateRegionParser(regions);
        return parser.parse(html);
    }
}
