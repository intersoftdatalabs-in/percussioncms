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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rxfix.dbfixes;

import com.percussion.rxfix.IPSFix;
import com.percussion.rxfix.PSFixResult;
import com.percussion.util.PSPreparedStatement;
import com.percussion.util.PSStringTemplate;
import com.percussion.utils.jdbc.PSConnectionHelper;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PSFixFormUrl extends PSFixDBBase implements IPSFix {

    private boolean fixSuccess = false;
    /**
     * Ctor for the class
     *
     * @throws SQLException
     * @throws NamingException
     */
    public PSFixFormUrl() throws NamingException, SQLException {
        super();
    }
    @Override
    public void fix(boolean preview)
            throws Exception {
        super.fix(preview);
        fixContentFormUrl();
    }
    /**
     * Fixes form url by changing all "/perc-form-processor/forms" to
     * "/perc-form-processor/form/" in the RENDEDERDFORM column of the
     * CT_PERCFORMASSET table.
     *
     * @throws SQLException if any error occurs during DB access.
     */
    private void fixContentFormUrl() throws Exception
    {
        try (Connection dbConnection = PSConnectionHelper.getDbConnection()) {
            // Check if the table exists before attempting to query
            if (!tableExists(dbConnection, "CT_PERCFORMASSET")) {
                logInfo(null, "Table CT_PERCFORMASSET does not exist, skipping form URL fix");
                return;
            }

            logInfo(null,"Finding all forms");
            PSStringTemplate ms_allForms = new PSStringTemplate("Select CONTENTID, REVISIONID, NAME, RENDEREDFORM FROM "
                    +" {schema}.CT_PERCFORMASSET"
                    + " WHERE RENDEREDFORM LIKE '%\"/perc-form-processor/form%' ");
            try (PreparedStatement st = PSPreparedStatement.getPreparedStatement(
                    dbConnection,
                    ms_allForms.expand(m_defDict))) {

                ResultSet results = st.executeQuery();
                while (results.next()) {
                    int contentid = results.getInt("CONTENTID");
                    int revisionid = results.getInt("REVISIONID");
                    String name = results.getString("NAME");
                    String renderedForm = results.getString("RENDEREDFORM");

                    logInfo(null, "Rendered form: " + name);

                    // Change all "/perc-form-processor/forms" to
                    // "/perc-form-processor/form/"
                    renderedForm = renderedForm.replaceAll(
                            "action=\"/perc-form-processor/forms/\"",
                            "action=\"/perc-form-processor/forms/form/\"");

                    renderedForm = renderedForm.replaceAll(
                            "action=\"/perc-form-processor/form/\"",
                            "action=\"/perc-form-processor/forms/form/\"");

                    logInfo(null, "updating 'RENDEREDFORM' column in for contentid = "
                            + contentid
                            + " and revision = "
                            + revisionid
                            + " and name = "
                            + name);

                    logInfo(null, "Rendered form after upgrade: " + name);

                    PSStringTemplate ms_updateForms = new PSStringTemplate("UPDATE  {schema}.CT_PERCFORMASSET  SET RENDEREDFORM = ?"
                            + " WHERE CONTENTID = ?" + " and REVISIONID = ?");
                    PreparedStatement ps2 = dbConnection.prepareStatement(ms_updateForms.expand(m_defDict));
                    ps2.setString(1, renderedForm);
                    ps2.setInt(2, contentid);
                    ps2.setInt(3, revisionid);
                    ps2.executeUpdate();
                }
            }
            log(PSFixResult.Status.SUCCESS,null,"Forms Urls Fixed");
            fixSuccess=true;
        }

    }

    /**
     * Checks if a table exists in the database.
     *
     * @param connection the database connection
     * @param tableName the table name to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if an error occurs checking the table
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return tables.next();
        }
    }
    public boolean removeStartupOnSuccess(){
        return fixSuccess;
    }

    @Override
    public String getOperation() {
        return "Fix FormUrls";
    }
}
