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
   * Update an existing locale by language string or numeric id. Language string is immutable.
   *
   * @return detail, or {@code null} when not found / unsafe key
   */
  LocaleDetail updateLocale(URI baseUri, String idOrLang, LocaleDetail body);

  /** Delete a locale by language string or numeric id. */
  void deleteLocale(URI baseUri, String idOrLang);
}
