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
package com.percussion.itemmanagement.service.impl;

import com.percussion.itemmanagement.data.PSItemEditorBinaryMeta;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.util.PSPurgableTempFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** Field-name sanitizing and sibling filename helpers for editor binary uploads. */
public final class PSItemEditorBinarySupport {

  private static final Pattern FIELD_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,79}$");

  private PSItemEditorBinarySupport() {}

  public static String requireFieldName(String field) {
    if (StringUtils.isBlank(field) || !FIELD_NAME.matcher(field.trim()).matches()) {
      throw new IllegalArgumentException("Invalid binary field name.");
    }
    return field.trim();
  }

  public static String sanitizeFilename(String filename) {
    if (StringUtils.isBlank(filename)) {
      return "upload.bin";
    }
    String name = filename.replace('\\', '/');
    int slash = name.lastIndexOf('/');
    if (slash >= 0) {
      name = name.substring(slash + 1);
    }
    name = name.replaceAll("[\\r\\n\\t]", "").trim();
    return StringUtils.isBlank(name) ? "upload.bin" : name;
  }

  public static String extensionOf(String filename) {
    String name = sanitizeFilename(filename);
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return ".bin";
    }
    String ext = name.substring(dot).toLowerCase(Locale.ROOT);
    if (!ext.matches("^\\.[A-Za-z0-9]{1,12}$")) {
      return ".bin";
    }
    return ext;
  }

  public static String sibling(String field, String suffix) {
    return field + suffix;
  }

  public static String firstNonBlank(Map<String, Object> fields, String... names) {
    if (fields == null) {
      return "";
    }
    for (String name : names) {
      Object raw = fields.get(name);
      if (raw instanceof CharSequence text && StringUtils.isNotBlank(text)) {
        return text.toString().trim();
      }
    }
    return "";
  }

  public static boolean isPresent(Object raw) {
    if (raw == null) {
      return false;
    }
    if (raw instanceof byte[] bytes) {
      return bytes.length > 0;
    }
    if (raw instanceof CharSequence text) {
      return StringUtils.isNotBlank(text);
    }
    return true;
  }

  public static PSItemEditorBinaryMeta toMeta(PSContentItem item, String field) {
    PSItemEditorBinaryMeta meta = new PSItemEditorBinaryMeta();
    if (item != null) {
      meta.setContentId(item.getId());
    }
    meta.setField(field);
    Map<String, Object> fields = item == null ? null : item.getFields();
    meta.setFilename(
        firstNonBlank(fields, sibling(field, "_filename"), sibling(field, "_ext")));
    meta.setContentType(firstNonBlank(fields, sibling(field, "_type"), sibling(field, "_mime")));
    meta.setPresent(fields != null && isPresent(fields.get(field)));
    return meta;
  }

  public static PSPurgableTempFile writeTemp(
      InputStream in, String filename, String contentType) throws IOException {
    String safeName = sanitizeFilename(filename);
    String ext = extensionOf(safeName);
    PSPurgableTempFile temp =
        new PSPurgableTempFile("edt", ext, null, safeName, contentType, null);
    try (OutputStream out = Files.newOutputStream(temp.toPath())) {
      in.transferTo(out);
    }
    return temp;
  }

  public static void applyBinary(
      PSContentItem item, String field, PSPurgableTempFile temp, String filename, String type) {
    Map<String, Object> fields = item.getFields();
    fields.put(field, temp);
    fields.put(sibling(field, "_filename"), sanitizeFilename(filename));
    fields.put(sibling(field, "_ext"), extensionOf(filename));
    if (StringUtils.isNotBlank(type)) {
      fields.put(sibling(field, "_type"), type);
    }
    item.setFields(fields);
  }
}
