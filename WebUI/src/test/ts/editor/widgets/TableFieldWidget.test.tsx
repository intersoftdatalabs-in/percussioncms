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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TableFieldWidget } from "../../../../main/ts/editor/widgets/TableFieldWidget";

describe("TableFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("adds a row and writes the cell through onChange", () => {
    const onChange = vi.fn();
    render(
      <TableFieldWidget
        name="hours"
        value=""
        readOnly={false}
        onChange={onChange}
      />,
    );
    expect(screen.queryByTestId("editor-table-cell-hours-0-0")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-table-add-hours"));
    expect(onChange).toHaveBeenCalledWith(
      JSON.stringify({ columns: ["value"], rows: [[""]] }),
    );
  });

  it("hides add and remove in read-only mode", () => {
    render(
      <TableFieldWidget
        name="hours"
        value='{"columns":["day"],"rows":[["Mon"]]}'
        readOnly
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByTestId("editor-table-cell-hours-0-0")).toHaveProperty(
      "readOnly",
      true,
    );
    expect(screen.queryByTestId("editor-table-add-hours")).toBeNull();
    expect(screen.queryByTestId("editor-table-remove-hours-0")).toBeNull();
  });
});
