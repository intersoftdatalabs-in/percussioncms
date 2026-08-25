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

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { PageWizard } from "@/home/create/PageWizard";

function mockJsonResponse(body: unknown) {
  const text = JSON.stringify(body);
  return {
    ok: true,
    status: 200,
    statusText: "OK",
    headers: {
      get: (name: string) =>
        name.toLowerCase() === "content-type" ? "application/json" : null,
    },
    text: async () => text,
    json: async () => body,
  };
}

describe("PageWizard templates", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function stubFetch(handlers: {
    sites: unknown;
    templates: unknown;
    folders?: unknown;
    contentType?: unknown;
  }) {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockImplementation(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/sitemanage/site/")) {
        return mockJsonResponse(handlers.sites);
      }
      if (url.includes("/sitetemplates/templates/")) {
        return mockJsonResponse(handlers.templates);
      }
      if (url.includes("/contenttypes/")) {
        return mockJsonResponse(
          handlers.contentType ?? { ContentTypeDetail: { allowedTemplates: [] } },
        );
      }
      if (url.includes("/pathmanagement/path/folder")) {
        return mockJsonResponse(handlers.folders ?? []);
      }
      return mockJsonResponse([]);
    });
  }

  it("selecting a site populates template options from the TemplateSummary envelope", async () => {
    stubFetch({
      sites: {
        SiteSummary: [
          { name: "OtherSite" },
          { name: "QaSite3002" },
        ],
      },
      templates: {
        TemplateSummary: [
          { id: "1-101-7", name: "Home" },
          { templateId: "tmpl-article", templateName: "Article" },
        ],
      },
    });

    render(<PageWizard onBack={() => undefined} />);
    await waitFor(() => screen.getByTestId("page-wizard"));

    fireEvent.change(screen.getByTestId("page-wizard-site"), {
      target: { value: "QaSite3002" },
    });

    await waitFor(() => {
      const sel = screen.getByTestId("page-wizard-template") as HTMLSelectElement;
      const values = Array.from(sel.options).map((o) => o.value);
      expect(values).toContain("1-101-7");
      expect(values).toContain("tmpl-article");
    });
    const sel = screen.getByTestId("page-wizard-template") as HTMLSelectElement;
    expect(sel.options[Array.from(sel.options).findIndex((o) => o.value === "1-101-7")]?.text).toBe(
      "Home",
    );
    expect(
      sel.options[Array.from(sel.options).findIndex((o) => o.value === "tmpl-article")]?.text,
    ).toBe("Article");
  });

  it("creates at repository folderPath for FastForward SITENAME (#3726)", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockImplementation(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/sitemanage/site/")) {
        return mockJsonResponse({
          SiteSummary: [
            {
              name: "Enterprise_Investments",
              folderPath: "//Sites/EnterpriseInvestments",
            },
            {
              name: "Corporate_Investments",
              folderPath: "//Sites/CorporateInvestments",
            },
          ],
        });
      }
      if (url.includes("/sitetemplates/templates/")) {
        return mockJsonResponse({
          TemplateSummary: [
            { id: "t-db", name: "Page - Database Template" },
          ],
        });
      }
      if (url.includes("/pathmanagement/path/item")) {
        return mockJsonResponse({
          PathItem: {
            path: "/Sites/Corporate_Investments/",
            folderPath: "//Sites/CorporateInvestments",
            name: "Corporate_Investments",
          },
        });
      }
      if (url.includes("/pathmanagement/path/folder")) {
        return mockJsonResponse([]);
      }
      if (url.includes("/pagemanagement/page") && init?.method === "POST") {
        return mockJsonResponse({
          Page: { id: "1-101-3726", name: "qa-create-3726.html" },
        });
      }
      return mockJsonResponse([]);
    });

    const openCreated = vi.fn().mockResolvedValue(true);
    render(<PageWizard onBack={() => undefined} openCreated={openCreated} />);
    await waitFor(() => screen.getByTestId("page-wizard"));
    fireEvent.change(screen.getByTestId("page-wizard-site"), {
      target: { value: "Corporate_Investments" },
    });
    await waitFor(() => {
      const folder = screen.getByTestId("page-wizard-folder") as HTMLSelectElement;
      expect(folder.value).toBe("/Sites/CorporateInvestments");
    });
    fireEvent.change(screen.getByTestId("page-wizard-template"), {
      target: { value: "t-db" },
    });
    fireEvent.change(document.getElementById("pw-title") as HTMLInputElement, {
      target: { value: "Qa Create 3726" },
    });
    fireEvent.click(screen.getByTestId("page-wizard-submit"));
    await waitFor(() => {
      expect(openCreated).toHaveBeenCalled();
    });
    const postCall = fetchMock.mock.calls.find((c) => {
      const url = String(c[0]);
      const init = c[1] as RequestInit | undefined;
      return url.includes("/pagemanagement/page") && init?.method === "POST";
    });
    expect(postCall).toBeTruthy();
    const body = JSON.parse(String((postCall?.[1] as RequestInit).body));
    expect(body.Page.folderPath).toBe("//Sites/CorporateInvestments");
    expect(openCreated.mock.calls[0]?.[0]).toMatchObject({
      id: "1-101-3726",
    });
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("falls back to percPage allowedTemplates when the site catalog is an empty-bean", async () => {
    stubFetch({
      sites: {
        SiteSummary: [{ name: "A" }, { name: "B" }],
      },
      templates: { TemplateSummary: { empty: false } },
      contentType: {
        ContentTypeDetail: {
          allowedTemplates: [
            { name: "t1", label: "Landing", guid: { stringValue: "1-101-9" } },
          ],
        },
      },
    });

    render(<PageWizard onBack={() => undefined} />);
    await waitFor(() => screen.getByTestId("page-wizard"));
    fireEvent.change(screen.getByTestId("page-wizard-site"), {
      target: { value: "B" },
    });

    await waitFor(() => {
      const sel = screen.getByTestId("page-wizard-template") as HTMLSelectElement;
      expect(Array.from(sel.options).some((o) => o.value === "1-101-9")).toBe(
        true,
      );
    });
  });
});
