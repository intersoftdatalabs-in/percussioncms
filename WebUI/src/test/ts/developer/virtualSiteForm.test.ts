/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_REPOSITORY,
  emptyVirtualSiteForm,
  formToVirtualProps,
  isVirtualSourceKind,
  normalizeSourceKindOption,
  validateVirtualSiteForm,
  virtualPropsToForm,
} from "../../../main/ts/developer/virtualSiteForm";

describe("virtualSiteForm helpers", () => {
  it("normalizeSourceKindOption maps blank/repository and git-filesystem", () => {
    expect(normalizeSourceKindOption(undefined)).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("  ")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("repository")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("Repository")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("git-filesystem")).toBe(SOURCE_KIND_GIT_FILESYSTEM);
    expect(normalizeSourceKindOption("Git-Filesystem")).toBe(SOURCE_KIND_GIT_FILESYSTEM);
    expect(normalizeSourceKindOption("future-adapter")).toBe(SOURCE_KIND_REPOSITORY);
  });

  it("isVirtualSourceKind treats blank/repository as traditional", () => {
    expect(isVirtualSourceKind(null)).toBe(false);
    expect(isVirtualSourceKind("")).toBe(false);
    expect(isVirtualSourceKind("repository")).toBe(false);
    expect(isVirtualSourceKind("git-filesystem")).toBe(true);
  });

  it("virtualPropsToForm and formToVirtualProps round-trip repository clear", () => {
    const form = virtualPropsToForm({
      sourceKind: null,
      rootPath: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    });
    expect(form).toEqual(emptyVirtualSiteForm());
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_REPOSITORY,
      rootPath: null,
      configFile: null,
      siteKey: null,
    });
  });

  it("virtualPropsToForm maps git-filesystem fields", () => {
    const form = virtualPropsToForm({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      configFile: "_config.yaml",
      siteKey: "product-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_GIT_FILESYSTEM);
    expect(form.rootPath).toBe("C:/docs");
    expect(form.configFile).toBe("_config.yaml");
    expect(form.siteKey).toBe("product-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "C:/docs",
      configFile: "_config.yaml",
      siteKey: "product-docs",
    });
  });

  it("formToVirtualProps trims and nulls empty optional fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "  /opt/docs  ",
      configFile: "  ",
      siteKey: "",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "/opt/docs",
      configFile: null,
      siteKey: null,
    });
  });

  it("validateVirtualSiteForm enforces root and simple config name", () => {
    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_REPOSITORY,
        rootPath: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "../escape",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "C:/docs",
        configFile: "subdir/config.yaml",
        siteKey: "",
      }),
    ).toBe("config-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "C:/docs",
        configFile: "_config.yaml",
        siteKey: "k",
      }),
    ).toBeNull();
  });
});
