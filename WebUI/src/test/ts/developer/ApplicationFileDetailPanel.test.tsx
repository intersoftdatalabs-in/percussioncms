/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { DEFAULT_SPA_BOOTSTRAP } from "../../../main/ts/app/bootstrap/types";
import * as appFilesApi from "../../../main/ts/api/developer/applicationFilesApi";
import {
  ApplicationFileDetailPanel,
  formatBytes,
  hasXmlParseError,
  stripXmlCommentsCdataAndPi,
} from "../../../main/ts/developer/ApplicationFileDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/applicationFilesApi", () => ({
  getApplicationFileDetail: vi.fn(),
  updateApplicationFile: vi.fn(),
  lockApplicationFile: vi.fn(),
  unlockApplicationFile: vi.fn(),
  getApplicationFileBytes: vi.fn(),
  replaceApplicationFileBytes: vi.fn(),
  APPLICATION_FILE_DESIGN_GAPS: ["gap-binary"],
}));

const getApplicationFileDetail = appFilesApi.getApplicationFileDetail as ReturnType<
  typeof vi.fn
>;
const updateApplicationFile = appFilesApi.updateApplicationFile as ReturnType<typeof vi.fn>;
const lockApplicationFile = appFilesApi.lockApplicationFile as ReturnType<typeof vi.fn>;
const unlockApplicationFile = appFilesApi.unlockApplicationFile as ReturnType<typeof vi.fn>;
const getApplicationFileBytes = appFilesApi.getApplicationFileBytes as ReturnType<
  typeof vi.fn
>;
const replaceApplicationFileBytes = appFilesApi.replaceApplicationFileBytes as ReturnType<
  typeof vi.fn
>;

function renderDetail(isAdmin: boolean, path = "ApplicationFiles/a.css") {
  return render(
    <BootstrapProvider value={{ ...DEFAULT_SPA_BOOTSTRAP, isAdmin }}>
      <ApplicationFileDetailPanel
        applicationName="sys_resources"
        path={path}
        onBack={() => undefined}
      />
    </BootstrapProvider>,
  );
}

describe("stripXmlCommentsCdataAndPi", () => {
  it("drops comments even when the body contains extra <!-- (CodeQL leftover)", () => {
    const { text, unclosed } = stripXmlCommentsCdataAndPi(
      "<!--<!-- --><root/>",
    );
    expect(unclosed).toBe(false);
    expect(text).toBe("<root/>");
    expect(text.includes("<!--")).toBe(false);
  });

  it("flags unclosed comments instead of leaving <!-- in the output", () => {
    const { text, unclosed } = stripXmlCommentsCdataAndPi("<root><!-- oops");
    expect(unclosed).toBe(true);
    expect(text.includes("<!--")).toBe(false);
  });
});

describe("hasXmlParseError", () => {
  it("accepts empty, self-closing, and balanced tags", () => {
    expect(hasXmlParseError("")).toBe(false);
    expect(hasXmlParseError("   ")).toBe(false);
    expect(hasXmlParseError("<root/>")).toBe(false);
    expect(hasXmlParseError("<root></root>")).toBe(false);
    expect(hasXmlParseError("<a><b/></a>")).toBe(false);
  });

  it("flags non-XML text and unbalanced tags", () => {
    expect(hasXmlParseError("not xml")).toBe(true);
    expect(hasXmlParseError("<root><unclosed>")).toBe(true);
    expect(hasXmlParseError("<a></b>")).toBe(true);
  });

  it("ignores comments, CDATA, and PIs for the tag stack", () => {
    expect(hasXmlParseError("<!--<!-- --><root/>")).toBe(false);
    expect(hasXmlParseError("<?xml version=\"1.0\"?><root></root>")).toBe(false);
    expect(hasXmlParseError("<root><![CDATA[<notatag>]]></root>")).toBe(false);
  });

  it("flags unclosed comments as a parse error", () => {
    expect(hasXmlParseError("<root><!-- oops")).toBe(true);
  });
});

