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
package com.percussion.services.virtualsite;

import java.io.IOException;
import java.util.List;

/**
 * SPI for Virtual Site content sources. Phase 1: {@link PSGitFilesystemVirtualSiteSource}.
 * Future adapters (SQL, API, …) implement this interface.
 */
public interface IPSVirtualSiteSource {

  /** Wire name for this source type (e.g. {@code git-filesystem}). */
  String sourceType();

  /**
   * Discover all page references under the configured root.
   *
   * @param config site config with root and versions
   * @return ordered discovery list (not necessarily final nav order)
   * @throws IOException on filesystem errors
   * @throws VirtualSiteException on contract validation failures
   */
  List<VirtualItemRef> discover(VirtualSiteConfig config) throws IOException, VirtualSiteException;

  /**
   * Load full content for a previously discovered ref.
   *
   * @param config site config
   * @param ref item reference
   * @return loaded item
   * @throws IOException on filesystem errors
   * @throws VirtualSiteException on parse/validation failures
   */
  VirtualItem load(VirtualSiteConfig config, VirtualItemRef ref)
      throws IOException, VirtualSiteException;
}
