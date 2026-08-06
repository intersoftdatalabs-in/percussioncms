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

package com.percussion.rest.pipelines;

import java.net.URI;
import java.util.List;

public interface IPipelinesAdaptor {

  /**
   * List server applications visible to the current security token.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param nameFilter optional case-insensitive name/description substring; blank = no filter
   * @param limit max rows to return (clamped by implementation)
   * @param offset zero-based offset into the sorted result
   * @return application summaries, never {@code null}
   */
  List<ApplicationSummary> listApplications(URI baseUri, String nameFilter, int limit, int offset);

  /**
   * Load one application by internal name or numeric id.
   *
   * @return detail, or {@code null} when not found / not visible
   */
  ApplicationDetail getApplication(URI baseUri, String idOrName);
}
