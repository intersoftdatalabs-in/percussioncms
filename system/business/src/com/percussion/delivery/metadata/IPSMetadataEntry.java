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

package com.percussion.delivery.metadata;

import java.util.Set;

import org.json.JSONException;

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
     * Returns a cloned set of properties. Changing the returned set does not affect the entry.
     * To change property values, use {@link #setProperties(Set)}.
     */
    Set<IPSMetadataProperty> getProperties();

    void setProperties(Set<IPSMetadataProperty> properties);

    void addProperty(IPSMetadataProperty prop);

    void clearProperties();

    String getJson() throws JSONException;
}
