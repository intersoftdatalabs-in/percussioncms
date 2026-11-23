/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as importExportApi from "../../../main/ts/api/developer/contentTypeImportExport";
import { ContentTypeImportWizard } from "../../../main/ts/developer/ContentTypeImportWizard";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/contentTypeImportExport", async (importOriginal) => {
  const actual =
    await importOriginal<
      typeof import("../../../main/ts/api/developer/contentTypeImportExport")
    >();
  return {
    ...actual,
    importContentType: vi.fn(),
  };
});

const importContentType = importExportApi.importContentType as ReturnType<typeof vi.fn>;

const SAMPLE_XML =
  '<ItemDefData appName="psx_ceimportedOne" objectType="1">' +
  '<PSXItemDefSummary name="importedOne" label="Imported One" /></ItemDefData>';

function xmlFile(text: string, name = "importedOne.xml"): File {
  return new File([text], name, { type: "application/xml" });
}

describe("ContentTypeImportWizard", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    importContentType.mockReset();
  });

  it("imports unique name and notifies parent", async () => {
    importContentType.mockResolvedValue({ name: "cd14unique", label: "Imported One" });
    const onImported = vi.fn();
    render(<ContentTypeImportWizard onImported={onImported} />);
    const fileInput = screen.getByTestId("developer-ct-import-file") as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [xmlFile(SAMPLE_XML)] } });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-import-name") as HTMLInputElement).value).toBe(
        "importedOne",
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "cd14unique" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(importContentType).toHaveBeenCalledTimes(1);
    });
    const posted = String(importContentType.mock.calls[0][0]);
    expect(posted).toContain("cd14unique");
    expect(screen.getByTestId("developer-ct-import-notice").textContent).toBe(
      DEV_MSG.CT_IMPORTED,
    );
    expect(onImported).toHaveBeenCalledWith({ name: "cd14unique", label: "Imported One" });
  });

  it("surfaces 400 invalid XML", async () => {
    importContentType.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "invalid content-type design XML" },
    });
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile("<not-xml")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "cd14bad" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
        DEV_MSG.CT_IMPORT_INVALID,
      );
    });
    expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
      "invalid content-type design XML",
    );
  });

  it("surfaces 409 duplicate name", async () => {
    importContentType.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content type already exists: percPage" },
    });
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-import-name") as HTMLInputElement).value).toBe(
        "importedOne",
      );
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
        DEV_MSG.CT_IMPORT_DUPLICATE,
      );
    });
    expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
      "already exists",
    );
  });

  it("surfaces missing-name rewrite as invalid XML", async () => {
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile("<ItemDefData></ItemDefData>")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "cd14missing" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
        DEV_MSG.CT_IMPORT_INVALID,
      );
    });
    expect(importContentType).not.toHaveBeenCalled();
  });

  it("rejects spaces in unique name without calling import", async () => {
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "has space" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toBe(
        DEV_MSG.CT_IMPORT_BAD_NAME,
      );
    });
    expect(importContentType).not.toHaveBeenCalled();
  });

  it("surfaces 403 forbidden", async () => {
    importContentType.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role is required" },
    });
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "cd14forbidden" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toContain(
        DEV_MSG.CT_IMPORT_FORBIDDEN,
      );
    });
  });

  it("blocks cataloged duplicate names client-side without POST", async () => {
    render(<ContentTypeImportWizard existingNames={["percPage", "cd14Taken"]} />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "PERCPAGE" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error").textContent).toBe(
        DEV_MSG.CT_IMPORT_DUPLICATE,
      );
    });
    expect(importContentType).not.toHaveBeenCalled();
  });

  it("accepts a dropped XML file and pre-fills the name", async () => {
    render(<ContentTypeImportWizard />);
    const section = screen.getByTestId("developer-ct-import");
    fireEvent.drop(section, {
      dataTransfer: { files: [xmlFile(SAMPLE_XML, "dropped.xml")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename").textContent).toBe(
        "dropped.xml",
      );
    });
    expect((screen.getByTestId("developer-ct-import-name") as HTMLInputElement).value).toBe(
      "importedOne",
    );
    expect(screen.getByTestId("developer-ct-import-dropzone").textContent).toContain(
      DEV_MSG.CT_IMPORT_DROP_HINT,
    );
  });

  it("ignores text drops without clearing the selected file", async () => {
    render(<ContentTypeImportWizard />);
    const section = screen.getByTestId("developer-ct-import");
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML, "kept.xml")] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename").textContent).toBe(
        "kept.xml",
      );
    });
    // Text dragged into the name field bubbles to the section with no files.
    fireEvent.drop(section, { dataTransfer: { files: [] } });
    expect(screen.getByTestId("developer-ct-import-filename").textContent).toBe(
      "kept.xml",
    );
    expect((screen.getByTestId("developer-ct-import-name") as HTMLInputElement).value).toBe(
      "importedOne",
    );
  });

  it("announces success via role=status and moves focus to errors", async () => {
    importContentType.mockResolvedValue({ name: "cd14polish", label: "Polished" });
    render(<ContentTypeImportWizard />);
    fireEvent.change(screen.getByTestId("developer-ct-import-file"), {
      target: { files: [xmlFile(SAMPLE_XML)] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-filename")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-import-name"), {
      target: { value: "cd14polish" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-notice")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-import-notice").getAttribute("role")).toBe(
      "status",
    );
  });

  it("moves focus into the error region on validation failure", async () => {
    render(<ContentTypeImportWizard />);
    fireEvent.click(screen.getByTestId("developer-ct-import-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import-error")).toBeTruthy();
    });
    expect(document.activeElement).toBe(screen.getByTestId("developer-ct-import-error"));
  });
});
