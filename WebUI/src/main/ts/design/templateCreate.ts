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

import { isValidAssemblerValue } from "./assemblerOptions";
import { DESIGN_MSG } from "./messages";

/** Same rules as TemplateAdaptor.validateCreateName. */
const NAME_RE = /^[A-Za-z][A-Za-z0-9._-]*$/;

export type TemplateCreateFieldError = "name" | "assembler" | null;

export interface TemplateCreateValidation {
  ok: boolean;
  field: TemplateCreateFieldError;
  message: string | null;
  name: string;
}

/**
 * Validate create-template form fields (operator-facing messages).
 */
export function validateTemplateCreateInput(
  rawName: string,
  assembler: string,
): TemplateCreateValidation {
  const name = (rawName || "").trim();
  if (!name) {
    return {
      ok: false,
      field: "name",
      message: DESIGN_MSG.TPL_CREATE_NAME_REQUIRED,
      name,
    };
  }
  if (/\s/.test(name)) {
    return {
      ok: false,
      field: "name",
      message: DESIGN_MSG.TPL_CREATE_NAME_SPACES,
      name,
    };
  }
  if (!NAME_RE.test(name)) {
    return {
      ok: false,
      field: "name",
      message: DESIGN_MSG.TPL_CREATE_NAME_FORMAT,
      name,
    };
  }
  if (!isValidAssemblerValue(assembler)) {
    return {
      ok: false,
      field: "assembler",
      message: DESIGN_MSG.TPL_CREATE_ASSEMBLER_REQUIRED,
      name,
    };
  }
  return { ok: true, field: null, message: null, name };
}
