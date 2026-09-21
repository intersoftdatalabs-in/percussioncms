/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  APPLICATION_FILE_MOVE_ROOT,
  APPLICATION_FILE_ROOT,
  createApplicationFolder,
  deleteApplicationPath,
  getApplicationFileBytes,
  getApplicationFileDetail,
  joinApplicationFilePath,
  listApplicationFiles,
  lockApplicationFile,
  moveApplicationPath,
  replaceApplicationFileBytes,
  unlockApplicationFile,
  updateApplicationFile,
  unwrapApplicationFile,
  wrapApplicationFileForWire,
  wrapApplicationFileMoveForWire,
} from "../../../../main/ts/api/developer/applicationFilesApi";
import { PATHS } from "../../../../main/ts/api/paths";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  put: vi.fn(),
  post: vi.fn(),
  del: vi.fn(),
  getBinary: vi.fn(),
  putBytes: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;
const post = client.post as ReturnType<typeof vi.fn>;
const del = client.del as ReturnType<typeof vi.fn>;
const getBinary = client.getBinary as ReturnType<typeof vi.fn>;
const putBytes = client.putBytes as ReturnType<typeof vi.fn>;

describe("applicationFilesApi", () => {
  beforeEach(() => {
    get.mockReset();
    put.mockReset();
    post.mockReset();
    del.mockReset();
    getBinary.mockReset();
    putBytes.mockReset();
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

  it("getApplicationFileDetail unwraps (PUT-create gap closed — empty designGaps)", async () => {
    get.mockResolvedValue({
      ApplicationFile: {
        applicationName: "sys_resources",
        path: "ApplicationFiles/a.txt",
        content: "hello",
      },
    });
    const detail = await getApplicationFileDetail("sys_resources", "ApplicationFiles/a.txt");
    expect(detail.content).toBe("hello");
    expect(detail.designGaps ?? []).toEqual([]);
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

  it("joinApplicationFilePath uses REST separators and rejects traversal", () => {
    expect(joinApplicationFilePath("ApplicationFiles", "qa-dir")).toBe(
      "ApplicationFiles/qa-dir",
    );
    expect(joinApplicationFilePath("", "root-dir")).toBe("root-dir");
    expect(() => joinApplicationFilePath("ApplicationFiles", "../x")).toThrow(/segment/i);
  });

  it("createApplicationFolder POSTs folders?path=", async () => {
    post.mockResolvedValue({
      ApplicationFile: { path: "ApplicationFiles/qa-dir", directory: true },
    });
    const created = await createApplicationFolder("sys_resources", "ApplicationFiles/qa-dir");
    expect(created.path).toBe("ApplicationFiles/qa-dir");
    expect(post.mock.calls[0][0]).toBe(
      `${PATHS.APPLICATION_FILES}/sys_resources/folders?path=ApplicationFiles%2Fqa-dir`,
    );
    expect(post.mock.calls[0][1]).toEqual({});
  });

  it("deleteApplicationPath DELETEs content?path=", async () => {
    del.mockResolvedValue(undefined);
    await deleteApplicationPath("sys_resources", "ApplicationFiles/qa-dir");
    expect(del.mock.calls[0][0]).toBe(
      `${PATHS.APPLICATION_FILES}/sys_resources/content?path=ApplicationFiles%2Fqa-dir`,
    );
  });

  it("moveApplicationPath POSTs wrapped from/to", async () => {
    post.mockResolvedValue({
      ApplicationFile: { path: "ApplicationFiles/b.txt" },
    });
    const moved = await moveApplicationPath(
      "sys_resources",
      "ApplicationFiles/a.txt",
      "ApplicationFiles/b.txt",
    );
    expect(moved.path).toBe("ApplicationFiles/b.txt");
    expect(APPLICATION_FILE_MOVE_ROOT).toBe("ApplicationFileMove");
    expect(post.mock.calls[0][0]).toBe(`${PATHS.APPLICATION_FILES}/sys_resources/move`);
    expect(post.mock.calls[0][1]).toEqual(
      wrapApplicationFileMoveForWire({
        fromPath: "ApplicationFiles/a.txt",
        toPath: "ApplicationFiles/b.txt",
      }),
    );
  });

  it("createApplicationFolder rejects blank path", async () => {
    await expect(createApplicationFolder("sys_resources", "  ")).rejects.toThrow(/path is required/i);
    expect(post).not.toHaveBeenCalled();
  });

  it("lockApplicationFile POSTs lock?path=", async () => {
    post.mockResolvedValue({ ObjectLockSummary: { locker: "Admin", session: "s1" } });
    const summary = await lockApplicationFile("sys_resources", "ApplicationFiles/a.txt");
    expect(summary.locker).toBe("Admin");
    expect(post.mock.calls[0][0]).toBe(
      `${PATHS.APPLICATION_FILES}/sys_resources/lock?path=ApplicationFiles%2Fa.txt`,
    );
  });

  it("unlockApplicationFile POSTs unlock?path=", async () => {
    post.mockResolvedValue(undefined);
    await unlockApplicationFile("sys_resources", "ApplicationFiles/a.txt");
    expect(post.mock.calls[0][0]).toBe(
      `${PATHS.APPLICATION_FILES}/sys_resources/unlock?path=ApplicationFiles%2Fa.txt`,
    );
  });

  it("getApplicationFileBytes GETs the binary endpoint and maps octet-stream", async () => {
    getBinary.mockResolvedValue({
      bytes: new Uint8Array([0x50, 0x4b, 0x03, 0x04]),
      contentType: "application/octet-stream",
      status: 200,
    });
    const payload = await getApplicationFileBytes(
      "sys_resources",
      "ApplicationFiles/blob.bin",
    );
    expect(payload.bytes).toEqual(new Uint8Array([0x50, 0x4b, 0x03, 0x04]));
    expect(payload.contentType).toBe("application/octet-stream");
    expect(getBinary.mock.calls[0][0]).toBe(
      `${PATHS.APPLICATION_FILES}/sys_resources/binary?path=ApplicationFiles%2Fblob.bin`,
    );
  });

  it("getApplicationFileBytes rejects blank app or path", async () => {
    await expect(getApplicationFileBytes("  ", "a.bin")).rejects.toThrow(/application name/i);
    await expect(getApplicationFileBytes("sys_resources", "  ")).rejects.toThrow(/path is required/i);
    expect(getBinary).not.toHaveBeenCalled();
  });

  it("replaceApplicationFileBytes PUTs raw bytes and unwraps the detail", async () => {
    putBytes.mockResolvedValue({
      ApplicationFile: {
        applicationName: "sys_resources",
        path: "ApplicationFiles/blob.bin",
        binary: true,
      },
    });
    const bytes = new Uint8Array([0x50, 0x4b, 0x03, 0x04, 0x00]);
    const detail = await replaceApplicationFileBytes(
      "sys_resources",
      "ApplicationFiles/blob.bin",
      bytes,
    );
    expect(detail.binary).toBe(true);
    const expectedPut = `${PATHS.APPLICATION_FILES}/sys_resources/binary?path=ApplicationFiles%2Fblob.bin`;
    expect(putBytes.mock.calls[0][0]).toBe(expectedPut);
    expect(putBytes.mock.calls[0][1]).toBe(bytes);
  });

  it("replaceApplicationFileBytes rejects blank path or missing bytes", async () => {
    await expect(
      replaceApplicationFileBytes("sys_resources", "  ", new Uint8Array([1])),
    ).rejects.toThrow(/path is required/i);
    await expect(
      replaceApplicationFileBytes("sys_resources", "a.bin", null as unknown as Uint8Array),
    ).rejects.toThrow(/bytes are required/i);
    expect(putBytes).not.toHaveBeenCalled();
  });
});
