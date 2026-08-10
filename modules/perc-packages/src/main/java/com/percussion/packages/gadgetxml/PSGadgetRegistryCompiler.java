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

package com.percussion.packages.gadgetxml;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestException;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compiles legacy product {@code GadgetRegistry.xml} into:
 *
 * <ul>
 *   <li>an aggregate modern {@link PSGadgetCatalog} ({@code gadget-catalog.json})
 *   <li>per-gadget {@link PSComponentPackageManifest} instances with {@code catalog.kind =
 *       "gadget"} (SPA / dashboard host components — no assembly templates)
 * </ul>
 *
 * <p>Gadgets are <strong>not</strong> Velocity/JEXL assembly templates; they share only the ADR-004
 * anti-XML packaging goal with widgets/pages. Per-gadget definition XML is already largely absent
 * from the tree — this compiler finishes the registry half of the migration path (issue #2771 /
 * parent #2630).
 */
public final class PSGadgetRegistryCompiler {

  /** Default package version when none is supplied. */
  public static final String DEFAULT_VERSION = "0.0.0";

  /** Catalog kind for dashboard gadgets (see component-package-manifest.md). */
  public static final String CATALOG_KIND_GADGET = "gadget";

  private PSGadgetRegistryCompiler() {
    // utility
  }

  /**
   * Compile a registry XML file.
   *
   * @param registryXmlPath path to {@code GadgetRegistry.xml}
   * @param version optional catalog / package version (may be null → {@value #DEFAULT_VERSION})
   * @return compile result with catalog + per-gadget packages
   * @throws PSGadgetRegistryException on parse/compile/validation failure
   * @throws IOException on I/O failure
   */
  public static PSGadgetRegistryCompileResult compile(Path registryXmlPath, String version)
      throws PSGadgetRegistryException, IOException {
    Objects.requireNonNull(registryXmlPath, "registryXmlPath");
    PSGadgetRegistryModel model = PSGadgetRegistryParser.parse(registryXmlPath);
    return compile(model, version);
  }

  /**
   * Compile a previously parsed registry model.
   *
   * @param model non-null parsed registry
   * @param version optional version string
   * @return compile result
   * @throws PSGadgetRegistryException on compile/validation failure
   */
  public static PSGadgetRegistryCompileResult compile(PSGadgetRegistryModel model, String version)
      throws PSGadgetRegistryException {
    Objects.requireNonNull(model, "model");
    if (model.getGadgets().isEmpty()) {
      throw new PSGadgetRegistryException("Cannot compile empty gadget registry");
    }

    String ver =
        version != null && !version.isBlank() ? version.trim() : DEFAULT_VERSION;

    PSGadgetCatalog catalog = new PSGadgetCatalog();
    catalog.setSchemaVersion(PSGadgetCatalog.SUPPORTED_SCHEMA_VERSION);
    catalog.setId("perc.gadgetCatalog");
    catalog.setName("Product Gadget Catalog");
    catalog.setVersion(ver);
    catalog.setDescription(
        "Modern product gadget catalog compiled from GadgetRegistry.xml (ADR-004 / #2771)");

    Map<String, PSComponentPackageManifest> packages = new LinkedHashMap<>();
    List<PSGadgetCatalog.Entry> entries = new ArrayList<>();

    for (PSGadgetRegistryEntry src : model.getGadgets()) {
      if (src == null) {
        continue;
      }
      String id = src.gadgetId();
      if (id == null || id.isBlank()) {
        throw new PSGadgetRegistryException(
            "Cannot derive gadget id for entry name='" + src.getName() + "'");
      }
      if (packages.containsKey(id)) {
        throw new PSGadgetRegistryException("Duplicate gadget id in registry: " + id);
      }

      PSComponentPackageManifest manifest = compileGadgetPackage(src, ver);
      packages.put(id, manifest);

      PSGadgetCatalog.Entry entry = new PSGadgetCatalog.Entry();
      entry.setId(id);
      entry.setName(src.getName());
      entry.setGroup(src.getGroup());
      entry.setBaseUri(src.getBaseUri());
      entry.setLegacyDefinitionFile(src.getLegacyDefinitionFile());
      entry.setDeprecated(src.isDeprecated());
      // Package-relative path when gadgets are written under gadgets/<id>/
      entry.setComponentPackageRef(
          "gadgets/" + id + "/" + PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
      entries.add(entry);
    }
    catalog.setGadgets(entries);

    return new PSGadgetRegistryCompileResult(model, catalog, packages);
  }

  /**
   * Compile a single registry entry into a gadget-kind component package manifest.
   *
   * @param entry non-null registry entry
   * @param version package version
   * @return validated manifest
   * @throws PSGadgetRegistryException on validation failure
   */
  public static PSComponentPackageManifest compileGadgetPackage(
      PSGadgetRegistryEntry entry, String version) throws PSGadgetRegistryException {
    Objects.requireNonNull(entry, "entry");
    String id = entry.gadgetId();
    if (id == null || id.isBlank()) {
      throw new PSGadgetRegistryException("Gadget entry missing id (name/baseuri)");
    }

    String ver =
        version != null && !version.isBlank() ? version.trim() : DEFAULT_VERSION;

    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId(id);
    manifest.setName(entry.getName() != null && !entry.getName().isBlank() ? entry.getName() : id);
    manifest.setVersion(ver);
    manifest.setDescription(
        "Dashboard gadget package for '"
            + manifest.getName()
            + "' (group="
            + (entry.getGroup() != null ? entry.getGroup() : "Custom")
            + ")");

    PSComponentPackageManifest.Publisher publisher = new PSComponentPackageManifest.Publisher();
    publisher.setName("Intersoft Data Labs, Inc.");
    publisher.setUrl("https://www.intsof.com");
    manifest.setPublisher(publisher);

    PSComponentPackageManifest.Catalog cat = new PSComponentPackageManifest.Catalog();
    cat.setKind(CATALOG_KIND_GADGET);
    cat.setTitle(manifest.getName());
    cat.setCategory(entry.getGroup() != null ? entry.getGroup() : "Custom");
    cat.setDescription(manifest.getDescription());
    cat.setAuthor("Intersoft Data Labs, Inc.");
    // Deprecated gadgets stay listed but are not palette-default visible.
    cat.setPaletteVisible(!entry.isDeprecated());
    manifest.setCatalog(cat);

    // Logical host resource: base URI folder under the classic repository tree (URL-style path).
    // Not an assembly template — validator allows catalog.kind=gadget without CT/templates.
    if (entry.getBaseUri() != null && !entry.getBaseUri().isBlank()) {
      String packageRelativeHost = toPackageRelativeHostPath(entry.getBaseUri());
      if (packageRelativeHost != null) {
        PSComponentPackageManifest.ResourceRef res = new PSComponentPackageManifest.ResourceRef();
        res.setPath("resources/host-ref.txt");
        res.setTarget(packageRelativeHost);
        res.setType("file");
        List<PSComponentPackageManifest.ResourceRef> resources = new ArrayList<>();
        resources.add(res);
        manifest.setResources(resources);
      }
    }

    try {
      PSComponentPackageManifestValidator.validate(manifest);
    } catch (PSComponentPackageManifestException e) {
      throw new PSGadgetRegistryException(
          "Compiled gadget package failed validation for id=" + id + ": " + e.getMessage(), e);
    }
    return manifest;
  }

  /**
   * Write compile result under {@code outputDir}:
   *
   * <pre>
   *   gadget-catalog.json
   *   gadgets/&lt;id&gt;/component-package.json
   *   gadgets/&lt;id&gt;/resources/host-ref.txt   (when baseUri present)
   * </pre>
   *
   * @param result non-null compile result
   * @param outputDir non-null destination root
   * @throws IOException on I/O failure
   */
  public static void writeArtifacts(PSGadgetRegistryCompileResult result, Path outputDir)
      throws IOException {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(outputDir, "outputDir");
    Files.createDirectories(outputDir);

    Path catalogPath = outputDir.resolve(PSGadgetCatalog.DEFAULT_CATALOG_FILE_NAME);
    PSGadgetCatalogIo.write(result.getCatalog(), catalogPath);

    for (Map.Entry<String, PSComponentPackageManifest> e : result.getGadgetPackages().entrySet()) {
      String id = e.getKey();
      PSComponentPackageManifest manifest = e.getValue();
      Path gadgetDir = outputDir.resolve("gadgets").resolve(id);
      Files.createDirectories(gadgetDir);
      Path manifestPath = gadgetDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
      PSComponentPackageManifestIo.write(manifest, manifestPath);

      if (manifest.getResources() != null) {
        for (PSComponentPackageManifest.ResourceRef res : manifest.getResources()) {
          if (res == null || res.getPath() == null || res.getPath().isBlank()) {
            continue;
          }
          if (res.getTarget() == null) {
            continue;
          }
          Path artifact = resolvePackageRelative(gadgetDir, res.getPath());
          Path parent = artifact.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          // host-ref.txt body is the package-relative install target (URL-style).
          Files.writeString(artifact, res.getTarget() + "\n");
        }
      }
    }
  }

  /**
   * Normalize a gadget registry {@code baseUri} to a package-relative host path using {@code /}
   * separators only. Strips all leading slashes (so both {@code /cm/gadgets/repository/x} and
   * {@code cm/gadgets/repository/x} work). Rejects blank paths and any segment containing
   * {@code ..}. Does not rewrite or inject the {@code cm/gadgets/repository/} prefix — the
   * registry {@code baseUri} is expected to already identify the install target.
   */
  static String toPackageRelativeHostPath(String baseUri) {
    if (baseUri == null || baseUri.isBlank()) {
      return null;
    }
    String p = baseUri.trim().replace('\\', '/');
    while (p.startsWith("/")) {
      p = p.substring(1);
    }
    if (p.isEmpty() || p.contains("..")) {
      return null;
    }
    return p;
  }

  /**
   * Resolve a package-relative path (URL-style {@code /}) under {@code base} using portable {@link
   * Path#resolve(String)} per segment.
   */
  static Path resolvePackageRelative(Path base, String packageRelative) {
    Path p = base;
    for (String segment : packageRelative.split("/")) {
      if (segment.isEmpty() || ".".equals(segment)) {
        continue;
      }
      if ("..".equals(segment)) {
        throw new IllegalArgumentException(
            "Package-relative path must not contain '..': " + packageRelative);
      }
      p = p.resolve(segment);
    }
    return p;
  }
}
