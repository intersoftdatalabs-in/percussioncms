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
 *         &lt;element name="GetBookingsResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "getBookingsResult"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBookingsResponse")
public class GetBookingsResponse {
    @XmlElement(name = "GetBookingsResult")
    protected String getBookingsResult;

    /**
     * Gets the value of the getBookingsResult property.
     * @return Optional of getBookingsResult
     */
    public java.util.Optional<String> getGetBookingsResult() {
        return java.util.Optional.ofNullable(getBookingsResult);
    }
    /**
     * Sets the value of the getBookingsResult property.
     * @param value allowed object is {@link String }
     */
    public void setGetBookingsResult(String value) {
        this.getBookingsResult = value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBookingsResponse that = (GetBookingsResponse) o;
        return java.util.Objects.equals(getBookingsResult, that.getBookingsResult);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(getBookingsResult);
    }
    @Override
    public String toString() {
        return "GetBookingsResponse{" +
                "getBookingsResult='" + getBookingsResult + '\'' +
                '}';
    }
}
