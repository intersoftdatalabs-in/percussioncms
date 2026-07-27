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

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { LandingShell } from "@/app/LandingShell";

describe("LandingShell", () => {
  it("renders authenticated landing chrome", () => {
    render(<LandingShell bootstrap={{ userName: "demo", entry: "home" }} />);
    expect(screen.getByTestId("perc-spa-landing")).toBeDefined();
    expect(screen.getByTestId("perc-spa-landing-title").textContent).toMatch(
      /Welcome/i,
    );
    expect(screen.getByTestId("perc-spa-landing-user").textContent).toContain("demo");
    expect(screen.getByTestId("perc-spa-landing-entry").textContent).toBe("home");
  });
});
