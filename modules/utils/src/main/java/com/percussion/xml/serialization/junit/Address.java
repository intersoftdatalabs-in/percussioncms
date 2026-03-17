/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.xml.serialization.junit;

import java.util.Objects;

/**
 * A sample address class used in unit test of the {@link
 * com.percussion.xml.serialization.PSObjectSerializer} class. As can be seen it is a simple java
 * bean with a default ctor (required) and setXxx() and getXxx() methods.
 */
public class Address {
  private String street;

  private String addressLine2;

  private String town;

  private String state;

  private String zip;

  /** Default ctor. Required by serializer. */
  public Address() {}

  /**
   * Ctor taking all the required information to build the object.
   *
   * @param street the street address
   * @param town the town/city
   * @param state the state/province
   * @param zip the postal code
   */
  public Address(String street, String town, String state, String zip) {
    this.street = street;
    this.town = town;
    this.state = state;
    this.zip = zip;
  }

  /**
   * Sets the street address.
   * @param addressLine1 the street address line 1
   */
  public void setStreet(String addressLine1) {
    this.street = addressLine1;
  }

  /**
   * Sets the second address line.
   * @param addressLine2 the second address line
   */
  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  /**
   * Gets the state/province.
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state/province.
   * @param state the state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Gets the town/city.
   * @return the town
   */
  public String getTown() {
    return town;
  }

  /**
   * Sets the town/city.
   * @param town the town
   */
  public void setTown(String town) {
    this.town = town;
  }

  /**
   * Gets the postal code.
   * @return the zip code
   */
  public String getZip() {
    return zip;
  }

  /**
   * Sets the postal code.
   * @param zip the zip code
   */
  public void setZip(String zip) {
    this.zip = zip;
  }

  /**
   * Gets the street address.
   * @return the street address
   */
  public String getStreet() {
    return street;
  }

  /**
   * Gets the second address line.
   * @return the second address line
   */
  public String getAddressLine2() {
    return addressLine2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Address)) return false;
    Address address = (Address) o;
    return Objects.equals(getStreet(), address.getStreet())
        && Objects.equals(getAddressLine2(), address.getAddressLine2())
        && Objects.equals(getTown(), address.getTown())
        && Objects.equals(getState(), address.getState())
        && Objects.equals(getZip(), address.getZip());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStreet(), getAddressLine2(), getTown(), getState(), getZip());
  }
}
