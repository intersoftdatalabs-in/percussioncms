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

import com.percussion.error.PSIllegalArgumentException;
import com.percussion.error.PSSqlException;
import com.percussion.util.PSPreparedStatement;
import com.percussion.utils.jdbc.PSConnectionDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;


/**
 * The PSNativeStatement class defines a native statement which can
 * be executed as part of a query execution plan.
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSNativeStatement extends PSStatement {

   /**
    * Construct a query statement which can be executed as part of the
    * query execution plan. The query may contain place holders, which
    * must be filled prior to execution.
    *
    * @param   connKey      the connection key to use to get the db conn
    *
    * @param   blocks      the statement blocks comprising this statement
    */
   public PSNativeStatement(   int connKey,
                              IPSStatementBlock[] blocks)
      throws com.percussion.data.PSDataExtractionException
   {
      super(connKey, blocks, false);
   }


   /* ************  IPSExecutionStep Interface Implementation ************ */

   /**
    * Execute the native statement as a step in the execution plan.
    * The result set generated by executing this statement will be added to
    * the execution data.
    * <p>
    * The following steps are performed during execution:
    * <ol>
    * <li>PSQueryHandler filled in the statement before calling execute.
    * We can now check the cache to see if the statement already
    * exists in the cache (if caching is enabled for this pipe).</li>
    * <li>if the statement was in cache, we can add its results to the
    * execution data and return</li>
    * <li>otherwise, we need to execute the statement through the db pool
    * and add its results to the execution data</li>
    * <li>if caching is enabled, we also need to add its results to the
    * cache</li>
    * </ol>
    *
    * @param   data     the execution data associated with this plan
    *
    * @exception   SQLException If a SQL error occurs.
    */
   public void execute(PSExecutionData data)
      throws java.sql.SQLException,
         com.percussion.data.PSDataExtractionException,
         com.percussion.error.PSErrorException
   {
      String sqlString = buildSqlString(data);

      /* *TODO* (future)
       * if the statement was in cache, we can add its results to the
       * execution data and return
       */

      /* otherwise, we need to execute the statement through the db pool
       * and add its results to the execution data
       */
      Connection conn = null;
      PSConnectionDetail connDetail = null;
      PreparedStatement stmt   = null;

      try {
         try {
            conn = data.getDbConnection(m_connKey);
            connDetail = data.getDbConnectionDetail(m_connKey);
         } catch (PSIllegalArgumentException ie) {
            throw new SQLException(ie.getMessage());
         }

         // log the statement (if full user activity is on)
         logPreparedStatement(data, sqlString);
         /* workaround for JDK Bug Id  4423012 */
         boolean preparedStatementHasParams = (sqlString.indexOf("?") >= 0);
         stmt = PSPreparedStatement.getPreparedStatement(conn, sqlString);
         data.addPreparedStatement(stmt);

         try 
         {
            stmt.setMaxFieldSize(0);   // unlimited size permitted
         }
         catch (SQLException se) 
         {
            // if this is not supported, that's ok. otherwise, treat it
            // as an error
            if (!PSSqlException.hasFeatureNotSupported(se,
               connDetail.getDriver()))
            {
               throw se;
            }
         }
         
         if (preparedStatementHasParams)
            stmt.clearParameters();
         int bindStart = 1;
         for (int i = 0; i < m_blocks.length; i++) {
            bindStart = m_blocks[i].setColumnData(data, stmt, bindStart);
         }

         // Tune the ResultSet for the number of rows to handle
         int maxRows = data.getMaxRows();
         if (maxRows > 0)
            stmt.setMaxRows(maxRows);

         /* can't call stmt.executeQuery() in case it's a stored proc, need to
          * call execute() and then walk results and grab the first result set
          * found, skipping over update counts.
          */          
         ResultSet rs = null;
         boolean gotResultSet = stmt.execute();
         boolean done = false;
         while (rs == null && !done) 
         {
            // take first resultset we come across
            if (gotResultSet)
            {
               rs = stmt.getResultSet();
            }
            else
            {
               gotResultSet = stmt.getMoreResults();               
               done = (!gotResultSet && (stmt.getUpdateCount() == -1));               
            }
         } 
         
         if (rs != null)
            data.getResultSetStack().push(rs);

         /* *TODO* (future)
          * if caching is enabled, we must add the result set to the cache
          *
          * when this is the case, we must convert the result set
          * to our PSResultSet which allows it to be read multiple
          * times, etc.
          */
      } catch (java.sql.SQLException e) {
         if (conn != null) {
            SQLWarning wConn = conn.getWarnings();
            SQLWarning wStmt = null;

            if (stmt != null)
               wStmt = stmt.getWarnings();

            if ((wStmt != null) || (wConn != null)) {
               /* now add the warnings to the exception */
               SQLException cur = e;
               while (cur.getNextException() != null)
                  cur = cur.getNextException();

               if (wStmt != null) {
                  cur.setNextException(wStmt);
                  cur = wStmt;
               }

               if (wConn != null) {
                  while (cur.getNextException() != null)
                     cur = cur.getNextException();
                  cur.setNextException(wConn);
               }
            }

            if (stmt != null) {
               stmt.close();
               data.removePreparedStatement(stmt);
            }

            throw e;
         }
      }
   }
}

