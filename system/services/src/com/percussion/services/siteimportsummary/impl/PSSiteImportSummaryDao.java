/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.services.siteimportsummary.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.siteimportsummary.IPSSiteImportSummaryDao;
import com.percussion.services.siteimportsummary.data.PSSiteImportSummary;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.util.PSBaseBean;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@PSBaseBean("sys_siteImportSummaryDao")
@Transactional
@Repository
public class PSSiteImportSummaryDao implements IPSSiteImportSummaryDao
{
   private static final Logger log = LogManager.getLogger(IPSConstants.IMPORT_LOG);
   /**
    * Constant for the key used to generate summary ids.
    */
   private static final String SITE_IMPORT_SUMMARY_KEY = "PSX_SITEIMPORTSUMMARY";
   
   private IPSGuidManager m_guidMgr;

    @PersistenceContext
    private EntityManager entityManager;

    private Session getSession(){
        return entityManager.unwrap(Session.class);
    }
    
    @Transactional
   public void save(PSSiteImportSummary summary) throws IPSGenericDao.SaveException {
      Validate.notNull(summary);
      if (summary.getSummaryId() == -1)
      {
          summary.setSummaryId(m_guidMgr.createId(SITE_IMPORT_SUMMARY_KEY));
      }
      
      Session session = getSession();
      try
      {
          session.saveOrUpdate(summary);
      }
      catch (HibernateException e)
      {
          String msg = "database error " + e.getMessage();
          log.error(msg);
          throw new IPSGenericDao.SaveException(msg, e);
      }
      finally
      {
          session.flush();

      }  
   }

   public PSSiteImportSummary findBySummaryId(int summaryId)
   {
      PSSiteImportSummary summary = null;
      Session session = getSession();

          Query query = session.createQuery("from PSSiteImportSummary where summaryId = :summaryId");
          query.setParameter("summaryId", summaryId);

          List<PSSiteImportSummary> results = query.list(); 
          if (!results.isEmpty())
          {
             summary = results.get(0);
              if (results.size() > 1)
              {
                  log.error("More than one managed link found for linkid: {}", summaryId);
              }
          }
          return summary;

   }

   public PSSiteImportSummary findBySiteId(int siteId)
   {
      PSSiteImportSummary summary = null;
      Session session = getSession();

          Query query = session.createQuery("from PSSiteImportSummary where siteId = :siteId");
          query.setParameter("siteId", siteId);
          

          List<PSSiteImportSummary> results = query.list(); 
          if (!results.isEmpty())
          {
             summary = results.get(0);
              if (results.size() > 1)
              {
                  log.error("More than one managed link found for siteId: {}" , siteId);
              }
          }
          return summary;

   }

   @Transactional
   public void delete(PSSiteImportSummary summary)
   {
      Validate.notNull(summary);
      Session session = getSession();
      try
      {
          session.delete(summary);
      }
      catch (HibernateException e)
      {
          String msg = "Failed to delete site import summary: " + e.getMessage();
          log.error(msg);
      }
      finally
      {
          session.flush();

      }
   }

   @Autowired
   public void setGuidManager(IPSGuidManager guidMgr)
   {
       m_guidMgr = guidMgr;
   }

}
