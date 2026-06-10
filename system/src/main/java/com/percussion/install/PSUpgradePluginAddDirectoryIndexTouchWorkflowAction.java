/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.install;

import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.util.PSSqlHelper;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.w3c.dom.Element;

/**
 * Upgrade plugin to automate adding the sys_DirectoryIndexTouchWorkflowAction to Approve
 * transitions in the Default Workflow.
 */
public class PSUpgradePluginAddDirectoryIndexTouchWorkflowAction implements IPSUpgradePlugin {
  private PrintStream logger;
  private Properties m_dbProps = null;

  public PSPluginResponse process(IPSUpgradeModule module, Element elemData) {
    logger = module.getLogStream();
    Connection conn = null;

    try {
      m_dbProps = RxUpgrade.getRxRepositoryProps();
      m_dbProps.setProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY, "Y");
      conn = RxUpgrade.getJdbcConnection();
      conn.setAutoCommit(false);
      updateTransitions(conn);
    } catch (Exception e) {
      return new PSPluginResponse(PSPluginResponse.EXCEPTION, e.getLocalizedMessage());
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException se) {
          return new PSPluginResponse(PSPluginResponse.EXCEPTION, se.getLocalizedMessage());
        }
      }
    }
    return new PSPluginResponse(PSPluginResponse.SUCCESS, "");
  }

  private void updateTransitions(Connection conn) throws SQLException {
    String transitionTable = qualifyTableName("TRANSITIONS");
    String workflowTable = qualifyTableName("WORKFLOWAPPS");

    int workflowId = getWorkflowId(workflowTable, "Default Workflow", conn);
    if (workflowId <= 0) {
      logger.println("Default Workflow not found. Skipping transition actions update.");
      return;
    }

    String selectQuery =
        "SELECT TRANSITIONID, TRANSITIONACTIONS FROM "
            + transitionTable
            + " WHERE WORKFLOWAPPID = ? AND TRANSITIONLABEL = 'Approve'";
    String updateQuery =
        "UPDATE "
            + transitionTable
            + " SET TRANSITIONACTIONS = ? "
            + " WHERE WORKFLOWAPPID = ? AND TRANSITIONID = ?";

    String actionToAdd =
        "Java/global/percussion/extensions/general/sys_DirectoryIndexTouchWorkflowAction";

    try (PreparedStatement selectPs = conn.prepareStatement(selectQuery)) {
      selectPs.setInt(1, workflowId);
      try (ResultSet rs = selectPs.executeQuery()) {
        try (PreparedStatement updatePs = conn.prepareStatement(updateQuery)) {
          while (rs.next()) {
            int transId = rs.getInt("TRANSITIONID");
            String currentActions = rs.getString("TRANSITIONACTIONS");
            String newActions;

            if (currentActions == null || currentActions.trim().isEmpty()) {
              newActions = actionToAdd;
            } else if (currentActions.contains(actionToAdd)) {
              logger.println(
                  "Approve Transition ID: "
                      + transId
                      + " already has Directory Index touch action. Skipping.");
              continue;
            } else {
              newActions = currentActions.trim() + "," + actionToAdd;
            }

            logger.println(
                "Updating Approve Transition ID: "
                    + transId
                    + " to include Directory Index touch action.");
            updatePs.setString(1, newActions);
            updatePs.setInt(2, workflowId);
            updatePs.setInt(3, transId);
            updatePs.executeUpdate();
          }
        }
      }
    }

    conn.commit();
  }

  private String qualifyTableName(String table) {
    String database = m_dbProps.getProperty("DB_NAME");
    String schema = m_dbProps.getProperty("DB_SCHEMA");
    String driver = m_dbProps.getProperty("DB_DRIVER_NAME");
    return PSSqlHelper.qualifyTableName(table, database, schema, driver);
  }

  private int getWorkflowId(String workflowTable, String workflow, Connection conn)
      throws SQLException {
    int workflowId = -1;
    String query = "SELECT WORKFLOWAPPID FROM " + workflowTable + " WHERE WORKFLOWAPPNAME = ?";
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, workflow);
      try (ResultSet results = ps.executeQuery()) {
        if (results.next()) {
          workflowId = results.getInt("WORKFLOWAPPID");
        }
      }
    }
    return workflowId;
  }
}
