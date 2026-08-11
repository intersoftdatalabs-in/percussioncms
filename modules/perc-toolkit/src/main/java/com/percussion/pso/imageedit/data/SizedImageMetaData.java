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
package com.percussion.pso.imageedit.data;

/**
 * A Sized Image.
 *
 * @author DavidBenua
 */
public class SizedImageMetaData extends SimpleImageMetaData {
  /**
   * Creates a new SizedImageMetaData.
   */
  public SizedImageMetaData() {
    // default
  }

  private ImageSizeDefinition sizeDefinition;

  private int x = 0;
  private int y = 0;
  private Boolean constraint = true;

  /**
   * Returns the x.
   *
   * @return the result
   */
  public int getX() {
    return x;
  }

  /**
   * Sets the x.
   *
   * @param x the x
   */
  public void setX(int x) {
    this.x = x;
  }

  /**
   * Returns the y.
   *
   * @return the result
   */
  public int getY() {
    return y;
  }

  /**
   * Sets the y.
   *
   * @param y the y
   */
  public void setY(int y) {
    this.y = y;
  }

  /**
   * Returns the sizeDefinition.
   * @return the sizeDefinition
   */
  public ImageSizeDefinition getSizeDefinition() {
    return sizeDefinition;
  }

  /**
   * Sets the sizeDefinition.
   * @param sizeDefinition the sizeDefinition to set
   */
  public void setSizeDefinition(ImageSizeDefinition sizeDefinition) {
    this.sizeDefinition = sizeDefinition;
  }

  /**
   * toString operation.
   *
   * @return the result
   */
  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("SizedImageMetaData{");
    sb.append("sizeDefinition=").append(sizeDefinition);
    sb.append(", x=").append(x);
    sb.append(", y=").append(y);
    sb.append(", constraint=").append(constraint);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Returns whether constraint.
   *
   * @return the result
   */
  public Boolean isConstraint() {
    return constraint;
  }

  /**
   * Sets the constraint.
   *
   * @param constraint the constraint
   */
  public void setConstraint(Boolean constraint) {
    this.constraint = constraint;
  }
}
