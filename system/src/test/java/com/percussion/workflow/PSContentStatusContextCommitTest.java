/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Behavioral tests for the PR #1589 review thread databaseId 3670378976 hot-fix:
 * PSContentStatusContext.commit() must populate all 15 legacy CONTENTSTATUS
 * columns in the notifyUpdateItem map, not CONTENTID-only.
 *
 * <p>This test runs in the same package as PSContentStatusContext so the
 * package-private buildLegacyColumnMap() and default constructor are directly
 * accessible. The static initializer chain (PSConnectionMgr.getQualifiedIdentifier
 * + PSWorkFlowUtils.encryptWorkflowProps + loadProperties) is mocked in
 * {@link #beforeAll} so the class can load without a live DB or Spring
 * context.</p>
 */
public class PSContentStatusContextCommitTest {

  private static final String EXPECTED_RX_ROOT;
  private static final Path RXDEPLOY_DIR;
  private static final Path RXRUN_DIR;
  private static MockedStatic<PSSystemServiceLocator> LOCATOR_MOCK;
  private static MockedStatic<PSConnectionHelper> HELPER_MOCK;
  private static IPSSystemService MOCK_SVC;

  static {
    Path tmp;
    try {
      tmp = Files.createTempDirectory("percussion-test-rx-");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    tmp.toFile().deleteOnExit();
    RXDEPLOY_DIR = tmp;
    RXRUN_DIR = tmp.resolve("rxconfig").resolve("Workflow");
    EXPECTED_RX_ROOT = RXDEPLOY_DIR.toAbsolutePath().toString();
  }

  @BeforeAll
  static void beforeAll() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Files.createDirectories(RXRUN_DIR);

    String propsContent =
        "TESTWITHOUTSERVER=false\n"
            + "PSCONSOLETRACEMESSAGES=false\n"
            + "SMTP_PASSWORD=\n";
    Files.writeString(RXRUN_DIR.resolve("rxworkflow.properties"), propsContent, StandardCharsets.UTF_8);

    System.setProperty("rxdeploydir", EXPECTED_RX_ROOT);

    HELPER_MOCK = mockStatic(PSConnectionHelper.class);
    PSConnectionDetail detail = mock(PSConnectionDetail.class);
    when(detail.getDatabase()).thenReturn("RX");
    when(detail.getOrigin()).thenReturn("PERCUSSION");
    when(PSConnectionHelper.getConnectionDetail(any())).thenReturn(detail);

    MOCK_SVC = mock(IPSSystemService.class);
    when(MOCK_SVC.updateContentStatusState(anyInt(), anyInt(), any(String.class), anyInt(), anyInt(),
        anyInt(), anyBoolean(), any(java.util.Date.class), any(java.util.Date.class), anyInt(),
        any(java.util.Date.class), any(java.util.Date.class), any(java.util.Date.class),
        any(java.util.Date.class), any(java.util.Date.class))).thenReturn(1);

    LOCATOR_MOCK = mockStatic(PSSystemServiceLocator.class);
    when(PSSystemServiceLocator.getSystemService()).thenReturn(MOCK_SVC);
  }

  @AfterAll
  static void afterAll() {
    TimeZone.setDefault(null);
    if (LOCATOR_MOCK != null) {
      LOCATOR_MOCK.close();
    }
    if (HELPER_MOCK != null) {
      HELPER_MOCK.close();
    }
    System.clearProperty("rxdeploydir");
  }

  @Test
  void buildLegacyColumnMap_populatesAllFifteenColumns() throws Exception {
    PSContentStatusContext ctx = createContext();
    setField(ctx, "m_nContentID", 7);
    setField(ctx, "m_nStateID", 11);
    setField(ctx, "m_sCheckOutUserName", "alice");
    setField(ctx, "m_nCurrentRevision", 5);
    setField(ctx, "m_nEditRevision", 6);
    setField(ctx, "m_nTipRevision", 7);
    setField(ctx, "m_bRevisionLocked", true);
    setField(ctx, "m_LastTransitionDate", toSqlDate(Instant.parse("2026-07-28T12:00:00Z")));
    setField(ctx, "m_StateEnteredDate", toSqlDate(Instant.parse("2026-07-27T08:30:00Z")));
    setField(ctx, "m_nNextAgingTransition", 3);
    setField(ctx, "m_NextAgingDate", toSqlDate(Instant.parse("2026-08-01T00:00:00Z")));
    setField(ctx, "m_StartDate", toSqlDate(Instant.parse("2026-07-01T00:00:00Z")));
    setField(ctx, "m_ExpiryDate", toSqlDate(Instant.parse("2026-12-31T23:59:59Z")));
    setField(ctx, "m_ReminderDate", toSqlDate(Instant.parse("2026-12-24T09:00:00Z")));
    setField(ctx, "m_RepeatedAgingTransitionStartDate",
        toSqlDate(Instant.parse("2026-07-28T00:00:00Z")));

    @SuppressWarnings("unchecked")
    Map<String, String> columns = (Map<String, String>) invokeMethod(ctx, "buildLegacyColumnMap");

    assertEquals(15, columns.size(), "Legacy map must contain exactly 15 columns");
    assertEquals("11", columns.get("CONTENTSTATEID"));
    assertEquals("alice", columns.get("CONTENTCHECKOUTUSERNAME"));
    assertEquals("5", columns.get("CURRENTREVISION"));
    assertEquals("6", columns.get("EDITREVISION"));
    assertEquals("7", columns.get("TIPREVISION"));
    assertEquals("Y", columns.get("REVISIONLOCK"));
    assertNotNull(columns.get("LASTTRANSITIONDATE"));
    assertFalse(columns.get("LASTTRANSITIONDATE").isEmpty());
    assertNotNull(columns.get("STATEENTEREDDATE"));
    assertFalse(columns.get("STATEENTEREDDATE").isEmpty());
    assertEquals("3", columns.get("NEXTAGINGTRANSITION"));
    assertNotNull(columns.get("NEXTAGINGDATE"));
    assertFalse(columns.get("NEXTAGINGDATE").isEmpty());
    assertNotNull(columns.get("CONTENTSTARTDATE"));
    assertFalse(columns.get("CONTENTSTARTDATE").isEmpty());
    assertNotNull(columns.get("CONTENTEXPIRYDATE"));
    assertFalse(columns.get("CONTENTEXPIRYDATE").isEmpty());
    assertNotNull(columns.get("REMINDERDATE"));
    assertFalse(columns.get("REMINDERDATE").isEmpty());
    assertNotNull(columns.get("REPEATEDAGINGTRANSSTARTDATE"));
    assertFalse(columns.get("REPEATEDAGINGTRANSSTARTDATE").isEmpty());
    assertEquals("7", columns.get("CONTENTID"));
  }

  @Test
  void buildLegacyColumnMap_handlesNullDatesAndEmptyUser() throws Exception {
    PSContentStatusContext ctx = createContext();
    setField(ctx, "m_nContentID", 7);
    setField(ctx, "m_nStateID", 11);
    setField(ctx, "m_sCheckOutUserName", "");
    setField(ctx, "m_nCurrentRevision", 1);
    setField(ctx, "m_nEditRevision", 1);
    setField(ctx, "m_nTipRevision", 1);
    setField(ctx, "m_bRevisionLocked", false);
    setField(ctx, "m_LastTransitionDate", null);
    setField(ctx, "m_StateEnteredDate", null);
    setField(ctx, "m_nNextAgingTransition", 0);
    setField(ctx, "m_NextAgingDate", null);
    setField(ctx, "m_StartDate", null);
    setField(ctx, "m_ExpiryDate", null);
    setField(ctx, "m_ReminderDate", null);
    setField(ctx, "m_RepeatedAgingTransitionStartDate", null);

    @SuppressWarnings("unchecked")
    Map<String, String> columns = (Map<String, String>) invokeMethod(ctx, "buildLegacyColumnMap");

    assertEquals(15, columns.size());
    assertEquals("", columns.get("CONTENTCHECKOUTUSERNAME"));
    assertEquals("N", columns.get("REVISIONLOCK"));
    assertNull(columns.get("LASTTRANSITIONDATE"));
    assertNull(columns.get("STATEENTEREDDATE"));
    assertNull(columns.get("NEXTAGINGDATE"));
    assertNull(columns.get("CONTENTSTARTDATE"));
    assertNull(columns.get("CONTENTEXPIRYDATE"));
    assertNull(columns.get("REMINDERDATE"));
    assertNull(columns.get("REPEATEDAGINGTRANSSTARTDATE"));
  }

  // ---------- helpers --------------------------------------------------------

  private static PSContentStatusContext createContext() throws Exception {
    Constructor<PSContentStatusContext> ctor = PSContentStatusContext.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  private static void setField(PSContentStatusContext ctx, String field, Object value)
      throws Exception {
    java.lang.reflect.Field f = PSContentStatusContext.class.getDeclaredField(field);
    f.setAccessible(true);
    f.set(ctx, value);
  }

  private static Object invokeMethod(PSContentStatusContext ctx, String name) throws Exception {
    java.lang.reflect.Method m = PSContentStatusContext.class.getDeclaredMethod(name);
    m.setAccessible(true);
    return m.invoke(ctx);
  }

  private static java.sql.Date toSqlDate(Instant instant) {
    if (instant == null) {
      return null;
    }
    return new java.sql.Date(instant.toEpochMilli());
  }
}
