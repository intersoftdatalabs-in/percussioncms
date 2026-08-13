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
package com.percussion.services.assembly.impl.plugin;

import java.util.Map;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Binding resolution for {@link PSBinaryAssembler} without Spring / {@link PSAssemblerBase} static
 * init. Reads {@code $sys.mimetype} and {@code $sys.binary} via {@code Map<?,?>} (no unchecked
 * casts).
 */
public final class PSBinaryAssemblerSupport {

  static final String ERR_SYS_NOT_MAP = "$sys was not bound to a map.";
  static final String ERR_MIMETYPE_UNBOUND = "$sys.mimetype was not bound";
  static final String ERR_BINARY_UNBOUND = "$sys.binary was not bound";
  static final String ERR_MIMETYPE_TYPE =
      "$sys.mimetype must be bound to a property" + " or string value";
  static final String ERR_BINARY_TYPE = "$sys.binary must be bound to a property" + " or byte[] value";

  private PSBinaryAssemblerSupport() {}

  /**
   * Resolved {@code $sys} binary bindings. {@link #error()} is non-null on failure.
   *
   * @param error failure message, or {@code null} on success
   * @param mimetype resolved MIME type, {@code null} on failure
   * @param data {@link Property}, {@link Value}, or {@code byte[]}, {@code null} on failure
   */
  public record ResolvedSys(String error, String mimetype, Object data) {
    public boolean success() {
      return error == null;
    }
  }

  /**
   * Resolve {@code $sys.mimetype} / {@code $sys.binary} from assembly bindings.
   *
   * @param bindings item bindings, may be {@code null}
   * @return outcome, never {@code null}
   */
  public static ResolvedSys resolveSys(Map<String, ?> bindings) {
    if (bindings == null) {
      return new ResolvedSys(ERR_SYS_NOT_MAP, null, null);
    }
    Object sys = bindings.get("$sys");
    if (!(sys instanceof Map<?, ?> sysmap)) {
      return new ResolvedSys(ERR_SYS_NOT_MAP, null, null);
    }
    Object mtype = sysmap.get("mimetype");
    Object data = sysmap.get("binary");
    if (mtype == null) {
      return new ResolvedSys(ERR_MIMETYPE_UNBOUND, null, null);
    }
    if (data == null) {
      return new ResolvedSys(ERR_BINARY_UNBOUND, null, null);
    }
    String mimetype;
    if (mtype instanceof Property property) {
      try {
        mimetype = property.getString();
      } catch (RepositoryException e) {
        return new ResolvedSys("Problem extracting data from property", null, null);
      }
    } else if (mtype instanceof String stringType) {
      mimetype = stringType;
    } else {
      return new ResolvedSys(ERR_MIMETYPE_TYPE, null, null);
    }
    if (!(data instanceof Property || data instanceof Value || data instanceof byte[])) {
      return new ResolvedSys(ERR_BINARY_TYPE, null, null);
    }
    return new ResolvedSys(null, mimetype, data);
  }
}
