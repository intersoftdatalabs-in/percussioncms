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

/**
 * Optional DOM key helpers for {@code @mkd/language}.
 *
 * <p><strong>Preferred path:</strong> use {@link message} from {@code ./message}
 * (tracked) so keys associate without attributes. Spread {@link i18nKeyAttr}
 * only when you need an explicit key (collisions, non-message chrome).</p>
 */

import {
  MESSAGE_ID_ATTR,
  messageIdProps as mkdMessageIdProps,
} from "@mkd/language";

/** Attribute name expected by {@code @mkd/language} {@code messageIdAttr}. */
export const I18N_KEY_ATTR = MESSAGE_ID_ATTR;

export type I18nKeyAttrs = { [I18N_KEY_ATTR]: string };

/**
 * Spread onto a host when an explicit catalog key is required.
 * Prefer tracked {@link message} for normal chrome.
 *
 * @param key - full catalog key (e.g. {@code perc.ui.navMenu@Home})
 */
export function i18nKeyAttr(key: string): I18nKeyAttrs {
  return mkdMessageIdProps(key) as I18nKeyAttrs;
}
