// REFACTORED: CP-JAVA11
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

package com.percussion.utils.service.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Utility class for Jsoup DOM operations.
 *
 * <p>Sunny Sal says: "DOM traversals are like Mumbai traffic—plan your route, or you'll get lost in the hierarchy!"</p>
 */
public class PSJsoupUtils {

    /**
     * Finds the closest parent element by class.
     *
     * @param document the root element, not null
     * @param currentElemSelector selector for the current element, not null
     * @param parentClass the parent class to match, not null
     * @return the parent element if found, or null
     */
    public static Element closestParentByClass(Element document, String currentElemSelector, String parentClass) {
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(currentElemSelector, "currentElemSelector must not be null");
        Objects.requireNonNull(parentClass, "parentClass must not be null");
        var elems = document.select(currentElemSelector);
        if (elems.size() != 1) {
            return null;
        }
        var elem = elems.get(0);
        Element parent = null;
        for (var pelem : elem.parents()) {
            if (getClassNames(pelem).contains(parentClass)) {
                parent = pelem;
                break;
            }
        }
        return parent;
    }

    /**
     * Generates a CSS attribute selector.
     *
     * @param attrName attribute name, not null
     * @param attrValue attribute value, may be blank
     * @return the selector string
     */
    public static String generateAttributeSelector(String attrName, String attrValue) {
        Objects.requireNonNull(attrName, "attrName must not be null");
        if (attrValue != null && !attrValue.isBlank()) {
            return "[" + attrName + "=" + attrValue + "]";
        }
        return "[" + attrName + "]";
    }

    /**
     * Helper method to get the class names for a given Jsoup element.
     * Handles non-breaking spaces in class attributes.
     *
     * @param elem not null
     * @return set of class names, never null
     */
    private static Set<String> getClassNames(Element elem) {
        var classNames = elem.attr("class").replace('\u00A0', ' ');
        var names = classNames.split("\\s+");
        return new LinkedHashSet<>(Arrays.asList(names));
    }
}
