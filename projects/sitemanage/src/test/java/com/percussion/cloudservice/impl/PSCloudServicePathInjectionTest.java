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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.cloudservice.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/**
 * Path-injection regression for {@link PSCloudService#generateThumbUrl}: siteName and pageId are
 * validated with {@code requireSafeFileName} before any File I/O under {@code PSServer.getRxDir()}.
 *
 * <p>Uses {@link Unsafe#allocateInstance} so validation runs without constructing Spring deps or
 * requiring a live server (same pattern as {@code PSSiteDataServicePathInjectionTest}).
 */
public class PSCloudServicePathInjectionTest {

  private static final Unsafe UNSAFE;
  private static final Method GENERATE_THUMB_URL;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      GENERATE_THUMB_URL =
          PSCloudService.class.getDeclaredMethod("generateThumbUrl", String.class, String.class);
      GENERATE_THUMB_URL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static String invokeGenerateThumbUrl(String pageId, String siteName) throws Exception {
    Object instance = UNSAFE.allocateInstance(PSCloudService.class);
    try {
      return (String) GENERATE_THUMB_URL.invoke(instance, pageId, siteName);
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw ite;
    }
  }

  @Test
  @DisplayName("generateThumbUrl rejects parent traversal in siteName before File I/O")
  void rejectsTraversalInSiteName() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> invokeGenerateThumbUrl("page1", "../escape"),
            "siteName traversal must be rejected by requireSafeFileName");
    assertTrue(
        ex.getMessage() != null && !ex.getMessage().isBlank(),
        "validator should produce a message");
  }

  @Test
  @DisplayName("generateThumbUrl rejects parent traversal in pageId before File I/O")
  void rejectsTraversalInPageId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> invokeGenerateThumbUrl("../escape", "goodSite"),
        "pageId traversal must be rejected by requireSafeFileName");
  }

  @Test
  @DisplayName("generateThumbUrl rejects path separators in siteName")
  void rejectsSlashInSiteName() {
    assertThrows(
        IllegalArgumentException.class, () -> invokeGenerateThumbUrl("page1", "site/name"));
    assertThrows(
        IllegalArgumentException.class, () -> invokeGenerateThumbUrl("page1", "site\\name"));
  }

  @Test
  @DisplayName("generateThumbUrl rejects path separators in pageId")
  void rejectsSlashInPageId() {
    assertThrows(
        IllegalArgumentException.class, () -> invokeGenerateThumbUrl("page/id", "goodSite"));
  }

  @Test
  @DisplayName("generateThumbUrl rejects NUL in siteName")
  void rejectsNulInSiteName() {
    assertThrows(
        IllegalArgumentException.class, () -> invokeGenerateThumbUrl("page1", "site\0name"));
  }
}
