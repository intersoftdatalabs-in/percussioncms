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

import type { LogoutBootstrap } from "./types";

const LOGOUT_BOOTSTRAP_ID = "perc-logout-bootstrap";

/** Default login front door (same relative form as classic login action). */
export const DEFAULT_LOGIN_HREF = "login";

/**
 * Allowlist a post-logout login href. Rejects absolute URLs to other hosts,
 * protocol-relative URLs, and path traversal.
 */
export function sanitizeLoginHref(
  candidate: string | null | undefined,
  fallback: string = DEFAULT_LOGIN_HREF,
): string {
  if (typeof candidate !== "string") {
    return fallback;
  }
  const href = candidate.trim();
  if (!href || href.length > 2048) {
    return fallback;
  }
  // Block absolute / protocol-relative / javascript: etc.
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(href) || href.startsWith("//")) {
    return fallback;
  }
  if (href.includes("..") || href.includes("\\")) {
    return fallback;
  }
  // Path-absolute or simple relative product paths only.
  if (href.startsWith("/")) {
    // Must stay on CMS product surfaces (login / rxlogin / cm).
    if (
      href === "/login" ||
      href.startsWith("/login?") ||
      href === "/rxlogin.jsp" ||
      href.startsWith("/rxlogin.jsp?") ||
      href.startsWith("/cm/")
    ) {
      return href;
    }
    return fallback;
  }
  // Relative: allow "login", "rxlogin.jsp", optional query.
  if (/^(login|rxlogin\.jsp)(\?.*)?$/i.test(href)) {
    return href;
  }
  return fallback;
}

/**
 * Ensures the login href carries {@code j_locale} so re-login opens in the same language.
 * No-ops when the href already has {@code j_locale} or locale is empty.
 */
export function loginHrefWithLocale(loginHref: string, locale: string): string {
  const href = sanitizeLoginHref(loginHref, DEFAULT_LOGIN_HREF);
  const loc = typeof locale === "string" ? locale.trim() : "";
  if (!loc || /[?&]j_locale=/i.test(href)) {
    return href;
  }
  // Same allowlist shape as sanitizeLoginHref (relative or path-absolute product paths).
  const sep = href.includes("?") ? "&" : "?";
  return `${href}${sep}j_locale=${encodeURIComponent(loc)}`;
}

/**
 * Reads logout bootstrap from the host page JSON script tag.
 */
export function readLogoutBootstrap(): LogoutBootstrap {
  const el = document.getElementById(LOGOUT_BOOTSTRAP_ID);
  let raw: Partial<LogoutBootstrap> | null = null;
  if (el?.textContent) {
    try {
      raw = JSON.parse(el.textContent) as Partial<LogoutBootstrap>;
    } catch {
      console.error(
        `[PercModernUI] Failed to parse bootstrap JSON (#${LOGOUT_BOOTSTRAP_ID})`,
      );
    }
  }
  const locale =
    typeof raw?.locale === "string" && raw.locale ? raw.locale : "en-us";
  const loginHref = loginHrefWithLocale(
    sanitizeLoginHref(raw?.loginHref, DEFAULT_LOGIN_HREF),
    locale,
  );
  return {
    locale,
    loginHref,
  };
}
