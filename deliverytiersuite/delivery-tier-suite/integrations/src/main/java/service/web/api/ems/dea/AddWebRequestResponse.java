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

// REFACTORED: CP-JAVA11
package service.web.api.ems.dea;

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11 modernized: AddWebRequestResponse for EMS SOAP API.
 * <p>
 * Represents the response for AddWebRequest SOAP operation.
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "addWebRequestResult"
})
@XmlRootElement(name = "AddWebRequestResponse")
public final class AddWebRequestResponse {
    @XmlElement(name = "AddWebRequestResult")
    private String addWebRequestResult;

    public java.util.Optional<String> getAddWebRequestResult() {
        return java.util.Optional.ofNullable(addWebRequestResult);
    }
    public void setAddWebRequestResult(String value) {
        this.addWebRequestResult = value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddWebRequestResponse that = (AddWebRequestResponse) o;
        return Objects.equals(addWebRequestResult, that.addWebRequestResult);
    }
    @Override
    public int hashCode() {
        return Objects.hash(addWebRequestResult);
    }
    @Override
    public String toString() {
        return "AddWebRequestResponse{" +
                "addWebRequestResult='" + addWebRequestResult + '\'' +
                '}';
    }
}
