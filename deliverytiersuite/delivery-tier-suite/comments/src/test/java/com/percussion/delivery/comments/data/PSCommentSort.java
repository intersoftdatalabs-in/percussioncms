/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.comments.data;

/** Sort configuration for comment queries in the data package. */
public class PSCommentSort {
  public enum SORTBY {
    CREATEDDATE,
    EMAIL,
    USERNAME
  }

  private final SORTBY sortBy;
  private final boolean ascending;

  public PSCommentSort(SORTBY sortBy, boolean ascending) {
    this.sortBy = sortBy;
    this.ascending = ascending;
  }

  public SORTBY getSortBy() {
    return sortBy;
  }

  public boolean isAscending() {
    return ascending;
  }
}
