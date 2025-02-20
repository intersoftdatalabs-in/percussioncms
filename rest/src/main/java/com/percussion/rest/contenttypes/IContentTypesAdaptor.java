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

package com.percussion.rest.contenttypes;

import java.net.URI;
import java.util.List;

/***
 * Defines the interface that backend API implementations must implement
 * for ContentTypes.
 */
public interface IContentTypesAdaptor {

    /***
     * List all content types available to the System
     * @param baseUri Requesting URI
     * @return A list of all available Content Types
     */
    public List<ContentType> listContentTypes(URI baseUri);

    /***
     * List ContentTypes available for the specified Site
     * @param baseUri Originating URI
     * @param siteId Site Id for Site to filter Types by
     * @return An array of ContentTypes
     */
    public List<ContentType> listContentTypes(URI baseUri, int siteId);

}
