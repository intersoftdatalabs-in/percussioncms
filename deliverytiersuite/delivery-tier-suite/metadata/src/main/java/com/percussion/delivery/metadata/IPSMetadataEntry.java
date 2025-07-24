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

package com.percussion.delivery.metadata;

import java.util.Set;

public interface IPSMetadataEntry
{

    /**
     * @return the name
     */
    String getName();

    /**
     * @param name the name to set
     */
    void setName(String name);

    /**
     * @return the folder
     */
    String getFolder();

    /**
     * @param folder the folder to set
     */
    void setFolder(String folder);

    /**
     * @return the page path
     */
    String getPagepath();

    /**
     * @param path the pagepath to set
     */
    void setPagepath(String path);

    /**
     * @return the linktext
     */
    String getLinktext();

    /**
     * @param linktext the linktext to set
     */
    void setLinktext(String linktext);

    /**
     * @return the type
     */
    String getType();

    /**
     * @param type the type to set
     */
    void setType(String type);

    /**
     * @return the site
     */
    String getSite();

    /**
     * @param site the site to set
     */
    void setSite(String site);

    /**
     * @return the properties. This returns a cloned set of properties changing
     *         the value of these directly will not affect the property values
     *         in the entry. To change property values on the entry you must
     *         passed the properties back to the entries
     *         {@link #setProperties(Set)} method.
     */
    Set<IPSMetadataProperty> getProperties();

    /**
     * @param properties the properties to set
     */
    void setProperties(Set<IPSMetadataProperty> properties);

    void addProperty(IPSMetadataProperty prop);

    void clearProperties();

}
