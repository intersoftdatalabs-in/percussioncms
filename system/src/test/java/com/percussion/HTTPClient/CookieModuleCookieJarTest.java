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
package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Behavioral tests for typed cookie-jar deserialization (#3290 / parent #2299). */
@DisplayName("CookieModule cookie jar generics")
class CookieModuleCookieJarTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("round-trips ConcurrentHashMap of Cookie entries")
  void readCookieJar_roundTripsTypedCookies() throws Exception {
    Cookie cookie = new Cookie("sid", "abc123", "example.com", "/", null, false);
    ConcurrentHashMap<Cookie, Cookie> written = new ConcurrentHashMap<>();
    written.put(cookie, cookie);

    ConcurrentHashMap<Cookie, Cookie> loaded = readJar(written);

    assertEquals(1, loaded.size());
    Cookie found = loaded.get(cookie);
    assertEquals("sid", found.getName());
    assertEquals("abc123", found.getValue());
    assertEquals("example.com", found.getDomain());
    assertEquals("/", found.getPath());
  }

  @Test
  @DisplayName("empty ConcurrentHashMap is accepted")
  void readCookieJar_emptyMap() throws Exception {
    ConcurrentHashMap<Cookie, Cookie> loaded = readJar(new ConcurrentHashMap<Cookie, Cookie>());
    assertTrue(loaded.isEmpty());
  }

  @Test
  @DisplayName("HashMap payload is rejected (saveCookies writes ConcurrentHashMap)")
  void readCookieJar_rejectsHashMap() throws Exception {
    HashMap<Cookie, Cookie> wrongType = new HashMap<>();
    Cookie cookie = new Cookie("sid", "x", "example.com", "/", null, false);
    wrongType.put(cookie, cookie);

    IOException ex = assertThrows(IOException.class, () -> readJar(wrongType));
    assertTrue(ex.getMessage().contains("ConcurrentHashMap"));
  }

  @Test
  @DisplayName("non-Cookie map entries are rejected")
  void readCookieJar_rejectsNonCookieEntries() throws Exception {
    ConcurrentHashMap<Object, Object> raw = new ConcurrentHashMap<>();
    raw.put("JSESSIONID", "not-a-cookie");

    IOException ex = assertThrows(IOException.class, () -> readJar(raw));
    assertTrue(ex.getMessage().contains("non-Cookie"));
  }

  private ConcurrentHashMap<Cookie, Cookie> readJar(Object payload) throws Exception {
    Path jar = tempDir.resolve("cookies.ser");
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(jar))) {
      oos.writeObject(payload);
    }
    try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(jar))) {
      return CookieModule.readCookieJar(ois);
    }
  }
}
