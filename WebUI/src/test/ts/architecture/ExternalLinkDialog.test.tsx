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
import { ExternalLinkDialog } from "../../../main/ts/architecture/ExternalLinkDialog";

describe("ExternalLinkDialog (#3097)", () => {
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

  it("validates required fields before submit", () => {
    const onSubmit = vi.fn();
    render(
      <ExternalLinkDialog
        open
        mode="create"
        parentTitle="Home"
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-external-link-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-external-link-error")).toBeTruthy();
  });

  it("submits valid create values", () => {
    const onSubmit = vi.fn();
    render(
      <ExternalLinkDialog
        open
        mode="create"
        parentTitle="Home"
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-external-link-text"), {
      target: { value: "Partner" },
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-url"), {
      target: { value: "https://example.com" },
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-target"), {
      target: { value: "_blank" },
    });
    fireEvent.click(screen.getByTestId("architecture-external-link-submit"));
    expect(onSubmit).toHaveBeenCalledWith({
      linkTitle: "Partner",
      externalUrl: "https://example.com",
      target: "_blank",
    });
  });

  it("seeds edit mode from initial values", () => {
    render(
      <ExternalLinkDialog
        open
        mode="edit"
        parentTitle="Home"
        busy={false}
        initial={{
          linkTitle: "Old",
          externalUrl: "https://old.test",
          target: "_top",
        }}
        onCancel={() => undefined}
        onSubmit={() => undefined}
      />,
    );
    expect(
      (screen.getByTestId("architecture-external-link-text") as HTMLInputElement)
        .value,
    ).toBe("Old");
    expect(
      (screen.getByTestId("architecture-external-link-url") as HTMLInputElement)
        .value,
    ).toBe("https://old.test");
    expect(
      (
        screen.getByTestId(
          "architecture-external-link-target",
        ) as HTMLSelectElement
      ).value,
    ).toBe("_top");
  });

  it("submits edited values in edit mode", () => {
    const onSubmit = vi.fn();
    render(
      <ExternalLinkDialog
        open
        mode="edit"
        parentTitle="Home"
        busy={false}
        initial={{
          linkTitle: "Old",
          externalUrl: "https://old.test",
          target: "_self",
        }}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-external-link-text"), {
      target: { value: "New Title" },
    });
    fireEvent.change(screen.getByTestId("architecture-external-link-url"), {
      target: { value: "https://new.example" },
    });
    fireEvent.click(screen.getByTestId("architecture-external-link-submit"));
    expect(onSubmit).toHaveBeenCalledWith({
      linkTitle: "New Title",
      externalUrl: "https://new.example",
      target: "_self",
    });
  });
});
