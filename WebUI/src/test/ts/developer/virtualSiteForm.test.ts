/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  SOURCE_KIND_CSV_FILESYSTEM,
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_HTTP_JSON,
  SOURCE_KIND_OBJECT_STORAGE,
  SOURCE_KIND_REPOSITORY,
  SOURCE_KIND_SELECT_VALUES,
  SOURCE_KIND_SQL_DATABASE,
  emptyVirtualSiteForm,
  formToVirtualProps,
  isCsvFilesystemSourceKind,
  isGitFilesystemSourceKind,
  isHttpJsonSourceKind,
  isObjectStorageSourceKind,
  isSqlDatabaseSourceKind,
  isVirtualSourceKind,
  normalizeSourceKindOption,
  validateVirtualSiteForm,
  virtualPropsToForm,
} from "../../../main/ts/developer/virtualSiteForm";

describe("virtualSiteForm helpers", () => {
  it("SOURCE_KIND_SELECT_VALUES lists object-storage with the other product kinds (#3893)", () => {
    expect(SOURCE_KIND_SELECT_VALUES).toEqual([
      SOURCE_KIND_REPOSITORY,
      SOURCE_KIND_GIT_FILESYSTEM,
      SOURCE_KIND_CSV_FILESYSTEM,
      SOURCE_KIND_SQL_DATABASE,
      SOURCE_KIND_HTTP_JSON,
      SOURCE_KIND_OBJECT_STORAGE,
    ]);
  });

  it("normalizeSourceKindOption maps blank/repository, git-filesystem, csv-filesystem, sql-database, http-json, and object-storage", () => {
    expect(normalizeSourceKindOption(undefined)).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("  ")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("repository")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("Repository")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("git-filesystem")).toBe(SOURCE_KIND_GIT_FILESYSTEM);
    expect(normalizeSourceKindOption("Git-Filesystem")).toBe(SOURCE_KIND_GIT_FILESYSTEM);
    expect(normalizeSourceKindOption("csv-filesystem")).toBe(SOURCE_KIND_CSV_FILESYSTEM);
    expect(normalizeSourceKindOption("CSV-Filesystem")).toBe(SOURCE_KIND_CSV_FILESYSTEM);
    expect(normalizeSourceKindOption("sql-database")).toBe(SOURCE_KIND_SQL_DATABASE);
    expect(normalizeSourceKindOption("SQL-Database")).toBe(SOURCE_KIND_SQL_DATABASE);
    expect(normalizeSourceKindOption("http-json")).toBe(SOURCE_KIND_HTTP_JSON);
    expect(normalizeSourceKindOption("HTTP-JSON")).toBe(SOURCE_KIND_HTTP_JSON);
    expect(normalizeSourceKindOption("object-storage")).toBe(SOURCE_KIND_OBJECT_STORAGE);
    expect(normalizeSourceKindOption("Object-Storage")).toBe(SOURCE_KIND_OBJECT_STORAGE);
    expect(normalizeSourceKindOption("future-adapter")).toBe(SOURCE_KIND_REPOSITORY);
    expect(normalizeSourceKindOption("sql-api")).toBe(SOURCE_KIND_REPOSITORY);
  });

  it("isVirtualSourceKind treats blank/repository as traditional", () => {
    expect(isVirtualSourceKind(null)).toBe(false);
    expect(isVirtualSourceKind("")).toBe(false);
    expect(isVirtualSourceKind("repository")).toBe(false);
    expect(isVirtualSourceKind("git-filesystem")).toBe(true);
    expect(isVirtualSourceKind("csv-filesystem")).toBe(true);
    expect(isVirtualSourceKind("sql-database")).toBe(true);
    expect(isVirtualSourceKind("http-json")).toBe(true);
    expect(isVirtualSourceKind("object-storage")).toBe(true);
    expect(isGitFilesystemSourceKind("git-filesystem")).toBe(true);
    expect(isGitFilesystemSourceKind("csv-filesystem")).toBe(false);
    expect(isGitFilesystemSourceKind("sql-database")).toBe(false);
    expect(isGitFilesystemSourceKind("http-json")).toBe(false);
    expect(isGitFilesystemSourceKind("object-storage")).toBe(false);
    expect(isCsvFilesystemSourceKind("csv-filesystem")).toBe(true);
    expect(isCsvFilesystemSourceKind("git-filesystem")).toBe(false);
    expect(isCsvFilesystemSourceKind("sql-database")).toBe(false);
    expect(isCsvFilesystemSourceKind("http-json")).toBe(false);
    expect(isCsvFilesystemSourceKind("object-storage")).toBe(false);
    expect(isSqlDatabaseSourceKind("sql-database")).toBe(true);
    expect(isSqlDatabaseSourceKind("SQL-Database")).toBe(true);
    expect(isSqlDatabaseSourceKind("csv-filesystem")).toBe(false);
    expect(isSqlDatabaseSourceKind("git-filesystem")).toBe(false);
    expect(isSqlDatabaseSourceKind("http-json")).toBe(false);
    expect(isSqlDatabaseSourceKind("object-storage")).toBe(false);
    expect(isHttpJsonSourceKind("http-json")).toBe(true);
    expect(isHttpJsonSourceKind("HTTP-JSON")).toBe(true);
    expect(isHttpJsonSourceKind("sql-database")).toBe(false);
    expect(isHttpJsonSourceKind("csv-filesystem")).toBe(false);
    expect(isHttpJsonSourceKind("git-filesystem")).toBe(false);
    expect(isHttpJsonSourceKind("object-storage")).toBe(false);
    expect(isObjectStorageSourceKind("object-storage")).toBe(true);
    expect(isObjectStorageSourceKind("Object-Storage")).toBe(true);
    expect(isObjectStorageSourceKind("http-json")).toBe(false);
    expect(isObjectStorageSourceKind("sql-database")).toBe(false);
    expect(isObjectStorageSourceKind("git-filesystem")).toBe(false);
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

  it("virtualPropsToForm maps csv-filesystem and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "csv-filesystem",
      rootPath: "C:/csv-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_CSV_FILESYSTEM);
    expect(form.rootPath).toBe("C:/csv-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
      rootPath: "C:/csv-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for csv-filesystem clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
      rootPath: "  C:/csv-docs  ",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
      rootPath: "C:/csv-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("virtualPropsToForm maps sql-database and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "sql-database",
      rootPath: "C:/sql-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_SQL_DATABASE);
    expect(form.rootPath).toBe("C:/sql-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_SQL_DATABASE,
      rootPath: "C:/sql-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for sql-database clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_SQL_DATABASE,
      rootPath: "  C:/sql-docs  ",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_SQL_DATABASE,
      rootPath: "C:/sql-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
  });

  it("virtualPropsToForm maps http-json and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "http-json",
      rootPath: "C:/http-json-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_HTTP_JSON);
    expect(form.rootPath).toBe("C:/http-json-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_HTTP_JSON,
      rootPath: "C:/http-json-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for http-json clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_HTTP_JSON,
      rootPath: "  C:/http-json-docs  ",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_HTTP_JSON,
      rootPath: "C:/http-json-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(/authorization|api[_-]?key/i);
  });

  it("virtualPropsToForm maps object-storage and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "object-storage",
      rootPath: "C:/object-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_OBJECT_STORAGE);
    expect(form.rootPath).toBe("C:/object-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_OBJECT_STORAGE,
      rootPath: "C:/object-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for object-storage clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_OBJECT_STORAGE,
      rootPath: "  C:/object-docs  ",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_OBJECT_STORAGE,
      rootPath: "C:/object-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|access[_-]?key|secret|iam|s3:\/\//i,
    );
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

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
        rootPath: "C:/csv-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SQL_DATABASE,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SQL_DATABASE,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SQL_DATABASE,
        rootPath: "C:/sql-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_HTTP_JSON,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_HTTP_JSON,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_HTTP_JSON,
        rootPath: "C:/http-json-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OBJECT_STORAGE,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OBJECT_STORAGE,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OBJECT_STORAGE,
        rootPath: "C:/object-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();
  });
});
