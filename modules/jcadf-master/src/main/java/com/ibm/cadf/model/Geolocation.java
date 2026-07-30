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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;

/**
 * CADF {@code Geolocation} reference carrying lat/long coordinates, elevation, accuracy, and an
 * ICANN region label. {@link #isValid()} currently accepts any state; a future schema-driven
 * validation is the noted TODO at the original source.
 */
public class Geolocation extends CADFType {

  private static final long serialVersionUID = 1L;

  /** Geolocation id, may be {@code null}. */
  private String id;

  /** Latitude, may be {@code null}. */
  private String latitude;

  /** Longitude, may be {@code null}. */
  private String longitude;

  /** Elevation, may be {@code null}. */
  private String elevation;

  /** Accuracy of the geolocation, may be {@code null}. */
  private String accuracy;

  /** City, may be {@code null}. */
  private String city;

  /** State (or province), may be {@code null}. */
  private String state;

  /** ICANN region identifier, may be {@code null}. */
  private String regionICANN;

  /**
   * Constructs a geolocation with the supplied components. All arguments may be {@code null} unless
   * downstream code demands otherwise.
   *
   * @param id the geolocation id, never {@code null} or empty.
   * @param latitude the latitude, may be {@code null}.
   * @param longitude the longitude, may be {@code null}.
   * @param elevation the elevation, may be {@code null}.
   * @param accuracy the accuracy, may be {@code null}.
   * @param city the city, may be {@code null}.
   * @param state the state, may be {@code null}.
   * @param regionICANN the ICANN region, may be {@code null}.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Geolocation(
      String id,
      String latitude,
      String longitude,
      String elevation,
      String accuracy,
      String city,
      String state,
      String regionICANN)
      throws CADFException {
    super();
    this.id = id;
    this.latitude = latitude;
    this.longitude = longitude;
    this.elevation = elevation;
    this.accuracy = accuracy;
    this.city = city;
    this.state = state;
    this.regionICANN = regionICANN;
  }

  /**
   * Returns the geolocation id.
   *
   * @return the id, may be {@code null}.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the geolocation id.
   *
   * @param id the id, may be {@code null}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the latitude.
   *
   * @return the latitude, may be {@code null}.
   */
  public String getLatitude() {
    return latitude;
  }

  /**
   * Sets the latitude.
   *
   * @param latitude the latitude, may be {@code null}.
   */
  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  /**
   * Returns the longitude.
   *
   * @return the longitude, may be {@code null}.
   */
  public String getLongitude() {
    return longitude;
  }

  /**
   * Sets the longitude.
   *
   * @param longitude the longitude, may be {@code null}.
   */
  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  /**
   * Returns the elevation.
   *
   * @return the elevation, may be {@code null}.
   */
  public String getElevation() {
    return elevation;
  }

  /**
   * Sets the elevation.
   *
   * @param elevation the elevation, may be {@code null}.
   */
  public void setElevation(String elevation) {
    this.elevation = elevation;
  }

  /**
   * Returns the accuracy of the geolocation.
   *
   * @return the accuracy, may be {@code null}.
   */
  public String getAccuracy() {
    return accuracy;
  }

  /**
   * Sets the accuracy of the geolocation.
   *
   * @param accuracy the accuracy, may be {@code null}.
   */
  public void setAccuracy(String accuracy) {
    this.accuracy = accuracy;
  }

  /**
   * Returns the city associated with the geolocation.
   *
   * @return the city, may be {@code null}.
   */
  public String getCity() {
    return city;
  }

  /**
   * Sets the city associated with the geolocation.
   *
   * @param city the city, may be {@code null}.
   */
  public void setCity(String city) {
    this.city = city;
  }

  /**
   * Returns the state (or province) associated with the geolocation.
   *
   * @return the state, may be {@code null}.
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state (or province) associated with the geolocation.
   *
   * @param state the state, may be {@code null}.
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Returns the ICANN region associated with the geolocation.
   *
   * @return the ICANN region, may be {@code null}.
   */
  public String getRegionICANN() {
    return regionICANN;
  }

  /**
   * Sets the ICANN region associated with the geolocation.
   *
   * @param regionICANN the ICANN region, may be {@code null}.
   */
  public void setRegionICANN(String regionICANN) {
    this.regionICANN = regionICANN;
  }

  /**
   * Validates the geolocation state. Always returns {@code true}; see the noted TODO for
   * schema-driven validation.
   *
   * @return always {@code true}.
   */
  @Override
  public boolean isValid() {
    return true;
  }
}
