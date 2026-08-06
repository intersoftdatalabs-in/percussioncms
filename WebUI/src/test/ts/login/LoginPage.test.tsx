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

import { afterEach, describe, it, expect, vi } from "vitest";
import { act, render, screen, fireEvent } from "@testing-library/react";
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

  it("applies text direction from localeFormat bootstrap", () => {
    render(
      <LoginPage
        bootstrap={{
          ...baseBootstrap,
          selectedLocale: "ar",
          localeFormat: {
            languageString: "ar",
            textDir: "rtl",
            datePattern: "dd/MM/yyyy",
          },
        }}
      />,
    );
    expect(document.documentElement.dir).toBe("rtl");
    expect(screen.getByTestId("perc-login-page").getAttribute("data-text-dir")).toBe(
      "rtl",
    );
  });

  it("exposes data-i18n-key on localized pilot chrome", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-login-title").getAttribute("data-i18n-key")).toBe(
      "perc.ui.login.modern@Sign in",
    );
    expect(
      screen.getByTestId("perc-login-submit").getAttribute("data-i18n-key"),
    ).toBe("perc.ui.login.modern@Login");
    const usernameLabel = document.querySelector(
      'label[for="perc-login-username"]',
    );
    expect(usernameLabel?.getAttribute("data-i18n-key")).toBe(
      "perc.ui.login.modern@User name",
    );
  });

  it("renders front-door form fields and posts to /login", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);

    expect(screen.getByTestId("perc-login-page")).toBeDefined();
    expect(screen.getByTestId("perc-login-title").textContent).toMatch(
      /Sign in/i,
    );

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

    const redirect = screen.getByTestId(
      "perc-login-redirect",
    ) as HTMLInputElement;
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

  it("renders locale option labels as stable endonyms with SVG flags", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const trigger = screen.getByTestId("perc-login-locale");
    fireEvent.click(trigger);
    const list = screen.getByTestId("perc-login-locale-list");
    const text = list.textContent ?? "";
    // Endonyms (GH-1608) + SVG flags from country-flag-icons
    expect(text).toContain("en-us - English");
    expect(text).toContain("fr-fr - français");
    expect(text).toContain("es - español");
    expect(list.querySelectorAll("svg").length).toBeGreaterThanOrEqual(3);
    // Hidden field posts j_locale
    const hidden = screen.getByTestId(
      "perc-login-locale-value",
    ) as HTMLInputElement;
    expect(hidden.name).toBe("j_locale");
    expect(hidden.value).toBe("en-us");
  });

  it("keeps locale option labels stable when the UI locale changes (GH-1608)", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const trigger = screen.getByTestId("perc-login-locale");
    fireEvent.click(trigger);
    const before = screen.getByTestId("perc-login-locale-list").textContent;
    fireEvent.click(screen.getByTestId("perc-login-locale-option-fr-fr"));
    fireEvent.click(trigger);
    const after = screen.getByTestId("perc-login-locale-list").textContent;
    // Selecting French must not re-translate Spanish option into French.
    expect(after).toBe(before);
    expect(after).toContain("es - español");
    expect(after).toContain("fr-fr - français");
    expect(after).not.toMatch(/espagnol/);
  });

  it("keeps ship endonyms when Intl.DisplayNames is unavailable", () => {
    const original = (Intl as unknown as { DisplayNames?: unknown })
      .DisplayNames;
    // @ts-expect-error simulate runtime without DisplayNames
    delete Intl.DisplayNames;
    try {
      render(<LoginPage bootstrap={baseBootstrap} />);
      fireEvent.click(screen.getByTestId("perc-login-locale"));
      const text = screen.getByTestId("perc-login-locale-list").textContent ?? "";
      expect(text).toContain("en-us - English (United States)");
      expect(text).toContain("fr-fr - français (France)");
      expect(text).toContain("es - español");
    } finally {
      (Intl as unknown as { DisplayNames?: unknown }).DisplayNames = original;
    }
  });

  it("injects a TMX script tag on dropdown change", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    fireEvent.click(screen.getByTestId("perc-login-locale"));
    fireEvent.click(screen.getByTestId("perc-login-locale-option-fr-fr"));
    const tag = document.querySelector(
      'script[data-perc-tmx-locale="fr-fr"]',
    ) as HTMLScriptElement | null;
    expect(tag).not.toBeNull();
    expect(tag!.src).toContain("sys_lang=fr-fr");
    expect(tag!.src).toContain("prefix=perc.ui.");
    expect(tag!.src).toContain("mode=js");
    const hidden = screen.getByTestId(
      "perc-login-locale-value",
    ) as HTMLInputElement;
    expect(hidden.value).toBe("fr-fr");
  });
  it("resolves chrome via stubbed window.I18N when present", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
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

  it("re-reads I18N chrome after locale change once TMX load resolves (GH-1609)", async () => {
    // Start with English fallbacks (no I18N). After dropdown change, simulate
    // the loaded Hindi TMX by installing window.I18N before the script load
    // handler fires — ensureTmxLoaded resolves on the script load event.
    delete (window as { I18N?: unknown }).I18N;
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-login-title").textContent).toMatch(
      /Sign in/i,
    );

    await act(async () => {
      fireEvent.click(screen.getByTestId("perc-login-locale"));
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("perc-login-locale-option-fr-fr"));
    });

    // Install translated catalog and fire the injected script's load event.
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => {
        const map: Record<string, string> = {
          "perc.ui.login.modern@Sign in": "Iniciar sesión",
          "perc.ui.login.modern@User name": "Nombre de usuario",
          "perc.ui.login.modern@Password": "Contraseña",
          "perc.ui.login.modern@Locale": "Idioma",
          "perc.ui.login.modern@Login": "Iniciar sesión",
        };
        return map[k] ?? k;
      },
    };
    await vi.waitFor(() => {
      expect(
        document.querySelector('script[data-perc-tmx-locale="fr-fr"]'),
      ).not.toBeNull();
    });
    const tag = document.querySelector(
      'script[data-perc-tmx-locale="fr-fr"]',
    ) as HTMLScriptElement;
    await act(async () => {
      tag.dispatchEvent(new Event("load"));
    });

    // Wait for setTmxReady re-render after ensureTmxLoaded resolves.
    await vi.waitFor(() => {
      expect(screen.getByTestId("perc-login-title").textContent).toBe(
        "Iniciar sesión",
      );
    });
    expect(screen.getByTestId("perc-login-submit").textContent).toBe(
      "Iniciar sesión",
    );
    expect(document.documentElement.lang).toBe("fr-fr");
    expect(document.title).toContain("Iniciar sesión");
  });

  it("preserves CSRF hidden inputs and username across dropdown change", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const username = screen.getByTestId(
      "perc-login-username",
    ) as HTMLInputElement;
    fireEvent.change(username, { target: { value: "admin" } });
    const csrf = screen.getByTestId("perc-login-csrf") as HTMLInputElement;

    fireEvent.click(screen.getByTestId("perc-login-locale"));
    fireEvent.click(screen.getByTestId("perc-login-locale-option-fr-fr"));

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
