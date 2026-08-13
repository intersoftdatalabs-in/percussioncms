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
package com.percussion.cms.objectstore;

import com.percussion.data.PSNextNumberAligner;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.NamingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Advances {@code NEXTNUMBER} for {@code PSX_OBJECTACL} when seed / FastForward SYSIDs sit at or
 * above the next allocated value so folder ACL inserts do not hit {@code PK_PSX_OBJECTACL} (#3282).
 */
public final class PSObjectAclNextNumberReconciler {

  public static final String NEXTNUMBER_KEY = "PSX_OBJECTACL";

  private static final String TABLE = "PSX_OBJECTACL";
  private static final String SYSID_COL = "SYSID";

  private static final Logger log = LogManager.getLogger(PSObjectAclNextNumberReconciler.class);

  private PSObjectAclNextNumberReconciler() {}

  /** NEXTNUMBER peek / advance used by unit tests and the server adapter. */
  public interface NextNumberView {
    int peek(String key);

    void advanceTo(String key, int nextId);
  }

  /** Highest persisted {@code PSX_OBJECTACL.SYSID}, or {@code < 0} when the table is empty. */
  public interface MaxSysidSource {
    int maxSysid();
  }

  /**
   * If {@code peek} would collide with {@code max(SYSID)}, advance NEXTNUMBER to the first free id.
   *
   * @return {@code true} when NEXTNUMBER was moved
   */
  public static boolean reconcile(NextNumberView numbers, MaxSysidSource used) {
    if (numbers == null || used == null) {
      throw new IllegalArgumentException("numbers and used are required");
    }
    int peek = numbers.peek(NEXTNUMBER_KEY);
    int maxUsed = used.maxSysid();
    if (!PSNextNumberAligner.wouldCollide(peek, maxUsed)) {
      return false;
    }
    int free = PSNextNumberAligner.nextFreeId(peek, maxUsed);
    numbers.advanceTo(NEXTNUMBER_KEY, free);
    return true;
  }

  /**
   * Best-effort runtime align using the live guid manager and JDBC. Failures are logged and
   * swallowed so folder default-ACL persist can still attempt a save.
   *
   * @return {@code true} when NEXTNUMBER was moved
   */
  public static boolean reconcileOnServer() {
    try {
      IPSGuidManager gm = PSGuidManagerLocator.getGuidMgr();
      NextNumberView view =
          new NextNumberView() {
            @Override
            public int peek(String key) {
              return gm.peekNextNumber(key);
            }

            @Override
            public void advanceTo(String key, int nextId) {
              gm.fixNextNumber(key, nextId);
            }
          };
      return reconcile(view, PSObjectAclNextNumberReconciler::loadMaxSysid);
    } catch (RuntimeException e) {
      log.warn(
          "Could not reconcile PSX_OBJECTACL NEXTNUMBER before folder ACL persist: {}",
          e.getMessage());
      return false;
    }
  }

  static int loadMaxSysid() {
    try {
      Connection c = PSConnectionHelper.getDbConnection();
      try {
        String table = qualifiedTable(PSConnectionHelper.getConnectionDetail());
        String sql = "select max(" + SYSID_COL + ") from " + table;
        try (PreparedStatement st = c.prepareStatement(sql);
            ResultSet rs = st.executeQuery()) {
          if (!rs.next()) {
            return -1;
          }
          int value = rs.getInt(1);
          return rs.wasNull() ? -1 : value;
        }
      } finally {
        c.close();
      }
    } catch (SQLException | NamingException e) {
      throw new IllegalStateException("Failed to read max(PSX_OBJECTACL.SYSID)", e);
    }
  }

  static String qualifiedTable(PSConnectionDetail details) {
    if (details == null) {
      return TABLE;
    }
    String schema = details.getOrigin();
    if ("mysql".equalsIgnoreCase(details.getDriver()) && (schema == null || schema.isBlank())) {
      schema = details.getDatabase();
    }
    if (schema == null || schema.isBlank()) {
      return TABLE;
    }
    return schema + "." + TABLE;
  }
}
