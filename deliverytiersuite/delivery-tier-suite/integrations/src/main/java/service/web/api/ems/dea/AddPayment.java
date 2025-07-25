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
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="TransactionDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="PaymentTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="CheckNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="PaymentAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/&gt;
 *         &lt;element name="InvoiceNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Notes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "userName",
    "password",
    "transactionDate",
    "paymentTypeID",
    "checkNo",
    "paymentAmount",
    "invoiceNo",
    "notes"
})
@XmlRootElement(name = "AddPayment")
public final class AddPayment {
    @XmlElement(name = "UserName")
    private final String userName;
    @XmlElement(name = "Password")
    private final String password;
    @XmlElement(name = "TransactionDate", required = true)
    @XmlSchemaType(name = "dateTime")
    private final XMLGregorianCalendar transactionDate;
    @XmlElement(name = "PaymentTypeID")
    private final int paymentTypeID;
    @XmlElement(name = "CheckNo")
    private final String checkNo;
    @XmlElement(name = "PaymentAmount", required = true)
    private final BigDecimal paymentAmount;
    @XmlElement(name = "InvoiceNo")
    private final String invoiceNo;
    @XmlElement(name = "Notes")
    private final String notes;

    private AddPayment(Builder builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.transactionDate = builder.transactionDate;
        this.paymentTypeID = builder.paymentTypeID;
        this.checkNo = builder.checkNo;
        this.paymentAmount = builder.paymentAmount;
        this.invoiceNo = builder.invoiceNo;
        this.notes = builder.notes;
    }

    public java.util.Optional<String> getUserName() { return java.util.Optional.ofNullable(userName); }
    public java.util.Optional<String> getPassword() { return java.util.Optional.ofNullable(password); }
    public XMLGregorianCalendar getTransactionDate() { return transactionDate; }
    public int getPaymentTypeID() { return paymentTypeID; }
    public java.util.Optional<String> getCheckNo() { return java.util.Optional.ofNullable(checkNo); }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public java.util.Optional<String> getInvoiceNo() { return java.util.Optional.ofNullable(invoiceNo); }
    public java.util.Optional<String> getNotes() { return java.util.Optional.ofNullable(notes); }

    public static class Builder {
        private String userName;
        private String password;
        private XMLGregorianCalendar transactionDate;
        private int paymentTypeID;
        private String checkNo;
        private BigDecimal paymentAmount;
        private String invoiceNo;
        private String notes;
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder transactionDate(XMLGregorianCalendar transactionDate) { this.transactionDate = transactionDate; return this; }
        public Builder paymentTypeID(int paymentTypeID) { this.paymentTypeID = paymentTypeID; return this; }
        public Builder checkNo(String checkNo) { this.checkNo = checkNo; return this; }
        public Builder paymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; return this; }
        public Builder invoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public AddPayment build() { return new AddPayment(this); }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddPayment that = (AddPayment) o;
        return paymentTypeID == that.paymentTypeID &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(transactionDate, that.transactionDate) &&
                java.util.Objects.equals(checkNo, that.checkNo) &&
                java.util.Objects.equals(paymentAmount, that.paymentAmount) &&
                java.util.Objects.equals(invoiceNo, that.invoiceNo) &&
                java.util.Objects.equals(notes, that.notes);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, transactionDate, paymentTypeID, checkNo, paymentAmount, invoiceNo, notes);
    }
    @Override
    public String toString() {
        return "AddPayment{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", transactionDate=" + transactionDate +
                ", paymentTypeID=" + paymentTypeID +
                ", checkNo='" + checkNo + '\'' +
                ", paymentAmount=" + paymentAmount +
                ", invoiceNo='" + invoiceNo + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
