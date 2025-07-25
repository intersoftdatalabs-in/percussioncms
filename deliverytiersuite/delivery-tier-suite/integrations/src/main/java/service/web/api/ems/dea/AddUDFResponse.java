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
 * Java 11 modernized: AddUDFResponse for EMS SOAP API.
 * <p>
 * Represents the response for AddUDF SOAP operation.
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "addUDFResult"
})
@XmlRootElement(name = "AddUDFResponse")
public final class AddUDFResponse {

    @XmlElement(name = "AddUDFResult")
    private String addUDFResult;

    public java.util.Optional<String> getAddUDFResult() {
        return java.util.Optional.ofNullable(addUDFResult);
    }
    public void setAddUDFResult(String value) {
        this.addUDFResult = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddUDFResponse that = (AddUDFResponse) o;
        return java.util.Objects.equals(addUDFResult, that.addUDFResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(addUDFResult);
    }

    @Override
    public String toString() {
        return "AddUDFResponse{" +
                "addUDFResult='" + addUDFResult + '\'' +
                '}';
    }
}
