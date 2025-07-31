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
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="GetCategoryStatesResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getCategoryStatesResult"
})
@XmlRootElement(name = "GetCategoryStatesResponse")
public final class GetCategoryStatesResponse {

    @XmlElement(name = "GetCategoryStatesResult")
    private final String getCategoryStatesResult;

    private GetCategoryStatesResponse(Builder builder) {
        this.getCategoryStatesResult = builder.getCategoryStatesResult;
    }

    /**
     * Gets the value of the getCategoryStatesResult property.
     *
     * @return the category states result, or null if not set
     */
    public String getGetCategoryStatesResult() {
        return getCategoryStatesResult;
    }

    /**
     * Builder for GetCategoryStatesResponse.
     * <p>
     * Example usage:
     * <pre>
     * var response = new GetCategoryStatesResponse.Builder()
     *     .withCategoryStatesResult("Active")
     *     .build();
     * </pre>
     */
    public static class Builder {
        private String getCategoryStatesResult;

        /**
         * Sets the category states result value.
         * @param getCategoryStatesResult the result string
         * @return this builder instance
         */
        public Builder withCategoryStatesResult(String getCategoryStatesResult) {
            this.getCategoryStatesResult = getCategoryStatesResult;
            return this;
        }

        /**
         * Builds a new immutable GetCategoryStatesResponse instance.
         * @return a new GetCategoryStatesResponse
         */
        public GetCategoryStatesResponse build() {
            return new GetCategoryStatesResponse(this);
        }
    }
}
