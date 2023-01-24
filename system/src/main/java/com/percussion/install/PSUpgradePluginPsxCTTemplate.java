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
package com.percussion.install;

import com.percussion.security.SecureStringUtils;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.tablefactory.tools.PSCatalogTableData;
import com.percussion.util.PSSqlHelper;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This upgrade plug-in will insert entries into the PSX_CONTENTTYPE_TEMPLATE
 * table based on the VARIANTID/CONTENTTYPEID values of the CONTENTVARIANTS
 * table.  The PSX_TEMPLATE table will be populated using the contents of the
 * CONTENTVARIANTS table. Orphaned data cleanup will also be performed on the
 * CONTENTVARIANTS table prior to the mapping insertion.  The CONTENTVARIANTS
 * table will be dropped.  It will also write to the log file showing table rows 
 * that were inserted.
 */
public class PSUpgradePluginPsxCTTemplate implements IPSUpgradePlugin
{
   /**
    * Implements process method of IPSUpgardePlugin.
    */
   public PSPluginResponse process(IPSUpgradeModule config, Element elemData)
   {
      m_config = config;
      log("Executing PSUpgradePluginPsxCTTemplate...");

      Connection conn = null;
            
      try
      {
         m_dbmsDef = new PSJdbcDbmsDef(RxUpgrade.getRxRepositoryProps());
         conn = RxUpgrade.getJdbcConnection();
         
         if (conn == null)
         {
            log(
               "Could not establish connection with database\n\n");
            log("Table modifications aborted");

            return null;
         }

         conn.setAutoCommit(false);
         
         // Perform orphan data cleanup
         orphanDataCleanup(conn);
         
         // Populate PSX_TEMPLATE from cleaned up CONTENTVARIANTS
         populatePsxTemplate(conn);
         
         // Updates PSX_TEMPLATE names
         updateNames(conn);
         
         // Insert mappings into PSX_CONTENTTYPE_TEMPLATE
         if (!insertMappings(conn))
         {
            log("No modifications were made.");
         }
         
         // Drop CONTENTVARIANTS table
         dropContentVariantsTable(conn);
         
         conn.commit();
      }
      catch (Exception e)
      {
         try
         {
            if (conn != null)
               conn.rollback();
         }
         catch (SQLException se)
         {
         }

         e.printStackTrace(m_config.getLogStream());
      }
      finally
      {
         if (conn != null)
         {
            try
            {
               conn.close();
            }
            catch (SQLException e)
            {
            }
            conn = null;
         }
         
         log("leaving the process() of the plugin...");
      }
      
      return null;
   }

   /**
    * Inserts a new entry (VARIANTID/CONTENTTYPEID mapping) into the 
    * PSX_CONTENTTYPE_TEMPLATE table for each VARIANTID found in the 
    * CONTENTVARIANTS table.
    *
    * @param conn the database connection object, cannot be <code>null</code>.
    * @return <code>true</code> if modifications were made.
    */
   private boolean insertMappings(final Connection conn)
   {
      log("Inserting VARIANTID/CONTENTTYPEID mappings.");

      String qualCVTableName = qualifyTableName(SecureStringUtils.sanitizeStringForSQLStatement(CV_TABLE));
      
      String qualCTTTableName = qualifyTableName(SecureStringUtils.sanitizeStringForSQLStatement(PSXCTT_TABLE));
      
      String queryStmt = "SELECT " + qualCVTableName + ".VARIANTID," + 
                         qualCVTableName + ".CONTENTTYPEID FROM " + qualCVTableName;
      
      boolean modifications = false;
      int mappings = 0;
            
      try
      {
         // statement for query
         PreparedStatement stmt = conn.prepareStatement(queryStmt);
         
         // get the variantid/contenttypeid mappings
         try(ResultSet rs = stmt.executeQuery(queryStmt)) {

            while (rs.next()) {
               int variantId = rs.getInt("VARIANTID");
               int contenttypeId = rs.getInt("CONTENTTYPEID");
               int templatetypeId = variantId;
               int templateId = variantId;
               int version = 0;

               String insertStmt = "INSERT INTO " + qualCTTTableName +
                       " (TEMPLATE_TYPE_ID,VERSION,CONTENTTYPEID,TEMPLATE_ID) " +
                       "VALUES (?,?,?,?)";

               PreparedStatement stmt2 = conn.prepareStatement(insertStmt);
               stmt2.setInt(1,templatetypeId);
               stmt2.setInt(2,version);
               stmt2.setInt(3, contenttypeId);
               stmt2.setInt(4,templateId);

               // insert map entry into new table
               stmt2.execute(insertStmt);

               modifications = true;
               mappings++;
            }
         }

         log("Inserted " + mappings + " mapping(s) into " + PSXCTT_TABLE + ".");
      }
      catch (SQLException e)
      {
            e.printStackTrace(m_config.getLogStream());
      }
     
      return modifications;
   }

