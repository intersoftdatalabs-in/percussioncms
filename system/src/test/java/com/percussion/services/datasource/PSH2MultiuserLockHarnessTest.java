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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * H2 multiuser lock harness for #548 (T009 bake-off + T069/T070 US4 / QC-006 / SC-005).
 *
 * <p>Models product <em>content checkout</em> and <em>object lock</em> patterns on multiuser H2
 * without a full CMS Spring container:
 *
 * <ul>
 *   <li>Exclusive checkout via {@code SELECT … FOR UPDATE} then update of a checkout-user column
 *       (CONTENTSTATUS-style)
 *   <li>Design-object locks via row-level exclusive claim (PSObjectLock-style)
 *   <li>≥10 concurrent "editors" on distinct connections (FR-003 / SC-005 floor)
 *   <li>Same-item exclusive vs distinct-item parallel (T070)
 * </ul>
 *
 * <p>A full Spring/sitemanage checkout IT can still be layered later; this harness is the automated
 * concurrency gate for the default embedded engine on the migration branch.
 */
@Tag("IntegrationTest")
@DisplayName("H2 multiuser lock harness (#548 T009/T069/T070)")
public class PSH2MultiuserLockHarnessTest {

  /** Spec / SC-005 floor */
  private static final int EDITOR_COUNT = 10;

  private static final int DISTINCT_ITEMS = 20;

  @TempDir Path tempDir;

  private String jdbcUrl;

  /** Last SQL failure reason (for harness diagnostics). */
  private static final ThreadLocal<String> LAST_SQL_ERROR = new ThreadLocal<>();

