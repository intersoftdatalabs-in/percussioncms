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

  it("emits CMS datetime text with the time when datetime-local changes", () => {
    const onChange = vi.fn();
    render(
      <DateFieldWidget
        name="event_at"
        value="2026-01-01 09:00:00"
        kind="datetime"
        readOnly={false}
        onChange={onChange}
      />,
    );
    const input = screen.getByTestId("editor-field-event_at") as HTMLInputElement;
    expect(input.getAttribute("data-editor-kind")).toBe("datetime");
    expect(input.type).toBe("datetime-local");
    expect(input.value).toBe("2026-01-01T09:00");
    fireEvent.change(input, { target: { value: "2026-09-18T14:30" } });
    expect(onChange).toHaveBeenCalledWith("2026-09-18 14:30:00");
  });

  it("clears a date and a datetime and hides Clear date when empty or read-only", () => {
    const onChange = vi.fn();
    const view = render(
      <DateFieldWidget
        name="sys_contentstartdate"
        value="2026-01-01"
        kind="date"
        readOnly={false}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByTestId("editor-date-clear-sys_contentstartdate"));
    expect(onChange).toHaveBeenCalledWith("");
    view.unmount();

    const datetime = render(
      <DateFieldWidget
        name="event_at"
        value="2026-01-01 09:00:00"
        kind="datetime"
        readOnly={false}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByTestId("editor-date-clear-event_at"));
    expect(onChange).toHaveBeenLastCalledWith("");
    datetime.unmount();

    const empty = render(
      <DateFieldWidget
        name="event_at"
        value=""
        kind="datetime"
        readOnly={false}
        onChange={onChange}
      />,
    );
    expect(screen.queryByTestId("editor-date-clear-event_at")).toBeNull();
    empty.unmount();

    const readOnlyDatetime = render(
      <DateFieldWidget
        name="event_at"
        value="2026-01-01 09:00:00"
        kind="datetime"
        readOnly
        onChange={onChange}
      />,
    );
    expect(screen.queryByTestId("editor-date-clear-event_at")).toBeNull();
    expect((screen.getByTestId("editor-field-event_at") as HTMLInputElement).disabled).toBe(true);
    readOnlyDatetime.unmount();

    render(
      <DateFieldWidget
        name="sys_contentstartdate"
        value="2026-01-01"
        kind="date"
        readOnly
        onChange={onChange}
      />,
    );
    expect(screen.queryByTestId("editor-date-clear-sys_contentstartdate")).toBeNull();
  });
});
