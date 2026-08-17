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

import React, { useState } from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppLayout } from "../../../../main/ts/app/layout/AppLayout";
import { dispatchSessionCommunityChanged } from "../../../../main/ts/app/layout/sessionCommunity";

vi.mock("../../../../main/ts/ui-themes/components", () => ({
  BrandBar: () => <div data-testid="brand-bar-stub" />,
  BrandFooter: () => <div data-testid="brand-footer-stub" />,
}));

vi.mock("../../../../main/ts/app/layout/TopNav", () => ({
  TopNav: () => <div data-testid="topnav-stub" />,
}));

let instanceSeq = 0;

function OutletProbe(): React.ReactElement {
  const [id] = useState(() => {
    instanceSeq += 1;
    return instanceSeq;
  });
  return <div data-testid="outlet-probe" data-instance={String(id)} />;
}

describe("AppLayout session community remount (#3506)", () => {
  beforeEach(() => {
    instanceSeq = 0;
  });

  afterEach(() => {
    cleanup();
  });

  it("remounts the main outlet after a session community change", () => {
    render(
      <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/home"]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="home" element={<OutletProbe />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    const first = screen.getByTestId("outlet-probe");
    expect(first.getAttribute("data-instance")).toBe("1");
    expect(
      screen.getByTestId("perc-spa-outlet").getAttribute(
        "data-session-community-epoch",
      ),
    ).toBe("0");

    act(() => {
      dispatchSessionCommunityChanged("Corporate");
    });

    const remounted = screen.getByTestId("outlet-probe");
    expect(remounted.getAttribute("data-instance")).toBe("2");
    expect(
      screen.getByTestId("perc-spa-outlet").getAttribute(
        "data-session-community-epoch",
      ),
    ).toBe("1");
    expect(screen.getByTestId("topnav-stub")).toBeTruthy();
  });
});
