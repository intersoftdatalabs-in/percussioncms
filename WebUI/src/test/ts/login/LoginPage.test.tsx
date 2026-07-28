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

import { afterEach, describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { LoginPage } from "@/login/LoginPage";
import { __resetTmxLoaderCache } from "@/login/tmxLoader";
import type { LoginBootstrap } from "@/login/types";

const baseBootstrap: LoginBootstrap = {
  locales: [
    { name: "en-us", displayName: "English (United States)" },
    { name: "fr-fr", displayName: "French (France)" },
    { name: "es", displayName: "Spanish" },
  ],
  selectedLocale: "en-us",
  username: "",
  error: null,
  autocomplete: "on",
  defaultRedirect: "/cm/app/spa.jsp?entry=home",
  csrfTokenName: "OWASP_CSRFTOKEN",
  csrfTokenValue: "test-csrf-token",
  formAction: "login",
};

describe("LoginPage", () => {
  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
    document
      .querySelectorAll("script[data-perc-tmx-locale]")
      .forEach((s) => s.remove());
    __resetTmxLoaderCache();
  });

  it("renders front-door form fields and posts to /login", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);

    expect(screen.getByTestId("perc-login-page")).toBeDefined();
    expect(screen.getByTestId("perc-login-title").textContent).toMatch(/Sign in/i);

    const form = screen.getByTestId("perc-login-form") as HTMLFormElement;
    expect(form.method.toLowerCase()).toBe("post");
    // jsdom resolves relative action against the document URL
    expect(form.getAttribute("action")).toBe("login");
    expect(form.enctype).toMatch(/multipart\/form-data/i);

    expect(screen.getByTestId("perc-login-username")).toBeDefined();
    expect(screen.getByTestId("perc-login-password")).toBeDefined();
    expect(screen.getByTestId("perc-login-locale")).toBeDefined();
    expect(screen.getByTestId("perc-login-submit")).toBeDefined();

    const csrf = screen.getByTestId("perc-login-csrf") as HTMLInputElement;
    expect(csrf.name).toBe("OWASP_CSRFTOKEN");
    expect(csrf.value).toBe("test-csrf-token");

    const redirect = screen.getByTestId("perc-login-redirect") as HTMLInputElement;
    expect(redirect.name).toBe("sys_redirect");
    expect(redirect.value).toBe("/cm/app/spa.jsp?entry=home");
  });

  it("shows server error without interpreting HTML", () => {
    render(
      <LoginPage
        bootstrap={{
          ...baseBootstrap,
          error: "<script>alert(1)</script> bad credentials",
        }}
      />,
    );
    const err = screen.getByTestId("perc-login-error");
    // React text content escapes — no script child
    expect(err.textContent).toContain("bad credentials");
    expect(err.querySelector("script")).toBeNull();
  });

  it("updates username field state", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const input = screen.getByTestId("perc-login-username") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "admin" } });
    expect(input.value).toBe("admin");
  });

  it("renders Intersoft brand chrome", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-brand-bar")).toBeDefined();
    expect(screen.getByTestId("perc-brand-footer")).toBeDefined();
  });

  it("renders locale option labels in the selected viewer's native language", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const select = screen.getByTestId("perc-login-locale") as HTMLSelectElement;
    const options = Array.from(select.options).map((o) => o.textContent ?? "");
    // English viewer sees English names; fr-fr label is regional so includes region.
    expect(options.some((t) => /English/.test(t))).toBe(true);
    expect(options.some((t) => /French/.test(t))).toBe(true);
    expect(options.some((t) => /^es - Spanish/.test(t))).toBe(true);
    expect(options.some((t) => /^fr-fr - French/.test(t))).toBe(true);
  });

  it("re-renders option labels in the new viewer's language on change", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const select = screen.getByTestId("perc-login-locale") as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "fr-fr" } });
    const options = Array.from(select.options).map((o) => o.textContent ?? "");
    // French viewer now sees French native names.
    expect(options.some((t) => /^fr-fr - français/.test(t))).toBe(true);
    expect(options.some((t) => /^es - espagnol/.test(t))).toBe(true);
  });

  it("falls back to server displayName when Intl.DisplayNames is unavailable", () => {
    const original = (Intl as unknown as { DisplayNames?: unknown })
      .DisplayNames;
    // @ts-expect-error simulate runtime without DisplayNames
    delete Intl.DisplayNames;
    try {
      render(<LoginPage bootstrap={baseBootstrap} />);
      const select = screen.getByTestId(
        "perc-login-locale",
      ) as HTMLSelectElement;
      const options = Array.from(select.options).map((o) => o.textContent ?? "");
      expect(options).toContain("en-us - English (United States)");
      expect(options).toContain("fr-fr - French (France)");
      expect(options).toContain("es - Spanish");
    } finally {
      (Intl as unknown as { DisplayNames?: unknown }).DisplayNames = original;
    }
  });

  it("injects a TMX script tag on dropdown change", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const select = screen.getByTestId("perc-login-locale") as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "fr-fr" } });
    const tag = document.querySelector(
      'script[data-perc-tmx-locale="fr-fr"]',
    ) as HTMLScriptElement | null;
    expect(tag).not.toBeNull();
    expect(tag!.src).toContain("sys_lang=fr-fr");
    expect(tag!.src).toContain("prefix=perc.ui.");
    expect(tag!.src).toContain("mode=js");
  });

  it("resolves chrome via stubbed window.I18N when present", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N =
      {
        message: (k: string) => {
          if (k === "perc.ui.login.modern@Sign in") return "Connexion";
          if (k === "perc.ui.login.modern@User name") return "Nom d'utilisateur";
          if (k === "perc.ui.login.modern@Password") return "Mot de passe";
          if (k === "perc.ui.login.modern@Login") return "Se connecter";
          return k;
        },
      };
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-login-title").textContent).toBe(
      "Connexion",
    );
    expect(screen.getByTestId("perc-login-submit").textContent).toBe(
      "Se connecter",
    );
  });

  it("preserves CSRF hidden inputs and username across dropdown change", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const username = screen.getByTestId(
      "perc-login-username",
    ) as HTMLInputElement;
    fireEvent.change(username, { target: { value: "admin" } });
    const csrf = screen.getByTestId("perc-login-csrf") as HTMLInputElement;

    const select = screen.getByTestId("perc-login-locale") as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "fr-fr" } });

    expect(username.value).toBe("admin");
    expect(csrf.value).toBe("test-csrf-token");
  });

  it("keeps the existing toMatch(/Sign in/i) assertion via fallback when I18N absent", () => {
    // Sanity check: chrome always shows something resembling "Sign in" even
    // when window.I18N is undefined, because t() falls back to the @-suffix.
    delete (window as { I18N?: unknown }).I18N;
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-login-title").textContent).toMatch(
      /Sign in/i,
    );
  });

  it("does not introduce jQuery (product lock #2)", () => {
    const jquerySpy = vi.fn();
    (window as unknown as { jQuery?: unknown }).jQuery = jquerySpy;
    (window as unknown as { $?: unknown }).$ = jquerySpy;
    try {
      render(<LoginPage bootstrap={baseBootstrap} />);
      expect(jquerySpy).not.toHaveBeenCalled();
      expect((window as { jQuery?: unknown }).jQuery).toBe(jquerySpy);
    } finally {
      delete (window as { jQuery?: unknown }).jQuery;
      delete (window as { $?: unknown }).$;
    }
  });
});
