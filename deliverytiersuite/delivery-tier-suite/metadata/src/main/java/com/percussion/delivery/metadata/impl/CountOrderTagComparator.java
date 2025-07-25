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

package com.percussion.delivery.metadata.impl;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

/**
 * Comparator for sorting JSONObjects by tag count in descending order.
 *
 * @author davidpardini
 */
public class CountOrderTagComparator implements Comparator<JSONObject>
{

    private static final Logger log = LogManager.getLogger(CountOrderTagComparator.class);

    @Override
    public int compare(JSONObject o1, JSONObject o2)
    {
        try
        {
            var count1 = o1.optInt(PSMetadataTagsHelper.TAG_COUNT, 0);
            var count2 = o2.optInt(PSMetadataTagsHelper.TAG_COUNT, 0);
            return Integer.compare(count2, count1);
        }
        catch (Exception e)
        {
            log.error("Error comparing tag counts: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return 0;
        }
    }
}