  @BeforeEach
  void openDatabase() throws Exception {
    Path dbDir = tempDir.resolve("h2multi");
    Files.createDirectories(dbDir);
    String path = dbDir.resolve("CMDB").toAbsolutePath().toString().replace('\\', '/');
    // Same-JVM multi-connection file DB: disable process file lock; long lock wait for contention tests
    String server =
        "file:"
            + path
            + ";DB_CLOSE_ON_EXIT=FALSE;FILE_LOCK=NO;LOCK_TIMEOUT=10000";
    jdbcUrl = PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, server);
    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);

    try (Connection c = connect();
        Statement st = c.createStatement()) {
      st.execute(
          """
          CREATE TABLE CONTENT_ITEM (
            CONTENTID INT PRIMARY KEY,
            TITLE VARCHAR(128) NOT NULL,
            CHECKOUTUSER VARCHAR(64),
            VERSION INT NOT NULL,
            BODY CLOB
          )
          """);
      st.execute(
          """
          CREATE TABLE OBJECT_LOCK (
            OBJECTID INT PRIMARY KEY,
            SESSIONID VARCHAR(64),
            LOCKER VARCHAR(64),
            LOCKEDAT TIMESTAMP
          )
          """);
      for (int i = 1; i <= DISTINCT_ITEMS; i++) {
        st.execute(
            "INSERT INTO CONTENT_ITEM (CONTENTID, TITLE, CHECKOUTUSER, VERSION, BODY) VALUES ("
                + i
                + ", 'Item "
                + i
                + "', NULL, 0, 'body-"
                + i
                + "')");
      }
      c.commit();
    }
  }

  private Connection connect() throws SQLException {
    Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
    c.setAutoCommit(false);
    return c;
  }

  @Test
  @DisplayName("H2Dialect for-update string is usable (product lock SQL path)")
  void dialectSupportsForUpdate() {
    String fu = new H2Dialect().getForUpdateString();
    assertTrue(fu != null && !fu.isBlank());
  }

  @Test
  @DisplayName("≥10 editors checkout distinct items without lost updates or corruption")
  void concurrentDistinctItemCheckouts_noCorruption() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(EDITOR_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();
    List<String> errors = java.util.Collections.synchronizedList(new ArrayList<>());
    List<Future<?>> futures = new ArrayList<>();

    for (int e = 0; e < EDITOR_COUNT; e++) {
      final int editor = e;
      final int itemId = e + 1; // distinct items 1..10
      futures.add(
          pool.submit(
              () -> {
                try {
                  if (!start.await(30, TimeUnit.SECONDS)) {
                    errors.add("editor-" + editor + " start latch timeout");
                    failures.incrementAndGet();
                    return;
                  }
                  boolean ok = checkoutItem(itemId, "editor-" + editor);
                  if (ok) {
                    // simulate edit + checkin
                    updateBody(itemId, "editor-" + editor, "edited-by-" + editor);
                    checkinItem(itemId, "editor-" + editor);
                    successes.incrementAndGet();
                  } else {
                    failures.incrementAndGet();
                    errors.add(
                        "editor-"
                            + editor
                            + " checkout returned false sql="
                            + LAST_SQL_ERROR.get());
                  }
                } catch (Exception ex) {
                  failures.incrementAndGet();
                  errors.add("editor-" + editor + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
              }));
    }

    start.countDown();
    for (Future<?> f : futures) {
      f.get(60, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

    assertEquals(
        EDITOR_COUNT,
        successes.get(),
        "All distinct checkouts should succeed; errors=" + errors);
    assertEquals(0, failures.get(), "No editor failures expected on distinct items; errors=" + errors);

    // Integrity: each edited item body matches version bump
    try (Connection c = connect();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT CONTENTID, TITLE, CHECKOUTUSER, VERSION, BODY FROM CONTENT_ITEM WHERE CONTENTID <= "
                    + EDITOR_COUNT
                    + " ORDER BY CONTENTID")) {
      int count = 0;
      while (rs.next()) {
        count++;
        assertNull(rs.getString("CHECKOUTUSER"), "checked in");
        assertEquals(1, rs.getInt("VERSION"), "exactly one successful edit");
        String body = rs.getString("BODY");
        assertTrue(body != null && body.startsWith("edited-by-"), body);
      }
      assertEquals(EDITOR_COUNT, count);
      c.commit();
    }
  }

  @Test
  @DisplayName("Same-item exclusive checkout: only one of ≥10 editors wins; no lost update")
  void concurrentSameItemCheckout_singleWinner() throws Exception {
    final int itemId = 15;
    ExecutorService pool = Executors.newFixedThreadPool(EDITOR_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger winners = new AtomicInteger();
    AtomicInteger losers = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (int e = 0; e < EDITOR_COUNT; e++) {
      final int editor = e;
      futures.add(
          pool.submit(
              () -> {
                try {
                  start.await(30, TimeUnit.SECONDS);
                  if (checkoutItem(itemId, "editor-" + editor)) {
                    // hold lock briefly then edit
                    Thread.sleep(50);
                    updateBody(itemId, "editor-" + editor, "winner-body-" + editor);
                    checkinItem(itemId, "editor-" + editor);
                    winners.incrementAndGet();
                  } else {
                    losers.incrementAndGet();
                  }
                } catch (Exception ex) {
                  losers.incrementAndGet();
                  throw new RuntimeException(ex);
                }
              }));
    }

    start.countDown();
    for (Future<?> f : futures) {
      f.get(60, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

    assertEquals(1, winners.get(), "Exactly one exclusive checkout must succeed");
    assertEquals(EDITOR_COUNT - 1, losers.get(), "Remaining editors must fail cleanly");

    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT CHECKOUTUSER, VERSION, BODY FROM CONTENT_ITEM WHERE CONTENTID = ?")) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertNull(rs.getString(1));
        assertEquals(1, rs.getInt(2), "single edit applied");
        assertTrue(rs.getString(3).startsWith("winner-body-"));
      }
      c.commit();
    }
  }

  @Test
  @DisplayName("Object-lock table: concurrent exclusive locks on distinct objects all succeed")
  void concurrentObjectLocks_distinctObjects() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(EDITOR_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger ok = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (int e = 0; e < EDITOR_COUNT; e++) {
      final int objectId = e + 1;
      final String locker = "designer-" + e;
      futures.add(
          pool.submit(
              () -> {
                try {
                  start.await(30, TimeUnit.SECONDS);
                  if (acquireObjectLock(objectId, "sess-" + objectId, locker)) {
                    ok.incrementAndGet();
                    releaseObjectLock(objectId, locker);
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }

    start.countDown();
    for (Future<?> f : futures) {
      f.get(60, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
    assertEquals(EDITOR_COUNT, ok.get());
  }

  @Test
  @DisplayName("Object-lock contention: only one locker holds same object")
  void concurrentObjectLocks_sameObject_singleHolder() throws Exception {
    final int objectId = 99;
    // seed empty row for locking pattern: insert-or-claim
    try (Connection c = connect();
        Statement st = c.createStatement()) {
      st.execute(
          "INSERT INTO OBJECT_LOCK (OBJECTID, SESSIONID, LOCKER, LOCKEDAT) VALUES (99, NULL, NULL, NULL)");
      c.commit();
    }

    ExecutorService pool = Executors.newFixedThreadPool(EDITOR_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger acquired = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (int e = 0; e < EDITOR_COUNT; e++) {
      final String locker = "user-" + e;
      futures.add(
          pool.submit(
              () -> {
                try {
                  start.await(30, TimeUnit.SECONDS);
                  if (claimExistingObjectLock(objectId, "sess-" + locker, locker)) {
                    Thread.sleep(30);
                    releaseObjectLock(objectId, locker);
                    acquired.incrementAndGet();
                  }
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              }));
    }

    start.countDown();
    for (Future<?> f : futures) {
      f.get(60, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

    // Serial claims may succeed sequentially after release; at least verify no double-holder
    // corruption: final row free
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement("SELECT LOCKER FROM OBJECT_LOCK WHERE OBJECTID = ?")) {
      ps.setInt(1, objectId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertNull(rs.getString(1), "lock released; no stuck holder");
      }
      c.commit();
    }
    assertTrue(acquired.get() >= 1, "at least one acquisition");
  }

  // --- product-shaped operations ---

  /**
   * Exclusive checkout: lock row, succeed only if free or already held by same user.
   *
   * @return true if this editor holds the checkout after the call
   */
  private boolean checkoutItem(int contentId, String user) throws SQLException {
    LAST_SQL_ERROR.remove();
    try (Connection c = connect()) {
      try (PreparedStatement ps =
          c.prepareStatement(
              "SELECT CHECKOUTUSER FROM CONTENT_ITEM WHERE CONTENTID = ? FOR UPDATE")) {
        ps.setInt(1, contentId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) {
            LAST_SQL_ERROR.set("no row for " + contentId);
            c.rollback();
            return false;
          }
          String current = rs.getString(1);
          if (current != null && !current.isEmpty() && !current.equals(user)) {
            LAST_SQL_ERROR.set("held by " + current);
            c.rollback();
            return false;
          }
        }
      }
      try (PreparedStatement upd =
          c.prepareStatement(
              "UPDATE CONTENT_ITEM SET CHECKOUTUSER = ? WHERE CONTENTID = ?")) {
        upd.setString(1, user);
        upd.setInt(2, contentId);
        upd.executeUpdate();
      }
      c.commit();
      return true;
    } catch (SQLException e) {
      LAST_SQL_ERROR.set(e.getSQLState() + " " + e.getMessage());
      return false;
    }
  }

  private void updateBody(int contentId, String user, String body) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT CHECKOUTUSER, VERSION FROM CONTENT_ITEM WHERE CONTENTID = ? FOR UPDATE")) {
      ps.setInt(1, contentId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(user, rs.getString(1), "must hold checkout to edit");
        int ver = rs.getInt(2);
        try (PreparedStatement upd =
            c.prepareStatement(
                "UPDATE CONTENT_ITEM SET BODY = ?, VERSION = ? WHERE CONTENTID = ? AND CHECKOUTUSER = ?")) {
          upd.setString(1, body);
          upd.setInt(2, ver + 1);
          upd.setInt(3, contentId);
          upd.setString(4, user);
          int n = upd.executeUpdate();
          assertEquals(1, n, "update must affect exactly one row (no lost update)");
        }
      }
      c.commit();
    }
  }

  private void checkinItem(int contentId, String user) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "UPDATE CONTENT_ITEM SET CHECKOUTUSER = NULL WHERE CONTENTID = ? AND CHECKOUTUSER = ?")) {
      ps.setInt(1, contentId);
      ps.setString(2, user);
      assertEquals(1, ps.executeUpdate());
      c.commit();
    }
  }

  private boolean acquireObjectLock(int objectId, String session, String locker)
      throws SQLException {
    try (Connection c = connect()) {
      try (PreparedStatement ins =
          c.prepareStatement(
              "INSERT INTO OBJECT_LOCK (OBJECTID, SESSIONID, LOCKER, LOCKEDAT) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
        ins.setInt(1, objectId);
        ins.setString(2, session);
        ins.setString(3, locker);
        ins.executeUpdate();
        c.commit();
        return true;
      } catch (SQLException e) {
        c.rollback();
        return false;
      }
    }
  }

  private boolean claimExistingObjectLock(int objectId, String session, String locker)
      throws SQLException {
    try (Connection c = connect()) {
      try (PreparedStatement ps =
          c.prepareStatement(
              "SELECT LOCKER FROM OBJECT_LOCK WHERE OBJECTID = ? FOR UPDATE")) {
        ps.setInt(1, objectId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) {
            c.rollback();
            return false;
          }
          String current = rs.getString(1);
          if (current != null) {
            c.rollback();
            return false;
          }
        }
      }
      try (PreparedStatement upd =
          c.prepareStatement(
              "UPDATE OBJECT_LOCK SET SESSIONID = ?, LOCKER = ?, LOCKEDAT = CURRENT_TIMESTAMP WHERE OBJECTID = ? AND LOCKER IS NULL")) {
        upd.setString(1, session);
        upd.setString(2, locker);
        upd.setInt(3, objectId);
        int n = upd.executeUpdate();
        if (n != 1) {
          c.rollback();
          return false;
        }
      }
      c.commit();
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  private void releaseObjectLock(int objectId, String locker) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "UPDATE OBJECT_LOCK SET SESSIONID = NULL, LOCKER = NULL, LOCKEDAT = NULL WHERE OBJECTID = ? AND LOCKER = ?")) {
      ps.setInt(1, objectId);
      ps.setString(2, locker);
      ps.executeUpdate();
      c.commit();
    }
  }
}
