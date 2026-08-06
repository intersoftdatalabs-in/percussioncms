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
 * Markers for third-party {@code @mkd/language} to skip non-catalog DOM.
 *
 * <p>Product chrome that uses {@link message} should <em>not</em> be ignored.
 * User-generated values (folder names, search hits, usernames, password fields,
 * gadget iframes) should be ignored so correction triggers do not attach.</p>
 *
 * <p>Library also honors {@code data-mkd-lang-ignore} and class
 * {@code mkd-lang-ignore} (same values as these constants).</p>
 */

/** Attribute accepted by {@code @mkd/language} {@code respectIgnore}. */
export const MKD_LANG_IGNORE_ATTR = "data-mkd-lang-ignore" as const;

/** Class accepted by {@code @mkd/language} {@code respectIgnore}. */
export const MKD_LANG_IGNORE_CLASS = "mkd-lang-ignore" as const;

/**
 * Spread onto a container that holds user content / non-TMX values.
 *
 * @example
 * ```tsx
 * <div {...mkdLangIgnoreProps()} data-testid="explorer-tree">…</div>
 * ```
 */
export function mkdLangIgnoreProps(): {
  [MKD_LANG_IGNORE_ATTR]: "1";
  className: typeof MKD_LANG_IGNORE_CLASS;
} {
  return {
    [MKD_LANG_IGNORE_ATTR]: "1",
    className: MKD_LANG_IGNORE_CLASS,
  };
}

/**
 * Merge ignore class into an existing className string.
 */
export function withMkdLangIgnoreClass(className?: string): string {
  return [MKD_LANG_IGNORE_CLASS, className].filter(Boolean).join(" ");
}
