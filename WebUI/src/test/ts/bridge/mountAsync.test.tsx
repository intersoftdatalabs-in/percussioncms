/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  mountReactComponent,
  unmountReactComponent,
} from "../../../main/ts/bridge";

describe("bridge async mount (§2.9)", () => {
  afterEach(() => {
    document.body.innerHTML = "";
    vi.restoreAllMocks();
  });

  it("mounts HelloWorld after load", async () => {
    document.body.innerHTML = '<div id="c1"></div>';
    mountReactComponent("c1", "HelloWorld", { name: "Test" });
    await vi.waitFor(() => {
      expect(document.getElementById("c1")?.textContent).toContain("Hello");
    });
  });

  it("unknown name does not throw", () => {
    document.body.innerHTML = '<div id="c2"></div>';
    const err = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => mountReactComponent("c2", "NotARealComponent")).not.toThrow();
    expect(err).toHaveBeenCalled();
  });

  it("unmount before resolve leaves no content", async () => {
    document.body.innerHTML = '<div id="c3"></div>';
    mountReactComponent("c3", "HelloWorld", { name: "Race" });
    unmountReactComponent("c3");
    await new Promise((r) => setTimeout(r, 50));
    expect(document.getElementById("c3")?.textContent || "").not.toContain(
      "Hello",
    );
  });
});
