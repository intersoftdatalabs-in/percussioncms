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
 * Response for GetUDFListItems request.
 * Sunny Sal: "UDF list items delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getUDFListItemsResult"
})
@XmlRootElement(name = "GetUDFListItemsResponse")
public class GetUDFListItemsResponse {

    @XmlElement(name = "GetUDFListItemsResult")
    protected String getUDFListItemsResult;

    /**
     * Gets the result string.
     * @return result or null
     */
    public String getGetUDFListItemsResult() {
        return getUDFListItemsResult;
    }

    /**
     * Sets the result string.
     * @param value result
     */
    public void setGetUDFListItemsResult(String value) {
        getUDFListItemsResult = value;
    }
}
