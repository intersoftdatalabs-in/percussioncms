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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.security.IPSTypedPrincipal;
import com.percussion.services.security.IPSAclEntry;
import com.percussion.services.security.PSTypedPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the {@code isCommunityChildCandidate} helper extracted from {@link
 * PSAclDefDependencyHandler#getChildDependencies} (issue #6041, complexity.cognitive residual).
 * The full child-dependency path requires a live CMS (role manager, community handler); these tests
 * lock the branching predicate of the extracted helper so the refactor cannot silently change which
 * ACL entries resolve to community child dependencies.
 */
public class PSAclDefChildDependenciesTest {

  @Test
  @DisplayName("community entry with a concrete name is a child candidate")
  public void communityEntryWithConcreteNameIsCandidate() {
    // given a community-typed entry naming a concrete community
    IPSAclEntry entry = communityEntry("MyCommunity");

    // when the candidate predicate is evaluated
    boolean candidate = PSAclCommunityEntries.isCommunityChildCandidate(entry);

    // then the entry resolves to a community child dependency
    assertTrue(candidate);
  }

  @Test
  @DisplayName("any-community placeholder entry is not a child candidate")
  public void anyCommunityPlaceholderIsNotCandidate() {
    // given a community-typed entry for the any-community placeholder
    IPSAclEntry entry = communityEntry(PSTypedPrincipal.ANY_COMMUNITY_ENTRY);

    // when the candidate predicate is evaluated
    boolean candidate = PSAclCommunityEntries.isCommunityChildCandidate(entry);

    // then no child dependency is resolved for the placeholder
    assertFalse(candidate);
  }

  @Test
  @DisplayName("non-community entry is not a child candidate")
  public void nonCommunityEntryIsNotCandidate() {
    // given a role-typed entry with a concrete name
    IPSAclEntry entry = mock(IPSAclEntry.class);
    IPSTypedPrincipal principal = mock(IPSTypedPrincipal.class);
    when(entry.getTypedPrincipal()).thenReturn(principal);
    when(principal.getPrincipalType()).thenReturn(IPSTypedPrincipal.PrincipalTypes.ROLE);
    when(entry.getName()).thenReturn("Admin");

    // when the candidate predicate is evaluated
    boolean candidate = PSAclCommunityEntries.isCommunityChildCandidate(entry);

    // then no child dependency is resolved for the non-community entry
    assertFalse(candidate);
  }

  private static IPSAclEntry communityEntry(String name) {
    IPSAclEntry entry = mock(IPSAclEntry.class);
    IPSTypedPrincipal principal = mock(IPSTypedPrincipal.class);
    when(entry.getTypedPrincipal()).thenReturn(principal);
    when(principal.getPrincipalType()).thenReturn(IPSTypedPrincipal.PrincipalTypes.COMMUNITY);
    when(entry.getName()).thenReturn(name);
    return entry;
  }
}
