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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.itemmanagement.data.PSItemEditorBinaryMeta;
import com.percussion.share.dao.impl.PSContentItem;
import java.io.ByteArrayInputStream;
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
    }
  }
}
