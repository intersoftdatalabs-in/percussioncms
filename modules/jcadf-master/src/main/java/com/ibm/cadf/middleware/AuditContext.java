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

package com.ibm.cadf.middleware;

public class AuditContext
{
    private String targetName;

    private String targetUrl;

    private String targetUsername;

    private String targetEndpointName;
    
    private String observerName;

    private String initiatorIP;

    private String iniatorName;

    private String agentName;
    private String activity;
    private String guidID;
    private String path;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public String getGuidID() {
        return guidID;
    }

    public void setGuidID(String guidID) {
        this.guidID = guidID;
    }



    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }


    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }



    public AuditContext()
    {
    }

    public String getTargetName()
    {
        return targetName;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public String getTargetUrl()
    {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl)
    {
        this.targetUrl = targetUrl;
    }

    public String getTargetUsername()
    {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername)
    {
        this.targetUsername = targetUsername;
    }

    public String getObserverName()
    {
        return observerName;
    }

    public void setObserverName(String observerName)
    {
        this.observerName = observerName;
    }

    public String getInitiatorIP()
    {
        return initiatorIP;
    }

    public void setInitiatorIP(String initiatorIP)
    {
        this.initiatorIP = initiatorIP;
    }

    public String getIniatorName()
    {
        return iniatorName;
    }

    public void setIniatorName(String iniatorName)
    {
        this.iniatorName = iniatorName;
    }

    public String getTargetEndpointName()
    {
        return targetEndpointName;
    }

    public void setTargetEndpointName(String targetEndpointName)
    {
        this.targetEndpointName = targetEndpointName;
    }

    
}
