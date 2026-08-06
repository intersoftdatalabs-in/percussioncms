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

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { I18nText } from "@/i18n/I18nText";

describe("I18nText", () => {
  it("renders fallback label and data-i18n-key", () => {
    render(
      <I18nText
        as="h1"
        msgKey="perc.ui.home.modern@Home"
        data-testid="i18n-text-home"
      />,
    );
    const el = screen.getByTestId("i18n-text-home");
    expect(el.tagName).toBe("H1");
    expect(el.getAttribute("data-i18n-key")).toBe("perc.ui.home.modern@Home");
    expect(el.classList.contains("mkd-lang-target")).toBe(true);
    expect(el.textContent).toMatch(/Home/i);
  });
});
