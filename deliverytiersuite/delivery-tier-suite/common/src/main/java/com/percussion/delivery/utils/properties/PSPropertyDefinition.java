  public int getMaxLength() {
    return maxLength;
  }
/*
  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }
 * distributed under the License is distributed on an "AS IS" BASIS,
  public String getValidationRegEx() {
    return validationRegEx;
  }
import java.util.List;
  public void setValidationRegEx(String validationRegEx) {
    this.validationRegEx = validationRegEx;
  }
})
  public String getHelpText() {
    return helpText;
  }
  @XmlAttribute
  public void setHelpText(String helpText) {
    this.helpText = helpText;
  }
  private String validationMessage;
  public String getDisplayRegEx() {
    return displayRegEx;
  }
   * <p>This accessor method returns a reference to the live list,
  public void setDisplayRegEx(String displayRegEx) {
    this.displayRegEx = displayRegEx;
  }
   * @return never <code>null</code>.
  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }
  }
  public Object getPropertyValue() {
    return propertyValue;
  }
    return defaultValue;
  public void setPropertyValue(Object propertyValue) {
    this.propertyValue = propertyValue;
  }
  }
  /**
   * <p>Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType>
   *   &lt;complexContent>
   *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
   *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
   *       &lt;attribute name="display_value" type="{http://www.w3.org/2001/XMLSchema}string" />
   *     &lt;/restriction>
   *   &lt;/complexContent>
   * &lt;/complexType>
   * </pre>
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "")
  public static class EnumValue {
			 */
    @XmlAttribute(required = true)
    protected String value;
    @XmlAttribute(name = "display_value")
    protected String displayValue;

    public String getValue() {
      return value;
    }
			 * Sets the Validation regular expression that can be used
    public void setValue(String value) {
      this.value = value;
    }
			 * May be null.
    public String getDisplayValue() {
      return displayValue;
    }
			 */
    public void setDisplayValue(String value) {
      this.displayValue = value;
    }
  }
}
			 * format the display of this property.
			 * 
			 * @param displayRegEx the displayRegEx to set
			 */
			public void setDisplayRegEx(String displayRegEx) {
				this.displayRegEx = displayRegEx;
			}


			/**
			 * @return the validationMessage
			 */
			public String getValidationMessage() {
				return validationMessage;
			}

			/**
			 * @param validationMessage the validationMessage to set
			 */
			public void setValidationMessage(String validationMessage) {
				this.validationMessage = validationMessage;
			}


			/**
			 * When populated contains the current value
			 * if any for this property.
			 * 
			 * @return the propertyValue
			 */
			public Object getPropertyValue() {
				return propertyValue;
			}

			/**
			 * Sets the Value for this property. 
			 * 
			 * @param propertyValue the propertyValue to set
			 */
			public void setPropertyValue(Object propertyValue) {
				this.propertyValue = propertyValue;
			}


			/**
	         * <p>Java class for anonymous complex type.
	         * 
	         * <p>The following schema fragment specifies the expected content contained within this class.
	         * 
	         * <pre>
	         * &lt;complexType>
	         *   &lt;complexContent>
	         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	         *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
	         *       &lt;attribute name="display_value" type="{http://www.w3.org/2001/XMLSchema}string" />
	         *     &lt;/restriction>
	         *   &lt;/complexContent>
	         * &lt;/complexType>
	         * </pre>
	         * 
	         * 
	         */
	        @XmlAccessorType(XmlAccessType.FIELD)
	        @XmlType(name = "")
	        public static class EnumValue {

	            @XmlAttribute(required = true)
	            protected String value;
	            @XmlAttribute(name = "display_value")
	            protected String displayValue;

	            /**
	             * Gets the value of the value property.
	             * 
	             * @return
	             *     possible object is
	             *     {@link String }
	             *     
	             */
	            public String getValue() {
	                return value;
	            }

	            /**
	             * Sets the value of the value property.
	             * 
	             * @param value
	             *     allowed object is
	             *     {@link String }
	             *     
	             */
	            public void setValue(String value) {
	                this.value = value;
	            }

	            /**
	             * Gets the value of the displayValue property.
	             * 
	             * @return
	             *     possible object is
	             *     {@link String }
	             *     
	             */
	            public String getDisplayValue() {
	                return displayValue;
	            }

	            /**
	             * Sets the value of the displayValue property.
	             * 
	             * @param value
	             *     allowed object is
	             *     {@link String }
	             *     
	             */
	            public void setDisplayValue(String value) {
	                this.displayValue = value;
	            }

	        }
	 }


