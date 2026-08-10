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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Parse and write {@link PSComponentPackageManifest} as JSON (ship format).
 *
 * <p>Uses portable {@link Path} / {@link Files} I/O and UTF-8. Does not validate structural rules —
 * call {@link PSComponentPackageManifestValidator} after parse when required.
 */
public final class PSComponentPackageManifestIo {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private PSComponentPackageManifestIo() {
    // utility
  }

  /**
   * Parse a JSON string into a manifest. Does not validate.
   *
   * @param json non-null JSON document
   * @return parsed model (never null)
   * @throws PSComponentPackageManifestException if JSON is empty or not a valid document
   */
  public static PSComponentPackageManifest parse(String json)
      throws PSComponentPackageManifestException {
    Objects.requireNonNull(json, "json");
    if (json.isBlank()) {
      throw new PSComponentPackageManifestException("Manifest JSON is empty");
    }
    try {
      PSComponentPackageManifest manifest = GSON.fromJson(json, PSComponentPackageManifest.class);
      if (manifest == null) {
        throw new PSComponentPackageManifestException("Manifest JSON parsed to null");
      }
      // Gson leaves collection fields null when omitted; normalize for callers.
      normalizeCollections(manifest);
      return manifest;
    } catch (JsonSyntaxException e) {
      throw new PSComponentPackageManifestException("Invalid manifest JSON: " + e.getMessage(), e);
    } catch (JsonParseException e) {
      throw new PSComponentPackageManifestException("Failed to parse manifest JSON", e);
    }
  }

  /**
   * Parse UTF-8 JSON from a reader.
   *
   * @param reader non-null reader
   * @return parsed model
   * @throws PSComponentPackageManifestException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSComponentPackageManifest parse(Reader reader)
      throws PSComponentPackageManifestException, IOException {
    Objects.requireNonNull(reader, "reader");
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[4096];
    int n;
    while ((n = reader.read(buf)) >= 0) {
      sb.append(buf, 0, n);
    }
    return parse(sb.toString());
  }

  /**
   * Read and parse a UTF-8 JSON file.
   *
   * @param path non-null path to the manifest file
   * @return parsed model
   * @throws PSComponentPackageManifestException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSComponentPackageManifest read(Path path)
      throws PSComponentPackageManifestException, IOException {
    Objects.requireNonNull(path, "path");
    String json = Files.readString(path, StandardCharsets.UTF_8);
    return parse(json);
  }

  /**
   * Serialize a manifest to pretty-printed JSON.
   *
   * @param manifest non-null model
   * @return JSON text (never null)
   */
  public static String toJson(PSComponentPackageManifest manifest) {
    Objects.requireNonNull(manifest, "manifest");
    normalizeCollections(manifest);
    return GSON.toJson(manifest);
  }

  /**
   * Write a manifest as UTF-8 JSON to a path (creates parent directories if needed).
   *
   * @param manifest non-null model
   * @param path non-null destination path
   * @throws IOException on I/O failure
   */
  public static void write(PSComponentPackageManifest manifest, Path path) throws IOException {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(path, "path");
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(path, toJson(manifest), StandardCharsets.UTF_8);
  }

  /**
   * Write a manifest as UTF-8 JSON to a writer.
   *
   * @param manifest non-null model
   * @param writer non-null writer
   * @throws IOException on I/O failure
   */
  public static void write(PSComponentPackageManifest manifest, Writer writer) throws IOException {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(writer, "writer");
    writer.write(toJson(manifest));
  }

  private static void normalizeCollections(PSComponentPackageManifest manifest) {
    if (manifest.getDependencies() == null) {
      manifest.setDependencies(new java.util.ArrayList<>());
    }
    if (manifest.getContentTypes() == null) {
      manifest.setContentTypes(new java.util.ArrayList<>());
    }
    if (manifest.getTemplates() == null) {
      manifest.setTemplates(new java.util.ArrayList<>());
    }
    if (manifest.getSlots() == null) {
      manifest.setSlots(new java.util.ArrayList<>());
    }
    if (manifest.getResources() == null) {
      manifest.setResources(new java.util.ArrayList<>());
    }
    if (manifest.getUserPreferences() == null) {
      manifest.setUserPreferences(new java.util.ArrayList<>());
    }
    if (manifest.getCssPreferences() == null) {
      manifest.setCssPreferences(new java.util.ArrayList<>());
    }
    if (manifest.getTemplates() != null) {
      for (PSComponentPackageManifest.TemplateRef t : manifest.getTemplates()) {
        if (t != null && t.getBindings() == null) {
          t.setBindings(new java.util.ArrayList<>());
        }
      }
    }
    if (manifest.getSlots() != null) {
      for (PSComponentPackageManifest.SlotRef s : manifest.getSlots()) {
        if (s == null) {
          continue;
        }
        if (s.getAllowedContentTypes() == null) {
          s.setAllowedContentTypes(new java.util.ArrayList<>());
        }
        if (s.getLayout() == null) {
          s.setLayout(new java.util.LinkedHashMap<>());
        }
        if (s.getStyles() == null) {
          s.setStyles(new java.util.LinkedHashMap<>());
        }
      }
    }
    if (manifest.getUserPreferences() != null) {
      for (PSComponentPackageManifest.UserPreference p : manifest.getUserPreferences()) {
        if (p != null && p.getEnumValues() == null) {
          p.setEnumValues(new java.util.ArrayList<>());
        }
      }
    }
  }
}
