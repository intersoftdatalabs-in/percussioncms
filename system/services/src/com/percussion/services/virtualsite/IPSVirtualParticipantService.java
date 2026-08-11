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
import java.util.Collection;
import java.util.Optional;

/**
 * Registry of Virtual Site page identities for link integrity and stable cross-page references.
 *
 * <p>Each registration maps a frontmatter {@code id} (stable id) to the published HTML path for a
 * given site key (and version is recorded on the participant).
 *
 * <h2>Lifetime (Phase 1)</h2>
 *
 * <ul>
 *   <li><strong>Process-scoped default</strong> — entries live in memory until {@link
 *       #clear(String)}, {@link #clearAll()}, or JVM exit. Suitable for unit tests and one-shot
 *       offline builds that do not need cross-process durability.
 *   <li><strong>Optional durable store</strong> — implementations may accept a portable {@link
 *       java.nio.file.Path} base (for example build {@code outputRoot/_meta} or a deploy/config
 *       directory). When configured, {@link #flush(String)} persists JSONL and construction (or
 *       explicit reload) reloads prior registrations after a process restart.
 *   <li><strong>Full rebuild replaces a site</strong> — {@link PSVirtualSiteBuildService} clears
 *       the site key at the start of a build, then upserts every discovered page, then flushes.
 *       That way a second build does not incorrectly retain pages removed from the source tree,
 *       and all current frontmatter ids are re-registered.
 * </ul>
 *
 * <p>Phase 1 deliberately avoids CMS content IDs and {@code PSX_MANAGEDLINK}.
 */
public interface IPSVirtualParticipantService {

  /** Insert or replace a participant for {@code participant.siteKey()} / {@code stableId()}. */
  void upsert(VirtualParticipant participant);

  /** Lookup by site key and frontmatter stable id; empty when never registered or cleared. */
  Optional<VirtualParticipant> find(String siteKey, String stableId);

  /** All participants for a site (empty collection when none). */
  Collection<VirtualParticipant> list(String siteKey);

  /**
   * Persist all participants for a site (implementation-specific). No-op when no durable store is
   * configured.
   */
  void flush(String siteKey) throws IOException;

  /**
   * Remove all participants for one site from memory and, when durable storage is configured,
   * delete that site's store file.
   */
  void clear(String siteKey) throws IOException;

  /**
   * Reset the entire registry (all sites). Clears memory and, when durable storage is configured,
   * removes known store files under the store directory.
   */
  void clearAll() throws IOException;
}
