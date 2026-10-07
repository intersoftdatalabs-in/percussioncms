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
package com.percussion.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Reload of {@link PSSecurityFilter} must not publish an empty rule list. An empty list makes
 * anonymous modern-UI assets fall through to form login, and the browser then refuses the login
 * HTML as a stylesheet.
 */
@DisplayName("PSSecurityFilter config reload")
class PSSecurityFilterConfigReloadTest {

  private static final String SYSTEM_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <securityConfiguration>
        <path authType="anonymous">/cm/modern/*</path>
      </securityConfiguration>
      """;

  private static final String USER_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <securityConfiguration forceSecureLogin="no">
      </securityConfiguration>
      """;

  private Path root;
  private Path userConfig;

  @BeforeEach
  void writeConfigs() throws IOException {
    root = Files.createTempDirectory("ps-security-filter-reload");
    Path sys =
        root.resolve("WEB-INF")
            .resolve("config")
            .resolve("security")
            .resolve("system-security-conf.xml");
    userConfig =
        root.resolve("WEB-INF")
            .resolve("config")
            .resolve("user")
            .resolve("security")
            .resolve("user-security-conf.xml");
    Files.createDirectories(sys.getParent());
    Files.createDirectories(userConfig.getParent());
    Files.writeString(sys, SYSTEM_XML);
    Files.writeString(userConfig, USER_XML);
  }

  @AfterEach
  void deleteConfigs() throws IOException {
    if (root == null) {
      return;
    }
    try (var walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }

  @Test
  void unchangedMtimeKeepsTheSameEntryList() throws Exception {
    PSSecurityFilter filter = newFilter();
    List<PSSecurityFilter.SecurityEntry> first = filter.getConfiguredEntries();
    assertEquals(1, first.size());

    filter.loadConfigs();

    assertSame(first, filter.getConfiguredEntries());
    assertEquals(
        PSSecurityFilter.AuthType.ANONYMOUS,
        auth(filter, "/cm/modern/assets/perc-modern-ui.css"));
    assertEquals(PSSecurityFilter.AuthType.FORM, auth(filter, "/cm/app/spa.jsp"));
  }

  @Test
  void reloadKeepsModernAssetsAnonymousWhileOtherPathsStayForm() throws Exception {
    PSSecurityFilter filter = newFilter();
    List<PSSecurityFilter.SecurityEntry> before = filter.getConfiguredEntries();

    assertTrue(userConfig.toFile().setLastModified(userConfig.toFile().lastModified() + 2000L));
    filter.loadConfigs();

    assertTrue(filter.getConfiguredEntries() != before);
    assertEquals(1, filter.getConfiguredEntries().size());
    assertEquals(
        PSSecurityFilter.AuthType.ANONYMOUS,
        auth(filter, "/cm/modern/assets/perc-modern-ui.css"));
    assertEquals(
        PSSecurityFilter.AuthType.ANONYMOUS, auth(filter, "/cm/modern/assets/perc-modern-ui.js"));
    assertEquals(PSSecurityFilter.AuthType.FORM, auth(filter, "/cm/app/spa.jsp"));
  }

  @Test
  void concurrentReloadNeverTreatsModernCssAsFormLogin() throws Exception {
    PSSecurityFilter filter = newFilter();
    AtomicInteger misses = new AtomicInteger();
    AtomicBoolean run = new AtomicBoolean(true);
    ExecutorService pool = Executors.newFixedThreadPool(8);
    List<Future<?>> readers = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      readers.add(
          pool.submit(
              () -> {
                while (run.get()) {
                  if (auth(filter, "/cm/modern/assets/perc-modern-ui.css")
                      != PSSecurityFilter.AuthType.ANONYMOUS) {
                    misses.incrementAndGet();
                  }
                  if (auth(filter, "/cm/app/spa.jsp") != PSSecurityFilter.AuthType.FORM) {
                    misses.incrementAndGet();
                  }
                }
                return null;
              }));
    }
    Future<?> writer =
        pool.submit(
            () -> {
              long stamp = userConfig.toFile().lastModified();
              for (int n = 0; n < 30; n++) {
                stamp += 1000L;
                Files.writeString(userConfig, USER_XML + "<!-- reload " + n + " -->\n");
                assertTrue(userConfig.toFile().setLastModified(stamp));
                filter.loadConfigs();
              }
              return null;
            });

    try {
      writer.get(30, TimeUnit.SECONDS);
    } finally {
      run.set(false);
    }
    for (Future<?> reader : readers) {
      reader.get(30, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    assertEquals(0, misses.get());
    assertEquals(
        PSSecurityFilter.AuthType.ANONYMOUS, auth(filter, "/cm/modern/assets/perc-modern-ui.css"));
    assertEquals(PSSecurityFilter.AuthType.FORM, auth(filter, "/cm/app/spa.jsp"));
  }

  private PSSecurityFilter newFilter() throws Exception {
    PSSecurityFilter filter = new PSSecurityFilter();
    filter.initSecurityConfiguration(root.toString());
    return filter;
  }

  private static PSSecurityFilter.AuthType auth(PSSecurityFilter filter, String path)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath(path);
    request.setMethod("GET");
    return filter.calculateAuthType(request);
  }
}