   /**
    * Performs orphan data cleanup on the CONTENTVARIANTS table.  Must be 
    * called before the <code>insertMappings</code> method.
    *
    * @param conn the database connection object, cannot be <code>null</code>.
    */
   private void orphanDataCleanup(final Connection conn)
   {
      log("Performing orphan data cleanup.");

      String qualCVTableName = qualifyTableName(SecureStringUtils.sanitizeStringForSQLStatement(CV_TABLE));
      
      String qualCTTableName = qualifyTableName(SecureStringUtils.sanitizeStringForSQLStatement(CT_TABLE));
      
      String deleteStmt = "DELETE FROM " + qualCVTableName + " WHERE " + 
                          qualCVTableName + ".CONTENTTYPEID NOT IN (SELECT " + 
                          qualCTTableName + ".CONTENTTYPEID FROM " + qualCTTableName + ")";
      
      try
      {
         // execute delete
         Statement stmt = conn.createStatement();
         int rowsDeleted = stmt.executeUpdate(deleteStmt);      
                  
         log("Removed " + rowsDeleted + " row(s) from " + CV_TABLE + ".");
      }
      catch (SQLException e)
      {
            e.printStackTrace(m_config.getLogStream());
      }
   }
   
   /**
    * Populates the PSX_TEMPLATE table with the data contained in the
    * CONTENTVARIANTS table.  This new table takes the place of CONTENTVARIANTS,
    * which will instead become a view.
    *
    * @param conn the database connection object, cannot be <code>null</code>.
    */
   private void populatePsxTemplate(final Connection conn)
   {
      log("Populating " + PSXTEMP_TABLE + " from " + CV_TABLE + ".");

      String qualCVTableName = qualifyTableName(CV_TABLE);
      
      String qualTMPTableName = qualifyTableName(PSXTEMP_TABLE);
      
      String locationPrefix = "c.LOCATIONPREFIX";
      String locationSuffix = "c.LOCATIONSUFFIX";
      String publishWhen = "c.PUBLISHWHEN";
      try
      {
         PSJdbcTableSchema cvSchema = PSCatalogTableData.catalogTable(
            m_dbmsDef, CV_TABLE);
         
         //in some previous versions this column may not exist
         if (cvSchema.getColumn(LOCATIONPREFIX) == null)
            locationPrefix = "";
                  
         //in some previous versions this column may not exist
         if (cvSchema.getColumn(LOCATIONSUFFIX) == null)
            locationSuffix = "";
                  
         //in some previous versions this column may not exist
         if (cvSchema.getColumn(PUBLISHWHEN) == null)
            publishWhen = "";
      }
      catch (IOException e)
      {
         log("Could not read properties file");
         log(e.getMessage());
      }
      catch(SQLException e)
      {
         log("Could not read table metadata");
         log(e.getMessage());
      }
      catch (PSJdbcTableFactoryException e)
      {
         log("Could not read table metadata");
         log(e.getMessage());
      }
      catch (SAXException e)
      {
         log("Could not read table metadata");
         log(e.getMessage());
      }
      
      String columnNames = "TEMPLATE_ID, VERSION, LABEL, STYLESHEETNAME,"
         + " ASSEMBLYURL, OUTPUTFORMAT, AATYPE, DESCRIPTION";
      
      String columnValues = "c.VARIANTID, 0, c.VARIANTDESCRIPTION,"
         + " c.STYLESHEETNAME, c.ASSEMBLYURL, c.OUTPUTFORMAT, c.AATYPE,"
         + " c.DESCRIPTION";
      
      if (publishWhen.trim().length() > 0)
      {
         columnNames += ", " + PUBLISHWHEN;
         columnValues += ", " + publishWhen;
      }
      
      if (locationPrefix.trim().length() > 0)
      {
         columnNames += ", " + LOCATIONPREFIX;
         columnValues += ", " + locationPrefix;
      }
      
      if (locationSuffix.trim().length() > 0)
      {
         columnNames += ", " + LOCATIONSUFFIX;
         columnValues += ", " + locationSuffix;
      }
      
      String insertStmt = "INSERT INTO "
         + qualTMPTableName
         + " (" + columnNames + ")"
         + " SELECT "
         + columnValues
         + " FROM " 
         + qualCVTableName 
         + " c"; 
           
      try
      {
         Statement stmt = conn.createStatement();
         int rowsInserted = stmt.executeUpdate(insertStmt);      
                 
         log("Inserted " + rowsInserted + " row(s) into " + PSXTEMP_TABLE + ".");
      }
      catch (SQLException e)
      {
            e.printStackTrace(m_config.getLogStream());
      }
       
   }
   
