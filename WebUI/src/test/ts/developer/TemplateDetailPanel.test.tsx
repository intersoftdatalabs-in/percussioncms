/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  getTemplateDetail,
  listSlots,
} from "../../../main/ts/api/developer/assemblyApi";
import { TemplateDetailPanel } from "../../../main/ts/developer/TemplateDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import * as sourceViewer from "../../../main/ts/developer/templateSourceViewer";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getTemplateDetail: vi.fn(),
  listSlots: vi.fn().mockResolvedValue([]),
  updateTemplateDetail: vi.fn(),
}));

// ObjectAclSection loads ACL via separate API; stub so detail-load stays isolated.
vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

const getTemplateDetailMock = vi.mocked(getTemplateDetail);
const listSlotsMock = vi.mocked(listSlots);

const multiLineSource = "<html>\n<body>$sys.variables\n</body>\n</html>";

const sampleDetail = {
  name: "perc.page",
  label: "Page",
  description: "Page template",
  templateSource: multiLineSource,
  bindings: [{ executionOrder: 1, variable: "$x", expression: "1" }],
  slots: [{ name: "target", label: "Target" }],
  designGaps: ["read-only"],
};

describe("TemplateDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getTemplateDetailMock.mockReset();
    listSlotsMock.mockReset();
    listSlotsMock.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    delete (window as { I18N?: unknown }).I18N;
  });

  it("loads detail on success with bindings slots and source", async () => {
    getTemplateDetailMock.mockResolvedValue(sampleDetail);
    listSlotsMock.mockResolvedValue([
      { name: "target", label: "Target", description: "Main slot" },
    ]);
    const onBack = vi.fn();
    render(<TemplateDetailPanel idOrName="perc.page" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-title").textContent).toContain("Page");
    expect(screen.getByTestId("developer-tpl-bindings")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-bindings-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-lines")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-ln-1")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-ln-4")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-copy")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows Show more for long binding expressions and toggles expand", async () => {
    const longExpr =
      "$sys.item.fields.title + $sys.item.fields.summary + $sys.variables.site + $sys.page";
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      templateSource: multiLineSource,
      bindings: [
        { executionOrder: 1, variable: "$short", expression: "1" },
        { executionOrder: 2, variable: "$long", expression: longExpr },
      ],
    });
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-binding-expr-1")).toBeTruthy();
    });

    // Short expression: no expand control
    expect(screen.queryByTestId("developer-tpl-binding-expr-expand-0")).toBeNull();

    // Long expression: Show more present, aria-expanded false
    const expandBtn = screen.getByTestId("developer-tpl-binding-expr-expand-1");
    expect(expandBtn.textContent).toMatch(/Show more/i);
    expect(expandBtn.getAttribute("aria-expanded")).toBe("false");

    const expr = screen.getByTestId("developer-tpl-binding-expr-1") as HTMLTextAreaElement;
    expect(expr.value).toBe(longExpr);
    expect(expr.style.maxWidth).toBe("320px");
    expect(expr.style.overflow).toBe("hidden");

    fireEvent.click(expandBtn);
    expect(expandBtn.getAttribute("aria-expanded")).toBe("true");
    expect(expandBtn.textContent).toMatch(/Show less/i);
    expect(expr.style.overflow).toBe("auto");

    fireEvent.click(expandBtn);
    expect(expandBtn.getAttribute("aria-expanded")).toBe("false");
    expect(expandBtn.textContent).toMatch(/Show more/i);
  });

  it("shows empty bindings slots and source sections when detail has none", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.empty",
      label: "Empty",
      bindings: [],
      slots: [],
      templateSource: "",
      designGaps: [],
    });
    listSlotsMock.mockResolvedValue([]);
    render(<TemplateDetailPanel idOrName="perc.empty" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-title")).toBeTruthy();
    });
    const bindings = screen.getByTestId("developer-tpl-bindings");
    expect(bindings.textContent).toContain(DEV_MSG.TPL_NONE);
    expect(screen.queryByTestId("developer-tpl-bindings-table")).toBeNull();

    const slots = screen.getByTestId("developer-tpl-slots");
    expect(slots.textContent).toContain(DEV_MSG.TPL_NONE);
    expect(screen.queryByTestId("developer-tpl-slots-table")).toBeNull();

    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    const sourceEdit = screen.getByTestId("developer-tpl-source-edit") as HTMLTextAreaElement;
    expect(sourceEdit.value).toBe("");
  });

  it("toggles preview highlight and copy feedback", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      templateSource: multiLineSource,
    });
    const copySpy = vi.spyOn(sourceViewer, "copyTextToClipboard").mockResolvedValue(true);
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-tpl-source-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-source-hl-1")).toBeTruthy();
    expect(screen.queryByTestId("developer-tpl-source-edit")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-tpl-source-copy"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-copy-feedback").textContent).toMatch(
        /Copied/i,
      );
    });
    expect(copySpy).toHaveBeenCalledWith(multiLineSource);

    fireEvent.click(screen.getByTestId("developer-tpl-source-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    });
  });

  it("mounts ObjectAclSection with template kind and object guid (#3319)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: { stringValue: "0-4-42" },
    });
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-acl-stub")).toBeTruthy();
    });
    const acl = screen.getByTestId("developer-tpl-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("template");
    expect(acl.getAttribute("data-object-guid")).toBe("0-4-42");
    expect(screen.getByTestId("developer-tpl-detail-guid").textContent).toBe("0-4-42");
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#3319)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
    });
    render(
      <TemplateDetailPanel
        idOrName="perc.page"
        catalogGuid="0-4-7"
        onBack={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-guid").textContent).toBe("0-4-7");
    expect(screen.getByTestId("developer-tpl-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-4-7",
    );
  });

  it("uses guidString when nested guid is absent (#3319)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-4-19",
    });
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-guid").textContent).toBe("0-4-19");
    expect(screen.getByTestId("developer-tpl-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-4-19",
    );
  });

  it("synthesizes object guid from templateId when guid is omitted (#3319)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
      templateId: 12,
    });
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-guid").textContent).toBe("0-4-12");
    expect(screen.getByTestId("developer-tpl-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-4-12",
    );
  });

  it("passes empty guid to ObjectAclSection when none can be resolved (#3319)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.empty",
      label: "Empty",
      guid: undefined,
      guidString: undefined,
    });
    render(<TemplateDetailPanel idOrName="perc.empty" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-tpl-acl-stub").getAttribute("data-object-guid")).toBe("");
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getTemplateDetailMock.mockRejectedValue(new SessionRedirectError());
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-tpl-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-tpl-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getTemplateDetailMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-error").textContent).toBe(
      `${DEV_MSG.TPL_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getTemplateDetailMock.mockRejectedValue(new Error("network down"));
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-error").textContent).toBe(
      `${DEV_MSG.TPL_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-tpl-detail-title")).toBeNull();
  });

  it("shows fallback for non-Error rejection", async () => {
    getTemplateDetailMock.mockRejectedValue("boom");
    render(<TemplateDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-error").textContent).toBe(
      DEV_MSG.TPL_DETAIL_ERROR,
    );
  });

  it("keeps load errors inside the detail panel (#3377)", async () => {
    getTemplateDetailMock.mockRejectedValue(new Error("missing field"));
    render(
      <div>
        <div data-testid="developer-chrome">Developer chrome</div>
        <TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />
      </div>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-chrome")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-back")).toBeTruthy();
    expect(screen.queryByTestId("route-error")).toBeNull();
    expect(screen.queryByText(/Unable to load Developer/i)).toBeNull();
  });

  it("unwraps envelope bindings/slots and stringifies source without crashing (#3377)", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      templateSource: { wrapped: true },
      bindings: {
        TemplateBinding: [{ executionOrder: 1, variable: "$x", expression: "1" }],
      },
      slots: { Slot: [{ name: "target", label: "Target" }] },
    } as never);
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-bindings-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    expect(screen.queryByTestId("developer-tpl-detail-error")).toBeNull();
  });
});
