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
package com.percussion.activity.service.impl;

import static org.apache.commons.lang.Validate.notNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.percussion.activity.data.*;
import org.apache.commons.lang.StringUtils;

import com.percussion.activity.service.IPSTrafficService;
import com.percussion.share.service.impl.PSXmlDataHandler;
import com.percussion.share.service.impl.jaxb.Pair;
import com.percussion.share.service.impl.jaxb.Property;
import com.percussion.share.service.impl.jaxb.Response;
import com.percussion.share.service.impl.jaxb.Result;
import com.percussion.share.service.impl.jaxb.Property.Pvalues;

/**
 * This handler which provides sample effectiveness data from an xml file.
 * 
 * @author luisteixeira
 */
public class PSTrafficDataHandler extends PSXmlDataHandler implements IPSTrafficService
{
    public PSContentTraffic getContentTraffic(PSContentTrafficRequest request) 
    {
        notNull(request);
        
        PSContentTraffic trafficResponse = new PSContentTraffic();
                
        Map<String, Object> props = new HashMap<>();
        props.put("path", request.getPath());
        props.put("granularity", request.getGranularity());
        props.put("startDate", request.getStartDate());
        props.put("endDate", request.getEndDate());
        String traf = request.getTrafficRequested().toString();
        props.put("trafficRequested", traf);
        props.put("usage", request.getUsage());
        
        Response response = getData(props);
        if (response != null)
        {
            props.clear();
            List<Result> results = response.getResult();
            for (Result result : results)
            {
                List<Property> propList = result.getProperty();
                if (!propList.isEmpty())
                {
                    for(Property prop : propList)
                    {
                        if(StringUtils.equalsIgnoreCase("dates", prop.getName()))
                        {
                            trafficResponse.setDates(getStringList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("endDate", prop.getName()))
                        {
                            trafficResponse.setEndDate(prop.getValue());
                        }
                        if(StringUtils.equalsIgnoreCase("livePages", prop.getName()))
                        {
                            trafficResponse.setLivePages(getIntList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("newPages", prop.getName()))
                        {
                            trafficResponse.setNewPages(getIntList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("pageUpdates", prop.getName()))
                        {
                            trafficResponse.setPageUpdates(getIntList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("site", prop.getName()))
                        {
                            trafficResponse.setSite(prop.getValue());
                        }
                        if(StringUtils.equalsIgnoreCase("siteId", prop.getName()))
                        {
                            trafficResponse.setSiteId(prop.getValue());
                        }
                        if(StringUtils.equalsIgnoreCase("startDate", prop.getName()))
                        {
                            trafficResponse.setStartDate(prop.getValue());
                        }
                        if(StringUtils.equalsIgnoreCase("takeDowns", prop.getName()))
                        {
                            trafficResponse.setTakeDowns(getIntList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("updateTotals", prop.getName()))
                        {
                            trafficResponse.setUpdateTotals(getIntList(prop));
                        }
                        if(StringUtils.equalsIgnoreCase("visits", prop.getName()))
                        {
                            trafficResponse.setVisits(getIntList(prop));
                        }
                    }
                }
            }
        }
        
        return trafficResponse;
    }

    @Override
    public PSTrafficDetailsList getTrafficDetails(PSTrafficDetailsRequest request)
    {
        notNull(request);
        
        PSContentTraffic trafficResponse = new PSContentTraffic();
                
        Map<String, Object> props = new HashMap<>();
        props.put("path", request.getPath());
        props.put("startDate", request.getStartDate());
        props.put("endDate", request.getEndDate());
        
        Response response = getData(props);
        if (response != null)
        {
            List<Result> results = response.getResult();
            if (!results.isEmpty())
            {
                Result result = results.get(0);
                List<Property> propList = result.getProperty();
                if (!propList.isEmpty())
                {
                    Property prop = propList.get(0);
                    Pvalues pvalues = prop.getPvalues();
                    if (pvalues != null)
                    {
                        List<Pair> pairList = pvalues.getPair();
                        for (Pair pair : pairList)
                        {
                           
                        }
                    }
                }
            }
        }

        return null;
    }
    
    private List<Integer> getIntList(Property prop)
    {
        List<Integer> intVal = new ArrayList<>();
        List<String> stringVal = new ArrayList<>();
        stringVal = prop.getPvalues().getPvalue();
        for (int i = 0; i < stringVal.size(); i++)
        {
            intVal.add(i, Integer.parseInt(stringVal.get(i)));
        }
            
        return intVal;
    }
    
    private List<String> getStringList(Property prop)
    {
        return prop.getPvalues().getPvalue();
    }
}
