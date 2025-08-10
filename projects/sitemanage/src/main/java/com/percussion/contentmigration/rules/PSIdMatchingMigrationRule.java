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

public class PSIdMatchingMigrationRule extends PSBaseMatchingMigrationRule {

  @Override
  protected String matchOnRule(
      String widgetId, org.jsoup.nodes.Document sourceDoc, org.jsoup.nodes.Document targetDoc) {
    var regionElem = findEnclosingRegionElement(widgetId, sourceDoc);
    if (regionElem == null) {
      return null;
    }
    var regionId = regionElem.id();
    var elems = targetDoc.select("#" + regionId);
    if (elems.size() != 1) {
      return null;
    }
    return elems.get(0).html();
  }
}
