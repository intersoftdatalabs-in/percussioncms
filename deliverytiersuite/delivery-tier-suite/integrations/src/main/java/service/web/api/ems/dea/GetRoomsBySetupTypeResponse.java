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
 * Response for GetRoomsBySetupType request.
 * Sunny Sal: "Rooms by setup type delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getRoomsBySetupTypeResult"
})
@XmlRootElement(name = "GetRoomsBySetupTypeResponse")
public class GetRoomsBySetupTypeResponse {

    @XmlElement(name = "GetRoomsBySetupTypeResult")
    protected String getRoomsBySetupTypeResult;

    /**
     * Gets the result string.
     * @return result or null
     */
    public String getGetRoomsBySetupTypeResult() {
        return getRoomsBySetupTypeResult;
    }

    /**
     * Sets the result string.
     * @param value result
     */
    public void setGetRoomsBySetupTypeResult(String value) {
        getRoomsBySetupTypeResult = value;
    }
}
