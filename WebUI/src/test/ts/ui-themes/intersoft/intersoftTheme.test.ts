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

import { describe, it, expect } from "vitest";
import { intersoftTheme } from "../../../../main/ts/ui-themes/intersoft/intersoftTheme";

describe("intersoftTheme", () => {
  it("is registered as the default intersoft distribution theme", () => {
    expect(intersoftTheme.meta.id).toBe("intersoft");
    expect(intersoftTheme.meta.isDefault).toBe(true);
    expect(intersoftTheme.meta.label).toBe("Intersoft Data Labs");
    expect(intersoftTheme.meta.version).toMatch(/^\d+\.\d+\.\d+/);
  });

  it("credits Intersoft Data Labs as the publisher and Percussion CMS as the product", () => {
    expect(intersoftTheme.brand.publisher).toBe("Intersoft Data Labs");
    expect(intersoftTheme.brand.productName).toBe("Percussion CMS");
    expect(intersoftTheme.brand.publisherUrl).toContain("intsof.com");
    expect(intersoftTheme.brand.tagline).toContain("Intelligent");
  });

  it("references brand assets under the /cm/themes/intersoft/brand/ path", () => {
    expect(intersoftTheme.brand.logoHorizontal).toBe(
      "/cm/themes/intersoft/brand/intersoft-logo-horizontal.png",
    );
    expect(intersoftTheme.brand.logoMark).toBe(
      "/cm/themes/intersoft/brand/intersoft-mark.png",
    );
  });

  it("uses the sampled intersoft.com brand palette (navy + steel blue + amber accent)", () => {
    expect(intersoftTheme.colors.brand[500]).toBe("#4a6aa3");
    expect(intersoftTheme.colors.brand[900]).toBe("#0b224a");
    expect(intersoftTheme.colors.semantic.accent).toBe("#fbb03b");
    expect(intersoftTheme.colors.semantic.primary).toBe(
      intersoftTheme.colors.brand[500],
    );
    expect(intersoftTheme.colors.semantic.primaryHover).toBe(
      intersoftTheme.colors.brand[700],
    );
  });

  it("exposes a 10-step color scale", () => {
    const steps: Array<keyof typeof intersoftTheme.colors.brand> = [
      50, 100, 200, 300, 400, 500, 600, 700, 800, 900,
    ];
    for (const step of steps) {
      expect(intersoftTheme.colors.brand[step]).toMatch(/^#[0-9a-f]{6}$/i);
    }
  });

  it("flattens tokens to kebab-case CSS custom properties", () => {
    const vars = intersoftTheme.toCssVariables();
    expect(vars["--color-brand-500"]).toBe("#4a6aa3");
    expect(vars["--color-brand-900"]).toBe("#0b224a");
    expect(vars["--color-accent"]).toBe("#fbb03b");
    expect(vars["--color-text"]).toBe("#181c2c");
    expect(vars["--color-text-inverse"]).toBe("#ffffff");
    expect(vars["--color-border"]).toBe("#d8dee9");
    expect(vars["--color-danger"]).toBe("#d63637");
    expect(vars["--color-danger-text"]).toBe("#7a1a12");
    expect(vars["--color-danger-strong"]).toBe("#991b1b");
    expect(vars["--color-danger-border"]).toBe("#fecaca");
    expect(vars["--color-danger-surface"]).toBe("#fef2f2");
    expect(vars["--font-font-family"]).toContain("Inter");
    expect(vars["--font-font-family-heading"]).toContain("Rubik");
    expect(vars["--font-font-size-base"]).toBe("14");
    expect(vars["--space-scale-4"]).toBe("16");
    expect(vars["--space-radii-md"]).toBe("4");
    expect(vars["--space-shadows-md"]).toContain("rgba(11, 34, 74");
  });

  it("includes brand metadata in the CSS variable map for chrome styling", () => {
    const vars = intersoftTheme.toCssVariables();
    expect(vars["--brand-publisher"]).toBe("Intersoft Data Labs");
    expect(vars["--brand-product"]).toBe("Percussion CMS");
    expect(vars["--brand-tagline"]).toContain("Intelligent");
    expect(vars["--brand-logo-horizontal"]).toContain("intersoft-logo-horizontal.png");
    expect(vars["--brand-logo-mark"]).toContain("intersoft-mark.png");
  });

  it("is immutable at the top level", () => {
    expect(() => {
      (intersoftTheme as unknown as { meta: { id: string } }).meta.id = "x";
    }).toThrow();
  });
});
