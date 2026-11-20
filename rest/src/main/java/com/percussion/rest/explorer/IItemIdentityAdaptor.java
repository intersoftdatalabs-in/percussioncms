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

package com.percussion.rest.explorer;

import java.net.URI;
import java.util.List;

/**
 * Adaptor contract for Explorer row visibility identity (#6783 / parent #4530).
 *
 * <p>HTTP lives in {@code ItemIdentityResource}; the implementation lives in sitemanage
 * ({@code com.percussion.apibridge.ItemIdentityAdaptor}).
 */
public interface IItemIdentityAdaptor {

  /**
   * Resolve visibility identity for the given row ids.
   *
   * @param baseUri request base URI (routing context)
   * @param ids row ids in pathmanagement / itemmanagement id space; never {@code null} or empty,
   *     bounded by the resource
   * @return identity per resolvable id, in request order; unknown or unauthorized ids are omitted,
   *     never failed
   */
  List<ItemIdentity> lookupBatch(URI baseUri, List<String> ids);
}
