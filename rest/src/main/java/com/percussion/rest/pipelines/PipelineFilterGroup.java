/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Nested AND/OR filter group for a native pipeline selector. Root must be {@code type=GROUP}.
 */
@XmlRootElement(name = "PipelineFilterGroup")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Nested AND/OR filter group (GROUP nodes + PREDICATE leaves)")
public class PipelineFilterGroup {

  /** GROUP (default) or PREDICATE. */
  private String type;

  /** AND or OR when type is GROUP. */
  private String op;

  private List<PipelineFilterGroup> children;

  private String leftKind;
  private String left;
  private String operator;
  private String rightKind;
  private String right;
  private Boolean omitWhenNull;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public List<PipelineFilterGroup> getChildren() {
    return children;
  }

  public void setChildren(List<PipelineFilterGroup> children) {
    this.children = children != null ? children : new ArrayList<>();
  }

  public String getLeftKind() {
    return leftKind;
  }

  public void setLeftKind(String leftKind) {
    this.leftKind = leftKind;
  }

  public String getLeft() {
    return left;
  }

  public void setLeft(String left) {
    this.left = left;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getRightKind() {
    return rightKind;
  }

  public void setRightKind(String rightKind) {
    this.rightKind = rightKind;
  }

  public String getRight() {
    return right;
  }

  public void setRight(String right) {
    this.right = right;
  }

  public Boolean getOmitWhenNull() {
    return omitWhenNull;
  }

  public void setOmitWhenNull(Boolean omitWhenNull) {
    this.omitWhenNull = omitWhenNull;
  }
}
