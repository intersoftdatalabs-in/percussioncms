/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { CatalogSearchFilter, matchesCatalogQuery } from "../../../main/ts/developer/CatalogSearchFilter";
import { DEV_MSG } from "../../../main/ts/developer/messages";

describe("matchesCatalogQuery", () => {
  it("matches every row on an empty or blank query", () => {
    expect(matchesCatalogQuery(["Alpha"], "")).toBe(true);
    expect(matchesCatalogQuery(["Alpha"], "   ")).toBe(true);
    expect(matchesCatalogQuery(["Alpha"], null)).toBe(true);
    expect(matchesCatalogQuery(["Alpha"], undefined)).toBe(true);
  });

  it("matches case-insensitively across any field", () => {
    expect(matchesCatalogQuery(["Default Community", "DefaultComm"], "defaultcomm")).toBe(
      true,
    );
    expect(matchesCatalogQuery(["List", "rffList"], "RFF")).toBe(true);
    expect(matchesCatalogQuery(["Alpha", "Beta"], "alp")).toBe(true);
  });

  it("trims the query before matching", () => {
    expect(matchesCatalogQuery(["Orphan"], "  orphan ")).toBe(true);
  });

  it("rejects rows with no matching field", () => {
    expect(matchesCatalogQuery(["Alpha", "Beta"], "zzz-no-such-row")).toBe(false);
    expect(matchesCatalogQuery([null, undefined], "alpha")).toBe(false);
    expect(matchesCatalogQuery([], "alpha")).toBe(false);
  });

  it("matches numeric fields by string form", () => {
    expect(matchesCatalogQuery([7, "DefaultComm"], "7")).toBe(true);
  });
});

function Harness({ onQueryChange }: { onQueryChange?: (next: string) => void }) {
  const [query, setQuery] = useState("");
  return (
    <CatalogSearchFilter
      testId="developer-qa-search"
      query={query}
      onQueryChange={(next) => {
        setQuery(next);
        onQueryChange?.(next);
      }}
    />
  );
}

describe("CatalogSearchFilter", () => {
  it("renders a labeled search input bound to the query", () => {
    const seen: string[] = [];
    render(<Harness onQueryChange={(next) => seen.push(next)} />);
    const input = screen.getByTestId("developer-qa-search");
    expect(input.getAttribute("type")).toBe("search");
    expect(screen.getByTestId("developer-qa-search-label").textContent).toBe(
      DEV_MSG.CATALOG_SEARCH_LABEL,
    );
    fireEvent.change(input, { target: { value: "alp" } });
    expect((input as HTMLInputElement).value).toBe("alp");
    expect(seen).toEqual(["alp"]);
  });

  it("clearing the input restores the empty query", () => {
    const seen: string[] = [];
    render(<Harness onQueryChange={(next) => seen.push(next)} />);
    const input = screen.getByTestId("developer-qa-search");
    fireEvent.change(input, { target: { value: "alp" } });
    fireEvent.change(input, { target: { value: "" } });
    expect((input as HTMLInputElement).value).toBe("");
    expect(seen).toEqual(["alp", ""]);
  });
});
