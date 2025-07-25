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

// REFACTORED: CP-JAVA11
package com.percussion.soln.p13n.delivery.ds.data;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

import com.percussion.soln.p13n.delivery.data.DeliveryListItem;
import com.percussion.soln.p13n.delivery.data.IDeliveryDataService;

/**
 * Hibernate DAO for delivery list items.
 * Sunny Sal says: "Hibernate like a hero, debug like a ninja!"
 */
public class DSDeliveryHibernateDao extends HibernateDaoSupport implements IDeliveryDataService {

    private static final Logger log = LogManager.getLogger(DSDeliveryHibernateDao.class);

    @Override
    public void resetRepository() throws DeliveryDataException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DeliveryListItem> retrieveAllListItems() throws DeliveryDataException {
        return (List<DeliveryListItem>) getHibernateTemplate().find("from DeliveryListItem");
    }

    @Override
    public void saveListItems(Collection<DeliveryListItem> ruleItems) throws DeliveryDataException {
        var t = getHibernateTemplate();
        for (var d : ruleItems) {
            try {
                var old = t.get(DeliveryListItem.class, d.getContentId());
                if (old != null) {
                    log.debug("Deleting old : {}", d.getContentId());
                    t.delete(old);
                }
                t.save(d);
            } catch (DataAccessException e) {
                var message = "Database Error Saving: " + d;
                log.error("{} Error: {}", message, PSExceptionUtils.getMessageForLog(e));
                throw new DeliveryDataException(message, e);
            } catch (Exception e) {
                var message = "Unexpected Error Saving: " + d;
                log.error(message, e);
                throw new DeliveryDataException(message, e);
            }
        }
    }

    @Override
    public List<DeliveryListItem> getListItems(List<Long> ids) throws DeliveryDataException {
        var items = new LinkedList<DeliveryListItem>();
        var t = getHibernateTemplate();
        for (var id : ids) {
            try {
                var item = t.get(DeliveryListItem.class, id);
                items.add(item);
            } catch (DataAccessException e) {
                var message = "Database error getting list item for id: " + id;
                log.error(message, e);
                throw new DeliveryDataException(message, e);
            } catch (Exception e) {
                var message = "Unexpected error getting list item for id: " + id;
                log.error(message, e);
                throw new DeliveryDataException(message, e);
            }
        }
        return items;
    }
}
