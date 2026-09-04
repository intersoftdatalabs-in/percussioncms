/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  APPLICATION_FILE_ROOT,
  getApplicationFileDetail,
  listApplicationFiles,
  updateApplicationFile,
  unwrapApplicationFile,
  wrapApplicationFileForWire,
} from "../../../../main/ts/api/developer/applicationFilesApi";
import { PATHS } from "../../../../main/ts/api/paths";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  put: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;

describe("applicationFilesApi", () => {
  beforeEach(() => {
    get.mockReset();
    put.mockReset();
  });

  it("wrapApplicationFileForWire uses ApplicationFile root for UNWRAP_ROOT_VALUE", () => {
    expect(APPLICATION_FILE_ROOT).toBe("ApplicationFile");
    expect(wrapApplicationFileForWire({ content: "x=1" })).toEqual({
      ApplicationFile: { content: "x=1" },
    });
  });

  it("unwrapApplicationFile accepts wrapped and flat payloads", () => {
    expect(
      unwrapApplicationFile({
        ApplicationFile: { path: "ApplicationFiles/a.txt", content: "hi" },
      }).path,
    ).toBe("ApplicationFiles/a.txt");
    expect(
      unwrapApplicationFile({ path: "ApplicationFiles/b.txt", content: "yo" }).path,
    ).toBe("ApplicationFiles/b.txt");
  });

  it("listApplicationFiles GETs /applicationfiles/{app}", async () => {
    get.mockResolvedValue([{ path: "ApplicationFiles/style.css", name: "style.css" }]);
    const list = await listApplicationFiles("sys_resources");
    expect(list).toHaveLength(1);
    expect(list[0].path).toBe("ApplicationFiles/style.css");
    expect(get).toHaveBeenCalledWith(`${PATHS.APPLICATION_FILES}/sys_resources`);
  });

  it("listApplicationFiles rejects blank app", async () => {
    await expect(listApplicationFiles("  ")).rejects.toThrow(/application name/i);
    expect(get).not.toHaveBeenCalled();
  });

  it("getApplicationFileDetail unwraps and fills designGaps", async () => {
    get.mockResolvedValue({
      ApplicationFile: {
        applicationName: "sys_resources",
        path: "ApplicationFiles/a.txt",
        content: "hello",
      },
    });
    const detail = await getApplicationFileDetail("sys_resources", "ApplicationFiles/a.txt");
    expect(detail.content).toBe("hello");
    expect(detail.designGaps?.length).toBeGreaterThan(0);
    const expectedGet = `${PATHS.APPLICATION_FILES}/sys_resources/content?path=ApplicationFiles%2Fa.txt`;
    expect(get.mock.calls[0][0]).toBe(expectedGet);
  });

  it("updateApplicationFile PUTs wrapped content and unwraps response", async () => {
    put.mockResolvedValue({
      ApplicationFile: {
        applicationName: "sys_resources",
        path: "ApplicationFiles/a.txt",
        content: "updated",
      },
    });
    const detail = await updateApplicationFile("sys_resources", "ApplicationFiles/a.txt", {
      content: "updated",
    });
    expect(detail.content).toBe("updated");
    const expectedPut = `${PATHS.APPLICATION_FILES}/sys_resources/content?path=ApplicationFiles%2Fa.txt`;
    expect(put.mock.calls[0][0]).toBe(expectedPut);
    expect(put.mock.calls[0][1]).toEqual({
      ApplicationFile: { content: "updated" },
    });
    // CSRF: put() from ../client injects OWASP_CSRFTOKEN via buildHeaders; API layer
    // does not pass a redundant header (peer mutating APIs share the same put helper).
  });

  it("updateApplicationFile rejects missing content before PUT", async () => {
    await expect(
      updateApplicationFile("sys_resources", "ApplicationFiles/a.txt", {
        content: null as unknown as string,
      }),
    ).rejects.toThrow(/content is required/i);
    expect(put).not.toHaveBeenCalled();
  });
});
