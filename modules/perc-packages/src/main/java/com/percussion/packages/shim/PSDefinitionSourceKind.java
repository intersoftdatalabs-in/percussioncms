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
 * #MODERN_COMPONENT_PACKAGE} only. The legacy Widget / Page / Gadget definition-XML fallback was
 * retired (#2852) — the {@code LEGACY_*} constants below are retained for metrics / dashboard
 * compatibility (counters read {@code 0}) and are never produced by {@code
 * PSLegacyDefinitionXmlShim} anymore. Product packages must not depend on the legacy path.
 */
public enum PSDefinitionSourceKind {

  /** Modern ship format: {@code component-package.json} (Component Package Manifest). */
  MODERN_COMPONENT_PACKAGE,

  /** Legacy Widget definition XML under {@code rxconfig/Widgets} (or package staging equivalent). */
  LEGACY_WIDGET_XML,

  /** Legacy Page meta / definition XML dialect. */
  LEGACY_PAGE_XML,

  /** Legacy per-gadget OpenSocial-style definition XML. */
  LEGACY_GADGET_XML
}
