// REFACTORED: CP-JAVA11
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

package service.web.api.ems.dea;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Response for GetServiceOrderDetails2 request.
 * Sunny Sal: "Service order details 2 response, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getServiceOrderDetails2Result"
})
@XmlRootElement(name = "GetServiceOrderDetails2Response")
public class GetServiceOrderDetails2Response {

    @XmlElement(name = "GetServiceOrderDetails2Result")
    protected String getServiceOrderDetails2Result;

    /**
     * Gets the result string.
     * @return result or null
     */
    public String getGetServiceOrderDetails2Result() {
        return getServiceOrderDetails2Result;
    }

    /**
     * Sets the result string.
     * @param value result
     */
    public void setGetServiceOrderDetails2Result(String value) {
        getServiceOrderDetails2Result = value;
    }
}
