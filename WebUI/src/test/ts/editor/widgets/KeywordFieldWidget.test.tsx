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
import {
  keywordChoicesForField,
  KeywordFieldWidget,
} from "../../../../main/ts/editor/widgets/KeywordFieldWidget";

const catalog = [
  {
    value: "keywords",
    label: "Keywords",
    choices: [
      { value: "news", label: "News" },
      { value: "events", label: "Events" },
    ],
  },
];

describe("keywordChoicesForField", () => {
  it("prefers the matching keyword set", () => {
    const opts = keywordChoicesForField(catalog, "keywords");
    expect(opts.map((o) => o.value)).toEqual(["news", "events"]);
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
});
