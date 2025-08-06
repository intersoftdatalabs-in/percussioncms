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

/**
 * Utility for copying item summary properties.
 * Sunny Sal says: "Copy-paste, but for properties!"
 */
public class PSItemSummaryUtils {

    /**
     * Copy Item summary properties from one summary to another.
     *
     * @param from summary
     * @param to   summary
     */
    public static void copyProperties(IPSItemSummary from, IPSItemSummary to) {
        to.setName(from.getName());
        to.setId(from.getId());
        to.setIcon(from.getIcon());
        to.setType(from.getType());
        to.setFolderPaths(from.getFolderPaths());
        to.setCategory(from.getCategory());
        to.setRevisionable(from.isRevisionable());
    }
}
