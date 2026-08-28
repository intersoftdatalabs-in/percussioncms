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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { KeywordSummary } from "../../../../main/ts/api/developer/types";
import {
  catalogText,
  keywordChoicesForField,
  KeywordFieldWidget,
} from "../../../../main/ts/editor/widgets/KeywordFieldWidget";

const catalog: KeywordSummary[] = [
  {
    value: "keywords",
    label: "Keywords",
    choices: [
      { value: "news", label: "News" },
      { value: "events", label: "Events" },
    ],
  },
];

describe("catalogText", () => {
  it("stringifies numbers and nested objects", () => {
    expect(catalogText(42)).toBe("42");
    expect(catalogText({ value: 7 })).toBe("7");
    expect(catalogText({ label: "Keywords" })).toBe("Keywords");
    expect(catalogText(null)).toBe("");
  });
});

describe("keywordChoicesForField", () => {
  it("prefers the matching keyword set", () => {
    const opts = keywordChoicesForField(catalog, "keywords");
    expect(opts.map((o) => o.value)).toEqual(["news", "events"]);
  });

  it("coerces numeric catalog values without throwing and still matches/dedupes", () => {
    const numeric = [
      {
        value: 42,
        label: "Keywords",
        choices: [
          { value: 1, label: "News" },
          { value: 1, label: "News dup" },
          { value: 2, label: "Events" },
        ],
      },
    ] as unknown as KeywordSummary[];
    expect(() => keywordChoicesForField(numeric, "42")).not.toThrow();
    expect(() => keywordChoicesForField(numeric, "Keywords")).not.toThrow();
    expect(keywordChoicesForField(numeric, "42").map((o) => o.value)).toEqual([
      "1",
      "2",
    ]);
    expect(keywordChoicesForField(numeric, "Keywords").map((o) => o.value)).toEqual(
      ["1", "2"],
    );
  });

  it("coerces object catalog values without throwing and still matches/dedupes", () => {
    const objects = [
      {
        value: { value: "keywords" },
        label: { label: "Keywords" },
        choices: [
          { value: { value: "news" }, label: { label: "News" } },
          { value: { value: "news" }, label: { label: "News dup" } },
          { value: { value: "events" }, label: { label: "Events" } },
        ],
      },
    ] as unknown as KeywordSummary[];
    expect(() => keywordChoicesForField(objects, "keywords")).not.toThrow();
    const opts = keywordChoicesForField(objects, "keywords");
    expect(opts.map((o) => o.value)).toEqual(["news", "events"]);
    expect(opts.map((o) => o.label)).toEqual(["News", "Events"]);
  });
});

describe("KeywordFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders choices and reports the selected value", async () => {
    const onChange = vi.fn();
    render(
      <KeywordFieldWidget
        name="keywords"
        value=""
        readOnly={false}
        onChange={onChange}
        loadKeywords={async () => catalog}
      />,
    );
    await waitFor(() => {
      expect(screen.getByText("News")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-field-keywords"), {
      target: { value: "events" },
    });
    expect(onChange).toHaveBeenCalledWith("events");
  });

  it("renders a numeric selected value without crashing", async () => {
    const onChange = vi.fn();
    render(
      <KeywordFieldWidget
        name="keywords"
        value={7 as unknown as string}
        readOnly={false}
        onChange={onChange}
        loadKeywords={async () =>
          [
            {
              value: 7,
              label: "Keywords",
              choices: [{ value: 7, label: "Seven" }],
            },
          ] as unknown as typeof catalog
        }
      />,
    );
    await waitFor(() => {
      expect(screen.getByText("Seven")).toBeTruthy();
    });
    const select = screen.getByTestId("editor-field-keywords") as HTMLSelectElement;
    expect(select.value).toBe("7");
  });
});
