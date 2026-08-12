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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from "react-router";
import { describe, expect, it } from "vitest";
import {
  normalizeWorkflowAdminTab,
  WorkflowAdminShell,
} from "../../../main/ts/workflowAdmin/WorkflowAdminShell";

function LocationProbe(): React.ReactElement {
  const loc = useLocation();
  return <div data-testid="location-pathname">{loc.pathname}</div>;
}

function renderRedirect(initialTab?: string) {
  return render(
    <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/workflow"]}>
      <Routes>
        <Route
          path="/workflow"
          element={<WorkflowAdminShell initialTab={initialTab} />}
        />
        <Route path="/admin/:tab" element={<LocationProbe />} />
        <Route path="/admin" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("WorkflowAdminShell", () => {
  it("is redirect-only into unified Admin paths (#3088)", () => {
    const { getByTestId } = renderRedirect();
    expect(getByTestId("location-pathname").textContent).toBe(
      "/admin/workflow",
    );
  });

  it("maps initialTab to Admin tab path", () => {
    const { getByTestId } = renderRedirect("roles");
    expect(getByTestId("location-pathname").textContent).toBe("/admin/roles");
  });

  it("normalizeWorkflowAdminTab defaults and validates", () => {
    expect(normalizeWorkflowAdminTab(undefined)).toBe("workflow");
    expect(normalizeWorkflowAdminTab("users")).toBe("users");
    expect(normalizeWorkflowAdminTab("nope")).toBe("workflow");
  });
});
