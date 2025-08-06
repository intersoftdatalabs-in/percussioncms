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

public interface IPSContentMigrationRule {
    /**
     * Finds the matching content based on the rule implementation and returns it.
     * Returns {@code null} if not found, so that other rules can be applied.
     *
     * @param widgetId widget id, must not be {@code null}
     * @param sourceDoc rendered page or template document, must not be {@code null}
     * @param targetDoc target page document, must not be {@code null}
     * @return matched content or {@code null} if not found
     */
    String findMatchingContent(String widgetId, org.jsoup.nodes.Document sourceDoc, org.jsoup.nodes.Document targetDoc);

    String ATTR_WIDGET_ID = "widgetid";
    String CLASS_PERC_REGION = "perc-region";
    String PERC_CLASS_PREFIX = "perc-";
}
