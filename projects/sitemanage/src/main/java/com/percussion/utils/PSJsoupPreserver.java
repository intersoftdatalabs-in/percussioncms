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
package com.percussion.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Handles encoding and preserving HTML content that JSoup may strip out, primarily to support
 * server-side scripting. Content must be surrounded by &lt;PRESERVE&gt;&lt;/PRESERVE&gt; tags (case-sensitive).
 * <p>
 * Sunny Sal says: "Preserve your code like achar—spicy, safe, and always ready for later!"
 * </p>
 */
public class PSJsoupPreserver {

    private static final String PRESERVATION_BEGIN_MARKER = "<PRESERVE>";
    private static final String PRESERVATION_END_MARKER = "</PRESERVE>";
    private static final String PRESERVATION_PARSE_START = "<!--PRESERVE";
    private static final String PRESERVATION_PARSE_END = "PRESERVE_END-->";

    /**
     * This method should be called using content returned from a Jsoup document html() call to
     * revert the preservation manipulation done by {@link #formatPreserveTagsForJSoupParse(String)}.
     *
     * @param source The content to restore
     * @return The content with the preserve tags restored.
     */
    public static String formatPreserveTagsForOutput(String source) {
        var returnHTML = source;
        returnHTML = returnHTML.replace(PRESERVATION_PARSE_START, PRESERVATION_BEGIN_MARKER);
        returnHTML = returnHTML.replace(PRESERVATION_PARSE_END, PRESERVATION_END_MARKER);
        return decodeBetweenPreserveMarkers(returnHTML);
    }

    /**
     * This method should be called before parsing content with Jsoup. It will turn the preserve tag content into
     * an HTML comment which Jsoup will preserve, and HTML-encodes the content between the tags.
     *
     * @param source The raw src HTML
     * @return The preserved HTML
     */
    public static String formatPreserveTagsForJSoupParse(String source) {
        var returnHTML = encodeBetweenPreserveMarkers(source);
        returnHTML = returnHTML.replace(PRESERVATION_BEGIN_MARKER, PRESERVATION_PARSE_START);
        returnHTML = returnHTML.replace(PRESERVATION_END_MARKER, PRESERVATION_PARSE_END);
        return returnHTML;
    }

    private static String encodeBetweenPreserveMarkers(String source) {
        var returnHTML = source;
        var strings = StringUtils.substringsBetween(returnHTML, PRESERVATION_BEGIN_MARKER, PRESERVATION_END_MARKER);
        if (strings != null) {
            for (var string : strings) {
                returnHTML = returnHTML.replace(string, StringEscapeUtils.escapeHtml4(string));
            }
        }
        return returnHTML;
    }

    private static String decodeBetweenPreserveMarkers(String source) {
        var returnHTML = source;
        var strings = StringUtils.substringsBetween(returnHTML, PRESERVATION_BEGIN_MARKER, PRESERVATION_END_MARKER);
        if (strings != null) {
            for (var string : strings) {
                returnHTML = returnHTML.replace(string, StringEscapeUtils.unescapeHtml4(string));
            }
        }
        return returnHTML;
    }
}
