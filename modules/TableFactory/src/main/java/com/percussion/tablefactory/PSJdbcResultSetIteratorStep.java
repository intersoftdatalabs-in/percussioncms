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
package com.percussion.tablefactory;

import com.percussion.util.PSSQLStatement;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is a step which executes the query specified by <code>sqlQuery</code>
 * in its constructor. Its stores the result set returned by executing the
 * sql query. This encapsulated result set can be iterated over using the
 * <code>next()</code> method of this class. Finally when the iteration is over,
 * <code>close()</code> must be closed to close the encapsulated
 * <code>ResultSet</code> and <code>Statement</code> objects.
 */
public class PSJdbcResultSetIteratorStep extends PSJdbcSqlStatement
{
   /**
    * Constructor.
    *
    * @param sqlQuery The sql query to execute, may not be <code>null</code> or
    * empty.
    * @param dbmsDef The database where the table is located,
    * may not be <code> null</code>.
    * @param tableSchema schema of the table to query,
    * may not be <code>null</code>.
    * @param rowAction the action to be set on the row of data returned from
    * the <code>next()</code> method, should be one of the following values:
    * <code>PSJdbcRowData.ACTION_INSERT</code> or
    * <code>PSJdbcRowData.ACTION_UPDATE</code> or
    * <code>PSJdbcRowData.ACTION_REPLACE</code> or
    * <code>PSJdbcRowData.ACTION_DELETE</code> or
    * <code>PSJdbcRowData.ACTION_INSERT_IF_NOT_EXIST</code>
    *
    * @throws IllegalArgumentException if <code>sqlQuery</code> or
    * <code>dbmsDef</code> or <code>tableSchema</code> is <code>null</code>
    * or if <code>sqlQuery</code> is <code>null</code> or empty
    */
   public PSJdbcResultSetIteratorStep(String sqlQuery,
      PSJdbcDbmsDef dbmsDef, PSJdbcTableSchema tableSchema, int rowAction)
   {
      super(sqlQuery);
      if (sqlQuery == null || sqlQuery.trim().length() == 0)
         throw new IllegalArgumentException(
            "sqlQuery may not be null or empty");

      if (dbmsDef == null)
         throw new IllegalArgumentException("dbmsDef may not be null");

      if (tableSchema == null)
         throw new IllegalArgumentException("tableSchema may not be null");

      m_statement = sqlQuery;
      m_dbmsDef = dbmsDef;
      m_tableSchema = tableSchema;
      m_rowAction = rowAction;
   }

   /**
    * Constructor.
    * Note: When using this constructor do not call the <code>next()</code>
    * method, call the <code>hasNext()</code> method.
    *
    * @param sqlQuery The sql query to execute, may not be <code>null</code> or
    * empty.
    * @param dbmsDef The database where the table is located,
    * may not be <code> null</code>.

    * @throws IllegalArgumentException if <code>sqlQuery</code> or
    * <code>dbmsDef</code> is invalid
    */
   public PSJdbcResultSetIteratorStep(String sqlQuery, PSJdbcDbmsDef dbmsDef)
   {
      super(sqlQuery);
      if (sqlQuery == null || sqlQuery.trim().length() == 0)
         throw new IllegalArgumentException(
            "sqlQuery may not be null or empty");

      if (dbmsDef == null)
         throw new IllegalArgumentException("dbmsDef may not be null");

      m_statement = sqlQuery;
      m_dbmsDef = dbmsDef;
   }

   /**
    * @see com.percussion.tablefactory.PSJdbcSqlStatement
    *
    * Use the <code>next()</code> method to iterate through the result set.
    * <code>next()</code> returns <code>null</code> when the result set
    * iteration is over, then call <code>close()</code> to close all open
    * database resources (result set and statement).
    *
    * @return always returns 0.
    */
   public int execute(Connection conn) throws SQLException
   {
      if (conn == null)
         throw new IllegalArgumentException("conn may not be null");

      // execute the sql query and store the result set
      m_stmt = PSSQLStatement.getStatement(conn);
      m_rs = m_stmt.executeQuery(m_statement);
      // get the list of column names in the result set
      m_columns.clear();
      ResultSetMetaData rsmd = m_rs.getMetaData();
      int numberOfColumns = rsmd.getColumnCount();
      for (int i = 1; i <= numberOfColumns; i++)
         m_columns.add(rsmd.getColumnName(i));

      return 0;
   }

