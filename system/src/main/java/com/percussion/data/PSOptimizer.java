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

package com.percussion.data;

import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndJoin;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.server.PSApplicationHandler;
import com.percussion.util.PSCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The PSOptimizer abstract class providers support routines for
 * classes extending it (PSQueryOptimizer and PSUpdateOptimizer).
 */
public abstract class PSOptimizer
{
   private static final Logger log = LogManager.getLogger(PSOptimizer.class);
   /**
    * Get a database meta data object, which may retrieve a cached entry or
    * create a new meta data object.
    *
    * @param   login      the login object defining the database
    *
    * @return            the meta data object for the specified database or
    * <code>null</code> if none is cached
    */
   public static PSDatabaseMetaData getCachedDatabaseMetaData(
      PSBackEndLogin login)
      throws SQLException
   {
      return PSMetaDataCache.getCachedDatabaseMetaData(login);
   }
   
   /**
    * Get a database meta data object, which may retrieve a cached entry or
    * create a new meta data object.
    * 
    * @param dataSource the datasource, never <code>null</code>
    * @return the meta data object for the specified datasource
    */
   public static PSDatabaseMetaData getCachedDatabaseMetaData(String dataSource)
   {
      return PSMetaDataCache.getCachedDatabaseMetaData(dataSource);
   }

   /**
    * Get the metadata based on the information in the backend table.
    * 
    * @param table the backend table, never <code>null</code>.  After this
    * method call the table will have connection detail set on it.
    * 
    * @return the metadata, may be <code>null</code>.
    * @throws SQLException if the connection detail cannot be obtained.
    */
   public static PSDatabaseMetaData getCachedDatabaseMetaData(
      PSBackEndTable table) throws SQLException
   {
      if (table == null)
      {
         throw new IllegalArgumentException("table may not be null");
      }
      
      PSDatabaseMetaData dbmd = PSMetaDataCache.getCachedDatabaseMetaData(
         table.getDataSource());
      table.setConnectionDetail(dbmd.getConnectionDetail());
      
      return dbmd;
   }
   
   /**
    * Determine if an index exists for efficient querying of the specified
    * columns.
    *
    * @param   builder   the builder object containing the WHERE clauses
    *                   to check
    *
    * @param   meta      the database meta data to use for the check
    */
   public static boolean isQueryIndexAvailable(
      java.util.ArrayList colList, PSTableMetaData meta)
      throws java.sql.SQLException
   {
      if ((colList == null) || (colList.size() == 0))
         return true;   // no index required if no queryable columns exist

      PSIndexStatistics[] indexStats = meta.getIndexStatistics();
      if (indexStats == null || indexStats.length == 0)
         return false;

      // add the columns to a hash map for faster/simplified lookup
      java.util.HashMap colNameMap = new java.util.HashMap();
      for (int i = 0; i < colList.size(); i++) {
         PSBackEndColumn col = (PSBackEndColumn)colList.get(i);
         colNameMap.put(col.getColumn().toLowerCase(), col);
      }

      boolean hasIndex = false;

      for (int i = 0; (i < indexStats.length) && !hasIndex; i++)
      {
         PSIndexStatistics curIdxStat = indexStats[i];

         // if this is a real index (not table data)
         if (DatabaseMetaData.tableIndexStatistic != curIdxStat.getIndexType())
         {
            String[] idxCols = curIdxStat.getSortedColumns();
            if ((idxCols != null) && (idxCols.length >= colList.size()))
            {
               // we need to see if all the columns can be satisfied
               // using the left most (earliest) columns in the index
               hasIndex = true;
               for (int j = 0; j < colList.size(); j++)
               {
                  if (colNameMap.get(idxCols[j].toLowerCase()) == null) {
                     hasIndex = false;
                     break;   // no match means not found
                  }
               }
            }
         }
      }

      return hasIndex;
   }

   /**
    * Print the execution plan.
    */
   protected static void logExecutionPlan(
      PSApplicationHandler ah, PSDataSet ds, IPSExecutionStep[] steps)
   {
      for (int i = 0; i < steps.length; i++) {
         if (steps[i] != null) {
            String className = steps[i].getClass().getName();
            className = className.substring(className.lastIndexOf('.')+1);

            Object[] args = { ah.getName(), ds.getName(),
               String.valueOf(i+1), className, steps[i].toString() };

            ah.getLogHandler().write(
               new com.percussion.log.PSLogExecutionPlan(
                  ah.getId(), IPSDataErrors.EXEC_PLAN_LOG_SQL_PLAN, args));
         }
      }
   }

