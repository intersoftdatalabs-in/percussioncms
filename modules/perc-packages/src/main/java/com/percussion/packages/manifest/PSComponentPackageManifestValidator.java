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

package com.percussion.packages.manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Structural validation for {@link PSComponentPackageManifest}.
 *
 * <p>Enforces schema version, required identity fields, non-blank nested names, portable
 * package-relative path refs (URL-style {@code /} separators; no absolute OS paths), and the
 * presence of at least one content type or template (a component must ship something installable).
 */
public final class PSComponentPackageManifestValidator {

  private static final Pattern SCHEMA_VERSION = Pattern.compile("^\\d+\\.\\d+$");

  /**
   * Package-relative path: non-empty, no drive letter / UNC / leading root, no backslashes, no
   * {@code ..} path segments. URL / zip entry style only. Double-dot <em>filenames</em> (e.g.
   * {@code logo..png}) are allowed; only segment-shaped {@code ..} is rejected.
   */
  private static final Pattern RELATIVE_PACKAGE_PATH =
      Pattern.compile(
          "^(?!/)(?!\\\\)(?![A-Za-z]:)(?!\\\\\\\\)(?!.*(?:^|/)\\.\\.(?:/|$))[A-Za-z0-9_./\\-]+$");

  private PSComponentPackageManifestValidator() {
    // utility
  }

  /**
   * Validate a manifest; throw on the first failure.
   *
   * @param manifest non-null model
   * @throws PSComponentPackageManifestException when invalid
   */
  public static void validate(PSComponentPackageManifest manifest)
      throws PSComponentPackageManifestException {
    List<String> errors = validateCollecting(manifest);
    if (!errors.isEmpty()) {
      throw new PSComponentPackageManifestException(
          "Invalid component package manifest: " + String.join("; ", errors));
    }
  }

