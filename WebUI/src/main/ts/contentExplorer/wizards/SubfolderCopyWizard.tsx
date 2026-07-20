/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Subfolder Copy wizard (US7 / T077).
 *
 * <p>Three-step flow driven by {@link createWizard}:
 * {@code source → target → confirm → progress → done}. Uses
 * the existing {@code pathmanagement/path/moveItem} endpoint with
 * {@code copy:true}, so the wizard exhibits the same wire shape
 * the US1 {@link ReducedActions}-style copy uses (no new backend
 * surface).</p>
 */

import React, { useState } from "react";
import { moveItem } from "../../api/contentExplorer/pathApi";
import type { PSMoveFolderItem } from "../../api/contentExplorer/types";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import {
  advance,
  back,
  createWizard,
  finishWizard,
  isFinalStep,
  resetWizard,
  type WizardState,
} from "./state";

export interface SubfolderCopyWizardProps {
  /** Optional override for the move / copy transport. */
  submit?: (req: PSMoveFolderItem) => Promise<void>;
  initialSource?: string;
  initialTarget?: string;
  ariaLabel?: string;
  className?: string;
  onSettled?: (ok: boolean) => void;
}

const STEPS = ["source", "target", "confirm", "progress"];

export function SubfolderCopyWizard(
  props: SubfolderCopyWizardProps,
): React.JSX.Element {
  const {
    submit: submitOverride,
    initialSource = "",
    initialTarget = "",
    ariaLabel,
    className,
    onSettled,
  } = props;
  const [wizard, setWizard] = useState<WizardState>(() => createWizard(STEPS));
  const [sourcePath, setSourcePath] = useState(initialSource);
  const [targetPath, setTargetPath] = useState(initialTarget);

  async function handleRun(): Promise<void> {
    setWizard((w) => ({ ...w, submitting: true }));
    const req: PSMoveFolderItem = {
      sourcePath,
      targetPath,
      copy: true,
    };
    try {
      if (submitOverride) {
        await submitOverride(req);
      } else {
        await moveItem(req);
      }
      setWizard((w) => finishWizard(w, { kind: "ok" }));
      onSettled?.(true);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err ?? "unknown");
      setWizard((w) => finishWizard(w, { kind: "error", message: msg }));
      onSettled?.(false);
    }
  }

  function renderStep(): React.JSX.Element {
    switch (wizard.current) {
      case 0:
        return (
          <fieldset data-testid="subfolder-copy-step-source">
            <legend>{message(EXPLORER_MSG.SUBFOLDER_COPY_STEP_SOURCE)}</legend>
            <label
              htmlFor="subfolder-copy-source"
              style={{ display: "block", margin: "4px 0" }}
            >
              Source path
              <input
                id="subfolder-copy-source"
                type="text"
                data-testid="subfolder-copy-source"
                value={sourcePath}
                onChange={(e) => setSourcePath(e.target.value)}
                style={{ marginLeft: 8, width: 360 }}
              />
            </label>
          </fieldset>
        );
      case 1:
        return (
          <fieldset data-testid="subfolder-copy-step-target">
            <legend>{message(EXPLORER_MSG.SUBFOLDER_COPY_STEP_TARGET)}</legend>
            <label
              htmlFor="subfolder-copy-target"
              style={{ display: "block", margin: "4px 0" }}
            >
              Target path
              <input
                id="subfolder-copy-target"
                type="text"
                data-testid="subfolder-copy-target"
                value={targetPath}
                onChange={(e) => setTargetPath(e.target.value)}
                style={{ marginLeft: 8, width: 360 }}
              />
            </label>
          </fieldset>
        );
      case 2:
        return (
          <fieldset data-testid="subfolder-copy-step-confirm">
            <legend>{message(EXPLORER_MSG.SUBFOLDER_COPY_STEP_CONFIRM)}</legend>
            <ul
              style={{ listStyle: "none", padding: 0 }}
              data-testid="subfolder-copy-confirm-summary"
            >
              <li>
                <strong>Source:</strong> <code>{sourcePath}</code>
              </li>
              <li>
                <strong>Target:</strong> <code>{targetPath}</code>
              </li>
            </ul>
          </fieldset>
        );
      case 3:
        return (
          <fieldset data-testid="subfolder-copy-step-progress">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_PROGRESS)}</legend>
            <p
              role="status"
              aria-live="polite"
              data-testid="subfolder-copy-progress"
            >
              {wizard.submitting
                ? "Submitting\u2026"
                : wizard.result?.kind === "ok"
                  ? "Subfolder copy completed"
                  : wizard.result?.kind === "error"
                    ? `${message(EXPLORER_MSG.WIZARD_ERROR)}: ${wizard.result.message}`
                    : ""}
            </p>
          </fieldset>
        );
      default:
        return <></>;
    }
  }

  const finalStep = isFinalStep(wizard);
  const result = wizard.result;

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.SUBFOLDER_COPY_TITLE)}
      data-testid="subfolder-copy-wizard"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.SUBFOLDER_COPY_TITLE)}
      </h2>
      <p
        role="status"
        aria-live="polite"
        data-testid="subfolder-copy-step-count"
        style={{ color: "#888", margin: "0 0 8px 0" }}
      >
        {message(EXPLORER_MSG.WIZARD_STEP)} {wizard.current + 1}{" "}
        {message(EXPLORER_MSG.WIZARD_OF)} {STEPS.length}
      </p>
      {renderStep()}
      <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
        <button
          type="button"
          data-testid="subfolder-copy-back"
          disabled={wizard.current === 0 || wizard.submitting || result !== undefined}
          onClick={() => setWizard((w) => back(w))}
        >
          {message(EXPLORER_MSG.WIZARD_BACK)}
        </button>
        {!finalStep ? (
          <button
            type="button"
            data-testid="subfolder-copy-next"
            disabled={
              wizard.submitting ||
              (wizard.current === 0 && sourcePath.trim() === "") ||
              (wizard.current === 1 && targetPath.trim() === "")
            }
            onClick={() => setWizard((w) => advance(w))}
          >
            {message(EXPLORER_MSG.WIZARD_NEXT)}
          </button>
        ) : (
          <button
            type="button"
            data-testid="subfolder-copy-run"
            disabled={wizard.submitting || result !== undefined}
            onClick={() => void handleRun()}
          >
            {message(EXPLORER_MSG.WIZARD_SUBMIT)}
          </button>
        )}
        <button
          type="button"
          data-testid="subfolder-copy-cancel"
          onClick={() => setWizard((w) => resetWizard(w))}
        >
          {message(EXPLORER_MSG.WIZARD_CANCEL)}
        </button>
      </div>
    </section>
  );
}
