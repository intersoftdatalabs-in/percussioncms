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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


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
 *         &lt;element name="A_0" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="A_1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="A_2" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="A_3" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="A_4" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="A_5" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="A_6" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="A_7" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="A_8" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
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
    "a0",
    "a1",
    "a2",
    "a3",
    "a4",
    "a5",
    "a6",
    "a7",
    "a8"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBookingHistory")
public class GetBookingHistory {
    @XmlElement(name = "A_0")
    private String a0;
    @XmlElement(name = "A_1")
    private String a1;
    @XmlElement(name = "A_2", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar a2;
    @XmlElement(name = "A_3", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar a3;
    @XmlElement(name = "A_4")
    private ArrayOfInt a4;
    @XmlElement(name = "A_5")
    private ArrayOfInt a5;
    @XmlElement(name = "A_6")
    private ArrayOfInt a6;
    @XmlElement(name = "A_7")
    private ArrayOfInt a7;
    @XmlElement(name = "A_8")
    private boolean a8;

    public java.util.Optional<String> getA0() {
        return java.util.Optional.ofNullable(a0);
    }

    public void setA0(String value) {
        a0 = value;
    }

    public java.util.Optional<String> getA1() {
        return java.util.Optional.ofNullable(a1);
    }

    public void setA1(String value) {
        a1 = value;
    }

    public XMLGregorianCalendar getA2() {
        return a2;
    }

    public void setA2(XMLGregorianCalendar value) {
        a2 = value;
    }

    public XMLGregorianCalendar getA3() {
        return a3;
    }

    public void setA3(XMLGregorianCalendar value) {
        a3 = value;
    }

    public ArrayOfInt getA4() {
        return a4;
    }

    public void setA4(ArrayOfInt value) {
        a4 = value;
    }

    public ArrayOfInt getA5() {
        return a5;
    }

    public void setA5(ArrayOfInt value) {
        a5 = value;
    }

    public ArrayOfInt getA6() {
        return a6;
    }

    public void setA6(ArrayOfInt value) {
        a6 = value;
    }

    public ArrayOfInt getA7() {
        return a7;
    }

    public void setA7(ArrayOfInt value) {
        a7 = value;
    }

    public boolean isA8() {
        return a8;
    }

    public void setA8(boolean value) {
        a8 = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetBookingHistory)) return false;
        var that = (GetBookingHistory) o;
        return a8 == that.a8
                && java.util.Objects.equals(a0, that.a0)
                && java.util.Objects.equals(a1, that.a1)
                && java.util.Objects.equals(a2, that.a2)
                && java.util.Objects.equals(a3, that.a3)
                && java.util.Objects.equals(a4, that.a4)
                && java.util.Objects.equals(a5, that.a5)
                && java.util.Objects.equals(a6, that.a6)
                && java.util.Objects.equals(a7, that.a7);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(a0, a1, a2, a3, a4, a5, a6, a7, a8);
    }

    @Override
    public String toString() {
        return "GetBookingHistory{"
                + "a0='" + a0 + '\''
                + ", a1='" + a1 + '\''
                + ", a2=" + a2
                + ", a3=" + a3
                + ", a4=" + a4
                + ", a5=" + a5
                + ", a6=" + a6
                + ", a7=" + a7
                + ", a8=" + a8
                + '}';
    }
}
