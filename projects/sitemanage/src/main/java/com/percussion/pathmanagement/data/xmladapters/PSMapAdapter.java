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

package com.percussion.pathmanagement.data.xmladapters;

import com.percussion.pathmanagement.data.PSPathItemDisplayProperties;
import com.percussion.pathmanagement.data.PSPathItemDisplayProperty;
import com.percussion.pathmanagement.data.xmladapters.PSMapAdapter;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Custom XmlAdapter which takes a PSPathItem object (it has
 * a map of String objects) and maps it into a PSPathItemDisplayProperties
 * object.
 * 
 * @author federicoromanelli
 *
 */
public class PSMapAdapter extends XmlAdapter<PSPathItemDisplayProperties, Map<String, String>>
{

    @Override
    public Map<String, String> unmarshal(PSPathItemDisplayProperties v) throws Exception
    {
        HashMap<String, String> hashMap = new HashMap<>();
        
        for (PSPathItemDisplayProperty displayProp : v.getDisplayProperty())
            hashMap.put(displayProp.getName(), displayProp.getValue());
        
        return hashMap;
    }

    @Override
    public PSPathItemDisplayProperties marshal(Map<String, String> v) throws Exception
    {
        PSPathItemDisplayProperties displayProperties = new PSPathItemDisplayProperties();
        try
        {
        
            for (String propName : v.keySet())
            {
                PSPathItemDisplayProperty prop = new PSPathItemDisplayProperty();
                prop.setName(propName);
                prop.setValue(v.get(propName));
                
                displayProperties.getDisplayProperty().add(prop);
            }
        }
        catch (Exception e){}
        
        return displayProperties;
    }

}
