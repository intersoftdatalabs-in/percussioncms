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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.itemmanagement.data.PSItemEditorBinaryMeta;
import com.percussion.share.dao.impl.PSContentItem;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemEditorBinarySupportTest {

  @Test
  void requireFieldNameRejectsPathTokens() {
    assertEquals("img", PSItemEditorBinarySupport.requireFieldName("img"));
    assertEquals(
        "item_file_attachment",
        PSItemEditorBinarySupport.requireFieldName("item_file_attachment"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSItemEditorBinarySupport.requireFieldName("../img"));
    assertThrows(
        IllegalArgumentException.class, () -> PSItemEditorBinarySupport.requireFieldName("img/x"));
    assertThrows(IllegalArgumentException.class, () -> PSItemEditorBinarySupport.requireFieldName(""));
  }

  @Test
  void sanitizeFilenameStripsPaths() {
    assertEquals("photo.jpg", PSItemEditorBinarySupport.sanitizeFilename("C:\\tmp\\photo.jpg"));
    assertEquals("photo.jpg", PSItemEditorBinarySupport.sanitizeFilename("/tmp/photo.jpg"));
    assertEquals(".jpg", PSItemEditorBinarySupport.extensionOf("photo.jpg"));
    assertEquals(".bin", PSItemEditorBinarySupport.extensionOf("noext"));
  }

  @Test
  void toMetaReadsSiblingFilename() {
    PSContentItem item = new PSContentItem();
    item.setId("42");
    Map<String, Object> fields = new HashMap<>();
    fields.put("img", new byte[] {1, 2});
    fields.put("img_filename", "hero.png");
    fields.put("img_type", "image/png");
    item.setFields(fields);

    PSItemEditorBinaryMeta meta = PSItemEditorBinarySupport.toMeta(item, "img");
    assertEquals("42", meta.getContentId());
    assertEquals("img", meta.getField());
    assertEquals("hero.png", meta.getFilename());
    assertEquals("image/png", meta.getContentType());
    assertTrue(meta.isPresent());
    assertFalse(PSItemEditorBinarySupport.isPresent(new byte[0]));
    assertArrayEquals(new byte[] {1, 2}, PSItemEditorBinarySupport.readStoredBytes(new byte[] {1, 2}));
    assertNull(PSItemEditorBinarySupport.readStoredBytes(new byte[0]));
    assertEquals("hero.png", PSItemEditorBinarySupport.downloadFilename("hero.png", "img"));
    assertEquals("img.bin", PSItemEditorBinarySupport.downloadFilename("  ", "img"));
    assertEquals(
        "attachment; filename=\"hero.png\"",
        PSItemEditorBinarySupport.contentDisposition("hero.png"));
    assertFalse(PSItemEditorBinarySupport.contentDisposition("a\"\r\n.png").contains("\r"));
  }

  @Test
  void writeTempRejectsOversizedBody() {
    byte[] chunk = new byte[8192];
    InputStream unbounded =
        new InputStream() {
          long remaining = PSItemEditorBinarySupport.MAX_EDITOR_BINARY_BYTES + 1;

          @Override
          public int read() {
            if (remaining <= 0) {
              return -1;
            }
            remaining--;
            return 1;
          }

          @Override
          public int read(byte[] b, int off, int len) {
            if (remaining <= 0) {
              return -1;
            }
            int n = (int) Math.min(len, remaining);
            remaining -= n;
            return n;
          }
        };
    assertThrows(
        PSItemEditorBinarySupport.TooLargeException.class,
        () -> PSItemEditorBinarySupport.writeTemp(unbounded, "big.bin", "application/octet-stream"));
  }

  @Test
  void imageFieldRequiresImagePayload() {
    assertTrue(PSItemEditorBinarySupport.isImageFieldName("img"));
    assertTrue(PSItemEditorBinarySupport.isImageFieldName("hero_image"));
    assertFalse(PSItemEditorBinarySupport.isImageFieldName("item_file_attachment"));
    assertFalse(PSItemEditorBinarySupport.isImageFieldName("img_filename"));
    assertTrue(PSItemEditorBinarySupport.isImageContentType("image/png", "x.bin"));
    assertTrue(PSItemEditorBinarySupport.isImageContentType("", "hero.jpg"));
    assertFalse(PSItemEditorBinarySupport.isImageContentType("application/pdf", "spec.pdf"));
    PSItemEditorBinarySupport.requireImagePayload("img", "image/png", "hero.png");
    assertThrows(
        IllegalArgumentException.class,
        () -> PSItemEditorBinarySupport.requireImagePayload("img", "application/pdf", "spec.pdf"));
  }

  @Test
  void writeTempAndApplyBinary() throws Exception {
    PSContentItem item = new PSContentItem();
    item.setFields(new HashMap<>());
    byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
    try (var temp =
        PSItemEditorBinarySupport.writeTemp(
            new ByteArrayInputStream(data), "note.txt", "text/plain")) {
      assertTrue(Files.exists(temp.toPath()));
      PSItemEditorBinarySupport.applyBinary(item, "item_file_attachment", temp, "note.txt", "text/plain");
      assertEquals(temp, item.getFields().get("item_file_attachment"));
      assertEquals("note.txt", item.getFields().get("item_file_attachment_filename"));
      assertEquals(".txt", item.getFields().get("item_file_attachment_ext"));
      assertEquals("text/plain", item.getFields().get("item_file_attachment_type"));
      assertArrayEquals(data, PSItemEditorBinarySupport.readStoredBytes(temp));
    }
  }

  @Test
  void clearBinaryWritesBlankNotNullSoSaveDropsStoredBytes() {
    PSContentItem item = new PSContentItem();
    Map<String, Object> fields = new HashMap<>();
    fields.put("img", "png".getBytes(StandardCharsets.UTF_8));
    fields.put("img_filename", "hero.png");
    fields.put("img_type", "image/png");
    item.setFields(fields);
    PSItemEditorBinarySupport.clearBinary(item, "img");
    assertEquals("", item.getFields().get("img"));
    assertEquals("", item.getFields().get("img_filename"));
    assertEquals("", item.getFields().get("img_ext"));
    assertEquals("", item.getFields().get("img_type"));
    assertEquals("", item.getFields().get("img_mime"));
    assertFalse(PSItemEditorBinarySupport.isPresent(item.getFields().get("img")));
    PSItemEditorBinaryMeta meta = PSItemEditorBinarySupport.toMeta(item, "img");
    assertFalse(meta.isPresent());
    assertEquals("", meta.getFilename());
  }
}
