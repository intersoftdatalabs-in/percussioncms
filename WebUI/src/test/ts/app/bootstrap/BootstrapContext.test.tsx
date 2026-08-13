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

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import {
  BootstrapProvider,
  useSpaBootstrap,
  useSpaBootstrapOptional,
} from "../../../../main/ts/app/bootstrap/BootstrapContext";
import {
  DEFAULT_SPA_BOOTSTRAP,
  type SpaBootstrap,
} from "../../../../main/ts/app/bootstrap/types";

const sample: SpaBootstrap = {
  ...DEFAULT_SPA_BOOTSTRAP,
  userName: "Admin",
  isAdmin: true,
  entry: "explorer",
};

function OptionalProbe(): React.ReactElement {
  const value = useSpaBootstrapOptional();
  return (
    <div data-testid="optional">
      {value == null ? "missing" : value.userName}
    </div>
  );
}

function RequiredProbe(): React.ReactElement {
  const value = useSpaBootstrap();
  return <div data-testid="required">{value.userName || "empty"}</div>;
}

describe("BootstrapContext (#3331)", () => {
  afterEach(() => {
    cleanup();
  });

  it("useSpaBootstrapOptional is null without a provider and does not throw", () => {
    expect(() => render(<OptionalProbe />)).not.toThrow();
    expect(screen.getByTestId("optional").textContent).toBe("missing");
  });

  it("useSpaBootstrap falls back to default identity without a provider", () => {
    expect(() => render(<RequiredProbe />)).not.toThrow();
    expect(screen.getByTestId("required").textContent).toBe("empty");
  });

  it("both hooks read the provider value", () => {
    render(
      <BootstrapProvider value={sample}>
        <OptionalProbe />
        <RequiredProbe />
      </BootstrapProvider>,
    );
    expect(screen.getByTestId("optional").textContent).toBe("Admin");
    expect(screen.getByTestId("required").textContent).toBe("Admin");
  });
});
