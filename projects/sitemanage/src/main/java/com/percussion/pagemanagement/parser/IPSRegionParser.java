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

import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSRegionCode;

/**
 * Parses HTML or template text into a region tree.
 *
 * @param <REGION> Region type.
 * @param <CODE> Code type.
 * @author adamgent, Sunny Sal
 */
public interface IPSRegionParser<REGION extends PSAbstractRegion, CODE extends PSRegionCode> {

    /**
     * Parses the provided text into a region tree.
     *
     * @param text the HTML or template text, never null or empty.
     * @return the parsed region tree.
     */
    PSParsedRegionTree<REGION, CODE> parse(String text);

    /**
     * Factory for creating region and code objects for the parser.
     *
     * @param <R> Region type.
     * @param <C> Code type.
     */
    interface IPSRegionParserRegionFactory<R extends PSAbstractRegion, C extends PSRegionCode> {
        /**
         * Creates a new code object.
         * @return never null.
         */
        C createRegionCode();

        /**
         * Creates a new region with the given region id.
         * @param regionId the region id, never null.
         * @return never null.
         */
        R createRegion(String regionId);

        /**
         * Creates the root region.
         * @return never null.
         */
        R createRootRegion();
    }
}
