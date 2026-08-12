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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ReplaceLandingPageDialog } from "../../../main/ts/architecture/ReplaceLandingPageDialog";

describe("ReplaceLandingPageDialog (#3097)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("disables submit until page id is provided when ContentBrowser is off", () => {
    const onSubmit = vi.fn();
    render(
      <ReplaceLandingPageDialog
        open
        siteName="Demo"
        sectionTitle="About"
        busy={false}
        useContentBrowser={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    const submit = screen.getByTestId(
      "architecture-landing-submit",
    ) as HTMLButtonElement;
    expect(submit.disabled).toBe(true);
    fireEvent.click(submit);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("submits page id from field mode", () => {
    const onSubmit = vi.fn();
    render(
      <ReplaceLandingPageDialog
        open
        siteName="Demo"
        sectionTitle="About"
        busy={false}
        useContentBrowser={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-landing-page-id"), {
      target: { value: "page-guid-1" },
    });
    fireEvent.click(screen.getByTestId("architecture-landing-submit"));
    expect(onSubmit).toHaveBeenCalledWith("page-guid-1");
  });
});
