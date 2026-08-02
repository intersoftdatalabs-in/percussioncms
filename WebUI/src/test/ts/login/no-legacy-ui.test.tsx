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

import { afterEach, describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
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

describe("LoginPage legacy UI removal (GH-1688)", () => {
  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
    document
      .querySelectorAll("script[data-perc-tmx-locale]")
      .forEach((s) => s.remove());
    __resetTmxLoaderCache();
  });

  it("does not render the legacy UI checkbox", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(screen.queryByTestId("perc-login-select-ui")).toBeNull();
  });

  it("does not render an input named j_selectUI", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    const form = screen.getByTestId("perc-login-form") as HTMLFormElement;
    expect(form.querySelector('input[name="j_selectUI"]')).toBeNull();
  });

  it("does not write legacy UI preference to localStorage", () => {
    render(<LoginPage bootstrap={baseBootstrap} />);
    expect(
      localStorage.getItem("perc-login-select-ui-checked"),
    ).toBeNull();
  });
});
