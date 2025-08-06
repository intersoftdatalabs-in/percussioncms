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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.utils;

import com.percussion.sitemanage.importer.IPSConnectivity;
import java.io.IOException;
import org.apache.commons.lang.Validate;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

/**
 * Retrieves HTML documents using a provided IPSConnectivity.
 */
public class PSHtmlRetriever {

    private static final String UNHANDLED_CONTENT_TYPE = "Unhandled content type";
    private final IPSConnectivity conn;

    public PSHtmlRetriever(IPSConnectivity conn) {
        Validate.notNull(conn);
        this.conn = conn;
    }

    public Document getHtmlDocument() throws IOException {
        Document doc = null;
        try {
            doc = conn.get();
        } catch (IOException e) {
            if (!isUnhandledContentTypeException(e)) {
                throw e;
            }
        }
        return doc;
    }

    /**
     * Determines if the supplied exception indicates a JSoup unhandled content type.
     */
    private boolean isUnhandledContentTypeException(IOException e) {
        return e.getMessage() != null && e.getMessage().contains(UNHANDLED_CONTENT_TYPE);
    }
}
