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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LogDetailsPanel } from "@/publishing/components/LogDetailsPanel";
import {
  copyPublishLogItemLocation,
  publishLogItemCopyText,
} from "@/publishing/logDetails";

const ITEMS = {
  SitePublishItem: [
    {
      contentid: 44,
      fileLocation: " /sites/ff/index.html ",
      fileName: "index.html",
      status: "Success",
    },
    {
      contentid: 45,
      fileLocation: "   ",
      fileName: "",
      status: "Failed",
    },
    {
      contentid: 46,
      fileName: "only-name.html",
      status: "Success",
    },
  ],
};

describe("publishLogItemCopyText", () => {
  it("prefers a trimmed location, else file name, else blank", () => {
    expect(publishLogItemCopyText(ITEMS.SitePublishItem[0])).toBe(
      "/sites/ff/index.html",
    );
    expect(publishLogItemCopyText(ITEMS.SitePublishItem[1])).toBe("");
    expect(publishLogItemCopyText(ITEMS.SitePublishItem[2])).toBe(
      "only-name.html",
    );
  });

  it("does not report success when the write fails or the location is blank", async () => {
    const blank = await copyPublishLogItemLocation(ITEMS.SitePublishItem[1], async () => {
      throw new Error("should not write");
    });
    expect(blank).toEqual({ kind: "missing" });

    const failed = await copyPublishLogItemLocation(
      ITEMS.SitePublishItem[0],
      async () => false,
    );
    expect(failed).toEqual({ kind: "clipboard" });

    const thrown = await copyPublishLogItemLocation(
      ITEMS.SitePublishItem[0],
      async () => {
        throw new Error("denied");
      },
    );
    expect(thrown).toEqual({ kind: "clipboard" });
  });
});

describe("LogDetailsPanel copy location", () => {
  it("copies the location, names a missing location, and dismiss leaves the rows", async () => {
    const onClose = vi.fn();
    const writeLocation = vi.fn(async (text: string) => text.length > 0);
    const snapshot = JSON.stringify(ITEMS);

    render(
      <LogDetailsPanel
        details={ITEMS}
        onClose={onClose}
        writeLocation={writeLocation}
      />,
    );

    fireEvent.click(screen.getByTestId("publish-log-copy-location-0"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-copy-status")).toHaveTextContent(
        "Location copied",
      );
    });
    expect(writeLocation).toHaveBeenCalledWith("/sites/ff/index.html");
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("publish-log-copy-dismiss"));
    expect(screen.queryByTestId("publish-log-copy-status")).toBeNull();
    expect(screen.getByTestId("publish-log-copy-location-0")).toBeTruthy();
    expect(JSON.stringify(ITEMS)).toBe(snapshot);
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("publish-log-copy-location-1"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-copy-status")).toHaveTextContent(
        "No location to copy",
      );
    });
    expect(writeLocation).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("publish-log-details")).toBeTruthy();
  });

  it("keeps the panel open when the clipboard write fails", async () => {
    const onClose = vi.fn();
    render(
      <LogDetailsPanel
        details={ITEMS}
        onClose={onClose}
        writeLocation={async () => false}
      />,
    );
    fireEvent.click(screen.getByTestId("publish-log-copy-location-0"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-copy-status")).toHaveTextContent(
        "Could not copy location",
      );
    });
    expect(screen.getByTestId("publish-log-details")).toBeTruthy();
    expect(onClose).not.toHaveBeenCalled();
  });
});
