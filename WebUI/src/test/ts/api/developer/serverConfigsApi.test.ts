/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  SERVER_CONFIG_DESIGN_GAPS,
  getServerConfigDetail,
  updateServerConfig,
  withoutStaleServerConfigWriteGap,
} from "../../../../main/ts/api/developer/serverConfigsApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("serverConfigsApi SY-02 write", () => {
  it("withoutStaleServerConfigWriteGap drops pre-write save gaps", () => {
    expect(
      withoutStaleServerConfigWriteGap([
        "Configuration create / update / save not supported via this API",
        "Locking and concurrent edit are not exposed on this Developer surface",
      ]),
    ).toEqual(["Locking and concurrent edit are not exposed on this Developer surface"]);
  });

  it("getServerConfigDetail strips stale gaps and fills defaults when empty", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "LOG_CONFIG",
      designGaps: ["Configuration create / update / save not supported via this API"],
    });
    const detail = await getServerConfigDetail("LOG_CONFIG");
    expect(detail.designGaps).toEqual(SERVER_CONFIG_DESIGN_GAPS);
    spy.mockRestore();
  });

  it("updateServerConfig PUTs content and unwraps ServerConfig root", async () => {
    const putSpy = vi.spyOn(client, "put").mockResolvedValue({
      ServerConfig: {
        name: "LOG_CONFIG",
        content: "rootLogger=INFO",
        designGaps: SERVER_CONFIG_DESIGN_GAPS,
      },
    });
    const detail = await updateServerConfig("LOG_CONFIG", { content: "rootLogger=INFO" });
    expect(putSpy).toHaveBeenCalled();
    const [url, body] = putSpy.mock.calls[0];
    expect(String(url)).toContain("/LOG_CONFIG");
    expect(body).toEqual({ content: "rootLogger=INFO" });
    expect(detail.name).toBe("LOG_CONFIG");
    expect(detail.content).toBe("rootLogger=INFO");
    expect(detail.designGaps).toEqual(SERVER_CONFIG_DESIGN_GAPS);
  });

  it("updateServerConfig rejects missing content before PUT", async () => {
    const putSpy = vi.spyOn(client, "put");
    await expect(
      updateServerConfig("LOG_CONFIG", { content: null as unknown as string }),
    ).rejects.toThrow(/content is required/i);
    expect(putSpy).not.toHaveBeenCalled();
  });
});
