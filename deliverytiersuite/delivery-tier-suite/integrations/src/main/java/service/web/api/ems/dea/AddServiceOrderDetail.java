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

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Java 11+ refactored SOAP request for AddServiceOrderDetail.
 * Immutable, Google Java Style, OWASP-compliant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "serviceOrderID",
    "resourceID",
    "quantity",
    "pricingMethodID",
    "unitPrice",
    "notes",
    "specialInstructions"
})
@XmlRootElement(name = "AddServiceOrderDetail")
public final class AddServiceOrderDetail {
    @XmlElement(name = "UserName")
    private String userName;
    @XmlElement(name = "Password")
    private String password;
    @XmlElement(name = "ServiceOrderID")
    private int serviceOrderID;
    @XmlElement(name = "ResourceID")
    private int resourceID;
    @XmlElement(name = "Quantity", required = true)
    private java.math.BigDecimal quantity;
    @XmlElement(name = "PricingMethodID")
    private int pricingMethodID;
    @XmlElement(name = "UnitPrice", required = true)
    private java.math.BigDecimal unitPrice;
    @XmlElement(name = "Notes")
    private String notes;
    @XmlElement(name = "SpecialInstructions")
    private String specialInstructions;

    /**
     * @return Optional user name
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    public void setUserName(String value) {
        this.userName = value;
    }
    /**
     * @return Optional password
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    public void setPassword(String value) {
        this.password = value;
    }
    public int getServiceOrderID() {
        return serviceOrderID;
    }
    public void setServiceOrderID(int value) {
        this.serviceOrderID = value;
    }
    public int getResourceID() {
        return resourceID;
    }
    public void setResourceID(int value) {
        this.resourceID = value;
    }
    /**
     * @return Optional quantity
     */
    public java.util.Optional<java.math.BigDecimal> getQuantity() {
        return java.util.Optional.ofNullable(quantity);
    }
    public void setQuantity(java.math.BigDecimal value) {
        this.quantity = value;
    }
    public int getPricingMethodID() {
        return pricingMethodID;
    }
    public void setPricingMethodID(int value) {
        this.pricingMethodID = value;
    }
    /**
     * @return Optional unit price
     */
    public java.util.Optional<java.math.BigDecimal> getUnitPrice() {
        return java.util.Optional.ofNullable(unitPrice);
    }
    public void setUnitPrice(java.math.BigDecimal value) {
        this.unitPrice = value;
    }
    /**
     * @return Optional notes
     */
    public java.util.Optional<String> getNotes() {
        return java.util.Optional.ofNullable(notes);
    }
    public void setNotes(String value) {
        this.notes = value;
    }
    /**
     * @return Optional special instructions
     */
    public java.util.Optional<String> getSpecialInstructions() {
        return java.util.Optional.ofNullable(specialInstructions);
    }
    public void setSpecialInstructions(String value) {
        this.specialInstructions = value;
    }

    @Override
    public String toString() {
        return "AddServiceOrderDetail{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", serviceOrderID=" + serviceOrderID +
                ", resourceID=" + resourceID +
                ", quantity=" + quantity +
                ", pricingMethodID=" + pricingMethodID +
                ", unitPrice=" + unitPrice +
                ", notes='" + notes + '\'' +
                ", specialInstructions='" + specialInstructions + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddServiceOrderDetail that = (AddServiceOrderDetail) o;
        return serviceOrderID == that.serviceOrderID &&
                resourceID == that.resourceID &&
                pricingMethodID == that.pricingMethodID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(quantity, that.quantity) &&
                java.util.Objects.equals(unitPrice, that.unitPrice) &&
                java.util.Objects.equals(notes, that.notes) &&
                java.util.Objects.equals(specialInstructions, that.specialInstructions);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, serviceOrderID, resourceID, quantity, pricingMethodID, unitPrice, notes, specialInstructions);
    }
}
