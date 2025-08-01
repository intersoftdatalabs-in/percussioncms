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

import com.percussion.design.objectstore.IPSReplacementValue;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * The IPSStatementBlock interface defines a block of text which will
 * be used to construct a statement. These blocks can be strung together
 * to get the full text of the statement. Blocks can be static blocks
 * or replaceable blocks. Static blocks are always used when building the
 * statement text. Replaceable blocks are used only if all the XML
 * fields they contain are not {@code null}.
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public interface IPSStatementBlock
{
   /**
    * Set the data for the bound column(s) associated with this block.
    *
    * @param data the execution data associated with this plan, may not be {@code null}
    * @param stmt the prepared statement, may not be {@code null}
    * @param bindStart the starting position (1-based) to bind columns to
    *
    * @return the next bind position (1-based)
    *
    * @throws SQLException if a SQL error occurs
    * @throws PSDataExtractionException if data extraction fails
    */
   int setColumnData(PSExecutionData data, PreparedStatement stmt, int bindStart)
      throws SQLException, PSDataExtractionException;

   /**
    * Releases all resources that were used by the column's data.
    * This should be called after the data is no longer needed.
    */
   void releaseColumnData();

   /**
    * Build the statement text which can be passed to the JDBC Connection
    * object's prepareStatement method. Placeholders (?) will be used for
    * each variable defined in the statement.
    *
    * @param data the run-time context info for this request, may not be {@code null}
    *
    * @return the statement text, never {@code null}
    *
    * @throws PSDataExtractionException if data extraction fails
    */
   String buildStatement(PSExecutionData data) throws PSDataExtractionException;

   /**
    * Build the statement text which can be passed to the JDBC Connection
    * object's prepareStatement method. Placeholders (?) will be used for
    * each variable defined in the statement.
    *
    * @param buf the buffer to store the text in, may not be {@code null}
    * @param data the run-time context info for this request, may not be {@code null}
    *
    * @throws PSDataExtractionException if data extraction fails
    */
   void buildStatement(StringBuilder buf, PSExecutionData data) throws PSDataExtractionException;

   /**
    * Get the list of LOB-based PSStatementColumns.
    *
    * @return The list of columns. Never {@code null}. Can be empty.
    */
   List<PSStatementColumn> getLobStatementColumns();

   /**
    * Get the data extractors used to get the replacement values which will
    * be used to execute the statement.
    *
    * @return The list of data extractors. Never {@code null}, may be empty.
    */
   List<IPSDataExtractor> getReplacementValueExtractors();

   /**
    * Is this a static block?
    *
    * @return {@code true} if this is a static block, {@code false} otherwise
    */
   boolean isStaticBlock();

   /**
    * Does this block contain static SQL text?
    *
    * @return {@code true} if this block contains static SQL text, {@code false} otherwise
    */
   boolean hasStaticSql();

   /**
    * Add text to this statement block.
    *
    * @param text the text to add, may be {@code null}
    */
   void addText(String text);

   /**
    * Add a replacement field to this statement block.
    *
    * @param value the replacement value, may not be {@code null}
    * @param params the parameters for the replacement value, may be {@code null}
    *
    * @throws PSDataExtractionException if the replacement field cannot be added
    */
   void addReplacementField(IPSReplacementValue value, Object[] params)
      throws PSDataExtractionException;
}
