/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  SOURCE_KIND_CSV_FILESYSTEM,
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_HTTP_JSON,
  SOURCE_KIND_ICALENDAR,
  SOURCE_KIND_OBJECT_STORAGE,
  SOURCE_KIND_REPOSITORY,
  SOURCE_KIND_RSS_ATOM,
  SOURCE_KIND_SELECT_VALUES,
  SOURCE_KIND_ROBOTS_TXT,
  SOURCE_KIND_LLMS_TXT,
  SOURCE_KIND_OPENAPI_YAML,
  SOURCE_KIND_SITEMAP_XML,
  SOURCE_KIND_SQL_DATABASE,
  emptyVirtualSiteForm,
  formToVirtualProps,
  isCsvFilesystemSourceKind,
  isGitFilesystemSourceKind,
  isHttpJsonSourceKind,
  isIcalendarSourceKind,
  isLlmsTxtSourceKind,
  isOpenApiYamlSourceKind,
  isObjectStorageSourceKind,
  isRobotsTxtSourceKind,
  isRssAtomSourceKind,
  isSitemapXmlSourceKind,
  isSqlDatabaseSourceKind,
  isVirtualSourceKind,
  normalizeSourceKindOption,
  validateVirtualSiteForm,
  virtualPropsToForm,
} from "../../../main/ts/developer/virtualSiteForm";