   /**
    * Drops the CONTENTVARIANTS table.
    *
    * @param conn the database connection object, cannot be <code>null</code>.
    */
   private void dropContentVariantsTable(final Connection conn)
   {
      log("Dropping " + CV_TABLE + " table.");

      String qualCVTableName = qualifyTableName(CV_TABLE);
      
      String dropStmt = "DROP TABLE " + qualCVTableName;
      
      try
      {
         // execute drop
         Statement stmt = conn.createStatement();
         stmt.executeUpdate(dropStmt);      
                  
         log(CV_TABLE + " table has been dropped.");
      }
      catch (SQLException e)
      {
         e.printStackTrace(m_config.getLogStream());
      }
   }
   
   /**
    * This will update the NAME column of the PSX_TEMPLATE table with the values
    * of the LABEL column but with all whitespace and hyphens removed.
    * Uses {@link InstallUtil} to do name cleanup.
    * @throws SQLException 
    */
   private void updateNames(Connection conn) throws SQLException
   {
      String database = m_dbmsDef.getDataBase();
      String schema = m_dbmsDef.getSchema();
      String driver = m_dbmsDef.getDriver();
      
      String qualTEMPTableName = 
         PSSqlHelper.qualifyTableName(SecureStringUtils.sanitizeStringForSQLStatement(PSXTEMP_TABLE)
                 , database, schema, driver);
      
      String queryStmt = "SELECT " + qualTEMPTableName + ".LABEL, "
                  + qualTEMPTableName + ".TEMPLATE_ID "
                  + "FROM " + qualTEMPTableName;
      
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(queryStmt);
      Map labelsIds = new HashMap();
      
      while (rs.next())
         labelsIds.put(new Integer(rs.getInt(2)), rs.getString(1));
      
      int rowsUpdated = 0;
      Iterator keyIter = labelsIds.keySet().iterator();
      while (keyIter.hasNext())
      {
         Integer id = (Integer) keyIter.next();
         String label = (String) labelsIds.get(id);
         String name = InstallUtil.modifyName(label);
         
         name = name.replaceAll("'", "''");
         
         String updateStmt = "UPDATE " + qualTEMPTableName + " SET "
                      + qualTEMPTableName + ".NAME='" + name + "' WHERE "
                      + qualTEMPTableName + ".TEMPLATE_ID=" + id.intValue();
         
         stmt.executeUpdate(updateStmt);
         rowsUpdated++;
      }
      log("Updated " + rowsUpdated + " names");
   }
   
   /**
    * This will create a fully qualified table name. Depending on the provided
    * driver type we will return table, owner.table or db.owner.table.
    * 
    * @param table the table name to qualify, must be valid
    */
   private String qualifyTableName(String table)
   {
      String database = m_dbmsDef.getDataBase();
      String schema = m_dbmsDef.getSchema();
      String driver = m_dbmsDef.getDriver();

      return PSSqlHelper.qualifyTableName(table, database, schema, driver);
   }
   
   /**
    * Prints message to the log printstream if it exists
    * or just sends it to System.out
    *
    * @param msg the message to be logged, can be <code>null</code>.
    */
   private void log(String msg)
   {
      if (msg == null)
      {
         return;
      }

      if (m_config != null)
      {
         m_config.getLogStream().println(msg);
      }
      else
      {
         System.out.println(msg);
      }
   }

   private IPSUpgradeModule m_config;
   private PSJdbcDbmsDef m_dbmsDef;
   private static final String CV_TABLE = "CONTENTVARIANTS";
   private static final String PSXCTT_TABLE = "PSX_CONTENTTYPE_TEMPLATE";
   private static final String CT_TABLE = "CONTENTTYPES";
   private static final String PSXTEMP_TABLE = "PSX_TEMPLATE";
   private static final String LOCATIONPREFIX = "LOCATIONPREFIX";
   private static final String LOCATIONSUFFIX = "LOCATIONSUFFIX";
   private static final String PUBLISHWHEN = "PUBLISHWHEN";
}
