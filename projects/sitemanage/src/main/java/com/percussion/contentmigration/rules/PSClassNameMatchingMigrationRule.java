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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Find a content match based on the class name of an element. If multiple classes are present then
 * tries to find elements based on all classes first. If multiple matches are found, attempts to
 * find unique match by comparing class names on parent elements, walking up the parent chain until
 * a unique match is found. If no unique match is found (no match or multiple elements that match),
 * then <code>null</code> is returned.
 *
 * @author JaySeletz
 */
public class PSClassNameMatchingMigrationRule extends PSBaseMatchingMigrationRule {
  @Override
  protected String matchOnRule(String widgetId, Document sourceDoc, Document targetDoc) {
    var regionElem = findEnclosingRegionElement(widgetId, sourceDoc);
    if (regionElem == null) {
      return null;
    }
    var srcMatch = new PSClassNameMatcher(regionElem);
    var classNames = srcMatch.getCurrentElementClasses();
    if (classNames.isEmpty()) {
      return null;
    }
    var elems = findMatches(targetDoc, classNames);
    if (elems.isEmpty()) {
      return null;
    }
    if (elems.size() == 1) {
      return elems.get(0).html();
    }
    if (!srcMatch.hasMoreParents()) {
      return null;
    }
    var matches = new ArrayList<PSClassNameMatcher>();
    for (var elem : elems) {
      matches.add(new PSClassNameMatcher(elem));
    }
    var match = findParentMatch(srcMatch, matches);
    return match == null ? null : match.html();
  }

  /**
   * Find a match by class name for parent elements of the supplied elements
   *
   * @param srcMatch The element whose parents to match
   * @param targetElems The elements whose parents to check
   * @return The match if found, otherwise <code>null</code>.
   */
  private Element findParentMatch(PSClassNameMatcher srcMatch, List<PSClassNameMatcher> matches) {
    var classNames = srcMatch.getNextParentElementClasses();
    if (classNames.isEmpty()) {
      return null;
    }
    filterParentMatches(matches, classNames);
    if (matches.isEmpty()) {
      return null;
    }
    if (matches.size() == 1) {
      return matches.get(0).getSrcElement();
    }
    return findParentMatch(srcMatch, matches);
  }

  /**
   * Remove any matches from the supplied set whose immediate parents do not match on any of the
   * supplied classnames
   *
   * @param matches The matches to check, assumed not <code>null</code>, the set is modified.
   * @param classNames The set of classnames to check, assumed not <code>null<code/> or empty.
   */
  private void filterParentMatches(List<PSClassNameMatcher> matches, Set<String> classNames) {
    var iter = matches.iterator();
    while (iter.hasNext()) {
      var match = iter.next();
      if (!match.matchParentClasses(classNames)) {
        iter.remove();
      }
    }
  }

  private org.jsoup.select.Elements findMatches(Document targetDoc, Set<String> classNames) {
    var found = new org.jsoup.select.Elements();
    var buffer = new StringBuilder();
    for (var className : classNames) {
      buffer.append(".").append(className);
    }
    var clsSelector = buffer.toString();
    var elems = targetDoc.select(clsSelector);
    if (elems != null && elems.size() == 1) {
      found.addAll(elems);
    } else {
      for (var className : classNames) {
        elems = targetDoc.select("." + className);
        for (var element : elems) {
          if (!found.contains(element)) {
            found.add(element);
          }
        }
      }
    }
    return found;
  }
}
