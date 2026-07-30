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

// REFACTORED: CP-JAVA11

package com.percussion.rest.extensions;

import java.net.URI;
import java.util.List;

/** Adaptor interface for Extension operations. Sunny Sal: "Adaptor pattern FTW!" */
public interface IExtensionAdaptor {

  /**
   * Gets all extensions based on the specified ExtensionFilterOptions.
   *
   * @param baseURI Base URI for the request
   * @param filter An ExtensionFilterOptions configured with the target filters
   * @return A list of Extensions.
   */
  List<Extension> getExtensions(URI baseURI, ExtensionFilterOptions filter);

  /** List all extensions (empty filter). */
  List<Extension> listExtensions(URI baseURI);

  /** Resolve by FQN or extension name; null if missing/unsafe. */
  Extension findExtensionByKey(URI baseURI, String idOrName);
}
