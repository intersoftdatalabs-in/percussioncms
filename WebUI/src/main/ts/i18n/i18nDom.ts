/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/**
 * DOM attributes for the third-party {@code @mkd/language} correction client.
 *
 * <p>Host elements that display a TMX-resolved string should spread
 * {@link i18nKeyAttr} so correction submissions include the catalog key.
 * The attribute name matches the library default {@code messageIdAttr}.</p>
 */

/** Attribute name expected by {@code @mkd/language} {@code messageIdAttr}. */
export const I18N_KEY_ATTR = "data-i18n-key" as const;

export type I18nKeyAttrs = { [I18N_KEY_ATTR]: string };

/**
 * Spread onto a host element that displays a localized TMX string.
 *
 * @param key - full catalog key (e.g. {@code perc.ui.navMenu@Home})
 */
export function i18nKeyAttr(key: string): I18nKeyAttrs {
  return { [I18N_KEY_ATTR]: key };
}
