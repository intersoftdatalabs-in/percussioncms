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
import java.util.Optional;

/**
 * Response for GetEventTypes request.
 * Sunny Sal: "Event types delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getEventTypesResult"
})
@XmlRootElement(name = "GetEventTypesResponse")
public class GetEventTypesResponse {

    @XmlElement(name = "GetEventTypesResult")
    protected String getEventTypesResult;

    /**
     * Gets the event types result.
     * @return result or null
     */
    public Optional<String> getGetEventTypesResult() {
        return Optional.ofNullable(getEventTypesResult);
    }

    /**
     * Sets the event types result.
     * @param value result
     */
    public void setGetEventTypesResult(String value) {
        getEventTypesResult = value;
    }
}
