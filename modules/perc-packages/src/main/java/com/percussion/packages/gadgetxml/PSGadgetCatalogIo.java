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
import java.util.ArrayList;
import java.util.Objects;

/**
 * Parse and write {@link PSGadgetCatalog} as JSON (modern gadget registry ship format).
 *
 * <p>Uses portable {@link Path} / {@link Files} I/O and UTF-8.
 */
public final class PSGadgetCatalogIo {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private PSGadgetCatalogIo() {
    // utility
  }

  /**
   * Parse a JSON string into a catalog.
   *
   * @param json non-null JSON document
   * @return parsed model (never null)
   * @throws PSGadgetRegistryException if JSON is empty or not a valid document
   */
  public static PSGadgetCatalog parse(String json) throws PSGadgetRegistryException {
    Objects.requireNonNull(json, "json");
    if (json.isBlank()) {
      throw new PSGadgetRegistryException("Gadget catalog JSON is empty");
    }
    try {
      PSGadgetCatalog catalog = GSON.fromJson(json, PSGadgetCatalog.class);
      if (catalog == null) {
        throw new PSGadgetRegistryException("Gadget catalog JSON parsed to null");
      }
      if (catalog.getGadgets() == null) {
        catalog.setGadgets(new ArrayList<>());
      }
      return catalog;
    } catch (JsonSyntaxException e) {
      throw new PSGadgetRegistryException("Invalid gadget catalog JSON: " + e.getMessage(), e);
    } catch (JsonParseException e) {
      throw new PSGadgetRegistryException("Failed to parse gadget catalog JSON", e);
    }
  }

  /**
   * Parse UTF-8 JSON from a reader.
   *
   * @param reader non-null reader
   * @return parsed model
   * @throws PSGadgetRegistryException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSGadgetCatalog read(Reader reader) throws PSGadgetRegistryException, IOException {
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
   * Read UTF-8 JSON from a file path.
   *
   * @param path non-null path
   * @return parsed model
   * @throws PSGadgetRegistryException on parse failure
   * @throws IOException on I/O failure
   */
  public static PSGadgetCatalog read(Path path) throws PSGadgetRegistryException, IOException {
    Objects.requireNonNull(path, "path");
    return parse(Files.readString(path, StandardCharsets.UTF_8));
  }

  /**
   * Serialize catalog to pretty-printed JSON.
   *
   * @param catalog non-null model
   * @return JSON text
   */
  public static String toJson(PSGadgetCatalog catalog) {
    Objects.requireNonNull(catalog, "catalog");
    return GSON.toJson(catalog);
  }

  /**
   * Write UTF-8 pretty JSON to a path (parent directories created as needed).
   *
   * @param catalog non-null model
   * @param path non-null destination
   * @throws IOException on I/O failure
   */
  public static void write(PSGadgetCatalog catalog, Path path) throws IOException {
    Objects.requireNonNull(catalog, "catalog");
    Objects.requireNonNull(path, "path");
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    // UTF-8 JSON body only (no trailing newline) — matches PSComponentPackageManifestIo.write.
    try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      GSON.toJson(catalog, w);
    }
  }
}
