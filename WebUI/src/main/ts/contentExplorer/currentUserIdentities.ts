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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Resolve the identity set used by folder ACL self-lockout detection
 * ({@link detectSelfLockout} / {@code FolderSecurityPanel}).
 *
 * <p>Principals on {@code PSFolderPermission} lists are matched by name
 * only (USER and ROLE). The product session bootstrap exposes the
 * signed-in user name plus coarse role flags ({@code isAdmin} /
 * {@code isDesigner}); optional role names may be supplied when a richer
 * catalog is available.</p>
 *
 * <p>Pure helper — no DOM / fetch / React — so Vitest can lock the rules
 * without mounting the shell.</p>
 */

export interface CurrentUserIdentitySource {
  /** Signed-in CMS user name (e.g. spa bootstrap {@code userName}). */
  userName?: string | null;
  /** True when the session is an Admin user. */
  isAdmin?: boolean;
  /** True when the session is a Designer user. */
  isDesigner?: boolean;
  /**
   * Optional explicit role names (e.g. from a roles API). Deduped with
   * flag-derived role names below.
   */
  roles?: ReadonlyArray<string>;
}

/**
 * Built-in product role names inferred from SPA bootstrap flags.
 * Matches common folder ACL principal names used by the CMS seed data.
 */
export const BOOTSTRAP_ROLE_ADMIN = "Admin";
export const BOOTSTRAP_ROLE_DESIGNER = "Designer";

/**
 * Build a de-duplicated identity list for lockout detection.
 *
 * <p>Order: user name first, then flag-derived roles, then any extra
 * {@link CurrentUserIdentitySource.roles}. Empty / blank strings are
 * dropped. When nothing is available, returns an empty array (lockout
 * detection is a no-op — safer than inventing a principal).</p>
 */
export function resolveCurrentUserIdentities(
  source: CurrentUserIdentitySource,
): string[] {
  const out: string[] = [];
  const seen = new Set<string>();

  const push = (raw: string | null | undefined): void => {
    if (raw == null) return;
    const name = raw.trim();
    if (!name || seen.has(name)) return;
    seen.add(name);
    out.push(name);
  };

  push(source.userName);
  if (source.isAdmin) {
    push(BOOTSTRAP_ROLE_ADMIN);
  }
  if (source.isDesigner) {
    push(BOOTSTRAP_ROLE_DESIGNER);
  }
  for (const role of source.roles ?? []) {
    push(role);
  }
  return out;
}
