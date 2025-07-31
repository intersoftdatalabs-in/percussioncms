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

import javax.xml.bind.annotation.*;

/**
 * Java 11+ refactored version of DeleteServiceOrderDetailResponse SOAP response.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant.
 * <p>
 * Represents the response for deleting a service order detail.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "deleteServiceOrderDetailResult"
})
@XmlRootElement(name = "DeleteServiceOrderDetailResponse")
public final class DeleteServiceOrderDetailResponse {

    @XmlElement(name = "DeleteServiceOrderDetailResult")
    private String deleteServiceOrderDetailResult;

    /**
     * Gets the value of the deleteServiceOrderDetailResult property.
     *
     * @return an {@link Optional} containing the result string if present
     */
    public java.util.Optional<String> getDeleteServiceOrderDetailResult() {
        return java.util.Optional.ofNullable(deleteServiceOrderDetailResult);
    }

    /**
     * Sets the value of the deleteServiceOrderDetailResult property.
     *
     * @param value allowed object is {@link String }
     */
    public void setDeleteServiceOrderDetailResult(String value) {
        this.deleteServiceOrderDetailResult = value;
    }

    @Override
    public String toString() {
        return "DeleteServiceOrderDetailResponse{"
                + "deleteServiceOrderDetailResult='" + deleteServiceOrderDetailResult + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteServiceOrderDetailResponse)) return false;
        var that = (DeleteServiceOrderDetailResponse) o;
        return java.util.Objects.equals(deleteServiceOrderDetailResult, that.deleteServiceOrderDetailResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(deleteServiceOrderDetailResult);
    }
}
