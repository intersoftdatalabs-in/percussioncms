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
package com.percussion.delivery.metadata.impl;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

/**
 * @author davidpardini
 * 
 */
public class AlphaOrderTagComparator implements Comparator<JSONObject>
{
    private static final Logger log = LogManager.getLogger(AlphaOrderTagComparator.class);

    public int compare(JSONObject o1, JSONObject o2)
    {
        JSONObject ob1 = o1;
        JSONObject ob2 = o2;
        int returnCompare = 0;
        try
        {
            returnCompare = ((String) ob1.get(PSMetadataTagsHelper.TAG_NAME)).compareTo((String) ob2
                    .get(PSMetadataTagsHelper.TAG_NAME));
        }
        catch (JSONException e)
        {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return returnCompare;
    }
}
