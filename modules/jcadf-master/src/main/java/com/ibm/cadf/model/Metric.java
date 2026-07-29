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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Metric} label that names a measurable quantity (e.g., {@code bytes_sent}). Each
 * metric carries a unique id, a measurement unit, a human-readable name, and an optional bag of
 * key/value annotations; {@link #isValid()} enforces the non-empty id and unit constraint.
 */
public class Metric extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The metric id, may be {@code null}. */
  private String metricId;

  /** The measurement unit, may be {@code null}. */
  private String unit;

  /** The human-readable metric name, may be {@code null}. */
  private String name;

  /** Optional key/value annotations for this metric, may be {@code null}. */
  private Map<String, String> annotations;

  /**
   * Constructs a metric with the supplied id, unit, and human-readable name.
   *
   * @param metricId the unique metric id, never {@code null} or empty.
   * @param unit the unit of measure (e.g., {@code "bytes"}), never {@code null} or empty.
   * @param name the human-readable name, never {@code null} or empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Metric(String metricId, String unit, String name) throws CADFException {
    super();
    this.metricId = metricId;
    this.unit = unit;
    this.name = name;
  }

  /**
   * Returns the metric id.
   *
   * @return the id, may be {@code null} when not yet set.
   */
  public String getMetricId() {
    return metricId;
  }

  /**
   * Sets the metric id.
   *
   * @param metricId the id, may be {@code null}.
   */
  public void setMetricId(String metricId) {
    this.metricId = metricId;
  }

  /**
   * Returns the unit of measure.
   *
   * @return the unit, may be {@code null}.
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Sets the unit of measure.
   *
   * @param unit the unit, may be {@code null}.
   */
  public void setUnit(String unit) {
    this.unit = unit;
  }

  /**
   * Returns the human-readable metric name.
   *
   * @return the name, may be {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the human-readable metric name.
   *
   * @param name the name, may be {@code null}.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Appends a {@code key=value} annotation to this metric, creating the annotation map on first
   * use.
   *
   * @param key the annotation key, never {@code null}.
   * @param value the annotation value, may be {@code null}.
   */
  public void addAnnotation(String key, String value) {
    if (this.annotations == null) {
      this.annotations = new HashMap<>();
    }
    this.annotations.put(key, value);
  }

  /**
   * Validates that {@code metricId} and {@code unit} are both non-empty.
   *
   * @return {@code true} when both mandatory fields are populated.
   */
  @Override
  public boolean isValid() {
    return StringUtils.isNotEmpty(metricId) && StringUtils.isNotEmpty(unit);
  }
}
