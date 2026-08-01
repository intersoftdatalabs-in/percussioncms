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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { LocaleSelect } from "@/login/LocaleSelect";

const locales = [
  { name: "en-us", displayName: "English (United States)" },
  { name: "fr-fr", displayName: "French (France)" },
  { name: "de-de", displayName: "German (Germany)" },
];

describe("LocaleSelect", () => {
  it("posts value via hidden j_locale and opens SVG-flagged options", () => {
    const onChange = vi.fn();
    render(
      <LocaleSelect
        id="loc"
        locales={locales}
        value="en-us"
        onChange={onChange}
      />,
    );

    const hidden = screen.getByTestId("perc-login-locale-value") as HTMLInputElement;
    expect(hidden.name).toBe("j_locale");
    expect(hidden.value).toBe("en-us");

    fireEvent.click(screen.getByTestId("perc-login-locale"));
    const list = screen.getByTestId("perc-login-locale-list");
    expect(list.getAttribute("role")).toBe("listbox");
    expect(list.querySelectorAll("svg").length).toBe(3);

    fireEvent.click(screen.getByTestId("perc-login-locale-option-fr-fr"));
    expect(onChange).toHaveBeenCalledWith("fr-fr");
  });

  it("supports keyboard open and select", () => {
    const onChange = vi.fn();
    render(
      <LocaleSelect
        id="loc"
        locales={locales}
        value="en-us"
        onChange={onChange}
      />,
    );
    const trigger = screen.getByTestId("perc-login-locale");
    fireEvent.keyDown(trigger, { key: "ArrowDown" });
    expect(screen.getByTestId("perc-login-locale-list")).toBeDefined();
    fireEvent.keyDown(trigger, { key: "ArrowDown" }); // highlight fr-fr
    fireEvent.keyDown(trigger, { key: "Enter" });
    expect(onChange).toHaveBeenCalledWith("fr-fr");
  });
});
