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

package com.percussion.assetmanagement.service.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.service.IPSWidgetAssetRelationshipDao;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.services.guidmgr.PSGuidHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.SQLException;

import static com.percussion.util.PSSqlHelper.qualifyTableName;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * The service used to update Page & Asset relationships.
 * 
 * @author YuBingChen
 */
@Transactional
@Component("widgetAssetRelationshipDao")
public class PSWidgetAssetRelationshipDao implements IPSWidgetAssetRelationshipDao
{
    @PersistenceContext
    private EntityManager entityManager;

    private Session getSession(){
        return entityManager.unwrap(Session.class);
    }
    

    @Transactional
    public int updateWidgetNameForRelatedPages(String templateId, String widgetName, long widgetId)
    {
       
        
        widgetName = isBlank(widgetName) ? "NULL" : widgetName;
        int sortRank = PSGuidHelper.generateNext(PSTypeEnum.SORT_RANK).getUUID();
        
        Session sess = getSession();

        try
        {
            String sql = "update " + qualifyTableName(IPSConstants.PSX_RELATIONSHIPS) +
                    " set WIDGET_NAME= :name, SORT_RANK=:sortrank where SLOT_ID = :slotid"
                    + " and WIDGET_NAME IS NULL and OWNER_ID in (select CONTENTID from "
                    + qualifyTableName("CT_PAGE") + " where TEMPLATEID = :template)";

            SQLQuery query = sess.createSQLQuery(sql);
            query.setString("name",widgetName );
            query.setLong("slotid",widgetId);
            query.setInteger("sortrank",sortRank);
            query.setString("template",templateId);

            int result = query.executeUpdate();
            
            logger.debug("Updated {} rows in {} table.",result,IPSConstants.PSX_RELATIONSHIPS );

            return result;
        }
        catch (SQLException e)
        {
            logger.error("Failed to update relationship table: {}", PSExceptionUtils.getMessageForLog(e));
            logger.debug(e);
            throw new PSRuntimeException(e);
        }
      
    }
    

    private static final Logger logger = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);
}
