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
    expect(form.remoteUrl).toBe("");
    expect(form.branch).toBe("");
    expect(form.configFile).toBe("_config.yaml");
    expect(form.siteKey).toBe("product-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "C:/docs",
      remoteUrl: "",
      branch: "",
      configFile: "_config.yaml",
      siteKey: "product-docs",
    });
  });

  it("virtualPropsToForm and formToVirtualProps round-trip remote URL and branch", () => {
    const form = virtualPropsToForm({
      sourceKind: "git-filesystem",
      rootPath: "product-docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "release/8.2",
      configFile: "_config.yaml",
      siteKey: "docs",
      virtual: true,
    });
    expect(form.remoteUrl).toBe("https://git.example.com/org/docs.git");
    expect(form.branch).toBe("release/8.2");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "product-docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "release/8.2",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
  });

  it("formToVirtualProps trims and nulls empty optional fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "  /opt/docs  ",
      remoteUrl: "  ",
      branch: "  ",
      configFile: "  ",
      siteKey: "",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
      rootPath: "/opt/docs",
      remoteUrl: "",
      branch: "",
      configFile: null,
      siteKey: null,
    });
  });

  it("validateVirtualSiteForm enforces root and simple config name", () => {
    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_REPOSITORY,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "C:/docs",
        remoteUrl: "",
        branch: "",
        configFile: "subdir/config.yaml",
        siteKey: "",
      }),
    ).toBe("config-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
        rootPath: "C:/docs",
        remoteUrl: "https://git.example.com/org/docs.git",
        branch: "main",
        configFile: "_config.yaml",
        siteKey: "k",
      }),
    ).toBeNull();
  });
});
