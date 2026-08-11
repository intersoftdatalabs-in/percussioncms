/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";
import type { CreateSiteRequest } from "../../../main/ts/api/contentExplorer/siteCreateApi";
import { SiteCreateWizard } from "../../../main/ts/contentExplorer/wizards/SiteCreateWizard";
import { renderA11yGate } from "./a11y";

const baseTemplates = [
  { name: "perc.base.plain", id: "1", label: "Plain" },
  { name: "perc.base.other", id: "2", label: "Other" },
];

function renderWizard(
  overrides: Partial<ComponentProps<typeof SiteCreateWizard>> = {},
) {
  return render(
    <SiteCreateWizard
      loadBaseTemplates={async () => baseTemplates}
      {...overrides}
    />,
  );
}

describe("SiteCreateWizard (#3002)", () => {
  it("renders the 4-step wizard at details", async () => {
    renderWizard();
    expect(screen.getByTestId("site-create-wizard")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-details")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-count").textContent).toMatch(
      /Step 1.*of 4/,
    );
    expect(screen.getByTestId("site-create-traditional-note")).toBeTruthy();
    await waitFor(() => {
      expect(screen.queryByTestId("site-create-templates-loading")).toBeNull();
    });
  });

  it("Next is disabled until site name and template name are valid", async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.queryByTestId("site-create-templates-loading")).toBeNull();
    });
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    // Template name auto-seeds from site name.
    expect(
      (screen.getByTestId("site-create-template-name") as HTMLInputElement)
        .value,
    ).toBe("AcmeTemplate");
    expect(next.disabled).toBe(false);
  });

  it("filters invalid site name characters on input", async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.queryByTestId("site-create-templates-loading")).toBeNull();
    });
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Bad Name!" },
    });
    expect(
      (screen.getByTestId("site-create-name") as HTMLInputElement).value,
    ).toBe("BadName");
  });

  it("advances through template and confirm steps", async () => {
    renderWizard();
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    expect(screen.getByTestId("site-create-step-template")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("site-create-base-template")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    expect(screen.getByTestId("site-create-step-confirm")).toBeTruthy();
    expect(screen.getByTestId("site-create-confirm-summary").textContent).toContain(
      "Acme",
    );
  });

  it("Run invokes submit and fires onCreated with /Sites path", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Acme", id: "9" });
    const onCreated = vi.fn();
    renderWizard({ submit, onCreated });
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-base-template")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-next"));
    expect(screen.getByTestId("site-create-step-progress")).toBeTruthy();
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.name).toBe("Acme");
    expect(req.baseTemplateName).toBe("perc.base.plain");
    expect(req.templateName).toBe("AcmeTemplate");
    expect(onCreated).toHaveBeenCalledWith(
      expect.objectContaining({
        siteName: "Acme",
        folderPath: "/Sites/Acme",
      }),
    );
    expect(screen.getByTestId("site-create-progress").textContent).toMatch(
      /Acme/,
    );
  });

  it("Run surfaces submit errors", async () => {
    const submit = vi.fn().mockRejectedValue(new Error("duplicate site"));
    renderWizard({ submit });
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-base-template")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    expect(screen.getByTestId("site-create-progress").textContent).toContain(
      "duplicate site",
    );
  });

  it("passes the zero serious/critical axe-core gate (step 0)", async () => {
    const { container } = renderWizard();
    await waitFor(() => {
      expect(screen.queryByTestId("site-create-templates-loading")).toBeNull();
    });
    await renderA11yGate(container);
  });
});
