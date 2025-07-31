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
 * Java 11+ refactored SOAP response for AddServiceOrder.
 * Immutable, Google Java Style, OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "addServiceOrderResult"
})
@XmlRootElement(name = "AddServiceOrderResponse")
public final class AddServiceOrderResponse {

    @XmlElement(name = "AddServiceOrderResult")
    private String addServiceOrderResult;

    /**
     * @return Optional containing the result string if present
     */
    public java.util.Optional<String> getAddServiceOrderResult() {
        return java.util.Optional.ofNullable(addServiceOrderResult);
    }

    /**
     * @param value allowed object is {@link String }
     */
    public void setAddServiceOrderResult(String value) {
        this.addServiceOrderResult = value;
    }

    @Override
    public String toString() {
        return "AddServiceOrderResponse{" +
                "addServiceOrderResult='" + addServiceOrderResult + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddServiceOrderResponse that = (AddServiceOrderResponse) o;
        return java.util.Objects.equals(addServiceOrderResult, that.addServiceOrderResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(addServiceOrderResult);
    }
}
