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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/**
 * One join edge in a backend data tank (classic {@code PSBackEndJoin}).
 *
 * <p>{@code left} / {@code right} are backend column references in the same form as mapper backends
 * and where-clause columns: {@code alias.column} or bare {@code column}.
 *
 * <p>{@link #joinType} is one of {@link #TYPE_INNER}, {@link #TYPE_LEFT}, {@link #TYPE_RIGHT},
 * {@link #TYPE_FULL}. Classic extension translators on joins are inventoried via {@link
 * #translatorPresent} and rejected by the generated SQL planner (use native SELECT).
 */
public class BackendJoinIr {

  public static final String TYPE_INNER = "INNER";
  public static final String TYPE_LEFT = "LEFT";
  public static final String TYPE_RIGHT = "RIGHT";
  public static final String TYPE_FULL = "FULL";

  private String joinType = TYPE_INNER;
  private String left;
  private String right;
  private boolean translatorPresent;

  public String getJoinType() {
    return joinType;
  }

  public void setJoinType(String joinType) {
    this.joinType = joinType != null && !joinType.isBlank() ? joinType : TYPE_INNER;
  }

  public String getLeft() {
    return left;
  }

  public void setLeft(String left) {
    this.left = left;
  }

  public String getRight() {
    return right;
  }

  public void setRight(String right) {
    this.right = right;
  }

  public boolean isTranslatorPresent() {
    return translatorPresent;
  }

  public void setTranslatorPresent(boolean translatorPresent) {
    this.translatorPresent = translatorPresent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BackendJoinIr that)) {
      return false;
    }
    return translatorPresent == that.translatorPresent
        && Objects.equals(joinType, that.joinType)
        && Objects.equals(left, that.left)
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(joinType, left, right, translatorPresent);
  }
}
