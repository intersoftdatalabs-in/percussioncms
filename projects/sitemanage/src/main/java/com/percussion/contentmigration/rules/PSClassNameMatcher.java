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
package com.percussion.contentmigration.rules;

import static com.percussion.contentmigration.rules.PSBaseMatchingMigrationRule.filterClassName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;

/**
 * Handles matching an element based on a set of class names, walking up the parent element chain.  Each time
 * a method is called that walks up the parent chain, the current element is set to the last parent
 * referenced (see {@link #getCurrentElement()}).
 * 
 * @author JaySeletz
 */
public class PSClassNameMatcher {
    private final Element element;
    private Element cur;
    private final List<Element> parents = new ArrayList<>();

    public PSClassNameMatcher(Element element) {
        Validate.notNull(element);
        this.element = element;
        cur = element;
        parents.addAll(cur.parents());
    }

    public Set<String> getCurrentElementClasses() {
        return getFilteredClassNames(cur);
    }

    public Element getCurrentElement() {
        return cur;
    }

    public Set<String> getNextParentElementClasses() {
        Set<String> classNames = new HashSet<>();
        while (classNames.isEmpty() && !parents.isEmpty()) {
            cur = parents.remove(0);
            classNames = getFilteredClassNames(cur);
        }
        return classNames;
    }

    private Set<String> getFilteredClassNames(Element elem) {
        var classNames = elem.classNames();
        filterClassName(classNames);
        return classNames;
    }

    public boolean hasMoreParents() {
        return !parents.isEmpty();
    }

    public Element getSrcElement() {
        return element;
    }

    public boolean matchParentClasses(Set<String> classNames) {
        Validate.notNull(classNames);
        if (parents.isEmpty()) {
            return false;
        }
        cur = parents.remove(0);
        return cur.classNames().stream().anyMatch(classNames::contains);
    }
}
