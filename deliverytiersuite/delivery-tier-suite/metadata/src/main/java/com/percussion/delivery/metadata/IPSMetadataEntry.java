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

package com.percussion.delivery.metadata;

import java.util.Set;

/**
 * Represents a metadata entry for a page.
 */
public interface IPSMetadataEntry {

    String getName();
    void setName(String name);

    String getFolder();
    void setFolder(String folder);

    String getPagepath();
    void setPagepath(String path);

    String getLinktext();
    void setLinktext(String linktext);

    String getType();
    void setType(String type);

    String getSite();
    void setSite(String site);

    /**
     * Returns a cloned set of properties. Changing these directly will not affect the entry.
     * To update property values, use {@link #setProperties(Set)}.
     */
    Set<IPSMetadataProperty> getProperties();
    void setProperties(Set<IPSMetadataProperty> properties);

    void addProperty(IPSMetadataProperty prop);
    void clearProperties();
}
