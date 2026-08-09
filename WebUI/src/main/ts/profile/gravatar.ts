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

/**
 * Gravatar helpers for profile avatar (#2397 / parent #2374 slice 5).
 *
 * <p>Hash uses SHA-256 of the trimmed, lower-cased email (Gravatar modern
 * recommendation). Client-side only — no API key. When external fetch is
 * disabled by server kill-switch, callers must not build or load these URLs.</p>
 */

/** Preference name for optional Gravatar email override (empty = use primary). */
export const GRAVATAR_EMAIL_PREF_NAME = "perc_profile_gravatar_email";

/** Default pixel size for header chip and profile preview. */
export const GRAVATAR_DEFAULT_SIZE = 80;

/**
 * Normalize email for hashing: trim + lowercase (RFC-style Gravatar contract).
 */
export function normalizeGravatarEmail(email: string): string {
  return (email ?? "").trim().toLowerCase();
}

/**
 * Resolve effective Gravatar email: override wins when non-blank, else primary.
 */
export function resolveGravatarEmail(
  overrideEmail: string | null | undefined,
  primaryEmail: string | null | undefined,
): string {
  const override = normalizeGravatarEmail(overrideEmail ?? "");
  if (override) {
    return override;
  }
  return normalizeGravatarEmail(primaryEmail ?? "");
}

/**
 * Hex SHA-256 of a UTF-8 string (browser / Node Web Crypto).
 */
export async function sha256Hex(value: string): Promise<string> {
  const cryptoObj = globalThis.crypto;
  if (!cryptoObj?.subtle) {
    throw new Error("Web Crypto API is not available for Gravatar hashing");
  }
  const data = new TextEncoder().encode(value);
  const digest = await cryptoObj.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export type GravatarUrlOptions = {
  size?: number;
  /**
   * Gravatar default image policy. Use {@code 404} so {@code onError} can fall
   * back to accessible initials without inventing a third-party face.
   */
  defaultImage?: string;
};

/**
 * Build a Gravatar avatar URL for a normalized (or raw) email.
 * Returns null when email is blank (no hashable identity).
 */
export async function buildGravatarUrl(
  email: string,
  options: GravatarUrlOptions = {},
): Promise<string | null> {
  const normalized = normalizeGravatarEmail(email);
  if (!normalized) {
    return null;
  }
  const hash = await sha256Hex(normalized);
  const size = Math.max(1, Math.min(2048, options.size ?? GRAVATAR_DEFAULT_SIZE));
  const d = options.defaultImage ?? "404";
  return `https://www.gravatar.com/avatar/${hash}?s=${size}&d=${encodeURIComponent(d)}`;
}

/**
 * Accessible initials fallback from a display name (1–2 letters).
 */
export function userInitials(displayName: string): string {
  const parts = (displayName ?? "")
    .trim()
    .split(/\s+/)
    .filter((p) => p.length > 0);
  if (parts.length === 0) {
    return "?";
  }
  if (parts.length === 1) {
    const single = parts[0];
    if (single.length === 1) {
      return single.toUpperCase();
    }
    return single.slice(0, 2).toUpperCase();
  }
  const first = parts[0][0] ?? "";
  const last = parts[parts.length - 1][0] ?? "";
  return (first + last).toUpperCase() || "?";
}

/**
 * Decide avatar presentation: external URL only when allowed and email present.
 */
export async function resolveAvatarPresentation(opts: {
  displayName: string;
  overrideEmail?: string | null;
  primaryEmail?: string | null;
  allowExternalAvatarFetch: boolean;
  size?: number;
}): Promise<{
  initials: string;
  gravatarEmail: string;
  imageUrl: string | null;
}> {
  const initials = userInitials(opts.displayName);
  const gravatarEmail = resolveGravatarEmail(
    opts.overrideEmail,
    opts.primaryEmail,
  );
  if (!opts.allowExternalAvatarFetch || !gravatarEmail) {
    return { initials, gravatarEmail, imageUrl: null };
  }
  const imageUrl = await buildGravatarUrl(gravatarEmail, {
    size: opts.size,
    defaultImage: "404",
  });
  return { initials, gravatarEmail, imageUrl };
}
