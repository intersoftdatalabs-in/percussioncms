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

function choosePageType(): void {
  fireEvent.click(screen.getByTestId("site-create-type-page"));
}

function chooseVirtualType(): void {
  fireEvent.click(screen.getByTestId("site-create-type-virtual"));
}

function advanceFromType(): void {
  fireEvent.click(screen.getByTestId("site-create-next"));
}

describe("SiteCreateWizard (#3521 / parent #3512)", () => {
  it("starts on the type picker with Traditional selected", () => {
    renderWizard();
    expect(screen.getByTestId("site-create-wizard")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-type")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-count").textContent).toMatch(
      /Step 1.*of 4/,
    );
    expect(
      (screen.getByTestId("site-create-type-traditional") as HTMLInputElement)
        .checked,
    ).toBe(true);
    expect(screen.getByTestId("site-create-traditional-note")).toBeTruthy();
    expect(screen.queryByTestId("site-create-step-details")).toBeNull();
  });

  it("Virtual: enables Next, hides managed nav and page template", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Docs" });
    const applyVirtual = vi.fn().mockResolvedValue({ virtual: true });
    renderWizard({ submit, applyVirtual });
    chooseVirtualType();
    expect(screen.getByTestId("site-create-virtual-note")).toBeTruthy();
    expect(screen.queryByTestId("site-create-type-unavailable")).toBeNull();
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(false);
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-details")).toBeTruthy();
    expect(screen.queryByTestId("site-create-managed-nav")).toBeNull();
    expect(screen.queryByTestId("site-create-template-name")).toBeNull();
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Docs" },
    });
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-confirm")).toBeTruthy();
    expect(screen.queryByTestId("site-create-step-template")).toBeNull();
    expect(screen.queryByTestId("site-create-confirm-template-name")).toBeNull();
    expect(screen.queryByTestId("site-create-confirm-managed-nav")).toBeNull();
    expect(screen.getByTestId("site-create-virtual-source-note")).toBeTruthy();
    fireEvent.change(screen.getByTestId("site-create-virtual-root"), {
      target: { value: "/opt/Percussion" },
    });
    fireEvent.click(next);
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.name).toBe("Docs");
    expect(req.managedNavigation).toBe(false);
    expect(req.pageBased).toBeUndefined();
    expect(req.virtualRootPath).toBe("/opt/Percussion");
    expect(applyVirtual).toHaveBeenCalledWith(
      "Docs",
      expect.objectContaining({
        sourceKind: "git-filesystem",
        rootPath: "/opt/Percussion",
      }),
    );
  });

  it("Virtual: skips PUT when root path is blank (Developer handoff)", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Handoff" });
    const applyVirtual = vi.fn();
    renderWizard({ submit, applyVirtual });
    chooseVirtualType();
    advanceFromType();
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Handoff" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.managedNavigation).toBe(false);
    expect(req.virtualRootPath).toBeUndefined();
    expect(applyVirtual).not.toHaveBeenCalled();
  });

  it("Traditional: Next on name only; no template step; nav optional", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Bare" });
    renderWizard({ submit });
    advanceFromType();
    expect(screen.getByTestId("site-create-step-details")).toBeTruthy();
    expect(screen.queryByTestId("site-create-template-name")).toBeNull();
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Bare" },
    });
    expect(next.disabled).toBe(false);
    const checkbox = screen.getByTestId(
      "site-create-managed-nav",
    ) as HTMLInputElement;
    expect(checkbox.checked).toBe(true);
    expect(checkbox.disabled).toBe(false);
    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(false);
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-confirm")).toBeTruthy();
    expect(screen.queryByTestId("site-create-step-template")).toBeNull();
    expect(screen.queryByTestId("site-create-confirm-template-name")).toBeNull();
    expect(
      screen.getByTestId("site-create-confirm-managed-nav").textContent,
    ).toMatch(/No/i);
    fireEvent.click(next);
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.name).toBe("Bare");
    expect(req.managedNavigation).toBe(false);
    expect(req.pageBased).toBeUndefined();
    expect(req.templateName).toBe("BareTemplate");
    expect(req.baseTemplateName).toBe("perc.base.plain");
  });

  it("Page: enables Next, locks managed nav, requires template", async () => {
    renderWizard();
    choosePageType();
    expect(screen.getByTestId("site-create-page-note")).toBeTruthy();
    expect(screen.getByTestId("site-create-step-count").textContent).toMatch(
      /Step 1.*of 5/,
    );
    const next = screen.getByTestId("site-create-next") as HTMLButtonElement;
    expect(next.disabled).toBe(false);
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-details")).toBeTruthy();
    const checkbox = screen.getByTestId(
      "site-create-managed-nav",
    ) as HTMLInputElement;
    expect(checkbox.checked).toBe(true);
    expect(checkbox.disabled).toBe(true);
    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(true);
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(next);
    expect(screen.getByTestId("site-create-step-template")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("site-create-base-template")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("site-create-template-name") as HTMLInputElement)
        .value,
    ).toBe("AcmeTemplate");
  });

  it("Page submit sends pageBased and forced managed nav", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "Acme", id: "9" });
    const onCreated = vi.fn();
    renderWizard({ submit, onCreated });
    choosePageType();
    advanceFromType();
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Acme" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    await waitFor(() => {
      expect(screen.getByTestId("site-create-base-template")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("site-create-template-name"), {
      target: { value: "AcmePageTmpl" },
    });
    fireEvent.change(screen.getByTestId("site-create-base-template"), {
      target: { value: "perc.base.other" },
    });
    fireEvent.click(screen.getByTestId("site-create-next"));
    expect(
      screen.getByTestId("site-create-confirm-template-name").textContent,
    ).toContain("AcmePageTmpl");
    expect(
      screen.getByTestId("site-create-confirm-base-template").textContent,
    ).toContain("perc.base.other");
    expect(
      screen.getByTestId("site-create-confirm-managed-nav").textContent,
    ).toMatch(/Yes/i);
    fireEvent.click(screen.getByTestId("site-create-next"));
    fireEvent.click(screen.getByTestId("site-create-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    });
    const req: CreateSiteRequest = submit.mock.calls[0]?.[0];
    expect(req.name).toBe("Acme");
    expect(req.pageBased).toBe(true);
    expect(req.managedNavigation).toBe(true);
    expect(req.templateName).toBe("AcmePageTmpl");
    expect(req.baseTemplateName).toBe("perc.base.other");
    expect(onCreated).toHaveBeenCalledWith(
      expect.objectContaining({
        siteName: "Acme",
        folderPath: "/Sites/Acme",
      }),
    );
  });

  it("filters invalid site name characters on input", () => {
    renderWizard();
    advanceFromType();
    fireEvent.change(screen.getByTestId("site-create-name"), {
      target: { value: "Bad Name!" },
    });
    expect(
      (screen.getByTestId("site-create-name") as HTMLInputElement).value,
    ).toBe("BadName");
  });

  it("Page Run surfaces submit errors", async () => {
    const submit = vi.fn().mockRejectedValue(new Error("duplicate site"));
    renderWizard({ submit });
    choosePageType();
    advanceFromType();
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

  it("passes the zero serious/critical axe-core gate (type step)", async () => {
    const { container } = renderWizard();
    await renderA11yGate(container);
  });
});
