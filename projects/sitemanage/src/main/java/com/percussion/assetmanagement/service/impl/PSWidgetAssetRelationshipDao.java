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
