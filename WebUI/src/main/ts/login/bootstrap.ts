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

import {
  DEFAULT_SPA_ENTRY_REDIRECT,
  sanitizeLoginRedirect,
} from "./redirect";
import type { LoginBootstrap, SpaLandingBootstrap } from "./types";

const LOGIN_BOOTSTRAP_ID = "perc-login-bootstrap";
const SPA_BOOTSTRAP_ID = "perc-bootstrap";

function parseJsonScript(elementId: string): unknown {
  const el = document.getElementById(elementId);
  if (!el || !el.textContent) {
    return null;
  }
  try {
    return JSON.parse(el.textContent);
  } catch {
    console.error(`[PercModernUI] Failed to parse bootstrap JSON (#${elementId})`);
    return null;
  }
}

function resolveCsrf(
  rawName: string | undefined,
  rawValue: string | undefined,
): { name: string; value: string } {
  let name =
    typeof rawName === "string" && rawName.trim() ? rawName.trim() : "OWASP_CSRFTOKEN";
  let value = typeof rawValue === "string" ? rawValue : "";

  if (!value) {
    const holder = document.getElementById("perc-csrf-holder");
    if (holder) {
      const hName = holder.getAttribute("data-csrf-name");
      const hValue = holder.getAttribute("data-csrf-value");
      if (hName) name = hName;
      if (hValue) value = hValue;
    }
  }
  if (!value && typeof window !== "undefined") {
    const tok = window.OWASP_CSRFTOKEN?.token;
    if (tok) value = tok;
  }
  return { name, value };
}

/**
 * Reads login bootstrap from the host page JSON script tag.
 */
export function readLoginBootstrap(): LoginBootstrap {
  const raw = parseJsonScript(LOGIN_BOOTSTRAP_ID) as Partial<LoginBootstrap> | null;
  const locales = Array.isArray(raw?.locales)
    ? raw!.locales.filter(
        (l) =>
          l &&
          typeof l.name === "string" &&
          typeof l.displayName === "string",
      )
    : [{ name: "en-us", displayName: "English (United States)" }];

  const csrf = resolveCsrf(raw?.csrfTokenName, raw?.csrfTokenValue);

  return {
    locales,
    selectedLocale:
      typeof raw?.selectedLocale === "string" ? raw.selectedLocale : "en-us",
    username: typeof raw?.username === "string" ? raw.username : "",
    error: typeof raw?.error === "string" && raw.error.length > 0 ? raw.error : null,
    autocomplete: raw?.autocomplete === "off" ? "off" : "on",
    defaultRedirect: sanitizeLoginRedirect(
      raw?.defaultRedirect,
      DEFAULT_SPA_ENTRY_REDIRECT,
    ),
    csrfTokenName: csrf.name,
    csrfTokenValue: csrf.value,
    // Prefer relative "login" (same as classic rxlogin) so context-path deployments work.
    formAction:
      typeof raw?.formAction === "string" && raw.formAction
        ? raw.formAction
        : "login",
    localeFormat: parseLocaleFormat(raw?.localeFormat),
  };
}

function parseLocaleFormat(raw: unknown): LoginBootstrap["localeFormat"] {
  if (!raw || typeof raw !== "object") {
    return null;
  }
  const o = raw as Record<string, unknown>;
  const languageString =
    typeof o.languageString === "string" ? o.languageString : "";
  if (!languageString) {
    return null;
  }
  const out: NonNullable<LoginBootstrap["localeFormat"]> = { languageString };
  const str = (k: string): string | undefined =>
    typeof o[k] === "string" ? (o[k] as string) : undefined;
  const num = (k: string): number | undefined =>
    typeof o[k] === "number" ? (o[k] as number) : undefined;
  out.textDir = str("textDir");
  out.datePattern = str("datePattern");
  out.timePattern = str("timePattern");
  out.dateTimePattern = str("dateTimePattern");
  out.decimalSep = str("decimalSep");
  out.groupingSep = str("groupingSep");
  out.currencyCode = str("currencyCode");
  out.currencyPattern = str("currencyPattern");
  out.firstDayOfWeek = num("firstDayOfWeek");
  out.measurementSystem = str("measurementSystem");
  out.defaultTz = str("defaultTz");
  out.numberingSystem = str("numberingSystem");
  out.calendar = str("calendar");
  return out;
}

/**
 * Reads authenticated SPA landing bootstrap (optional).
 */
export function readSpaLandingBootstrap(): SpaLandingBootstrap {
  const raw = parseJsonScript(SPA_BOOTSTRAP_ID) as Partial<SpaLandingBootstrap> | null;
  return {
    userName: typeof raw?.userName === "string" ? raw.userName : undefined,
    locale: typeof raw?.locale === "string" ? raw.locale : undefined,
    entry: typeof raw?.entry === "string" ? raw.entry : "home",
  };
}
