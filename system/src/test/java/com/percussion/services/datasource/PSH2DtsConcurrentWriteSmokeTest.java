/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * DTS-shaped concurrent write smoke on multiuser H2 (#548 T071 / SC-005).
 *
 * <p>Models a default-pool level of concurrent inserts/updates against a metadata-like table (page
 * path + payload) without a full Tomcat DTS stack. Asserts 0 silent loss and final row integrity.
 */
@Tag("IntegrationTest")
@DisplayName("H2 DTS concurrent write smoke (#548 T071)")
public class PSH2DtsConcurrentWriteSmokeTest {

  /** Documented default-ish pool concurrency for smoke (below CMS editor floor). */
  private static final int WRITERS = 8;

  private static final int WRITES_PER_WRITER = 25;

  @TempDir Path tempDir;

  private String jdbcUrl;

  @BeforeEach
  void openDatabase() throws Exception {
    Path dbDir = tempDir.resolve("h2dts");
    Files.createDirectories(dbDir);
    String path = dbDir.resolve("percmetadata").toAbsolutePath().toString().replace('\\', '/');
    String server =
        "file:" + path + ";DB_CLOSE_ON_EXIT=FALSE;FILE_LOCK=NO;LOCK_TIMEOUT=10000";
    jdbcUrl = PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, server);
    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);

    try (Connection c = connect();
        Statement st = c.createStatement()) {
      st.execute(
          """
          CREATE TABLE METADATA_PAGE (
            ID IDENTITY PRIMARY KEY,
            PAGEPATH VARCHAR(512) NOT NULL,
            SITE VARCHAR(128) NOT NULL,
            PAYLOAD CLOB,
            VERSION INT NOT NULL
          )
          """);
      st.execute("CREATE UNIQUE INDEX UX_META_PATH_SITE ON METADATA_PAGE (PAGEPATH, SITE)");
      c.commit();
    }
  }

  private Connection connect() throws SQLException {
    Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
    c.setAutoCommit(false);
    return c;
  }

  @Test
  @DisplayName("Concurrent DTS-shaped inserts/upserts leave full row set with no corruption")
  void concurrentMetadataWrites_noSilentLoss() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(WRITERS);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger ok = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();
    List<String> errors = java.util.Collections.synchronizedList(new ArrayList<>());
    List<Future<?>> futures = new ArrayList<>();

    for (int w = 0; w < WRITERS; w++) {
      final int writer = w;
      futures.add(
          pool.submit(
              () -> {
                try {
                  if (!start.await(30, TimeUnit.SECONDS)) {
                    fail.incrementAndGet();
                    errors.add("writer-" + writer + " latch timeout");
                    return;
                  }
                  for (int i = 0; i < WRITES_PER_WRITER; i++) {
                    String path = "/site/page-" + writer + "-" + i;
                    upsertPage(path, "site-" + (writer % 3), "payload-" + writer + "-" + i);
                  }
                  ok.incrementAndGet();
                } catch (Exception ex) {
                  fail.incrementAndGet();
                  errors.add(
                      "writer-"
                          + writer
                          + ": "
                          + ex.getClass().getSimpleName()
                          + ": "
                          + ex.getMessage());
                }
              }));
    }

    start.countDown();
    for (Future<?> f : futures) {
      f.get(90, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

    assertEquals(0, fail.get(), "no writer failures: " + errors);
    assertEquals(WRITERS, ok.get());

    int expected = WRITERS * WRITES_PER_WRITER;
    try (Connection c = connect();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM METADATA_PAGE")) {
      assertTrue(rs.next());
      assertEquals(expected, rs.getInt(1), "every concurrent write must land exactly once");
      c.commit();
    }

    // Spot-check payloads not blank / version positive
    try (Connection c = connect();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT COUNT(*) FROM METADATA_PAGE WHERE PAYLOAD IS NULL OR VERSION < 1")) {
      assertTrue(rs.next());
      assertEquals(0, rs.getInt(1), "no null payload or zero version");
      c.commit();
    }
  }

  private void upsertPage(String pagePath, String site, String payload) throws SQLException {
    try (Connection c = connect()) {
      try (PreparedStatement sel =
          c.prepareStatement(
              "SELECT ID, VERSION FROM METADATA_PAGE WHERE PAGEPATH = ? AND SITE = ? FOR UPDATE")) {
        sel.setString(1, pagePath);
        sel.setString(2, site);
        try (ResultSet rs = sel.executeQuery()) {
          if (rs.next()) {
            int id = rs.getInt(1);
            int ver = rs.getInt(2);
            // Optimistic version check: refuse lost updates if VERSION changed under us
            try (PreparedStatement upd =
                c.prepareStatement(
                    "UPDATE METADATA_PAGE SET PAYLOAD = ?, VERSION = ? "
                        + "WHERE ID = ? AND VERSION = ?")) {
              upd.setString(1, payload);
              upd.setInt(2, ver + 1);
              upd.setInt(3, id);
              upd.setInt(4, ver);
              int n = upd.executeUpdate();
              assertEquals(
                  1,
                  n,
                  "expected optimistic update of id=" + id + " at version=" + ver);
            }
          } else {
            try (PreparedStatement ins =
                c.prepareStatement(
                    "INSERT INTO METADATA_PAGE (PAGEPATH, SITE, PAYLOAD, VERSION) VALUES (?, ?, ?, 1)")) {
              ins.setString(1, pagePath);
              ins.setString(2, site);
              ins.setString(3, payload);
              assertEquals(1, ins.executeUpdate());
            }
          }
        }
      }
      c.commit();
    }
  }
}
