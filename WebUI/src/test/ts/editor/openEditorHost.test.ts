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

import { describe, expect, it, vi } from "vitest";
import {
  closeReservedWindow,
  openEditorHost,
  reserveEditorWindow,
} from "../../../main/ts/editor/openEditorHost";

function popup(): Window {
  return {} as Window;
}

describe("openEditorHost", () => {
  it("opens the React host from a numeric id", async () => {
    const openWindow = vi.fn().mockReturnValue(popup());
    const ok = await openEditorHost({ id: "55" }, { openWindow });
    expect(ok).toBe(true);
    expect(openWindow).toHaveBeenCalled();
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("entry=editor");
    expect(href).toContain("contentId=55");
    expect(href).not.toContain("view=editor");
  });

  it("resolves a CMS path when id is missing", async () => {
    const openWindow = vi.fn().mockReturnValue(popup());
    const findByPath = vi.fn().mockResolvedValue({ id: "1-101-88" });
    const ok = await openEditorHost(
      { path: "/Sites/Demo/About.html" },
      { openWindow, findByPath },
    );
    expect(ok).toBe(true);
    expect(findByPath).toHaveBeenCalledWith("/Sites/Demo/About.html");
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("contentId=88");
    expect(href).not.toContain("view=editor");
  });

  it("returns false when neither id nor path resolves", async () => {
    const openWindow = vi.fn();
    const ok = await openEditorHost(
      { path: "/Sites/Missing" },
      { openWindow, findByPath: async () => ({}) },
    );
    expect(ok).toBe(false);
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("returns false when the popup is blocked", async () => {
    const openWindow = vi.fn().mockReturnValue(null);
    const ok = await openEditorHost({ id: "55" }, { openWindow });
    expect(ok).toBe(false);
    expect(openWindow).toHaveBeenCalled();
  });

  it("navigates a reserved window instead of opening a second popup", async () => {
    const reserved = {
      closed: false,
      location: { href: "about:blank" },
      focus: vi.fn(),
    } as unknown as Window;
    const openWindow = vi.fn();
    const ok = await openEditorHost(
      { id: "55" },
      { openWindow, reservedWindow: reserved },
    );
    expect(ok).toBe(true);
    expect(openWindow).not.toHaveBeenCalled();
    expect(reserved.location.href).toContain("entry=editor");
    expect(reserved.location.href).toContain("contentId=55");
    expect(reserved.focus).toHaveBeenCalled();
  });

  it("reserves about:blank on the user gesture", () => {
    const reserved = { closed: false } as Window;
    const openWindow = vi.fn().mockReturnValue(reserved);
    expect(reserveEditorWindow(openWindow)).toBe(reserved);
    expect(openWindow).toHaveBeenCalledWith(
      "about:blank",
      "_blank",
      expect.any(String),
    );
  });

  it("closeReservedWindow ignores missing or already-closed windows", () => {
    closeReservedWindow(null);
    closeReservedWindow(undefined);
    closeReservedWindow({ closed: true, close: vi.fn() } as unknown as Window);
    const close = vi.fn();
    closeReservedWindow({ closed: false, close } as unknown as Window);
    expect(close).toHaveBeenCalled();
  });
});
