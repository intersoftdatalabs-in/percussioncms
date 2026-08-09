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

package com.percussion.rest.translations;

import java.net.URI;

/**
 * Adaptor for public content-item translation (P-Trans / #2429).
 *
 * <p>HTTP lives on {@link ContentTranslationsResource}; production impl is sitemanage apibridge
 * wrapping the same domain path as SOAP {@code content.NewTranslations} / {@code
 * IPSContentWs#newTranslations}.
 *
 * <p><strong>Out of scope here:</strong> in-flight translation queue filter and session
 * content-locale context (product disposition on #2411 / #2428).
 */
public interface IContentTranslationsAdaptor {

  /**
   * Create locale variants for the supplied source items.
   *
   * @param baseUri request base URI (HATEOAS reserved; may be null)
   * @param request create request; must include at least one item id
   * @return created variants; never null
   * @throws IllegalArgumentException for contract violations
   * @throws SecurityException when the caller cannot create translations
   */
  CreateTranslationsResult createTranslations(URI baseUri, CreateTranslationsRequest request);

  /**
   * List the requested item's locale plus translation-category dependents.
   *
   * @param baseUri request base URI (may be null)
   * @param itemId legacy content id or guid string
   * @return variants envelope; never null when the item is readable
   * @throws SecurityException / not-found mapped by the resource as 403/404
   */
  ItemTranslationVariants listItemVariants(URI baseUri, String itemId);
}
