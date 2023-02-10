/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.ant.install;

import com.percussion.install.PSLogger;
import com.percussion.tablefactory.PSJdbcDataTypeMap;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.tablefactory.PSJdbcTableDataCollection;
import com.percussion.tablefactory.PSJdbcTableFactory;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.tablefactory.PSJdbcTableSchemaCollection;
import com.percussion.tablefactory.install.RxLogTables;
import com.percussion.utils.container.IPSJdbcDbmsDefConstants;
import com.percussion.xml.PSXmlDocumentBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * This class creates backup of specified tables. If backup of the table is
 * successfully created, then it may drop the original table if
 * <code>dropTables</code> is <code>true</code>.  Create backup table name by
 * appending <code>suffix</code> to table name.
 *
 *<br>
 * Example Usage:
 *<br>
 *<pre>
 *
 * First set the taskdef:
 *
 *  <code>
 *  &lt;taskdef name="createTableBackupAction"
 *              class="com.percussion.ant.install.PSCreateTableBackupAction"
 *              classpathref="INSTALL.CLASSPATH"/&gt;
 *  </code>
 *
 * Now use the task to create a backup of the specified table(s).
 *
 *  <code>
 *  &lt;createTableBackupAction dropTables="false"
 *                  suffix="_BAK"
 *                  tables="TABLE1,TABLE2"/&gt;
 *  </code>
 *
 * </pre>
 *
 */
public class PSExportDatabase extends PSAction
{
    private static final String ERROR = "ERROR :";
   // see base class
   @SuppressFBWarnings("HARD_CODE_PASSWORD")
   @Override
   public void execute()
   {
        Connection conn = null;
         String propFile = getRootDir() + File.separator
         + "rxconfig/Installer/rxrepository.properties";

         File f = new File(propFile);
         if (!(f.exists() && f.isFile()))
            return;
        try(FileInputStream in = new FileInputStream(f)){

         Properties props = new Properties();
         props.load(in);
         props.setProperty(IPSJdbcDbmsDefConstants.PWD_ENCRYPTED_PROPERTY, "Y");
         PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
         PSJdbcDataTypeMap dataTypeMap = new PSJdbcDataTypeMap(
               props.getProperty("DB_BACKEND"),
               props.getProperty("DB_DRIVER_NAME"), null);
         conn = RxLogTables.createConnection(props);

          List<String> tableNames = getTableNames(conn, dbmsDef);
          tableNames=filterTableNames(tableNames);
          Document schemaDoc = PSXmlDocumentBuilder.createXmlDocument();
          Document dataDoc = PSXmlDocumentBuilder.createXmlDocument();

          PSJdbcTableDataCollection collData = new PSJdbcTableDataCollection();
          PSJdbcTableSchemaCollection collSchema =
                  new PSJdbcTableSchemaCollection();
          PSJdbcTableSchema tableSchema = null;
          PSJdbcTableData tableData = null;

          List<PSJdbcTableSchema> schemasToSort = new ArrayList<>();
        for (int i=0; i <tableNames.size(); i++) {
            String tblName = tableNames.get(i).trim();

            try {
                tableSchema = PSJdbcTableFactory.catalogTable(conn, dbmsDef,
                dataTypeMap, tblName, true);
                if(tableSchema != null) {
                    schemasToSort.add(tableSchema);
                }
            } catch (PSJdbcTableFactoryException ex) {
                PSLogger.logInfo(ERROR + ex.getMessage());
                PSLogger.logInfo(ex);
            }
        }

          Collections.sort(schemasToSort);

          for (PSJdbcTableSchema sortedSchema : schemasToSort)
          {
              collSchema.add(sortedSchema);

              tableData = sortedSchema.getTableData();
              if(tableData != null) {
                  collData.add(tableData);
              }

          }

          schemaDoc.appendChild(collSchema.toXml(schemaDoc));
          dataDoc.appendChild(collData.toXml(dataDoc));
          File defFile = new File(tableDefFile);
          defFile.getParentFile().mkdirs();

          File dataFile = new File(tableDataFile);
          dataFile.getParentFile().mkdirs();

          try (OutputStream os = new FileOutputStream(new File(tableDefFile))) {
              PSXmlDocumentBuilder.write(schemaDoc, os);
          }

          try (OutputStream os = new FileOutputStream(new File(tableDataFile))) {
              PSXmlDocumentBuilder.write(dataDoc, os);
          }
        } catch (IOException | PSJdbcTableFactoryException | SAXException | SQLException ex) {
            PSLogger.logInfo(ERROR + ex.getMessage());
            PSLogger.logInfo(ex);
        }

      finally
      {
         if (conn != null){
           try{
               conn.close();
           }catch (SQLException ex){
               PSLogger.logInfo("ERROR : " + ex.getMessage());
               PSLogger.logInfo(ex);
           }
         }
      }
   }

    private List<String> filterTableNames(List<String> tableNames) {
       return tableNames.stream().filter(name -> !name.endsWith("_BAK")).collect(Collectors.toList());
    }

    private List<String> getTableNames(Connection conn, PSJdbcDbmsDef dbmsDef) throws SQLException {
        DatabaseMetaData dmd = conn.getMetaData();
        List<String> tableNames = new ArrayList<>();
        try {
            String[] types = {"TABLE"};
            ResultSet rs = dmd.getTables(dbmsDef.getDataBase(), dbmsDef.getSchema(), "%", types);

            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
        }
        catch (SQLException ex) {
            PSLogger.logInfo("ERROR : " + ex.getMessage());
            PSLogger.logInfo(ex);
            throw ex;
        }

        return tableNames;
    }


    /***************************************************************************
    * Bean properties
    ***************************************************************************/

   /**
    * Returns the name of tables whose backup is to be created.
    *
    * @return the name of tables whose backup is to be created,
    * never <code>null</code>, may be empty array
    */
   public String[] getTableIncludes()
   {
      return tableIncludes;
   }

   /**
    * Sets the name of tables whose backup is to be created.
    *
    * @param tableIncludes name of tables whose backup is to be created,
    * never <code>null</code>, may be empty array
    */
   public void setTableIncludes(String tableIncludes)
   {
      this.tableIncludes = convertToArray(tableIncludes);
   }

    /**
     * Returns the name of tables whose backup is to be created.
     *
     * @return the name of tables whose backup is to be created,
     * never <code>null</code>, may be empty array
     */
    public String[] getTableExcludes()
    {
        return this.tableExcludes;
    }

    /**
     * Sets the name of tables whose backup is to be created.
     *
     * @param tableExcludes name of tables whose backup is to be created,
     * never <code>null</code>, may be empty array
     */
    public void setTableExcludes(String tableExcludes)
    {
        this.tableIncludes = convertToArray(tableExcludes);
    }


   public String getTableDefFile() { return tableDefFile; }

    public String getTableDataFile() { return tableDataFile; }


    public void setTableDefFile(String tableDefFile) { this.tableDefFile = tableDefFile;}
    public void setTableDataFile(String tableDataFile) { this.tableDataFile = tableDataFile;}

    /**************************************************************************
    * properties
    **************************************************************************/

    /**
     * Name of tables whose backup is to be created, never <code>null</code>,
     * may be empty
     */
    private String tableDefFile = null;

    private String tableDataFile = null;

   /**
    * Name of tables whose backup is to be created, never <code>null</code>,
    * may be empty
    */
   private String[] tableIncludes= null;
    /**
     * Name of tables whose backup is to be created, never <code>null</code>,
     * may be empty
     */
    private String[] tableExcludes= null;

}


