/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ServerList } from "@/publishing/components/ServerList";

describe("ServerList", () => {
  it("shows empty state", () => {
    render(
      <ServerList servers={[]} selectedId="" onSelect={vi.fn()} />,
    );
    expect(screen.getByTestId("server-list-empty")).toBeTruthy();
  });

  it("shows default star indicator", () => {
    render(
      <ServerList
        servers={[
          { serverId: "1", serverName: "Prod", isDefault: true },
          { serverId: "2", serverName: "Stage" },
        ]}
        selectedId="1"
        onSelect={vi.fn()}
      />,
    );
    expect(screen.getByText(/Prod/)).toBeTruthy();
    expect(screen.getByText(/★/)).toBeTruthy();
  });
});
