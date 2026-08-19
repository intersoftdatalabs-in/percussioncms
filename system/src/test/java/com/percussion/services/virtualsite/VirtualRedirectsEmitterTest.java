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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Path-safety coverage for {@link VirtualRedirectsEmitter} (CodeQL #1983). */
@Tag("UnitTest")
class VirtualRedirectsEmitterTest {

  @TempDir Path tempDir;

  @Test
  void emitWritesRedirectsJsonUnderOutputRoot() throws Exception {
    Path out = tempDir.resolve("out");
    Files.createDirectories(out);
    List<VirtualRedirect> redirects =
        List.of(new VirtualRedirect("/8.2/old/", "/8.2/new/", 301));

    List<String> written = VirtualRedirectsEmitter.emit(redirects, out, List.of());

    Path map = out.resolve(VirtualRedirectsEmitter.REDIRECTS_MAP_FILE);
    assertTrue(Files.isRegularFile(map), "missing redirects.json");
    assertTrue(
        map.toAbsolutePath().normalize().startsWith(out.toAbsolutePath().normalize()),
        "redirects.json escaped outputRoot: " + map);
    assertEquals(
        map,
        PSVirtualSiteBuildService.resolveHref(out, VirtualRedirectsEmitter.REDIRECTS_MAP_FILE));
    String json = Files.readString(map, StandardCharsets.UTF_8);
    assertTrue(json.contains("/8.2/old/"), json);
    assertTrue(written.contains(VirtualRedirectsEmitter.REDIRECTS_MAP_FILE), written::toString);
  }

  @Test
  void emitRejectsUnsafeOutputRoot() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                VirtualRedirectsEmitter.emit(
                    List.of(new VirtualRedirect("/old/", "/new/", 301)),
                    Path.of("a", "..", "..", "etc"),
                    List.of()));
    assertTrue(ex.getMessage().contains(".."), ex.getMessage());
  }
}
