/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as importExportApi from "../../../main/ts/api/developer/templateImportExport";
import { TemplateImportWizard } from "../../../main/ts/developer/TemplateImportWizard";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/templateImportExport", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/templateImportExport")>();
  return {
    ...actual,
    importTemplate: vi.fn(),
  };
});

const importTemplate = importExportApi.importTemplate as ReturnType<typeof vi.fn>;

const SAMPLE_XML =
  "<assembly-template>" +
  "<name>imported.one</name>" +
  "<label>Imported One</label>" +
  "</assembly-template>";

function xmlFile(text: string, name = "imported.one.xml"): File {
  return new File([text], name, { type: "application/xml" });
}

describe("TemplateImportWizard", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    importTemplate.mockReset();
  });

  it("imports unique name and notifies parent", async () => {
    importTemplate.mockResolvedValue({ name: "as08unique", label: "Imported One" });
    const onImported = vi.fn();
    render(<TemplateImportWizard onImported={onImported} />);
    const fileInput = screen.getByTestId("developer-tpl-import-file") as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [xmlFile(SAMPLE_XML)] } });
    await waitFor(() => {
      expect((screen.getByTestId("developer-tpl-import-name") as HTMLInputElement).value).toBe(
        "imported.one",
      );
    });
    fireEvent.change(screen.getByTestId("developer-tpl-import-name"), {
      target: { value: "as08unique" },
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(importTemplate).toHaveBeenCalledTimes(1);
    });
    const posted = String(importTemplate.mock.calls[0][0]);
    expect(posted).toContain("as08unique");
    expect(screen.getByTestId("developer-tpl-import-notice").textContent).toBe(DEV_MSG.TPL_IMPORTED);
    expect(onImported).toHaveBeenCalledWith({ name: "as08unique", label: "Imported One" });
  });

  it("surfaces 400 invalid XML", async () => {
    importTemplate.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "invalid assembly-template XML" },
    });
    render(<TemplateImportWizard />);
    fireEvent.change(screen.getByTestId("developer-tpl-import-file"), {
      target: { files: [xmlFile("<not-xml")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-filename")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain(
        DEV_MSG.TPL_IMPORT_INVALID,
      );
    });
    expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain(
      "invalid assembly-template XML",
    );
    expect(importTemplate).toHaveBeenCalledTimes(1);
  });

  it("surfaces 409 duplicate name", async () => {
    importTemplate.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Template already exists: perc.page" },
    });
    render(<TemplateImportWizard />);
    fireEvent.change(screen.getByTestId("developer-tpl-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-tpl-import-name") as HTMLInputElement).value).toBe(
        "imported.one",
      );
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain(
        DEV_MSG.TPL_IMPORT_DUPLICATE,
      );
    });
    expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain("already exists");
  });

  it("surfaces 403 non-Admin", async () => {
    importTemplate.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Forbidden" },
    });
    render(<TemplateImportWizard />);
    fireEvent.change(screen.getByTestId("developer-tpl-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-filename")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain(
        DEV_MSG.TPL_IMPORT_FORBIDDEN,
      );
    });
  });

  it("surfaces missing-name rewrite as invalid XML", async () => {
    render(<TemplateImportWizard />);
    fireEvent.change(screen.getByTestId("developer-tpl-import-file"), {
      target: { files: [xmlFile("<assembly-template></assembly-template>")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-tpl-import-name"), {
      target: { value: "as08missing" },
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-error").textContent).toContain(
        DEV_MSG.TPL_IMPORT_INVALID,
      );
    });
    expect(importTemplate).not.toHaveBeenCalled();
  });

  it("rejects spaces in unique name without calling import", async () => {
    render(<TemplateImportWizard />);
    fireEvent.change(screen.getByTestId("developer-tpl-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-tpl-import-name"), {
      target: { value: "has space" },
    });
    fireEvent.click(screen.getByTestId("developer-tpl-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-import-error").textContent).toBe(
        DEV_MSG.TPL_IMPORT_BAD_NAME,
      );
    });
    expect(importTemplate).not.toHaveBeenCalled();
  });
});
