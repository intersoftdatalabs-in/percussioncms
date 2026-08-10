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

/** Registry of Virtual Site page identities for link integrity (Phase 1: file/memory). */
public interface IPSVirtualParticipantService {

  void upsert(VirtualParticipant participant);

  Optional<VirtualParticipant> find(String siteKey, String stableId);

  Collection<VirtualParticipant> list(String siteKey);

  /** Persist all participants for a site (implementation-specific). */
  void flush(String siteKey) throws IOException;
}
