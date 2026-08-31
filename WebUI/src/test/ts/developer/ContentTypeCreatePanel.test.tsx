/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { ContentTypeCreatePanel } from "../../../main/ts/developer/ContentTypeCreatePanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypesApi")>();
  return {
    ...actual,
    createContentType: vi.fn(),
  };
});

const createContentType = contentTypesApi.createContentType as ReturnType<typeof vi.fn>;

describe("ContentTypeCreatePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    createContentType.mockReset();
  });

  it("disables save until the name is valid", () => {
    render(<ContentTypeCreatePanel onBack={() => undefined} />);
    const save = screen.getByTestId("developer-ct-create-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "bad name" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    expect(save.disabled).toBe(false);
  });

  it("does not POST an invalid name", () => {
    render(<ContentTypeCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "bad name" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    expect(createContentType).not.toHaveBeenCalled();
  });

  it("creates a type when the name is valid", async () => {
    createContentType.mockResolvedValue({
      name: "qaType",
      label: "QA",
      enabled: true,
    });
    const onCreated = vi.fn();
    render(<ContentTypeCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-create-label"), {
      target: { value: "QA" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    await waitFor(() => {
      expect(onCreated).toHaveBeenCalled();
    });
    expect(createContentType).toHaveBeenCalledWith(
      expect.objectContaining({ name: "qaType", label: "QA", enabled: true }),
    );
    expect(screen.getByTestId("developer-ct-create-notice").textContent).toBe(
      DEV_MSG.CT_CREATED,
    );
  });

  it("does not POST create twice when save is clicked twice", async () => {
    let resolveCreate: (value: { name: string }) => void = () => undefined;
    createContentType.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<ContentTypeCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    expect(createContentType).toHaveBeenCalledTimes(1);
    resolveCreate({ name: "qaType" });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-create-notice")).toBeTruthy();
    });
  });

  it("surfaces 400 invalid name from REST", async () => {
    createContentType.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "name cannot contain spaces" },
    });
    const onCreated = vi.fn();
    render(<ContentTypeCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-create-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-create-error").textContent).toContain(
      DEV_MSG.CT_INVALID_NAME,
    );
    expect(screen.getByTestId("developer-ct-create-error").textContent).toContain(
      "name cannot contain spaces",
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("surfaces 409 duplicate name", async () => {
    createContentType.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content type name already exists: percPage" },
    });
    const onCreated = vi.fn();
    render(<ContentTypeCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "percPage" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-create-error")).toBeTruthy();
    });
    expect(createContentType).toHaveBeenCalled();
    expect(screen.getByTestId("developer-ct-create-error").textContent).toContain(
      DEV_MSG.CT_DUPLICATE,
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("surfaces 403 non-Admin", async () => {
    createContentType.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ContentTypeCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-create-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-create-error").textContent).toContain(
      DEV_MSG.CT_FORBIDDEN,
    );
    expect(screen.getByTestId("developer-ct-create-error").textContent).toContain(
      "Admin role required",
    );
  });
});
