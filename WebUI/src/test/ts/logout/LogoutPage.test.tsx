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
import { render, screen } from "@testing-library/react";
import { LogoutPage } from "@/logout/LogoutPage";
import type { LogoutBootstrap } from "@/logout/types";

const baseBootstrap: LogoutBootstrap = {
  locale: "en-us",
  loginHref: "login",
};

describe("LogoutPage", () => {
  it("renders modern logout chrome matching login card structure", () => {
    render(<LogoutPage bootstrap={baseBootstrap} />);

    expect(screen.getByTestId("perc-logout-page")).toBeDefined();
    expect(screen.getByTestId("perc-logout-title").textContent).toMatch(
      /Signed out/i,
    );
    expect(screen.getByTestId("perc-logout-message").textContent).toMatch(
      /logged out/i,
    );
    expect(screen.getByTestId("perc-brand-bar")).toBeDefined();
    expect(screen.getByTestId("perc-brand-footer")).toBeDefined();
    expect(screen.getByTestId("perc-logout-logo")).toBeDefined();
  });

  it("links Sign in again to the allowlisted login href", () => {
    render(
      <LogoutPage bootstrap={{ locale: "en-us", loginHref: "/rxlogin.jsp" }} />,
    );
    const link = screen.getByTestId("perc-logout-sign-in") as HTMLAnchorElement;
    expect(link.getAttribute("href")).toBe("/rxlogin.jsp");
    expect(link.textContent).toMatch(/Sign in again/i);
  });


  it("exposes data-i18n-key on localized pilot chrome", () => {
    render(<LogoutPage bootstrap={baseBootstrap} />);
    expect(screen.getByTestId("perc-logout-title").getAttribute("data-i18n-key")).toBe(
      "perc.ui.logout.modern@Signed out",
    );
    expect(
      screen.getByTestId("perc-logout-message").getAttribute("data-i18n-key"),
    ).toBe("perc.ui.logout.modern@You have been logged out.");
    expect(
      screen.getByTestId("perc-logout-sign-in").getAttribute("data-i18n-key"),
    ).toBe("perc.ui.logout.modern@Sign in again");
  });
  it("does not render jQuery legacy markup or form post", () => {
    render(<LogoutPage bootstrap={baseBootstrap} />);
    expect(document.querySelector("#loginform")).toBeNull();
    expect(document.querySelector("table.perc-form")).toBeNull();
  });
});
