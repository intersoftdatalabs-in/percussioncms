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

import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestStatistics;


/**
 * The PSUpdateInsertStatement class is used to execute UPDATE and 
 * optionally INSERT statements. It first attempts to process the UPDATE.
 * If this processes no rows, an INSERT is then attempted.
 *
 * @see        com.percussion.design.objectstore.PSUpdatePipe
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSUpdateInsertStatement extends PSUpdateStatement
{
   /**
    * Construct an update statement which can be executed as part of the
    * update execution plan. The query may contain place holders, which
    * must be filled prior to execution.
    *   
    * @param   connKey      the connection key to use to get the db conn
    *
    * @param   updBlocks   the update statement blocks
    *
    * @param   insBlocks   the insert statement blocks
    */
   public PSUpdateInsertStatement(
      int connKey, IPSStatementBlock[] updBlocks,
      IPSStatementBlock[] insBlocks)
      throws com.percussion.data.PSDataExtractionException
   {
      super(connKey, updBlocks, TYPE_UPDATE);
      m_insertStatement
         = new PSUpdateStatement(connKey, insBlocks, TYPE_INSERT);
   }

   /**
    * Set the name of the node which we will iterate over executing this
    * statement.
    * As long as a node of the specified name exists, this statement should
    * be executed. This object does not actually make use of this
    * information. It will only execute once in the context specified to
    * the execute call. This is primarily used as the storage are so that
    * the PSTransactionSet object calling this knows what to iterate on.
    *
    * @param   nodeName      the name of the node to iterate on
    */
   public void setIteratorNode(String nodeName)
   {
      super.setIteratorNode(nodeName);
      m_insertStatement.setIteratorNode(nodeName);
   }

   /**
    * Get the data extractors used to get the replacement values which will
    * be used to execute the statement.
    *
    * @return            the list of replacement values
    */
   public java.util.List getReplacementValueExtractors()
   {
      java.util.ArrayList retList = new java.util.ArrayList();

      retList.addAll(super.getReplacementValueExtractors());
      retList.addAll(m_insertStatement.getReplacementValueExtractors());

      return retList;
   }

   /* ************  IPSExecutionStep Interface Implementation ************ */

   /**
    * Execute the data modification statement as a step in the execution
    * plan. A result set will be generated containing the number of rows
    * effected by the execution of this statement. The result set will be
    * added to the execution data.
    *
    * @param   data     the execution data associated with this plan
    *
    * @exception   SQLException
    *                     if a SQL error occurs
    */
   public void execute(PSExecutionData data)
      throws java.sql.SQLException,
         com.percussion.data.PSDataExtractionException,
         com.percussion.error.PSErrorException
   {
      // get the updated row count so we can see if this update affects
      // any rows
      PSRequest req = data.getRequest();
      PSRequestStatistics stats = req.getStatistics();
      int rowsUpdated = stats.getRowsUpdated();

      super.execute(data);      // process the update statement
      if (rowsUpdated == stats.getRowsUpdated()) {
         int level = req.getCurrentApplicationAccessLevel();
         if ((level & PSAclEntry.AACE_DATA_CREATE) ==
            PSAclEntry.AACE_DATA_CREATE)
         {   // no new updates and we're allowed to do inserts.
            // we need to execute the insert statement now.
            m_insertStatement.execute(data);
         }
      }
   }


   protected PSUpdateStatement   m_insertStatement;
}

