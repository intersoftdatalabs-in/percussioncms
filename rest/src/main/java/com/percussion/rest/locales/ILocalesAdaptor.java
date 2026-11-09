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

package com.percussion.rest.locales;

import java.net.URI;
import java.util.List;

public interface ILocalesAdaptor {

  /** List CMS locales (all statuses). */
  List<LocaleSummary> listLocales(URI baseUri);

  /**
   * Load one locale by language string (e.g. {@code en-us}) or numeric locale id.
   *
   * @return detail, or {@code null} when not found / unsafe key
   */
  LocaleDetail getLocale(URI baseUri, String idOrLang);

  /**
   * Create a CMS locale (language string + label required). Persists via content design WS with a
   * held lock released on save.
   */
  LocaleDetail createLocale(URI baseUri, LocaleDetail body);

  /**
   * Update an existing locale by language string or numeric id. Language string is immutable. A
   * label-only body updates the label; omitted description, status, baseLocale, and format stay. A
   * blank or whitespace label is not a write. A status-only body updates enabled status ({@code
   * active} / {@code inactive}); omitted label, description, baseLocale, and format stay. A
   * date-pattern-only {@code format} updates {@code datePattern}; omitted format fields stay. A
   * blank {@code datePattern} clears that pattern. A time-pattern-only {@code format} updates
   * {@code timePattern}; omitted format fields stay. A blank {@code timePattern} clears that
   * pattern. A date-time-pattern-only {@code format} updates {@code dateTimePattern}; omitted
   * format fields stay. A blank {@code dateTimePattern} clears that pattern. A
   * decimal-separator-only {@code format} updates {@code decimalSep}; omitted format fields stay.
   * A blank {@code decimalSep} clears that separator. A grouping-separator-only {@code format}
   * updates {@code groupingSep}; omitted format fields stay. A blank {@code groupingSep}
   * clears that separator. A currency-code-only {@code format} updates {@code currencyCode};
   * omitted format fields stay. A blank {@code currencyCode} clears that code. A
   * first-day-of-week-only {@code format} updates {@code firstDayOfWeek}; omitted format fields
   * stay. A blank {@code firstDayOfWeek} clears that value. Values outside 1–7 (Monday–Sunday) are
   * not a write. A measurement-system-only {@code format} updates {@code measurementSystem};
   * omitted format fields stay. A blank {@code measurementSystem} clears that value. Values other
   * than {@code us}, {@code uk}, and {@code metric} are not a write. A default-timezone-only
   * {@code format} updates {@code defaultTz}; omitted format fields stay. A blank
   * {@code defaultTz} clears that value. A value longer than 64 characters is not a write. A
   * numbering-system-only {@code format} updates {@code numberingSystem}; omitted format fields
   * stay. A blank {@code numberingSystem} clears that value. A value longer than 16 characters
   * is not a write. A calendar-only {@code format} updates {@code calendar}; omitted format
   * fields stay. A blank {@code calendar} clears that value. A value longer than 32 characters
   * is not a write. A fallback-chain-only {@code format} updates {@code fallbackChain};
   * omitted format fields stay. An empty list clears the stored chain (sent blank-only on
   * the wire, since nested empty arrays do not survive every JSON provider). Each entry
   * must be a known locale language string; an unknown or invalid entry, or the locale
   * itself, is not a write.
   *
   * @return detail, or {@code null} when not found / unsafe key
   */
  LocaleDetail updateLocale(URI baseUri, String idOrLang, LocaleDetail body);

  /** Delete a locale by language string or numeric id. */
  void deleteLocale(URI baseUri, String idOrLang);
}
