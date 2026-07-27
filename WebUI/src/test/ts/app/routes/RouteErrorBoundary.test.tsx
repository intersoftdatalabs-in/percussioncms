/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RouteErrorBoundary } from "../../../../main/ts/app/routes/RouteErrorBoundary";

function Boom(): React.ReactElement {
  throw new Error("boom");
}

describe("RouteErrorBoundary", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders user-facing fallback when a child throws", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <RouteErrorBoundary label="Admin tools">
        <Boom />
      </RouteErrorBoundary>,
    );
    expect(screen.getByTestId("route-error")).toBeTruthy();
    expect(screen.getByRole("alert").textContent).toMatch(/Admin tools/i);
  });
});
