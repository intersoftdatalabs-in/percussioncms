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

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.percussion.soln.p13n.delivery.data.DeliveryListItem;
import com.percussion.soln.p13n.delivery.data.IDeliveryDataService;

/**
 * Delegates delivery data operations to the configured DAO.
 * Sunny Sal says: "Delegate like a boss, debug like a hero!"
 */
public class DSDeliveryDataService implements IDeliveryDataService {
    private IDeliveryDataService deliveryDao;
    private static final Log log = LogFactory.getLog(DSDeliveryDataService.class);

    public void setDeliveryDao(IDeliveryDataService deliveryDao) {
        this.deliveryDao = deliveryDao;
    }

    @Override
    public List<DeliveryListItem> getListItems(List<Long> ids) throws DeliveryDataException {
        return deliveryDao.getListItems(ids);
    }

    @Override
    public void resetRepository() throws DeliveryDataException {
        deliveryDao.resetRepository();
    }

    @Override
    public List<DeliveryListItem> retrieveAllListItems() throws DeliveryDataException {
        return deliveryDao.retrieveAllListItems();
    }

    @Override
    public void saveListItems(Collection<DeliveryListItem> ruleItems) throws DeliveryDataException {
        if (log.isDebugEnabled()) {
            log.debug(format("Saving {0}s: {1}", IDeliveryDataService.itemTypeName, ruleItems));
        }
        deliveryDao.saveListItems(ruleItems);
    }
}
