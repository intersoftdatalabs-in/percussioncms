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

import { fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { describe, expect, it, vi } from "vitest";
import { DateFieldWidget } from "../../../../main/ts/editor/widgets/DateFieldWidget";

describe("DateFieldWidget", () => {
  it("emits CMS date text when the native date input changes", () => {
    const onChange = vi.fn();
    render(
      <DateFieldWidget
        name="sys_contentstartdate"
        value="2026-01-01"
        kind="date"
        readOnly={false}
        onChange={onChange}
      />,
    );
    const input = screen.getByTestId("editor-field-sys_contentstartdate") as HTMLInputElement;
    expect(input.type).toBe("date");
    fireEvent.change(input, { target: { value: "2026-09-18" } });
    expect(onChange).toHaveBeenCalledWith("2026-09-18");
  });
});
