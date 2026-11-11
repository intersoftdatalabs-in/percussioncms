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
package com.percussion.deployer.server.dependencies;

import com.percussion.security.IPSTypedPrincipal;
import com.percussion.services.security.IPSAclEntry;
import com.percussion.services.security.PSTypedPrincipal;

/**
 * Pure predicates over ACL entries used by {@link PSAclDefDependencyHandler#getChildDependencies}.
 *
 * <p>Lives outside the handler because the handler's static initializer requires a Spring
 * application context ({@code PSAclServiceLocator.getAclService()}), which makes any static helper
 * declared on the handler untestable in plain unit tests. The helpers here touch only principal
 * state, so they load anywhere.
 */
final class PSAclCommunityEntries {

  private PSAclCommunityEntries() {
    // utility class, never instantiated
  }

  /**
   * Checks whether an ACL entry is a deployable community reference: a community-typed entry that
   * is not the any-community placeholder. Uses {@link IPSTypedPrincipal#getPrincipalType()} instead
   * of the deprecated {@link IPSAclEntry#getType()}; both read the same principal type (see {@code
   * PSAclEntryImpl}).
   *
   * @param entry the ACL entry to check, assumed not <code>null</code>.
   * @return <code>true</code> when the entry names a concrete community.
   */
  static boolean isCommunityChildCandidate(IPSAclEntry entry) {
    return entry.getTypedPrincipal().getPrincipalType() == IPSTypedPrincipal.PrincipalTypes.COMMUNITY
        && !entry.getName().equals(PSTypedPrincipal.ANY_COMMUNITY_ENTRY);
  }
}
