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

package com.percussion.recent.dao.impl;

import com.percussion.recent.dao.IPSRecentDao;
import com.percussion.recent.data.PSRecent;
import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.share.dao.IPSGenericDao.SaveException;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.LinkedList;
import java.util.List;

@Repository("recentDao")
@Transactional
public class PSRecentDao implements IPSRecentDao
{

  @PersistenceContext
private EntityManager entityManager;

    private Session getSession(){
        return entityManager.unwrap(Session.class);
    }

    PSRecentDao()
    {
        
    }

    public List<PSRecent> find(String user, String siteName, RecentType type)
    {
        Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        CriteriaQuery<PSRecent> criteria = builder.createQuery(PSRecent.class);
        Root<PSRecent> recent = criteria.from(PSRecent.class);
        List<Predicate> predList = new LinkedList<>();

        if (user!=null) {
            predList.add(builder.equal(recent.get("user"), user));
        }
        if(siteName!=null) {
            predList.add(builder.equal(recent.get("siteName"), siteName));
        }
        if(type!=null) {
            predList.add( builder.equal(recent.get("type"), type));
        }
        Predicate[] preds = new Predicate[predList.size()];
        preds = predList.toArray(preds);
        criteria.where(preds);
        criteria.orderBy(builder.asc(recent.get("order")));
        return entityManager
                .createQuery(criteria)
                .getResultList();
    }
    

    public void saveAll(List<PSRecent> recentList)
    {
     
        for (PSRecent recent : recentList)
        {
            getSession().saveOrUpdate(recent);
        }
        
    }


    public void delete(PSRecent recent) 
    {
        getSession().delete(recent);
    }
    

    public void deleteAll(List<PSRecent> recentList)
    {
        for (PSRecent recent : recentList)
        {
            getSession().delete(recent);
        }
    }

    

    public void save(PSRecent recent) throws SaveException
    {
        getSession().saveOrUpdate(recent);
    }

 
}