   /**
    * Evaluates the passed tables to create a login plan. There are a number
    * of steps involved, which include determining if the tables belong to
    * a single database, or a number of separate databases. If the tables are
    * in different databases, then only the work that is local to a particular
    * database can make use of joins in the SQL statements.
    * 
    * Each separate database found while traversing the list of tables will
    * necessitate a separate login step.
    * 
    * @param ah The application handler, must never be <code>null</code>
    * @param beTables The list of tables, must never be <code>null</code>
    * @param logins A list of logins. The list must not be <code>null</code>
    * and may contain logins from an earlier call. New logins will be added
    * to the list.
    * @param connKeys A {@link java.util.ConcurrentHashMap} of connection keys, which
    * must not be <code>null</code> and which will be filled with the server
    * keys and identifying integers.
    * @param joins A collection of joins, may be <code>null</code>
    * @return the count of login elements
    * @throws java.sql.SQLException
    */
   protected static int createLoginPlan(
      PSApplicationHandler ah, PSCollection beTables,
      List logins, ConcurrentHashMap connKeys, PSCollection joins)
      throws java.sql.SQLException
   {
      if (ah == null)
      {
         throw new IllegalArgumentException("ah must never be null");
      }
      if (beTables == null)
      {
         throw new IllegalArgumentException("beTables must never be null");
      }
      if (logins == null)
      {
         throw new IllegalArgumentException("logins must never be null");
      }
      if (connKeys == null)
      {
         throw new IllegalArgumentException("connKeys must never be null");
      }
      
      int loginCount = logins.size();

      HashMap beTableDoubles = null;

      if (joins != null)
      {
         beTableDoubles = new HashMap();
         for (int i = 0; i < joins.size(); i++)
         {
            PSBackEndJoin j = (PSBackEndJoin) joins.get(i);
            if (j.getTranslator() != null)
            {
               PSBackEndColumn left  = j.getLeftColumn();
               PSBackEndColumn right = j.getRightColumn();

               if (left.getTable().isSameDatasource(right.getTable()))
               {
                  Object leftServerKey = left.getTable().getServerKey();
                  if (!beTableDoubles.containsKey(leftServerKey))
                     beTableDoubles.put(leftServerKey, null);
               }
            }
         }
      }

      /* For each back end in the collection:
       *  1) See if it a login step has already been created for this driver
       *     and server.
       *
       *  2) If so, then skip this back end. If not, then create a login
       *     step for this back end, using the default credentials if none
       *     were specified for this back end.
       *
       *  3) Add this driver and server to a hash so we can tell if we have
       *  treated this driver and server yet (see step 1).
       */
      for (int j = 0; j < beTables.size(); j++)
      {
         PSBackEndTable beTable = (PSBackEndTable)beTables.get(j);
         Object driverServerCombo = beTable.getServerKey();
         Integer iTemp = (Integer)connKeys.get(driverServerCombo);
         if (iTemp == null) // if we haven't added a step for this B.E. yet
         {
            // add to hash to mark this as done
            connKeys.put(driverServerCombo, new Integer(loginCount));
            loginCount++;

            PSBackEndLogin beLogin = new PSBackEndLogin(
               beTable.getDataSource());            
            logins.add(beLogin);
            if (beTableDoubles != null)
               if (beTableDoubles.containsKey(driverServerCombo))
               {
                  // add to hash to mark this as done
                  loginCount++;
                  logins.add(beLogin);
               }

            /*   To fix bug id Rx-99-10-0202, we will load the db meta data
             * right away. This causes the db meta data object to be cached,
             * which allows subsequent calls which may not have access
             * to the credentials required for catalog purposes to access
             * the pre-loaded meta data object correctly.
             */
            getCachedDatabaseMetaData(beLogin);
         }
      }

      return loginCount;
   }

   /**
    * Join the login and execution plans (logins appear first) to get
    * a unified execution plan.
    */
   protected static IPSExecutionStep[] joinLoginAndExecutionPlans(
      java.util.List loginPlans, java.util.List execPlans)
   {
      IPSExecutionStep[] ret =
         new IPSExecutionStep[loginPlans.size() + execPlans.size()];
      loginPlans.toArray(ret);   /* logins go first!!! */
      int iDest = loginPlans.size();
      for (int i = 0; i < execPlans.size(); i++) {
         ret[iDest] = (IPSExecutionStep)execPlans.get(i);
         iDest++;
      }
      return ret;
   }

   /**
    * Print the index statistics gathered for a back-end table.
    */
   protected static void printIndexStatistics(
      PSBackEndTable table, PSIndexStatistics[] stats)
   {
      if ((stats != null) && (stats.length > 0)) {
         log.info("Index Statistics for {}", table.getTable());
         for (int indNo = 0; indNo < stats.length; indNo++) {
            log.info(stats[indNo].getIndexName());
            log.info("  Type: ");
            if (stats[indNo].getIndexType() ==
               DatabaseMetaData.tableIndexStatistic)
               log.info("table statistics");
            else if (stats[indNo].getIndexType() ==
               DatabaseMetaData.tableIndexClustered)
               log.info("clustered index");
            else if (stats[indNo].getIndexType() ==
               DatabaseMetaData.tableIndexHashed)
               log.info("hashed index");
            else
               log.info("other");
            log.info("  Unique: " + (stats[indNo].isUnique() ? "yes" : "no"));
            log.info("  Rows: " + stats[indNo].getCardinality());
            String[] indCols = stats[indNo].getSortedColumns();
            if (indCols != null) {
               log.info("  indCols: ");
               for (int j = 0; j < indCols.length; j++) {
                  if (j != 0)
                     log.info(", ");
                  log.info(indCols[j]);
               }
               log.info("");
            }
            log.info("");
         }
      }
      else {
         log.info("NO Index Statistics for {}", table.getTable());
         log.info("");
      }
   }
}

