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
package com.percussion.search.lucene;

import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSSearchConfig;
import com.percussion.search.PSSearchAdmin;
import com.percussion.search.PSSearchEngine;
import com.percussion.search.PSSearchException;
import com.percussion.search.PSSearchIndexer;
import com.percussion.search.PSSearchQuery;
import com.percussion.server.PSServer;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.BooleanQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

public class PSSearchEngineImpl extends PSSearchEngine
{
   /**
    * ctor for search engine implementation. Validates the indexroot property
    * and then creates the search admin and read only search admin objects and
    * then sets the status code as initialized.
    * 
    * @throws PSSearchException
    */
   public PSSearchEngineImpl() throws PSSearchException
   {
      setIndexRootPath();
      m_searchAdmin = new PSSearchAdminImpl();
      m_searchAdminReadOnly = new PSSearchAdminReadOnly(m_searchAdmin);
      setStateCode(STATUS_INITIALIZED);
      ms_log.info("Lucene search engine initialized.");
      //Sets the max clause count to hard coded 10000,
      //@TODO we need to make it as property, as I am just fixing the bug for a patch I did not make it as a property.
      //Search properties are read from the config.xml.
      BooleanQuery.setMaxClauseCount(10000);
   }

   /**
    * Validates the index root property and sets it. If it is is not valid then
    * throws expection and the engine will not be initialized.
    * 
    * @throws PSSearchException
    */
   private void setIndexRootPath() throws PSSearchException
   {
      PSSearchConfig cfg = PSServer.getServerConfiguration().getSearchConfig();
      String indexRoot = cfg.getIndexDirectory();
      if (StringUtils.isBlank(indexRoot))
      {
         throw new PSSearchException(
               IPSLuceneErrors.INDEX_DIR_PARAM_INVALID_MISSING,
               IPSLuceneConstants.REQPROP_INDEX_ROOT_DIR);
      }
      File irDir = new File(indexRoot);
      if (irDir.exists() && !irDir.isDirectory())
      {
         if (StringUtils.isBlank(indexRoot))
         {
            throw new PSSearchException(
                  IPSLuceneErrors.INDEX_DIR_PARAM_INVALID_MISSING,
                  IPSLuceneConstants.REQPROP_INDEX_ROOT_DIR);
         }
      }
      m_indexRootPath = indexRoot + INDEXES_DIR + File.separator;
   }

   @Override
   protected PSSearchAdmin doGetSearchAdmin(boolean wantLocked)
   {
      if (wantLocked)
         return m_searchAdmin;
      else
         return m_searchAdminReadOnly;
   }

   @Override
   synchronized protected PSSearchIndexer doGetSearchIndexer() throws PSSearchException
   {
      if (m_searchIndexer==null) {
         m_searchIndexer = new PSSearchIndexerImpl();
      }
      return m_searchIndexer;
   }

   @Override
   protected PSSearchQuery doGetSearchQuery() throws PSSearchException
   {
      PSSearchQuery sq = PSSearchQueryImpl.getInstance();
      return sq;
   }

   @Override
   protected Element doGetStatus(Document doc, Element root)
         throws PSSearchException
   {
      Element engineStatNode = doc.createElement(STATUS_NODE);

      root.appendChild(engineStatNode);

      engineStatNode.setAttribute(STATUS_PROGRAM, ENGINE);

      if (isAvailable())
      {
         engineStatNode.setAttribute(RUN_STATE, STATE_RUNNING);
      }
      else
      {
         engineStatNode.setAttribute(RUN_STATE, STATE_STOPPED);
      }
      return root;
   }
   /*
    * Element, attribute and value strings for status document
    */
   private static final String STATUS_NODE = "status";
   private static final String STATUS_PROGRAM = "program";
   private static final String STATE_STOPPED = "stopped";
   private static final String STATE_RUNNING = "running";
   private static final String ENGINE = "engine";
   private static final String RUN_STATE = "state";   

   @Override
   protected void doReleaseSearchIndexer(PSSearchIndexer si)
         throws PSSearchException
   {
      //There is nothing to release in case of Lucene
      //The indexer needs to be closed before the server shutdown.
      si.close(true);
   }

   @Override
   protected void doReleaseSearchQuery(PSSearchQuery sq)
         throws PSSearchException
   {
      PSSearchQueryImpl.getInstance().close();
   }

   @Override
   protected void doShutdown(boolean force) throws PSSearchException
   {
      //Close all IndexWriter objects
      closeIndexWriters();
   }

   /**
    * Closes all Index Writers this should be called before shutdown of the
    * server.
    * 
    * @throws PSSearchException
    */
   private void closeIndexWriters() throws PSSearchException
   {
      Map<Long, IndexWriter> iws = PSSearchIndexerImpl.getLuceneIndexWriters();
      Iterator<Long> iter = iws.keySet().iterator();
      while(iter.hasNext())
      {
         Long ct = iter.next();
         String ermsg = "Failed to close the index writer for contenttype " +
               "id {0} during server shutdown.";
         Object[] args = {ct.toString()};
         ermsg = MessageFormat.format(ermsg, args);
         IndexWriter iw = iws.get(ct);
         if(iw!=null)
         {
            try
            {
               iw.close();
            }
            catch (CorruptIndexException e)
            {
               throw new PSSearchException(ermsg,e);
            }
            catch (IOException e)
            {
               throw new PSSearchException(ermsg,e);
            }
         }
      }
   }

   @Override
   protected void doStart() throws PSSearchException
   {
      //Nothing to start just set the status code to running
      setStateCode(STATUS_RUNNING);            
   }

   @Override
   public int getStateCode()
   {
      return m_stateCode;
   }

   @Override
   public boolean isAvailable(boolean resync) throws PSSearchException
   {
      return resync;
   }

   @Override
   protected void setStateCode(int code)
   {
      if (code != STATUS_INITIALIZED && code != STATUS_RUNNING
            && code != STATUS_TERMINATED && code != STATUS_TERMINATING
            && code != STATUS_PAUSED)
      {
         throw new IllegalArgumentException("Invalid state code supplied");
      }
      m_stateCode = code;  
   }
   /**
    * Convenient method to provide the root folder of lucene indexes.
    * @return Root folder of lucene index folder.
    */
   public static String getLuceneIndexRootPath()
   {
      return m_indexRootPath;
   }
   
   /**
    * Constant for the folder that holds the lucene indexes.
    */
   public static final String INDEXES_DIR = "indexes";
   
   /**
    * Created in ctor, then never changed.
    */
   private PSSearchAdminImpl m_searchAdmin;
   
   /**
    * Created in ctor, then never changed.
    */
   private PSSearchAdminReadOnly m_searchAdminReadOnly;

   /**
    * Reference to log for this class
    */
   private static final Logger ms_log = LogManager.getLogger(IPSConstants.SEARCH_LOG);
   
   /**
    * Holds the current state of the engine as one of the <code>STATUS_xxx
    * </code> values. Initialized in ctor. Access via standard accessor/mutator.
    */
   private int m_stateCode;
   
   
   /**
    * The root path of the lucene indexes. Initialized in Ctor never
    * <code>null</code> or empty after that.
    */
   private static String m_indexRootPath = null;
   
   /**
    * Search Indexer singleton
    */
   private PSSearchIndexer m_searchIndexer = null;
}
