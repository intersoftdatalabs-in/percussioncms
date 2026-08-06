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

import { afterEach, describe, expect, it } from "vitest";
import {
  DEFAULT_LOGIN_HREF,
  loginHrefWithLocale,
  readLogoutBootstrap,
  sanitizeLoginHref,
} from "@/logout/bootstrap";

describe("sanitizeLoginHref", () => {
  it("accepts relative login front doors", () => {
    expect(sanitizeLoginHref("login")).toBe("login");
    expect(sanitizeLoginHref("rxlogin.jsp")).toBe("rxlogin.jsp");
    expect(sanitizeLoginHref("login?return=/cm/app/spa.jsp?entry=home")).toBe(
      "login?return=/cm/app/spa.jsp?entry=home",
    );
  });

  it("accepts path-absolute product paths", () => {
    expect(sanitizeLoginHref("/login")).toBe("/login");
    expect(sanitizeLoginHref("/rxlogin.jsp")).toBe("/rxlogin.jsp");
    expect(sanitizeLoginHref("/cm/app/spa.jsp?entry=home")).toBe(
      "/cm/app/spa.jsp?entry=home",
    );
  });

  it("rejects open redirects and traversal", () => {
    expect(sanitizeLoginHref("https://evil.example/phish")).toBe(
      DEFAULT_LOGIN_HREF,
    );
    expect(sanitizeLoginHref("//evil.example/phish")).toBe(DEFAULT_LOGIN_HREF);
    expect(sanitizeLoginHref("javascript:alert(1)")).toBe(DEFAULT_LOGIN_HREF);
    expect(sanitizeLoginHref("../etc/passwd")).toBe(DEFAULT_LOGIN_HREF);
    expect(sanitizeLoginHref("/admin/secret")).toBe(DEFAULT_LOGIN_HREF);
    expect(sanitizeLoginHref("")).toBe(DEFAULT_LOGIN_HREF);
    expect(sanitizeLoginHref(null)).toBe(DEFAULT_LOGIN_HREF);
  });
});

describe("readLogoutBootstrap", () => {
  afterEach(() => {
    document.getElementById("perc-logout-bootstrap")?.remove();
  });

  it("returns defaults when bootstrap is missing", () => {
    const boot = readLogoutBootstrap();
    expect(boot.locale).toBe("en-us");
    expect(boot.loginHref).toBe(`${DEFAULT_LOGIN_HREF}?j_locale=en-us`);
  });

  it("reads and sanitizes host JSON", () => {
    const el = document.createElement("script");
    el.id = "perc-logout-bootstrap";
    el.type = "application/json";
    el.textContent = JSON.stringify({
      locale: "fr-fr",
      loginHref: "/rxlogin.jsp",
    });
    document.body.appendChild(el);

    const boot = readLogoutBootstrap();
    expect(boot.locale).toBe("fr-fr");
    expect(boot.loginHref).toBe("/rxlogin.jsp?j_locale=fr-fr");
  });

  it("falls back when loginHref is hostile", () => {
    const el = document.createElement("script");
    el.id = "perc-logout-bootstrap";
    el.type = "application/json";
    el.textContent = JSON.stringify({
      locale: "en-us",
      loginHref: "https://evil.example/",
    });
    document.body.appendChild(el);

    // Hostile href → DEFAULT_LOGIN_HREF, then j_locale appended from bootstrap locale
    expect(readLogoutBootstrap().loginHref).toBe(
      `${DEFAULT_LOGIN_HREF}?j_locale=en-us`,
    );
  });

  it("appends j_locale from bootstrap when host omitted it", () => {
    const el = document.createElement("script");
    el.id = "perc-logout-bootstrap";
    el.type = "application/json";
    el.textContent = JSON.stringify({
      locale: "fr-fr",
      loginHref: "login",
    });
    document.body.appendChild(el);

    expect(readLogoutBootstrap().loginHref).toBe("login?j_locale=fr-fr");
  });

  it("preserves j_locale already present on host href", () => {
    const el = document.createElement("script");
    el.id = "perc-logout-bootstrap";
    el.type = "application/json";
    el.textContent = JSON.stringify({
      locale: "fr-fr",
      loginHref: "login?j_locale=fr-fr",
    });
    document.body.appendChild(el);

    expect(readLogoutBootstrap().loginHref).toBe("login?j_locale=fr-fr");
  });
});

describe("loginHrefWithLocale", () => {
  it("appends j_locale with ? or & as needed", () => {
    expect(loginHrefWithLocale("login", "es-es")).toBe("login?j_locale=es-es");
    expect(loginHrefWithLocale("login?return=/cm/app", "de-de")).toBe(
      "login?return=/cm/app&j_locale=de-de",
    );
    expect(loginHrefWithLocale("/rxlogin.jsp", "hi-in")).toBe(
      "/rxlogin.jsp?j_locale=hi-in",
    );
  });

  it("does not double-append j_locale", () => {
    expect(loginHrefWithLocale("login?j_locale=fr-fr", "es-es")).toBe(
      "login?j_locale=fr-fr",
    );
  });
});