  /**
   * Validate and return all errors (empty list when valid).
   *
   * @param manifest non-null model
   * @return list of human-readable error messages (never null)
   */
  public static List<String> validateCollecting(PSComponentPackageManifest manifest) {
    Objects.requireNonNull(manifest, "manifest");
    List<String> errors = new ArrayList<>();

    requireText(errors, "schemaVersion", manifest.getSchemaVersion());
    if (manifest.getSchemaVersion() != null
        && !manifest.getSchemaVersion().isBlank()
        && !SCHEMA_VERSION.matcher(manifest.getSchemaVersion().trim()).matches()) {
      errors.add("schemaVersion must be major.minor (e.g. 1.0)");
    }
    if (manifest.getSchemaVersion() != null
        && !PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION.equals(
            manifest.getSchemaVersion().trim())) {
      errors.add(
          "unsupported schemaVersion '"
              + manifest.getSchemaVersion()
              + "' (supported: "
              + PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION
              + ")");
    }

    requireText(errors, "id", manifest.getId());
    requireText(errors, "name", manifest.getName());
    requireText(errors, "version", manifest.getVersion());

    if (manifest.getPublisher() != null) {
      // Publisher name optional but if present must be non-blank when set
      if (manifest.getPublisher().getName() != null
          && manifest.getPublisher().getName().isBlank()) {
        errors.add("publisher.name must not be blank when present");
      }
      if (manifest.getPublisher().getUrl() != null) {
        if (manifest.getPublisher().getUrl().isBlank()) {
          // Present but empty — reject so we do not store meaningless empty values
          errors.add("publisher.url must not be blank when present");
        } else if (containsBackslash(manifest.getPublisher().getUrl())) {
          // URL may use http(s); backslash is never valid in URLs we accept
          errors.add("publisher.url must not contain backslash characters");
        }
      }
    }

    if (manifest.getDependencies() != null) {
      int i = 0;
      for (PSComponentPackageManifest.Dependency dep : manifest.getDependencies()) {
        if (dep == null) {
          errors.add("dependencies[" + i + "] is null");
        } else {
          requireText(errors, "dependencies[" + i + "].name", dep.getName());
        }
        i++;
      }
    }

    if (manifest.getCatalog() != null) {
      PSComponentPackageManifest.Catalog cat = manifest.getCatalog();
      if (cat.getThumbnail() != null && !cat.getThumbnail().isBlank()) {
        requireRelativePath(errors, "catalog.thumbnail", cat.getThumbnail());
      }
      if (cat.getIcon() != null && !cat.getIcon().isBlank()) {
        requireRelativePath(errors, "catalog.icon", cat.getIcon());
      }
    }

    int ctIndex = 0;
    if (manifest.getContentTypes() != null) {
      for (PSComponentPackageManifest.ContentTypeRef ct : manifest.getContentTypes()) {
        if (ct == null) {
          errors.add("contentTypes[" + ctIndex + "] is null");
        } else {
          requireText(errors, "contentTypes[" + ctIndex + "].name", ct.getName());
          if (ct.getRef() != null && !ct.getRef().isBlank()) {
            requireRelativePath(errors, "contentTypes[" + ctIndex + "].ref", ct.getRef());
          }
        }
        ctIndex++;
      }
    }

    int tIndex = 0;
    if (manifest.getTemplates() != null) {
      for (PSComponentPackageManifest.TemplateRef t : manifest.getTemplates()) {
        if (t == null) {
          errors.add("templates[" + tIndex + "] is null");
        } else {
          requireText(errors, "templates[" + tIndex + "].name", t.getName());
          if (t.getSourceRef() != null && !t.getSourceRef().isBlank()) {
            requireRelativePath(errors, "templates[" + tIndex + "].sourceRef", t.getSourceRef());
          }
          if (t.getBindings() != null) {
            int b = 0;
            for (PSComponentPackageManifest.Binding binding : t.getBindings()) {
              if (binding == null) {
                errors.add("templates[" + tIndex + "].bindings[" + b + "] is null");
              } else {
                requireText(
                    errors, "templates[" + tIndex + "].bindings[" + b + "].variable", binding.getVariable());
              }
              b++;
            }
          }
        }
        tIndex++;
      }
    }

    int sIndex = 0;
    if (manifest.getSlots() != null) {
      for (PSComponentPackageManifest.SlotRef slot : manifest.getSlots()) {
        if (slot == null) {
          errors.add("slots[" + sIndex + "] is null");
        } else {
          requireText(errors, "slots[" + sIndex + "].name", slot.getName());
        }
        sIndex++;
      }
    }

    int rIndex = 0;
    if (manifest.getResources() != null) {
      for (PSComponentPackageManifest.ResourceRef res : manifest.getResources()) {
        if (res == null) {
          errors.add("resources[" + rIndex + "] is null");
        } else {
          requireText(errors, "resources[" + rIndex + "].path", res.getPath());
          if (res.getPath() != null && !res.getPath().isBlank()) {
            requireRelativePath(errors, "resources[" + rIndex + "].path", res.getPath());
          }
          if (res.getTarget() != null && !res.getTarget().isBlank()) {
            requireRelativePath(errors, "resources[" + rIndex + "].target", res.getTarget());
          }
        }
        rIndex++;
      }
    }

    if (manifest.getUserPreferences() != null) {
      int u = 0;
      for (PSComponentPackageManifest.UserPreference pref : manifest.getUserPreferences()) {
        if (pref == null) {
          errors.add("userPreferences[" + u + "] is null");
        } else {
          requireText(errors, "userPreferences[" + u + "].name", pref.getName());
        }
        u++;
      }
    }

    if (manifest.getCssPreferences() != null) {
      int c = 0;
      for (PSComponentPackageManifest.CssPreference pref : manifest.getCssPreferences()) {
        if (pref == null) {
          errors.add("cssPreferences[" + c + "] is null");
        } else {
          requireText(errors, "cssPreferences[" + c + "].name", pref.getName());
        }
        c++;
      }
    }

    boolean hasCt =
        manifest.getContentTypes() != null
            && manifest.getContentTypes().stream().anyMatch(Objects::nonNull);
    boolean hasTpl =
        manifest.getTemplates() != null
            && manifest.getTemplates().stream().anyMatch(Objects::nonNull);
    if (!hasCt && !hasTpl) {
      errors.add("manifest must declare at least one contentTypes[] or templates[] entry");
    }

    return errors;
  }

  private static void requireText(List<String> errors, String field, String value) {
    if (value == null || value.isBlank()) {
      errors.add(field + " is required");
    }
  }

  private static void requireRelativePath(List<String> errors, String field, String path) {
    String p = path.trim();
    if (!RELATIVE_PACKAGE_PATH.matcher(p).matches()) {
      errors.add(
          field
              + " must be a package-relative path using '/' separators"
              + " (no absolute OS paths, no '..' segments, no backslashes): '"
              + p
              + "'");
    }
  }

  private static boolean containsBackslash(String s) {
    return s.indexOf('\\') >= 0;
  }
}
