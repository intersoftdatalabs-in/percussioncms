/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as controlsApi from "../../../main/ts/api/developer/controlsApi";
import { ControlCreatePanel } from "../../../main/ts/developer/ControlCreatePanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/controlsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/controlsApi")>();
  return {
    ...actual,
    createControl: vi.fn(),
  };
});

const createControl = controlsApi.createControl as ReturnType<typeof vi.fn>;

describe("ControlCreatePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    createControl.mockReset();
  });

  it("disables save until the name is valid", () => {
    render(<ControlCreatePanel onBack={() => undefined} />);
    const save = screen.getByTestId("developer-ctl-create-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "bad name" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    expect(save.disabled).toBe(false);
  });

  it("does not POST an invalid name", () => {
    render(<ControlCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "bad name" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    expect(createControl).not.toHaveBeenCalled();
  });

    it("disables save for wildcard names", () => {
      render(<ControlCreatePanel onBack={() => undefined} />);
      const save = screen.getByTestId("developer-ctl-create-save") as HTMLButtonElement;
      fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
        target: { value: "qa*" },
      });
      expect(save.disabled).toBe(true);
      fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
        target: { value: "qa%" },
      });
      expect(save.disabled).toBe(true);
      fireEvent.click(save);
      expect(createControl).not.toHaveBeenCalled();
    });
  it("creates a user control when the name is valid", async () => {
    createControl.mockResolvedValue({
      name: "qaCtl",
      displayName: "QA",
      scope: "user",
    });
    const onCreated = vi.fn();
    render(<ControlCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-create-display"), {
      target: { value: "QA" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(onCreated).toHaveBeenCalled();
    });
    expect(createControl).toHaveBeenCalledWith(
      expect.objectContaining({ name: "qaCtl", displayName: "QA" }),
    );
    expect(screen.getByTestId("developer-ctl-create-notice").textContent).toBe(
      DEV_MSG.CTL_CREATED,
    );
    expect((screen.getByTestId("developer-ctl-create-name") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("omits xslSource when the optional field is blank", async () => {
    createControl.mockResolvedValue({ name: "qaCtl", scope: "user" });
    render(<ControlCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(createControl).toHaveBeenCalled();
    });
    expect(createControl.mock.calls[0][0].xslSource).toBeUndefined();
    expect(createControl.mock.calls[0][0].description).toBeUndefined();
  });

  it("sends xslSource when provided", async () => {
    createControl.mockResolvedValue({ name: "qaCtl", scope: "user" });
    render(<ControlCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.change(screen.getByTestId("developer-ctl-create-xsl"), {
      target: { value: "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(createControl).toHaveBeenCalled();
    });
    expect(createControl.mock.calls[0][0].xslSource).toContain("xsl:stylesheet");
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: { name: string }) => void = () => undefined;
    createControl.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<ControlCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    expect(createControl).toHaveBeenCalledTimes(1);
    resolveCreate({ name: "qaCtl" });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-create-notice")).toBeTruthy();
    });
  });

  it("surfaces 400 invalid name from REST", async () => {
    createControl.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain whitespace" },
    });
    const onCreated = vi.fn();
    render(<ControlCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-create-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-create-error").textContent).toContain(
      DEV_MSG.CTL_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-ctl-create-error").textContent).toContain(
      "name cannot contain whitespace",
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name", async () => {
    createControl.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "A control with that name already exists" },
    });
    const onCreated = vi.fn();
    render(<ControlCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "sys_EditBox" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-create-error")).toBeTruthy();
    });
    expect(createControl).toHaveBeenCalled();
    expect(screen.getByTestId("developer-ctl-create-error").textContent).toContain(
      DEV_MSG.CTL_DUPLICATE,
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin", async () => {
    createControl.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ControlCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ctl-create-name"), {
      target: { value: "qaCtl" },
    });
    fireEvent.click(screen.getByTestId("developer-ctl-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ctl-create-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ctl-create-error").textContent).toContain(
      DEV_MSG.CTL_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-ctl-create-error").textContent).toContain(
      "Admin role required",
    );
  });
});