describe("ApplicationFileDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    getApplicationFileDetail.mockReset();
    updateApplicationFile.mockReset();
    lockApplicationFile.mockReset();
    unlockApplicationFile.mockReset();
    getApplicationFileBytes.mockReset();
    replaceApplicationFileBytes.mockReset();
    lockApplicationFile.mockResolvedValue({ locker: "Admin", session: "s1" });
    unlockApplicationFile.mockResolvedValue(undefined);
  });

  it("shows detail load 404 via APPFILE_DETAIL_ERROR path", async () => {
    getApplicationFileDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
      DEV_MSG.APPFILE_DETAIL_ERROR,
    );
  });

  it("shows detail load 500", async () => {
    getApplicationFileDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        "(500)",
      );
    });
  });

  it("maps save 403 to APPFILE_FORBIDDEN", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    updateApplicationFile.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-lock-status").textContent).toContain(
        DEV_MSG.APPFILE_LOCKED,
      );
    });
    fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
      target: { value: "body{color:red}" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_FORBIDDEN,
      );
    });
  });

  it("maps save 500 to APPFILE_SAVE_ERROR", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    updateApplicationFile.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-lock-status").textContent).toContain(
        DEV_MSG.APPFILE_LOCKED,
      );
    });
    fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
      target: { value: "body{color:red}" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_SAVE_ERROR,
      );
    });
  });

  it("disables save for non-admin and shows admin hint", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-lock"],
    });
    renderDetail(false);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-save")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-appfile-save") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(screen.getByTestId("developer-appfile-admin-hint").textContent).toBe(
      DEV_MSG.APPFILE_SAVE_ADMIN_ONLY,
    );
    expect(updateApplicationFile).not.toHaveBeenCalled();
  });

  it("blocks editor when contentLength exceeds ceiling", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/big.bin",
      name: "big.bin",
      content: "x",
      contentLength: 3 * 1024 * 1024,
      designGaps: ["gap-lock"],
    });
    renderDetail(true, "ApplicationFiles/big.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toBe(
        DEV_MSG.APPFILE_TOO_LARGE,
      );
    });
    expect(screen.queryByTestId("developer-appfile-content-editor")).toBeNull();
  });

  it("blocks malformed XML save when window.confirm is unavailable", async () => {
    const confirmDesc = Object.getOwnPropertyDescriptor(window, "confirm");
    Object.defineProperty(window, "confirm", {
      configurable: true,
      value: undefined,
    });
    try {
      getApplicationFileDetail.mockResolvedValue({
        applicationName: "sys_resources",
        path: "ApplicationFiles/a.xml",
        name: "a.xml",
        content: "<root/>",
        designGaps: ["gap-lock"],
      });
      renderDetail(true, "ApplicationFiles/a.xml");
      await waitFor(() => {
        expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-appfile-lock"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-appfile-lock-status").textContent).toContain(
          DEV_MSG.APPFILE_LOCKED,
        );
      });
      fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
        target: { value: "<root><unclosed>" },
      });
      fireEvent.click(screen.getByTestId("developer-appfile-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-appfile-detail-error").textContent).toBe(
          DEV_MSG.APPFILE_XML_BLOCKED,
        );
      });
      expect(updateApplicationFile).not.toHaveBeenCalled();
    } finally {
      if (confirmDesc) {
        Object.defineProperty(window, "confirm", confirmDesc);
      }
    }
  });

  it("disables save until lock is held then unlocks", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{}",
      designGaps: ["gap-binary"],
    });
    updateApplicationFile.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/a.css",
      name: "a.css",
      content: "body{color:red}",
      designGaps: ["gap-binary"],
    });
    renderDetail(true);
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-save")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-appfile-save") as HTMLButtonElement).disabled).toBe(
      true,
    );
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(lockApplicationFile).toHaveBeenCalledWith(
        "sys_resources",
        "ApplicationFiles/a.css",
      );
    });
    fireEvent.change(screen.getByTestId("developer-appfile-content-editor"), {
      target: { value: "body{color:red}" },
    });
    fireEvent.click(screen.getByTestId("developer-appfile-save"));
    await waitFor(() => {
      expect(updateApplicationFile).toHaveBeenCalled();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-unlock"));
    await waitFor(() => {
      expect(unlockApplicationFile).toHaveBeenCalled();
    });
  });

  it("formatBytes renders portable human-readable sizes", () => {
    expect(formatBytes(0)).toBe("0 B");
    expect(formatBytes(null)).toBe("—");
    expect(formatBytes(2048)).toBe("2 KB");
    expect(formatBytes(3.5 * 1024 * 1024)).toBe("3.5 MB");
  });

  it("shows the binary download/replace view instead of the text editor", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 3 * 1024 * 1024,
      designGaps: [],
    });
    renderDetail(true, "ApplicationFiles/blob.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-binary")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-appfile-content-editor")).toBeNull();
    expect(screen.queryByTestId("developer-appfile-save")).toBeNull();
    expect(screen.getByTestId("developer-appfile-download")).toBeTruthy();
    expect(screen.getByTestId("developer-appfile-replace")).toBeTruthy();
    // Binary view is not a text-too-large error: the >2MB gate is bypassed.
    expect(screen.queryByTestId("developer-appfile-detail-error")).toBeNull();
  });

  it("downloads binary bytes through a blob anchor", async () => {
    const originalCreate = URL.createObjectURL;
    const originalRevoke = URL.revokeObjectURL;
    URL.createObjectURL = vi.fn(() => "blob:mock");
    URL.revokeObjectURL = vi.fn();
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => undefined);
    try {
      getApplicationFileDetail.mockResolvedValue({
        applicationName: "sys_resources",
        path: "ApplicationFiles/blob.bin",
        name: "blob.bin",
        binary: true,
        contentLength: 12,
        designGaps: [],
      });
      getApplicationFileBytes.mockResolvedValue({
        bytes: new Uint8Array([0x50, 0x4b, 0x03, 0x04]),
        contentType: "application/octet-stream",
      });
      renderDetail(true, "ApplicationFiles/blob.bin");
      await waitFor(() => {
        expect(screen.getByTestId("developer-appfile-download")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-appfile-download"));
      await waitFor(() => {
        expect(getApplicationFileBytes).toHaveBeenCalledWith(
          "sys_resources",
          "ApplicationFiles/blob.bin",
        );
      });
      expect(clickSpy).toHaveBeenCalledTimes(1);
      expect(URL.createObjectURL).toHaveBeenCalledWith(
        expect.objectContaining({ type: "application/octet-stream" }),
      );
      expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:mock");
    } finally {
      URL.createObjectURL = originalCreate;
      URL.revokeObjectURL = originalRevoke;
      clickSpy.mockRestore();
    }
  });

  it("download failure maps to APPFILE_DOWNLOAD_ERROR", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 12,
      designGaps: [],
    });
    getApplicationFileBytes.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    renderDetail(true, "ApplicationFiles/blob.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-download")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-download"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_DOWNLOAD_ERROR,
      );
    });
  });

  it("keeps replace disabled until the file is locked", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 12,
      designGaps: [],
    });
    renderDetail(true, "ApplicationFiles/blob.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-replace")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("developer-appfile-replace") as HTMLInputElement).disabled,
    ).toBe(true);
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-appfile-replace") as HTMLInputElement).disabled,
      ).toBe(false);
    });
  });

  it("replaces binary bytes after acquiring the lock", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 12,
      designGaps: [],
    });
    replaceApplicationFileBytes.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 21,
      designGaps: [],
    });
    renderDetail(true, "ApplicationFiles/blob.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-replace")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-appfile-replace") as HTMLInputElement).disabled,
      ).toBe(false);
    });
    // Plain file-like object: host Blob.arrayBuffer() is not reliable in jsdom.
    const file = {
      name: "replacement.bin",
      arrayBuffer: vi.fn().mockResolvedValue(new Uint8Array([1, 2, 3, 4]).buffer),
    };
    fireEvent.change(screen.getByTestId("developer-appfile-replace"), {
      target: { files: [file] },
    });
    await waitFor(() => {
      expect(replaceApplicationFileBytes).toHaveBeenCalledTimes(1);
    });
    const [app, p, bytes] = replaceApplicationFileBytes.mock.calls[0];
    expect(app).toBe("sys_resources");
    expect(p).toBe("ApplicationFiles/blob.bin");
    expect(Array.from(bytes)).toEqual([1, 2, 3, 4]);
    expect(screen.getByTestId("developer-appfile-editor-notice").textContent).toBe(
      DEV_MSG.APPFILE_REPLACED,
    );
    expect((screen.getByTestId("developer-appfile-replace") as HTMLInputElement).value).toBe(
      "",
    );
  });

  it("flips back to the text editor when a replacement is UTF-8 text", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.txt",
      name: "blob.txt",
      binary: true,
      contentLength: 5,
      designGaps: [],
    });
    replaceApplicationFileBytes.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.txt",
      name: "blob.txt",
      binary: false,
      content: "hello",
      contentLength: 5,
      designGaps: [],
    });
    renderDetail(true, "ApplicationFiles/blob.txt");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-replace")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-appfile-replace") as HTMLInputElement).disabled,
      ).toBe(false);
    });
    const file = {
      name: "text.txt",
      arrayBuffer: vi.fn().mockResolvedValue(new Uint8Array([104, 105]).buffer),
    };
    fireEvent.change(screen.getByTestId("developer-appfile-replace"), {
      target: { files: [file] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-content-editor")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-appfile-content-editor") as HTMLTextAreaElement).value)
      .toBe("hello");
  });

  it("maps replace 404 to APPFILE_NOT_FOUND", async () => {
    getApplicationFileDetail.mockResolvedValue({
      applicationName: "sys_resources",
      path: "ApplicationFiles/blob.bin",
      name: "blob.bin",
      binary: true,
      contentLength: 12,
      designGaps: [],
    });
    replaceApplicationFileBytes.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    renderDetail(true, "ApplicationFiles/blob.bin");
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-replace")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-appfile-lock"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-appfile-replace") as HTMLInputElement).disabled,
      ).toBe(false);
    });
    const file = {
      name: "replacement.bin",
      arrayBuffer: vi.fn().mockResolvedValue(new Uint8Array([1]).buffer),
    };
    fireEvent.change(screen.getByTestId("developer-appfile-replace"), {
      target: { files: [file] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-appfile-detail-error").textContent).toContain(
        DEV_MSG.APPFILE_NOT_FOUND,
      );
    });
  });
});