describe("virtualSiteForm helpers", () => {
  it("SOURCE_KIND_SELECT_VALUES lists object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt, and openapi-yaml with the other product kinds", () => {
    expect(SOURCE_KIND_SELECT_VALUES).toEqual([
      SOURCE_KIND_REPOSITORY,
      SOURCE_KIND_GIT_FILESYSTEM,
      SOURCE_KIND_CSV_FILESYSTEM,
      SOURCE_KIND_SQL_DATABASE,
      SOURCE_KIND_HTTP_JSON,
      SOURCE_KIND_OBJECT_STORAGE,
      SOURCE_KIND_RSS_ATOM,
      SOURCE_KIND_ICALENDAR,
      SOURCE_KIND_SITEMAP_XML,
      SOURCE_KIND_ROBOTS_TXT,
      SOURCE_KIND_LLMS_TXT,
      SOURCE_KIND_OPENAPI_YAML,
    ]);
  });

  it("normalizeSourceKindOption maps blank/repository, git-filesystem, csv-filesystem, sql-database, http-json, object-storage, rss-atom, icalendar, and sitemap-xml", () => {
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
    expect(normalizeSourceKindOption("rss-atom")).toBe(SOURCE_KIND_RSS_ATOM);
    expect(normalizeSourceKindOption("RSS-Atom")).toBe(SOURCE_KIND_RSS_ATOM);
    expect(normalizeSourceKindOption("icalendar")).toBe(SOURCE_KIND_ICALENDAR);
    expect(normalizeSourceKindOption("ICalendar")).toBe(SOURCE_KIND_ICALENDAR);
    expect(normalizeSourceKindOption("sitemap-xml")).toBe(SOURCE_KIND_SITEMAP_XML);
    expect(normalizeSourceKindOption("Sitemap-XML")).toBe(SOURCE_KIND_SITEMAP_XML);
    expect(normalizeSourceKindOption("robots-txt")).toBe(SOURCE_KIND_ROBOTS_TXT);
    expect(normalizeSourceKindOption("Robots-TXT")).toBe(SOURCE_KIND_ROBOTS_TXT);
    expect(normalizeSourceKindOption("llms-txt")).toBe(SOURCE_KIND_LLMS_TXT);
    expect(normalizeSourceKindOption("Llms-TXT")).toBe(SOURCE_KIND_LLMS_TXT);
    expect(normalizeSourceKindOption("openapi-yaml")).toBe(SOURCE_KIND_OPENAPI_YAML);
    expect(normalizeSourceKindOption("OpenAPI-YAML")).toBe(SOURCE_KIND_OPENAPI_YAML);
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
    expect(isVirtualSourceKind("rss-atom")).toBe(true);
    expect(isVirtualSourceKind("icalendar")).toBe(true);
    expect(isVirtualSourceKind("sitemap-xml")).toBe(true);
    expect(isVirtualSourceKind("robots-txt")).toBe(true);
    expect(isVirtualSourceKind("llms-txt")).toBe(true);
    expect(isVirtualSourceKind("openapi-yaml")).toBe(true);
    expect(isGitFilesystemSourceKind("git-filesystem")).toBe(true);
    expect(isGitFilesystemSourceKind("csv-filesystem")).toBe(false);
    expect(isGitFilesystemSourceKind("sql-database")).toBe(false);
    expect(isGitFilesystemSourceKind("http-json")).toBe(false);
    expect(isGitFilesystemSourceKind("object-storage")).toBe(false);
    expect(isGitFilesystemSourceKind("rss-atom")).toBe(false);
    expect(isGitFilesystemSourceKind("icalendar")).toBe(false);
    expect(isGitFilesystemSourceKind("sitemap-xml")).toBe(false);
    expect(isGitFilesystemSourceKind("robots-txt")).toBe(false);
    expect(isCsvFilesystemSourceKind("csv-filesystem")).toBe(true);
    expect(isCsvFilesystemSourceKind("git-filesystem")).toBe(false);
    expect(isCsvFilesystemSourceKind("sql-database")).toBe(false);
    expect(isCsvFilesystemSourceKind("http-json")).toBe(false);
    expect(isCsvFilesystemSourceKind("object-storage")).toBe(false);
    expect(isCsvFilesystemSourceKind("rss-atom")).toBe(false);
    expect(isCsvFilesystemSourceKind("icalendar")).toBe(false);
    expect(isCsvFilesystemSourceKind("sitemap-xml")).toBe(false);
    expect(isSqlDatabaseSourceKind("sql-database")).toBe(true);
    expect(isSqlDatabaseSourceKind("SQL-Database")).toBe(true);
    expect(isSqlDatabaseSourceKind("csv-filesystem")).toBe(false);
    expect(isSqlDatabaseSourceKind("git-filesystem")).toBe(false);
    expect(isSqlDatabaseSourceKind("http-json")).toBe(false);
    expect(isSqlDatabaseSourceKind("object-storage")).toBe(false);
    expect(isSqlDatabaseSourceKind("rss-atom")).toBe(false);
    expect(isSqlDatabaseSourceKind("icalendar")).toBe(false);
    expect(isSqlDatabaseSourceKind("sitemap-xml")).toBe(false);
    expect(isHttpJsonSourceKind("http-json")).toBe(true);
    expect(isHttpJsonSourceKind("HTTP-JSON")).toBe(true);
    expect(isHttpJsonSourceKind("sql-database")).toBe(false);
    expect(isHttpJsonSourceKind("csv-filesystem")).toBe(false);
    expect(isHttpJsonSourceKind("git-filesystem")).toBe(false);
    expect(isHttpJsonSourceKind("object-storage")).toBe(false);
    expect(isHttpJsonSourceKind("rss-atom")).toBe(false);
    expect(isHttpJsonSourceKind("icalendar")).toBe(false);
    expect(isHttpJsonSourceKind("sitemap-xml")).toBe(false);
    expect(isObjectStorageSourceKind("object-storage")).toBe(true);
    expect(isObjectStorageSourceKind("Object-Storage")).toBe(true);
    expect(isObjectStorageSourceKind("http-json")).toBe(false);
    expect(isObjectStorageSourceKind("sql-database")).toBe(false);
    expect(isObjectStorageSourceKind("git-filesystem")).toBe(false);
    expect(isObjectStorageSourceKind("rss-atom")).toBe(false);
    expect(isObjectStorageSourceKind("icalendar")).toBe(false);
    expect(isObjectStorageSourceKind("sitemap-xml")).toBe(false);
    expect(isRssAtomSourceKind("rss-atom")).toBe(true);
    expect(isRssAtomSourceKind("RSS-Atom")).toBe(true);
    expect(isRssAtomSourceKind("object-storage")).toBe(false);
    expect(isRssAtomSourceKind("http-json")).toBe(false);
    expect(isRssAtomSourceKind("git-filesystem")).toBe(false);
    expect(isRssAtomSourceKind("icalendar")).toBe(false);
    expect(isRssAtomSourceKind("sitemap-xml")).toBe(false);
    expect(isIcalendarSourceKind("icalendar")).toBe(true);
    expect(isIcalendarSourceKind("ICalendar")).toBe(true);
    expect(isIcalendarSourceKind("rss-atom")).toBe(false);
    expect(isIcalendarSourceKind("object-storage")).toBe(false);
    expect(isIcalendarSourceKind("git-filesystem")).toBe(false);
    expect(isIcalendarSourceKind("sitemap-xml")).toBe(false);
    expect(isSitemapXmlSourceKind("sitemap-xml")).toBe(true);
    expect(isSitemapXmlSourceKind("Sitemap-XML")).toBe(true);
    expect(isSitemapXmlSourceKind("icalendar")).toBe(false);
    expect(isSitemapXmlSourceKind("rss-atom")).toBe(false);
    expect(isSitemapXmlSourceKind("git-filesystem")).toBe(false);
    expect(isSitemapXmlSourceKind("robots-txt")).toBe(false);
    expect(isRobotsTxtSourceKind("robots-txt")).toBe(true);
    expect(isRobotsTxtSourceKind("Robots-TXT")).toBe(true);
    expect(isRobotsTxtSourceKind("sitemap-xml")).toBe(false);
    expect(isRobotsTxtSourceKind("git-filesystem")).toBe(false);
    expect(isLlmsTxtSourceKind("llms-txt")).toBe(true);
    expect(isLlmsTxtSourceKind("Llms-TXT")).toBe(true);
    expect(isLlmsTxtSourceKind("robots-txt")).toBe(false);
    expect(isLlmsTxtSourceKind("git-filesystem")).toBe(false);
    expect(isOpenApiYamlSourceKind("openapi-yaml")).toBe(true);
    expect(isOpenApiYamlSourceKind("OpenAPI-YAML")).toBe(true);
    expect(isOpenApiYamlSourceKind("llms-txt")).toBe(false);
    expect(isOpenApiYamlSourceKind("git-filesystem")).toBe(false);
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

  it("virtualPropsToForm maps rss-atom and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "rss-atom",
      rootPath: "C:/rss-atom-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_RSS_ATOM);
    expect(form.rootPath).toBe("C:/rss-atom-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_RSS_ATOM,
      rootPath: "C:/rss-atom-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for rss-atom clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_RSS_ATOM,
      rootPath: "  C:/rss-atom-docs  ",
      remoteUrl: "https://feeds.example.com/blog.xml",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_RSS_ATOM,
      rootPath: "C:/rss-atom-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|feed[_-]?url|credential|token/i,
    );
  });

  it("virtualPropsToForm maps icalendar and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "icalendar",
      rootPath: "C:/icalendar-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_ICALENDAR);
    expect(form.rootPath).toBe("C:/icalendar-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_ICALENDAR,
      rootPath: "C:/icalendar-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for icalendar clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_ICALENDAR,
      rootPath: "  C:/icalendar-docs  ",
      remoteUrl: "https://caldav.example.com/calendar.ics",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_ICALENDAR,
      rootPath: "C:/icalendar-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|caldav|credential|token/i,
    );
  });

  it("virtualPropsToForm maps sitemap-xml and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "sitemap-xml",
      rootPath: "C:/sitemap-xml-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_SITEMAP_XML);
    expect(form.rootPath).toBe("C:/sitemap-xml-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_SITEMAP_XML,
      rootPath: "C:/sitemap-xml-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for sitemap-xml clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_SITEMAP_XML,
      rootPath: "  C:/sitemap-xml-docs  ",
      remoteUrl: "https://example.com/sitemap.xml",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_SITEMAP_XML,
      rootPath: "C:/sitemap-xml-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|crawl|credential|token/i,
    );
  });

  it("virtualPropsToForm maps robots-txt and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "robots-txt",
      rootPath: "C:/robots-txt-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_ROBOTS_TXT);
    expect(form.rootPath).toBe("C:/robots-txt-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_ROBOTS_TXT,
      rootPath: "C:/robots-txt-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for robots-txt clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_ROBOTS_TXT,
      rootPath: "  C:/robots-txt-docs  ",
      remoteUrl: "https://example.com/robots.txt",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_ROBOTS_TXT,
      rootPath: "C:/robots-txt-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|crawl|credential|token/i,
    );
  });

  it("virtualPropsToForm maps llms-txt and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "llms-txt",
      rootPath: "C:/llms-txt-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_LLMS_TXT);
    expect(form.rootPath).toBe("C:/llms-txt-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_LLMS_TXT,
      rootPath: "C:/llms-txt-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for llms-txt clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_LLMS_TXT,
      rootPath: "  C:/llms-txt-docs  ",
      remoteUrl: "https://example.com/llms.txt",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_LLMS_TXT,
      rootPath: "C:/llms-txt-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|crawl|credential|token/i,
    );
  });

  it("virtualPropsToForm maps openapi-yaml and PUT omits Git remotes", () => {
    const form = virtualPropsToForm({
      sourceKind: "openapi-yaml",
      rootPath: "C:/openapi-docs",
      virtual: true,
    });
    expect(form.sourceKind).toBe(SOURCE_KIND_OPENAPI_YAML);
    expect(form.rootPath).toBe("C:/openapi-docs");
    expect(formToVirtualProps(form)).toEqual({
      sourceKind: SOURCE_KIND_OPENAPI_YAML,
      rootPath: "C:/openapi-docs",
      remoteUrl: "",
      branch: "",
    });
  });

  it("formToVirtualProps for openapi-yaml clears leftover Git remote fields", () => {
    const body = formToVirtualProps({
      sourceKind: SOURCE_KIND_OPENAPI_YAML,
      rootPath: "  C:/openapi-docs  ",
      remoteUrl: "https://example.com/openapi.yaml",
      branch: "main",
      configFile: "_config.yaml",
      siteKey: "docs",
    });
    expect(body).toEqual({
      sourceKind: SOURCE_KIND_OPENAPI_YAML,
      rootPath: "C:/openapi-docs",
      remoteUrl: "",
      branch: "",
    });
    expect(body).not.toHaveProperty("password");
    expect(JSON.stringify(body)).not.toMatch(
      /authorization|api[_-]?key|crawl|credential|token/i,
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

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_RSS_ATOM,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_RSS_ATOM,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_RSS_ATOM,
        rootPath: "C:/rss-atom-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ICALENDAR,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ICALENDAR,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ICALENDAR,
        rootPath: "C:/icalendar-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SITEMAP_XML,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SITEMAP_XML,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_SITEMAP_XML,
        rootPath: "C:/sitemap-xml-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ROBOTS_TXT,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ROBOTS_TXT,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_ROBOTS_TXT,
        rootPath: "C:/robots-txt-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_LLMS_TXT,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_LLMS_TXT,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_LLMS_TXT,
        rootPath: "C:/llms-txt-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OPENAPI_YAML,
        rootPath: "",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-required");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OPENAPI_YAML,
        rootPath: "../escape",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBe("root-unsafe");

    expect(
      validateVirtualSiteForm({
        sourceKind: SOURCE_KIND_OPENAPI_YAML,
        rootPath: "C:/openapi-docs",
        remoteUrl: "",
        branch: "",
        configFile: "",
        siteKey: "",
      }),
    ).toBeNull();
  });
});
