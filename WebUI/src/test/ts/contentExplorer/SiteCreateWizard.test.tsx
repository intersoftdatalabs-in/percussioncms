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

function renderWizard(
  overrides: Partial<ComponentProps<typeof SiteCreateWizard>> = {},
) {
  return render(<SiteCreateWizard {...overrides} />);
}

function chooseTraditionalAndOpenDetails(siteName = "Acme"): void {
  expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
  expect(
    (screen.getByTestId("site-create-type-traditional") as HTMLInputElement)
      .checked,
  ).toBe(true);
  fireEvent.click(screen.getByTestId("site-create-next"));
  expect(screen.getByTestId("site-create-step-details")).toBeTruthy();
  fireEvent.change(screen.getByTestId("site-create-name"), {
    target: { value: siteName },
  });
}

describe("SiteCreateWizard (#3522 / #3002)", () => {
  it("renders the type picker first with Traditional selected", () => {
    renderWizard();
    expect(screen.getByTestId("site-create-wizard")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
    expect(screen.getByTestId("site-create-type-traditional")).toBeTruthy();
    expect(screen.getByTestId("site-create-type-page")).toBeTruthy();
    expect(screen.getByTestId("site-create-type-virtual")).toBeTruthy();
    expect(
      (screen.getByTestId("site-create-type-traditional") as HTMLInputElement)
        .checked,
    ).toBe(true);
    expect(screen.getByTestId("site-create-traditional-note")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-count").textContent).toMatch(
      /Step 1.*of 4/,
    );
    expect(screen.queryByTestId("site-create-step-details")).toBeNull();
    expect(screen.queryByTestId("site-create-template-name")).toBeNull();
  });

  it("blocks Next on Page and Virtual with a clear message", () => {
    renderWizard();
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(false);

    fireEvent.click(screen.getByTestId("site-create-type-page"));
    expect(
      (screen.getByTestId("site-create-type-page") as HTMLInputElement).checked,
    ).toBe(true);
    expect(screen.getByTestId("site-create-type-unavailable")).toBeTruthy();
    expect(next.disabled).toBe(true);
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
    expect(screen.queryByTestId("site-create-step-details")).toBeNull();

    fireEvent.click(screen.getByTestId("site-create-type-virtual"));
    expect(
      (screen.getByTestId("site-create-type-virtual") as HTMLInputElement)
        .checked,
    ).toBe(true);
    expect(screen.getByTestId("site-create-type-unavailable")).toBeTruthy();
    expect(next.disabled).toBe(true);

    fireEvent.click(screen.getByTestId("site-create-type-traditional"));
    expect(next.disabled).toBe(false);
    expect(screen.queryByTestId("site-create-type-unavailable")).toBeNull();
  });

  it("Traditional skips template-name and base-template steps", () => {
    renderWizard();
    chooseTraditionalAndOpenDetails("Acme");
    expect(screen.queryByTestId("site-create-template-name")).toBeNull();
    expect(screen.queryByTestId("site-create-base-template")).toBeNull();

    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(false);
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-confirm")).toBeTruthy();
    expect(screen.getByTestId("site-create-confirm-type").textContent).toMatch(
      /Traditional/i,
    );
    expect(screen.getByTestId("site-create-confirm-summary").textContent).toContain(
      "Acme",
    );
    expect(screen.queryByTestId("site-create-template-name")).toBeNull();
    expect(screen.queryByTestId("site-create-base-template")).toBeNull();
  });

  it("Next on details is disabled until site name is valid", () => {
    renderWizard();
    fireEvent.click(screen.getByTestId("site-create-next"));
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    expect(next.disabled).toBe(false);
  });

  it("filters invalid site name characters on input", () => {
    renderWizard();
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Bad Name!" },
    });
    expect(
      (screen.getByTestId("site-create-name") as HTMLInputElement).value,
    ).toBe("BadName");
  });

  it("can opt out of managed navigation on a traditional site", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Bare" });
    renderWizard({ submit });
    chooseTraditionalAndOpenDetails("Bare");
    const checkbox = screen.getByTestId(
      "site-create-managed-nav",
    ) as HTMLInputElement;
    expect(checkbox.checked).toBe(true);
    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(false);
    fireEvent.click(screen.getByTestId("site-create-next"));
    expect(
      screen.getByTestId("site-create-confirm-managed-nav").textContent,
    ).toMatch(/No/i);
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.managedNavigation).toBe(false);
    expect(req.baseTemplateName).toBe("perc.base.plain");
    expect(req.templateName).toBe("BareTemplate");
  });

  it("Run invokes submit and fires onCreated with /Sites path", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Acme", id: "9" });
    const onCreated = vi.fn();
    renderWizard({ submit, onCreated });
    chooseTraditionalAndOpenDetails("Acme");
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
    expect(req.managedNavigation).toBe(true);
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
    chooseTraditionalAndOpenDetails("Acme");
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

  it("passes the zero serious/critical axe-core gate (type step)", async () => {
    const { container } = renderWizard();
    await renderA11yGate(container);
  });
});
