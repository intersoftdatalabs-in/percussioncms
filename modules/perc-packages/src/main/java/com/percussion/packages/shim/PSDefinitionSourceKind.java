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

package com.percussion.packages.shim;

/**
 * Source kind chosen by definition selection (ADR-004 / issue #2752).
 *
 * <p><strong>Modern required</strong>: selection resolves {@link
 * #MODERN_COMPONENT_PACKAGE} only. All legacy definition-XML fallbacks are removed — the
 * Widget fallback was retired (#2852) and removed (#6440), the Page fallback was removed
 * (#6446), and the Gadget fallback was removed (#6447). No {@code LEGACY_*} constant
 * remains. Product packages must not depend on any legacy path.
 */
public enum PSDefinitionSourceKind {

  /** Modern ship format: {@code component-package.json} (Component Package Manifest). */
  MODERN_COMPONENT_PACKAGE
}
