/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { describe, expect, it, vi } from "vitest";
import {
  CatalogHint,
  CatalogStatus,
  SimpleCatalogTable,
} from "../../../main/ts/developer/CatalogTable";

describe("CatalogTable helpers", () => {
  it("CatalogHint renders children", () => {
    render(<CatalogHint>browse hint</CatalogHint>);
    expect(screen.getByText("browse hint")).toBeTruthy();
  });

  it("CatalogStatus supports error role and test id", () => {
    render(
      <CatalogStatus testId="cat-err" error>
        boom
      </CatalogStatus>,
    );
    const el = screen.getByTestId("cat-err");
    expect(el.getAttribute("role")).toBe("alert");
    expect(el.textContent).toBe("boom");
  });

  it("CatalogStatus non-error omits role and keeps test id", () => {
    render(<CatalogStatus testId="cat-ok">ok</CatalogStatus>);
    const el = screen.getByTestId("cat-ok");
    expect(el.getAttribute("role")).toBeNull();
    expect(el.textContent).toBe("ok");
  });

  it("SimpleCatalogTable renders columns, rows, and open cells with indexed testids", () => {
    const onOpen = vi.fn();
    render(
      <SimpleCatalogTable
        tableTestId="cat-table"
        rowTestId="cat-row"
        columns={["Name", "Id"]}
        rows={[
          {
            key: "a",
            cells: [
              <button key="o" type="button" data-testid="cat-open" onClick={onOpen}>
                Alpha
              </button>,
              "1",
            ],
          },
          { key: "b", cells: ["Beta", "2"] },
        ]}
      />,
    );
    const table = screen.getByTestId("cat-table");
    expect(table.textContent).toContain("Name");
    expect(table.textContent).toContain("Alpha");
    expect(table.textContent).toContain("Beta");
    expect(screen.getByTestId("cat-row-0")).toBeTruthy();
    expect(screen.getByTestId("cat-row-1")).toBeTruthy();
    fireEvent.click(screen.getByTestId("cat-open"));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("SimpleCatalogTable row onClick opens when provided", () => {
    const onRow = vi.fn();
    render(
      <SimpleCatalogTable
        tableTestId="cat-table"
        rowTestId="cat-row"
        columns={["Name"]}
        rows={[{ key: "r1", onClick: onRow, cells: ["Row"] }]}
      />,
    );
    fireEvent.click(screen.getByTestId("cat-row-0"));
    expect(onRow).toHaveBeenCalledTimes(1);
  });

  it("SimpleCatalogTable clickable rows support Enter and Space", () => {
    const onRow = vi.fn();
    render(
      <SimpleCatalogTable
        tableTestId="cat-table"
        rowTestId="cat-row"
        columns={["Name"]}
        rows={[{ key: "r1", onClick: onRow, cells: ["Row"] }]}
      />,
    );
    const row = screen.getByTestId("cat-row-0");
    expect(row.getAttribute("role")).toBe("button");
    expect(row.tabIndex).toBe(0);
    fireEvent.keyDown(row, { key: "Enter" });
    fireEvent.keyDown(row, { key: " " });
    expect(onRow).toHaveBeenCalledTimes(2);
  });

  it("SimpleCatalogTable copies data-* identity attrs onto the row (#3269)", () => {
    render(
      <SimpleCatalogTable
        tableTestId="cat-table"
        rowTestId="cat-row"
        columns={["Name"]}
        rows={[
          {
            key: "by-author",
            dataAttrs: { "data-df-name": "By_Author", onclick: "ignored" },
            cells: ["By_Author"],
          },
          {
            key: "by-author-date",
            dataAttrs: { "data-df-name": "By_Author_And_Date" },
            cells: ["By_Author_And_Date"],
          },
        ]}
      />,
    );
    const exact = document.querySelectorAll('tr[data-df-name="By_Author"]');
    expect(exact).toHaveLength(1);
    expect(exact[0].getAttribute("data-testid")).toBe("cat-row-0");
    expect(exact[0].getAttribute("onclick")).toBeNull();
    expect(document.querySelectorAll('tr[data-df-name="By_Author_And_Date"]')).toHaveLength(1);
  });
});
