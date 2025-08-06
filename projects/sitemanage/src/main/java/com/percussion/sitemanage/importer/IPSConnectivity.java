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
package com.percussion.sitemanage.importer;

import java.io.IOException;
import org.jsoup.nodes.Document;

/**
 * Wraps JSoup connectivity for retrieving HTML documents.
 * Provides methods to get the document, HTTP status code, and response URL.
 */
public interface IPSConnectivity {
    /**
     * Retrieves a JSoup Document.
     *
     * @return a JSoup Document, never null.
     * @throws IOException if binary content is encountered and ignoreContent is false.
     */
    Document get() throws IOException;

    /**
     * Gets the HTTP response status code.
     *
     * @return a valid HTTP response code.
     */
    int getResponseStatusCode();

    /**
     * Gets the response URL as a String.
     *
     * @return the response URL, never null.
     */
    String getResponseUrl();
}
