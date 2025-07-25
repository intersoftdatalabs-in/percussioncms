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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java 11 modernized: ArrayOfString for EMS SOAP API.
 * <p>
 * Immutable, thread-safe, and OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfString", propOrder = {
    "string"
})
public final class ArrayOfString {

    @XmlElement(nillable = true)
    private List<String> string;

    /**
     * Gets the value of the string property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the string property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getString().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     * @return live List of strings (never null)
     */
    public List<String> getString() {
        if (string == null) {
            string = new ArrayList<>();
        }
        return this.string;
    }

    /**
     * @return Optional containing the internal list if initialized
     */
    public java.util.Optional<List<String>> getStringOptional() {
        return java.util.Optional.ofNullable(string);
    }

    @Override
    public String toString() {
        return "ArrayOfString{" +
                "string=" + string +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayOfString that = (ArrayOfString) o;
        return java.util.Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(string);
    }
}
