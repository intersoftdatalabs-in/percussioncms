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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Base class for migration rules, handles basic validation of parameters and provides some common
 * functionality likely to be needed by most matching rules.
 *
 * @author JaySeletz
 */
public abstract class PSBaseMatchingMigrationRule implements IPSContentMigrationRule {
  public static final String[] IGNORE_CLASS_PREFIXES = {"perc-", "vspan_", "hspan_", "ui-"};
  public static final List<String> IGNORE_CLASS_NAMES = Arrays.asList("ui-helper-clearfix");

  @Override
  public String findMatchingContent(String widgetId, Document sourceDoc, Document targetDoc) {
    Validate.notEmpty(widgetId);
    Validate.notNull(sourceDoc);
    Validate.notNull(targetDoc);
    return matchOnRule(widgetId, sourceDoc, targetDoc);
  }

  /**
   * Derived class implementation delegated from {@link #findMatchingContent(String, Document,
   * Document)}.
   */
  protected abstract String matchOnRule(String widgetId, Document sourceDoc, Document targetDoc);

  /** Finds the region element that is the immediate parent wrapping the widget. */
  protected static Element findEnclosingRegionElement(String widgetId, Document sourceDoc) {
    Validate.notNull(widgetId);
    Validate.notNull(sourceDoc);
    return com.percussion.utils.service.impl.PSJsoupUtils.closestParentByClass(
        sourceDoc,
        com.percussion.utils.service.impl.PSJsoupUtils.generateAttributeSelector(
            IPSContentMigrationRule.ATTR_WIDGET_ID, widgetId),
        IPSContentMigrationRule.CLASS_PERC_REGION);
  }

  /** Filters out system class names from the set. */
  public static void filterClassName(Set<String> classNames) {
    Validate.notNull(classNames);
    classNames.removeAll(IGNORE_CLASS_NAMES);
    classNames.removeIf(
        className ->
            className == null
                || className.isBlank()
                || Arrays.stream(IGNORE_CLASS_PREFIXES).anyMatch(className::startsWith));
  }
}
