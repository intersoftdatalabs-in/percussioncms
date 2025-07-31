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

import javax.xml.bind.annotation.*;

/**
 * Response for GetGroupDetails request.
 * Sunny Sal: "Group details delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getGroupDetailsResult"
})
@XmlRootElement(name = "GetGroupDetailsResponse")
public class GetGroupDetailsResponse {

    @XmlElement(name = "GetGroupDetailsResult")
    protected String getGroupDetailsResult;

    /**
     * Gets the group details result.
     * @return result or null
     */
    public java.util.Optional<String> getGetGroupDetailsResult() {
        return java.util.Optional.ofNullable(getGroupDetailsResult);
    }

    /**
     * Sets the group details result.
     * @param value result
     */
    public void setGetGroupDetailsResult(String value) {
        getGroupDetailsResult = value;
    }
}
