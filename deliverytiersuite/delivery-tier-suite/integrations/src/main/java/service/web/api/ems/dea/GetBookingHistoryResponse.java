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
 *         &lt;element name="GetBookingHistoryResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "getBookingHistoryResult"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBookingHistoryResponse")
public class GetBookingHistoryResponse {
    @XmlElement(name = "GetBookingHistoryResult")
    private String getBookingHistoryResult;

    public java.util.Optional<String> getGetBookingHistoryResult() {
        return java.util.Optional.ofNullable(getBookingHistoryResult);
    }

    public void setGetBookingHistoryResult(String value) {
        getBookingHistoryResult = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBookingHistoryResponse)) return false;
        var that = (GetBookingHistoryResponse) o;
        return java.util.Objects.equals(getBookingHistoryResult, that.getBookingHistoryResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getBookingHistoryResult);
    }

    @Override
    public String toString() {
        return "GetBookingHistoryResponse{"
                + "getBookingHistoryResult='" + getBookingHistoryResult + '\''
                + '}';
    }
}
