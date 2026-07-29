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

package com.percussion.rest.pipelines;

import java.net.URI;
import java.util.List;

public interface IPipelinesAdaptor {

  /**
   * List server applications visible to the current security token (non-hidden by default).
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @return application summaries, never {@code null}
   */
  List<ApplicationSummary> listApplications(URI baseUri);
}
