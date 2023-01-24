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
package com.percussion.services.utils.orm;

import com.percussion.utils.guid.IPSGuid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;

/**
 * This class is similar to
 * {@link com.percussion.services.utils.orm.PSDataCollectionHelper} in that
 * it manages hibernate queries to avoid problems when there are a lot of ids
 * in an 'in' clause. This class is simplified compared to the mentioned one
 * in that its use is simpler and more natural and does not require SQL.
 * <p>
 * A typical usage would be as follows:
 * 
 * <pre>
 * Session session = getSession();
 * try
 * {
 *    PSCriteriaQueryRepeater&lt;PSObjectLock&gt; cr = new PSCriteriaQueryRepeater&lt;PSObjectLock&gt;()
 *    {
 *       public void add(Criteria criteria, List&lt;IPSGuid&gt; ids)
 *       {
 *          criteria.add(Restrictions.in(&quot;objectId&quot;, PSGuidUtils
 *                .toFullLongList(ids)));
 *          if (!StringUtils.isBlank(lockSession))
 *             criteria.add(Restrictions.eq(&quot;lockSession&quot;,
 *                   getLockSession(lockSession)));
 *          if (!StringUtils.isBlank(locker))
 *             criteria.add(Restrictions.eq(&quot;locker&quot;, locker));
 *       }
 *    };
 *    return cr.query(ids, session, PSObjectLock.class);
 * }
 * finally
 * {
 *    releaseSession(session);
 * }
 * </pre>
 * 
 * @param <E> The type of the data object that is being loaded.
 */
public abstract class PSCriteriaQueryRepeater<E>
{
   /**
    * This method contains all of the restrictions that are added to the
    * criterian. It is called 1 or more times by the
    * {@link #query(List, Session, Class)} method to add the restrictions to
    * a subset of the ids that are involved in the search. It is usually
    * implemented anonymously.
    * 
    * @param c The criteria to use, will never be <code>null</code>.
    * 
    * @param ids The subset of ids small enough to prevent a problem with
    * hibernate. The subset is taken from the list supplied to the
    * {@link #query(List, Session, Class)} method. Will never be
    * <code>null</code> or empty.
    */
   abstract public void add(Criteria c, List<IPSGuid> ids);
   
   /**
    * Makes 1 or more queries (depending on the number of ids supplied) against
    * hibernate to perform a search to load a set of objects of the supplied
    * <code>clazz</code>.
    * 
    * @param ids The ids of the objects being loaded. Never <code>null</code>.
    * If empty, no results are returned.
    * @param sess Used to create the <code>Criteria</code> to make the query.
    * Never <code>null</code>.
    * @param clazz The object type that the query will return. Never
    * <code>null</code>.
    * @return An object for each id that is found in the database. The size may
    * be less than the number of ids supplied and the order is undefined. Never
    * <code>null</code>.
    */
   @SuppressWarnings("unchecked")
   public List<E> query(List<IPSGuid> ids, Session sess, Class clazz)
   {
      if (ids == null)
         throw new IllegalArgumentException("ids cannot be null");
      
      final int MAX = 950;
      int totalIds = ids.size();
      int loops = (totalIds-1)/MAX + 1;
      int j=0;
      Iterator<IPSGuid> idsIter = ids.iterator();
      List<E> results = new ArrayList<>();
      for (int i = 0; i < loops; i++)
      {
         Criteria criteria = sess.createCriteria(clazz);

         int currentMax = (i+1)*MAX;
         if (currentMax > totalIds)
            currentMax = totalIds;
         List<IPSGuid> currentIds = new ArrayList<>();
         for (; j < currentMax; j++)
         {
            currentIds.add(idsIter.next());
         }
         add(criteria, currentIds);
         List dbResults = criteria.list();
         if (dbResults != null)
         results.addAll(dbResults);
      }
      
      return results;
   }
}
