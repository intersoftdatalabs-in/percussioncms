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

package com.percussion.rest.sharedfields;

import java.net.URI;
import java.util.List;

public interface ISharedFieldsAdaptor {

  /**
   * List shared field groups from the content-editor shared definition.
   *
   * @param baseUri reserved for HATEOAS
   * @return group summaries, never {@code null}
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  List<SharedFieldGroupSummary> listGroups(URI baseUri);

  /**
   * Load one shared field group by name (case-insensitive).
   *
   * @return detail, or {@code null} when not found / unsafe name
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SharedFieldGroupDetail getGroup(URI baseUri, String name);
}
