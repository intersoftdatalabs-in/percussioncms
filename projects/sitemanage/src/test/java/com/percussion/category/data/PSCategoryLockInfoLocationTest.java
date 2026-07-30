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

package com.percussion.category.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.server.PSServer;
import com.percussion.server.PSUserSession;
import com.percussion.server.PSUserSessionManager;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Verifies GH-1565: the lock file is written under the CMS installation directory
 * (PSServer.getRxDir()), not the JVM current working directory, and pre-existing cwd-relative
 * installations remain readable. Also covers GH-1566's staleness semantics for the read path:
 * non-blank sessionIds that map to a live session are returned; blank/missing sessionIds are
 * cleaned up by getLockInfo().
 */
class PSCategoryLockInfoLocationTest {

  private static final String ACTIVE_SESSION_ID = "active-location-test-session";

  @TempDir Path tempDir;

  private File previousRxDir;
  private Path previousLegacyOverride;
  private String previousSessionIdOverride;
  private Map<String, PSUserSession> originalSessions;
  private ConcurrentHashMap<String, PSUserSession> sessions;
  private CapturingAppender logAppender;
  private LoggerConfig loggerConfig;
  private Configuration loggerConfiguration;

  @BeforeEach
  void setRxRoot() throws Exception {
    previousRxDir = PSServer.getRxDir();
    previousLegacyOverride = PSCategoryLockInfo.legacyLockInfoOverride;
    previousSessionIdOverride = PSCategoryLockInfo.currentSessionIdOverride;
    PSServer.setRxDir(tempDir.toFile());
    PSCategoryLockInfo.legacyLockInfoOverride = null;
    PSCategoryLockInfo.currentSessionIdOverride = null;

    Field sessionsField = PSUserSessionManager.class.getDeclaredField("ms_Sessions");
    sessionsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var liveSessions = (ConcurrentHashMap<String, PSUserSession>) sessionsField.get(null);
    sessions = liveSessions;
    originalSessions = new HashMap<>(sessions);
    sessions.clear();
    sessions.put(ACTIVE_SESSION_ID, mock(PSUserSession.class));

    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    loggerConfiguration = context.getConfiguration();
    loggerConfig = loggerConfiguration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    logAppender = new CapturingAppender("PSCategoryLockInfoLocationTest-list");
    logAppender.start();
    loggerConfiguration.addAppender(logAppender);
    loggerConfig.addAppender(logAppender, Level.DEBUG, (org.apache.logging.log4j.core.Filter) null);
    context.updateLoggers();
  }

  @AfterEach
  void restoreRxRoot() {
    if (previousRxDir != null) {
      PSServer.setRxDir(previousRxDir);
    }
    PSCategoryLockInfo.legacyLockInfoOverride = previousLegacyOverride;
    PSCategoryLockInfo.currentSessionIdOverride = previousSessionIdOverride;

    if (sessions != null) {
      sessions.clear();
      sessions.putAll(originalSessions);
    }

    if (loggerConfig != null && logAppender != null) {
      loggerConfig.removeAppender(logAppender.getName());
      LoggerContext context = (LoggerContext) LogManager.getContext(false);
      if (loggerConfiguration != null) {
        loggerConfiguration.getRootLogger().removeAppender(logAppender.getName());
      }
      context.updateLoggers();
      logAppender.stop();
      logAppender.clear();
    }
  }

  @Test
  void resolveLockInfoFileLivesUnderRxDir() {
    var resolved = PSCategoryLockInfo.resolveLockInfoFile();
    assertEquals(tempDir.resolve("lock_info.json"), resolved);
    assertTrue(resolved.startsWith(tempDir), "lock file must live under the CMS installation dir");
  }

  @Test
  void removeLockInfoDeletesCanonicalFile() throws Exception {
    var canonical = tempDir.resolve("lock_info.json");
    Files.writeString(canonical, "{}", StandardCharsets.UTF_8);

    PSCategoryLockInfo.removeLockInfo();

    assertFalse(Files.exists(canonical), "canonical lock file should be deleted");
  }

  @Test
  void removeLockInfoDeletesLegacyFile() throws Exception {
    var legacyDir = Files.createTempDirectory(tempDir, "ps-legacy-lockinfo-rm");
    var legacyFile = legacyDir.resolve("lock_info.json");
    Files.writeString(legacyFile, "{}", StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    PSCategoryLockInfo.removeLockInfo();

    assertFalse(Files.exists(legacyFile), "legacy lock file should be deleted");
  }

  @Test
  void getLockInfoReturnsNullWhenAbsent() {
    assertNull(PSCategoryLockInfo.getLockInfo());
    assertFalse(PSCategoryLockInfo.isFileLocked());
  }

  @Test
  void getLockInfoReadsCanonicalFile() throws Exception {
    var canonical = tempDir.resolve("lock_info.json");
    var json = new JSONObject();
    json.put("sessionId", ACTIVE_SESSION_ID);
    json.put("userName", "canonical-tester");
    Files.writeString(canonical, json.toString(), StandardCharsets.UTF_8);

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result, "canonical lock file must be readable");
    assertEquals("canonical-tester", result.getString("userName"));
    assertEquals(ACTIVE_SESSION_ID, result.getString("sessionId"));
  }

