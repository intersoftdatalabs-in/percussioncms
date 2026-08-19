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
package com.percussion.services.virtualsite;

import java.util.Objects;

/**
 * One Virtual Site redirect from optional {@code _redirects.yaml}.
 *
 * <p>{@code from} is a site-root path (leading {@code /}). {@code to} is a relative path or a
 * same-site absolute URL. {@code status} is a static-host hint (301/302/307/308).
 */
public final class VirtualRedirect {

  private final String from;
  private final String to;
  private final int status;

  public VirtualRedirect(String from, String to, int status) {
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
    this.status = status;
  }

  public String from() {
    return from;
  }

  public String to() {
    return to;
  }

  public int status() {
    return status;
  }
}
