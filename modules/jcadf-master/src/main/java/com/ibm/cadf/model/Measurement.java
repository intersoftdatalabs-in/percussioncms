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
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Measurement} that pairs a stringified result with either a full {@link Metric} or a
 * metric id. {@link #isValid()} requires exactly one of {@code metric} / {@code metricId} to be set
 * (xor) and the result to be non-empty.
 */
public class Measurement extends CADFType {
  private static final long serialVersionUID = 1L;

  /** The stringified measurement value, may be {@code null}. */
  private String result;

  /** The metric describing this measurement, may be {@code null}. */
  private Metric metric;

  /** The metric id, may be {@code null}. */
  private String metricId;

  /** The resource that produced the measurement, may be {@code null}. */
  private Resource calculatedBy;

  /**
   * Constructs a measurement from the supplied result, metric (optional), and metric id.
   *
   * @param result the stringified measurement value, never {@code null} or empty.
   * @param metric the optional metric instance describing this measurement; may be {@code null}
   *     when {@code metricId} is provided instead.
   * @param metricId the optional metric id; may be {@code null} when {@code metric} is provided.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Measurement(String result, Metric metric, String metricId) throws CADFException {
    super();
    this.result = result;
    this.metric = metric;
    this.metricId = metricId;
  }

  /**
   * Returns the stringified measurement result.
   *
   * @return the result, may be {@code null}.
   */
  public String getResult() {
    return result;
  }

  /**
   * Sets the stringified measurement result.
   *
   * @param result the result, may be {@code null}.
   */
  public void setResult(String result) {
    this.result = result;
  }

  /**
   * Returns the metric describing this measurement.
   *
   * @return the metric, may be {@code null} when {@link #getMetricId()} is set instead.
   */
  public Metric getMetric() {
    return metric;
  }

  /**
   * Sets the metric describing this measurement.
   *
   * @param metric the metric, may be {@code null}.
   */
  public void setMetric(Metric metric) {
    this.metric = metric;
  }

  /**
   * Returns the metric id when no full {@link Metric} instance is attached.
   *
   * @return the metric id, may be {@code null}.
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
   * Returns the resource that produced the measurement.
   *
   * @return the calculated-by resource, may be {@code null}.
   */
  public Resource getCalculatedBy() {
    return calculatedBy;
  }

  /**
   * Sets the resource that produced the measurement.
   *
   * @param calculatedBy the resource, may be {@code null}.
   */
  public void setCalculatedBy(Resource calculatedBy) {
    this.calculatedBy = calculatedBy;
  }

  /**
   * Validates that {@code result} is non-empty and exactly one of {@code metric} / {@code metricId}
   * is set.
   *
   * @return {@code true} when both constraints are satisfied.
   */
  @Override
  public boolean isValid() {
    return StringUtils.isNotEmpty(result) && (metric != null ^ StringUtils.isNotEmpty(metricId));
  }
}
