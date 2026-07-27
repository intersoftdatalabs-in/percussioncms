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

import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { LoginPage } from "@/login/LoginPage";
import type { LoginBootstrap } from "@/login/types";

const baseBootstrap: LoginBootstrap = {
  locales: [
    { name: "en-us", displayName: "English (United States)" },
    { name: "fr-fr", displayName: "French (France)" },
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
});
