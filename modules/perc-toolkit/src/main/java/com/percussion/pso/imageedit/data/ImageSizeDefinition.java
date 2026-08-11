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
 * Represents a single named image-size variant available in the image editor, including the
 * rendered dimensions and the Rhythmyx snippet / binary templates used to produce it.
 *
 * @author DavidBenua
 */
public class ImageSizeDefinition {
  /**
   * Creates a new ImageSizeDefinition.
   */
  public ImageSizeDefinition() {
    // default
  }

  private String code;
  private String label;
  private int height;
  private int width;
  private String snippetTemplate;
  private String binaryTemplate;

  /**
   * Returns the code.
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   * @param code the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the label.
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label.
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Returns the height.
   * @return the height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Sets the height.
   * @param height the height to set
   */
  public void setHeight(int height) {
    this.height = height;
  }

  /**
   * Returns the width.
   * @return the width
   */
  public int getWidth() {
    return width;
  }

  /**
   * Sets the width.
   * @param width the width to set
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Returns the snippetTemplate.
   * @return the snippetTemplate
   */
  public String getSnippetTemplate() {
    return snippetTemplate;
  }

  /**
   * Sets the snippetTemplate.
   * @param snippetTemplate the snippetTemplate to set
   */
  public void setSnippetTemplate(String snippetTemplate) {
    this.snippetTemplate = snippetTemplate;
  }

  /**
   * Returns the binaryTemplate.
   * @return the binaryTemplate
   */
  public String getBinaryTemplate() {
    return binaryTemplate;
  }

  /**
   * Sets the binaryTemplate.
   * @param binaryTemplate the binaryTemplate to set
   */
  public void setBinaryTemplate(String binaryTemplate) {
    this.binaryTemplate = binaryTemplate;
  }

  /**
   * toString operation.
   *
   * @return the result
   */
  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("ImageSizeDefinition{");
    sb.append("code='").append(code).append('\'');
    sb.append(", label='").append(label).append('\'');
    sb.append(", height=").append(height);
    sb.append(", width=").append(width);
    sb.append(", snippetTemplate='").append(snippetTemplate).append('\'');
    sb.append(", binaryTemplate='").append(binaryTemplate).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