   /**
    * Whether the resultset encapsulated by this object has more rows or not.
    *
    * @return <code>false</code> if the resultset encapsulated by this object
    * is <code>null</code> or if the resultset does not have any more rows left,
    * <code>true</code> otherwise.
    */
   public boolean hasNext() throws SQLException
   {
      if (m_rs == null)
         return false;
      return m_rs.next();
   }

   /**
    * Returns the next row of data. Calls <code>next()</code> on the encapsulted
    * <code>ResultSet</code> object.
    *
    * @return the next row of data, may be <code>null</code> if there are
    * no more rows in the result set
    *
    * @throws SQLException if a database access error occurs
    */
   public PSJdbcRowData next() throws SQLException, PSJdbcTableFactoryException
   {
      if (m_rs == null)
         return null;
      if (!m_rs.next())
         return null;

      List<PSJdbcColumnData> colDataList = new ArrayList<>();
      Iterator<String> it = m_columns.iterator();
      String colName = "";
      try
      {
         while (it.hasNext())
         {
            colName = it.next();
            PSJdbcColumnData colData = PSJdbcTableFactory.getColumnData(
               m_dbmsDef, m_tableSchema, m_rs, colName);
            colDataList.add(colData);
         }
      }
      catch (IOException e)
      {
         Object[] args = {m_tableSchema.getName(),
            "Column : " + colName + " " + e.getMessage()};
         throw new PSJdbcTableFactoryException(
            IPSTableFactoryErrors.SQL_CATALOG_DATA, args, e);
      }
      return new PSJdbcRowData(colDataList.iterator(), m_rowAction);
   }

   /**
    * Closes the encapsulted <code>ResultSet</code> and <code>Statement</code>
    * objects.
    */
   public void close()
   {
      if (m_rs != null)
      {
         try
         {
            m_rs.close();
         }
         catch (SQLException e)
         {
            PSJdbcTableFactory.logDebugMessage(
               "Error closing result set: " +
               PSJdbcTableFactoryException.formatSqlException(e));
         }
      }

      if (m_stmt != null)
      {
         try
         {
            m_stmt.close();
         }
         catch (SQLException e)
         {
            PSJdbcTableFactory.logDebugMessage(
               "Error closing statement: " +
               PSJdbcTableFactoryException.formatSqlException(e));
         }
         m_stmt = null;
      }
   }

   /**
    * Returns the meta data corresponding to the resultset encapsulated by this
    * object. The <code>execute()</code> method must be called before this
    * method is called. The <code>close()</code> method should not be called
    * before calling this method.
    *
    * @return the meta data for the encapsulated resultset,
    * may be <code>null</code> if the query execution returned a
    * <code>null</code> resultset (may happen on Sybase - NOT USED ANYMORE).
    *
    * @throws SQLException if a database access error occurs
    * @throws IllegalStateException if <code>execute()</code> method has not
    * yet been called, or if the <code>close()</code> method has already been
    * called.
    */
   public ResultSetMetaData getMetaData() throws SQLException
   {
      if (m_stmt == null)
         throw new IllegalStateException(
            "resultset is not yet created or has been closed");
      if (m_rs == null)
         return null;
      return m_rs.getMetaData();
   }

   /**
    * @see java.lang.Object
    */
   protected void finalize() throws Throwable
   {
      close();
      super.finalize();
   }


   /**
    * The result set obtained by executing the sql query in the
    * <code>execute()</code> method. Initialized in the <code>execute</code>
    * method, may be <code>null</code> if the query does not return any rows
    * (some databases return <code>null</code> result set instead of empty
    * result set)
    */
   private ResultSet m_rs = null;

   /**
    * the sql statement prepared from the sql query <code>m_statement</code>
    * in the <code>execute</code>. Initialized in the <code>execute</code>
    * method, never <code>null</code> after initialization.
    */
   private Statement m_stmt = null;

   /**
    * provides the database/schema information for the table, initialized in the
    * constructor, never <code>null</code> after initialization
    */
   private PSJdbcDbmsDef m_dbmsDef = null;

   /**
    * schema of the table from which data is being catalogged, initialized
    * in the constructor, never <code>null</code> after initialization
    */
   private PSJdbcTableSchema m_tableSchema = null;

   /**
    * the action to be set on the row of data returned from the
    * <code>next()</code> method, defaults to
    * <code>PSJdbcRowData.ACTION_INSERT</code>
    * Set in the constructor, never modified after that.
    */
   private int m_rowAction = PSJdbcRowData.ACTION_INSERT;

   /**
    * List of columns obtained from the result set metadata, populated in the
    * <code>execute</code> method, never <code>null</code>
    */
   private List<String> m_columns = new ArrayList<>();
}

