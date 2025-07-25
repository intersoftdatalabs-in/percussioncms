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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Java 11+ refactored version of GetAPIVersion SOAP request.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant. Use builder for instantiation.
 * <p>
 * // REFACTORED: CP-JAVA11
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "GetAPIVersion")
public final class GetAPIVersion {
    private GetAPIVersion() {}
    public static GetAPIVersion create() {
        return new GetAPIVersion();
    }
    @Override
    public String toString() {
        return "GetAPIVersion{}";
    }
    @Override
    public boolean equals(Object o) {
        return this == o || (o != null && getClass() == o.getClass());
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
