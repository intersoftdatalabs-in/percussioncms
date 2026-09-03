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

// REFACTORED: CP-JAVA11

package com.percussion.rest.extensions;

import java.net.URI;
import java.util.List;

/**
 * Adaptor interface for Extension catalog and Admin user-extension write (SY-01).
 *
 * <p>Write methods persist <strong>user</strong> extensions through {@code IPSExtensionService} /
 * {@code IPSExtensionManager}. System ({@code global/percussion/...}) and handler-owned
 * extensions are immutable.
 */
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

  /**
   * Admin. Register (install) a user extension under context {@code user/}.
   *
   * @param baseURI Base URI for the request
   * @param body required; {@code extensionName} and at least one supported interface
   * @return the persisted extension
   */
  Extension registerExtension(URI baseURI, Extension body);

  /**
   * Admin. Update mutable fields of a user extension identified by FQN or short name. Does not
   * mutate system or handler-owned extensions.
   *
   * @param baseURI Base URI for the request
   * @param idOrName FQN or extension name (same rules as {@link #findExtensionByKey})
   * @param body required writable fields
   * @return the persisted extension, or {@code null} when missing/unsafe
   */
  Extension updateExtension(URI baseURI, String idOrName, Extension body);

  /**
   * Admin. Delete a user extension by FQN or short name. Does not mutate system or handler-owned
   * extensions.
   *
   * @param baseURI Base URI for the request
   * @param idOrName FQN or extension name (same rules as {@link #findExtensionByKey})
   * @return {@code true} when deleted, {@code false} when missing/unsafe
   */
  boolean deleteExtension(URI baseURI, String idOrName);
}
