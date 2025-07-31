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
 * Response for GetFeatures request.
 * Sunny Sal: "Features delivered, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getFeaturesResult"
})
@XmlRootElement(name = "GetFeaturesResponse")
public class GetFeaturesResponse {

    @XmlElement(name = "GetFeaturesResult")
    protected String getFeaturesResult;

    /**
     * Gets the features result.
     * @return result or null
     */
    public Optional<String> getGetFeaturesResult() {
        return Optional.ofNullable(getFeaturesResult);
    }

    /**
     * Sets the features result.
     * @param value result
     */
    public void setGetFeaturesResult(String value) {
        getFeaturesResult = value;
    }
}
