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

/**
 * Reads the OWASP CSRFGuard token injected by {@code /JavaScriptServlet}.
 *
 * <p>CSRFGuard injects a global script that sets a token name and value on the
 * page. This module reads those values so the API client can attach the token
 * header to every request.</p>
 */

interface CsrfToken {
  headerName: string;
  token: string;
}

declare global {
  interface Window {
    OWASP_CSRFTOKEN?: { token: string };
  }
}

const DEFAULT_HEADER_NAME = "OWASP-CSRFTOKEN";

/**
 * Retrieves the current CSRF token from CSRFGuard globals or host meta tags.
 *
 * <p>{@code spa.jsp} / login inject {@code meta[name=_csrf]} and
 * {@code meta[name=_csrf_header]} as a reliable fallback when
 * {@code window.OWASP_CSRFTOKEN} is not yet populated.</p>
 *
 * @returns the token header name and value, or {@code null} if unavailable
 */
export function getCsrfToken(): CsrfToken | null {
  const tokenObj = window.OWASP_CSRFTOKEN;
  if (tokenObj?.token) {
    return { headerName: DEFAULT_HEADER_NAME, token: tokenObj.token };
  }
  if (typeof document !== "undefined") {
    const metaToken = document
      .querySelector('meta[name="_csrf"]')
      ?.getAttribute("content");
    const metaHeader = document
      .querySelector('meta[name="_csrf_header"]')
      ?.getAttribute("content");
    if (metaToken && metaToken.trim()) {
      return {
        headerName: metaHeader?.trim() || DEFAULT_HEADER_NAME,
        token: metaToken.trim(),
      };
    }
  }
  return null;
}
