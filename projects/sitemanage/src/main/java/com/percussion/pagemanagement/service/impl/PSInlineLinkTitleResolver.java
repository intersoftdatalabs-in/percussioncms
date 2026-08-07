/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.pagemanagement.service.impl;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves the HTML {@code title} attribute value for inline links/images inserted from a rich-text
 * control (parent epic #946 / slice #2242).
 *
 * <p><b>Fallback chain</b> (when a control setting names a field):
 *
 * <ol>
 *   <li>Configured field on the <em>target</em> item, if present and non-blank
 *   <li>{@value #DISPLAYTITLE_FIELD} on the target, if present and non-blank
 *   <li>Type default (pre-feature behavior): page {@code resource_link_title} / {@code
 *       page.getLinkTitle()}, or asset {@code displaytitle}
 *   <li>Empty string when nothing above yields a value
 * </ol>
 *
 * <p>When the configured field name is blank or absent, the type default is used immediately
 * (backward compatible with pre-feature insert behavior).
 */
public final class PSInlineLinkTitleResolver {

  /** Shared/system field used as the first fallback after a missing custom field. */
  public static final String DISPLAYTITLE_FIELD = "displaytitle";

  /** Default title field for assets / files / images when no control setting is configured. */
  public static final String ASSET_DEFAULT_TITLE_FIELD = DISPLAYTITLE_FIELD;

  /**
   * Default title field name for pages when no control setting is configured ({@code
   * resource_link_title} / page link title).
   */
  public static final String PAGE_DEFAULT_TITLE_FIELD = "resource_link_title";

  private PSInlineLinkTitleResolver() {
    // utility
  }

  /**
   * Resolves the inline link title for a target item.
   *
   * @param configuredFieldName field name from the source rich-text control setting ({@code
   *     InlineLinkTitleField} / TinyMCE {@code inlineLinkTitleField}); may be {@code null} or blank
   * @param fields target item field map; may be {@code null} (treated as empty)
   * @param typeDefault pre-feature default title for this target type (e.g. page link title or
   *     asset displaytitle value); may be {@code null}
   * @return never {@code null}; may be empty
   */
  public static String resolve(
      String configuredFieldName, Map<String, ?> fields, String typeDefault) {
    if (StringUtils.isNotBlank(configuredFieldName)) {
      String configured = fieldAsString(fields, configuredFieldName.trim());
      if (StringUtils.isNotBlank(configured)) {
        return configured;
      }
      // Custom field missing/empty → displaytitle (issue #2242 fallback)
      if (!DISPLAYTITLE_FIELD.equalsIgnoreCase(configuredFieldName.trim())) {
        String displayTitle = fieldAsString(fields, DISPLAYTITLE_FIELD);
        if (StringUtils.isNotBlank(displayTitle)) {
          return displayTitle;
        }
      }
    }
    return StringUtils.defaultString(typeDefault);
  }

  /**
   * Reads a single field value as a trimmed string. Non-string values use {@link
   * Object#toString()}. Blank or missing values yield {@code null}.
   *
   * @param fields may be {@code null}
   * @param fieldName may be {@code null} or blank
   * @return trimmed non-blank string, or {@code null}
   */
  public static String fieldAsString(Map<String, ?> fields, String fieldName) {
    if (fields == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    Object raw = fields.get(fieldName);
    if (raw == null) {
      // Case-insensitive key match for content-type field names that differ only by case
      for (Map.Entry<String, ?> e : fields.entrySet()) {
        if (e.getKey() != null && e.getKey().equalsIgnoreCase(fieldName)) {
          raw = e.getValue();
          break;
        }
      }
    }
    if (raw == null) {
      return null;
    }
    String text = raw instanceof String ? (String) raw : String.valueOf(raw);
    return StringUtils.isBlank(text) ? null : text.trim();
  }
}
