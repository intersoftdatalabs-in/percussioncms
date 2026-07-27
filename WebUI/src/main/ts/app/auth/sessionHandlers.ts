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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { DEFAULT_SPA_ENTRY_REDIRECT } from "../../login/redirect";
import { resolveSpaReturnFromLocation } from "../deepLinks/parseEntryQuery";

export const REACT_LOGIN_PATH = "/rxlogin.jsp";

/**
 * Latch so concurrent 401s only navigate once. Reset on each SPA boot so
 * remount/HMR does not permanently suppress redirects (#1526 review).
 */
let redirectingToLogin = false;

/** Reset the redirect latch (call from SPA boot; tests may also call). */
export function resetLoginRedirectLatch(): void {
  redirectingToLogin = false;
}

/**
 * Build React Login URL with allowlisted SPA return query (never hash).
 */
export function buildLoginReturnUrl(
  spaReturn: string = DEFAULT_SPA_ENTRY_REDIRECT,
): string {
  const safe =
    spaReturn.startsWith("/cm/app/spa.jsp") &&
    !spaReturn.includes("#") &&
    !spaReturn.includes("://") &&
    !spaReturn.includes("..")
      ? spaReturn
      : DEFAULT_SPA_ENTRY_REDIRECT;
  const params = new URLSearchParams();
  params.set("return", safe);
  return `${REACT_LOGIN_PATH}?${params.toString()}`;
}

/**
 * Current SPA document return URL from location.
 * Prefers query {@code entry} contract; falls back to path-based client routes (PR-9).
 */
export function currentSpaReturnUrl(
  search: string = typeof window !== "undefined" ? window.location.search : "",
  pathname: string = typeof window !== "undefined"
    ? window.location.pathname
    : "/cm/app/spa.jsp",
): string {
  try {
    return resolveSpaReturnFromLocation(pathname, search);
  } catch {
    return DEFAULT_SPA_ENTRY_REDIRECT;
  }
}

/**
 * Mid-session 401 / auth loss → React Login with query return URL.
 * Idempotent for concurrent API failures.
 */
export function redirectToLoginOnUnauthorized(
  options: { returnUrl?: string; reason?: string } = {},
): void {
  if (typeof window === "undefined") {
    return;
  }
  if (redirectingToLogin) {
    return;
  }
  // Already on login — do not loop
  if (window.location.pathname.endsWith("/rxlogin.jsp")) {
    return;
  }
  redirectingToLogin = true;
  const returnUrl = options.returnUrl ?? currentSpaReturnUrl();
  const target = buildLoginReturnUrl(returnUrl);
  if (options.reason) {
    console.info(`[PercModernUI] Session redirect to login: ${options.reason}`);
  }
  window.location.assign(target);
}

/** @deprecated Use {@link resetLoginRedirectLatch} */
export function __resetLoginRedirectLatchForTests(): void {
  resetLoginRedirectLatch();
}
