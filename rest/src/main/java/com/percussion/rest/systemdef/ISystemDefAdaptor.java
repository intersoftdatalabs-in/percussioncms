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

package com.percussion.rest.systemdef;

import java.net.URI;

public interface ISystemDefAdaptor {

  /**
   * Load the content-editor system definition field catalog.
   *
   * @param baseUri reserved for HATEOAS
   * @return detail, never {@code null} (empty fields when def missing)
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SystemDefDetail getSystemDef(URI baseUri);

  /**
   * Persist patches to existing system-def field properties. Admin only. Acquires the system-def
   * design lock for this request and releases it on save.
   *
   * <p>Supports patches to existing fields ({@code searchable}, occurrence / required). Null or
   * empty {@code fields} leaves the catalog unchanged. Does not create or delete fields. {@code
   * dataType}, {@code readOnly}, and {@code cacheTimeoutMinutes} are not written.
   *
   * @return persisted detail, never {@code null}
   * @throws IllegalArgumentException when input is invalid, a field name is unknown, or {@code
   *     occurrence} and {@code required} conflict
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefDetail updateSystemDef(URI baseUri, SystemDefDetail body);
}
