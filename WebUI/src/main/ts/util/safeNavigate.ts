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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Protocol / same-origin guard for server-supplied URLs that the modern
 * UI surfaces to {@code window.location}. Closes the
 * {@code javascript:} XSS vector flagged in the kilo-code-bot review on
 * PR #1396 for {@code ContextMenu} and {@code ActionToolbar}.
 *
 * <p>The guard accepts:</p>
 * <ul>
 *   <li>Relative URLs (no scheme) — e.g. {@code "/Rhythmyx/..."} or
 *       {@code "../foo/bar"}. Same-origin by definition.</li>
 *   <li>Protocol-relative URLs (no scheme, leading {@code //}) —
 *       inherits the current page's protocol; treated as same-origin
 *       via {@code new URL(url, base).origin}.</li>
 *   <li>Absolute URLs with an allowed protocol (default
 *       {@code http:}, {@code https:}, {@code mailto:}) AND whose
 *       {@code origin} matches {@code window.location.origin}.</li>
 * </ul>
 * <p>Everything else (notably {@code javascript:}, {@code data:},
 * {@code vbscript:}, {@code file:}, custom app protocols) is rejected
 * and {@link safeNavigate} returns {@code false} without touching
 * {@code window.location}.</p>
 */

/** Default allow-list of protocols for absolute URLs. */
export const DEFAULT_ALLOWED_PROTOCOLS: readonly string[] = [
  "http:",
  "https:",
  "mailto:",
];

/**
 * The outcome of {@link safeNavigate}. Captured so callers can decide
 * whether to log a console warning, surface a UI error, or invoke a
 * custom {@code onInvoke} fallback when the URL is rejected.
 */
export type SafeNavigateResult =
  | { ok: true; href: string }
  | { ok: false; reason: SafeNavigateRejectionReason; href: string };

export type SafeNavigateRejectionReason =
  | "javascript:"
  | "data:"
  | "vbscript:"
  | "file:"
  | "blob:"
  | "protocol-not-allowed"
  | "different-origin"
  | "invalid-url";

/**
 * Navigate to {@code url} after verifying that it passes the protocol /
 * same-origin whitelist. Returns the navigation outcome; the actual
 * assignment to {@code window.location.href} is performed only when
 * {@link SafeNavigateResult.ok} is true.
 *
 * @param url         The user / server-supplied URL string.
 * @param base        Optional base URL (defaults to {@code window.location.href}).
 *                    Pass a synthetic base in tests.
 * @param allowed     Override the default allow-list of protocols
 *                    (useful in tests; production callers should use
 *                    the default).
 * @returns The {@link SafeNavigateResult} for the URL.
 */
export function classifyUrl(
  url: string,
  base?: string,
  allowed: readonly string[] = DEFAULT_ALLOWED_PROTOCOLS,
): SafeNavigateResult {
  if (typeof url !== "string" || url.length === 0) {
    return { ok: false, reason: "invalid-url", href: String(url) };
  }
  // Fast-path rejection for the known-dangerous protocols so we never
  // attempt to construct a URL with `javascript:` etc.
  const trimmed = url.trim();
  const lower = trimmed.toLowerCase();
  if (lower.startsWith("javascript:")) {
    return { ok: false, reason: "javascript:", href: trimmed };
  }
  if (lower.startsWith("data:")) {
    return { ok: false, reason: "data:", href: trimmed };
  }
  if (lower.startsWith("vbscript:")) {
    return { ok: false, reason: "vbscript:", href: trimmed };
  }
  if (lower.startsWith("file:")) {
    return { ok: false, reason: "file:", href: trimmed };
  }
  if (lower.startsWith("blob:")) {
    return { ok: false, reason: "blob:", href: trimmed };
  }

  // Resolve to an absolute URL with the supplied (or current-page) base.
  const baseHref =
    base ??
    (typeof window !== "undefined" ? window.location.href : "http://localhost/");
  let parsed: URL;
  try {
    parsed = new URL(trimmed, baseHref);
  } catch {
    return { ok: false, reason: "invalid-url", href: trimmed };
  }

  const origin =
    typeof window !== "undefined" && !base
      ? window.location.origin
      : new URL(baseHref).origin;

  if (parsed.protocol === "" || parsed.origin === origin) {
    // Relative / same-origin / hash fragment → allowed.
    return { ok: true, href: parsed.href };
  }

  if (!allowed.includes(parsed.protocol)) {
    return { ok: false, reason: "protocol-not-allowed", href: trimmed };
  }

  // Some protocols (mailto:, tel:, sms:, etc.) have no origin per the
  // URL spec; the origin check is meaningful only for protocols that
  // can actually establish a same-site context. For those schemes,
  // an allowed protocol is enough — the address itself is the
  // security boundary.
  const ORIGINLESS_PROTOCOLS: ReadonlySet<string> = new Set([
    "mailto:",
    "tel:",
    "sms:",
  ]);
  if (ORIGINLESS_PROTOCOLS.has(parsed.protocol)) {
    return { ok: true, href: parsed.href };
  }

  if (parsed.origin !== origin) {
    return { ok: false, reason: "different-origin", href: trimmed };
  }

  return { ok: true, href: parsed.href };
}

/**
 * Navigate via {@code window.location.href = href} only when
 * {@link classifyUrl} accepts the URL. Returns the navigation outcome.
 * When running outside a browser (e.g. in Vitest with
 * {@code environment: "jsdom"}), the assignment still happens but
 * {@link window.location.href} is the jsdom-coordinated value.
 */
export function safeNavigate(
  url: string,
  base?: string,
  allowed: readonly string[] = DEFAULT_ALLOWED_PROTOCOLS,
): SafeNavigateResult {
  const result = classifyUrl(url, base, allowed);
  if (result.ok && typeof window !== "undefined") {
    window.location.href = result.href;
  }
  return result;
}