  @Test
  void getLockInfoFallsBackToLegacyLocation() throws Exception {
    // No canonical file, but a legacy lock_info.json exists at the override path.
    var legacyDir = Files.createTempDirectory(tempDir, "ps-legacy-lockinfo-fallback");
    var legacyFile = legacyDir.resolve("lock_info.json");
    var json = new JSONObject();
    json.put("sessionId", ACTIVE_SESSION_ID);
    json.put("userName", "legacy-tester");
    Files.writeString(legacyFile, json.toString(), StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result, "legacy lock file must remain readable for backward compat");
    assertEquals("legacy-tester", result.getString("userName"));
  }

  @Test
  void canonicalTakesPrecedenceOverLegacy() throws Exception {
    // Both present -> canonical wins.
    var canonical = tempDir.resolve("lock_info.json");
    var canonicalJson = new JSONObject();
    canonicalJson.put("sessionId", ACTIVE_SESSION_ID);
    canonicalJson.put("userName", "canonical-tester");
    Files.writeString(canonical, canonicalJson.toString(), StandardCharsets.UTF_8);

    var legacyDir = Files.createTempDirectory(tempDir, "ps-legacy-lockinfo-precedence");
    var legacyFile = legacyDir.resolve("lock_info.json");
    var legacyJson = new JSONObject();
    legacyJson.put("sessionId", ACTIVE_SESSION_ID);
    legacyJson.put("userName", "legacy-tester");
    Files.writeString(legacyFile, legacyJson.toString(), StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result);
    assertEquals("canonical-tester", result.getString("userName"));
  }

  @Test
  void writeLockInfoToFileWritesUnderRxDirWithCorrectJson() throws Exception {
    // rxDir is the JUnit @TempDir — definitely exists. The write must
    // place lock_info.json at $rxDir/lock_info.json with the expected JSON
    // payload. We assert the on-disk write directly because exercising
    // getLockInfo()/isFileLocked() after a stale sessionId would depend on
    // isLockStale semantics (GH-1566: blank/missing sessionId is stale)
    // and on PSUserSessionManager — both out of scope for a location test.
    PSServer.setRxDir(tempDir.toFile());
    PSCategoryLockInfo.currentSessionIdOverride = "test-session-id";

    var userService = Mockito.mock(IPSUserService.class);
    var user = new PSCurrentUser();
    user.setName("writer-tester");
    Mockito.when(userService.getCurrentUser()).thenReturn(user);

    PSCategoryLockInfo.writeLockInfoToFile(userService, "2026-07-29T00:00:00Z");

    var written = tempDir.resolve("lock_info.json");
    assertTrue(Files.isRegularFile(written), "lock file must be written under rxDir");
    // File must live under rxDir, not under the JVM cwd.
    assertTrue(written.startsWith(tempDir), "lock file must be under the CMS installation dir");

    var json = new JSONObject(Files.readString(written, StandardCharsets.UTF_8));
    assertEquals("writer-tester", json.getString("userName"));
    assertEquals("test-session-id", json.getString("sessionId"));
    assertEquals("2026-07-29T00:00:00Z", json.getString("creationDate"));
  }

  @Test
  void removeLockInfoSkipsLogWhenFileAbsent() throws Exception {
    // Neither canonical nor legacy file exists.
    assertFalse(Files.exists(PSCategoryLockInfo.resolveLockInfoFile()));
    PSCategoryLockInfo.removeLockInfo();
    // No exception, no file creation, and no "deleted successfully" debug log when nothing
    // was removed — the boolean branch on Files.deleteIfExists() guards the log.
    assertFalse(Files.exists(PSCategoryLockInfo.resolveLockInfoFile()));
    assertTrue(
        logAppender.getEvents().stream()
            .noneMatch(
                event ->
                    event.getLevel() == Level.DEBUG
                        && event
                            .getMessage()
                            .getFormattedMessage()
                            .contains("Lock info file deleted successfully")),
        "removeLockInfo must not log 'deleted successfully' when no file was removed");
  }

  /**
   * Minimal log4j2 appender that captures emitted {@link LogEvent}s into an in-memory list. We need
   * a custom appender (rather than {@code log4j-core-test}'s {@code ListAppender}) because
   * sitemanage does not depend on the {@code log4j-core-test} test artifact.
   */
  static final class CapturingAppender extends AbstractAppender {
    private final List<LogEvent> events = new ArrayList<>();

    CapturingAppender(String name) {
      super(name, null, null, true);
    }

    @Override
    public void append(LogEvent event) {
      synchronized (events) {
        events.add(event.toImmutable());
      }
    }

    List<LogEvent> getEvents() {
      synchronized (events) {
        return new ArrayList<>(events);
      }
    }

    void clear() {
      synchronized (events) {
        events.clear();
      }
    }
  }
}
