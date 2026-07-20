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
 * Site Copy wizard (US7 / T076).
 *
 * <p>Five-step flow driven by {@link createWizard}:
 * {@code source → target → options → confirm → progress → done}.
 * The host supplies the submit handler; the wizard collects the
 * {@link PSSiteCopyRequest} payload and delegates transport to the
 * supplied {@link SiteCopyWizardProps.submit} function (default
 * implementation POSTs to {@code /Rhythmyx/rest/sitemanage/site/copy}).</p>
 */

import React, { useState } from "react";
import type { PSSiteCopyRequest } from "../../api/contentExplorer/types";
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

export interface SiteCopyWizardProps {
  /** Optional submit override (default: POST /sitemanage/site/copy). */
  submit?: (request: PSSiteCopyRequest) => Promise<void>;
  /** Optional initial sources/target for deep-linking. */
  initialSource?: string;
  initialTarget?: string;
  ariaLabel?: string;
  className?: string;
  onSettled?: (ok: boolean) => void;
}

const STEPS = ["source", "target", "options", "confirm", "progress"];

export function SiteCopyWizard(
  props: SiteCopyWizardProps,
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
  const [sourceSite, setSourceSite] = useState(initialSource);
  const [targetSite, setTargetSite] = useState(initialTarget);
  const [targetFolder, setTargetFolder] = useState("/");
  const [workflows, setWorkflows] = useState("*");
  const [templates, setTemplates] = useState("*");

  async function defaultSubmit(req: PSSiteCopyRequest): Promise<void> {
    const { post } = await import("../../api/client");
    const { PATHS } = await import("../../api/paths");
    await post<void>(PATHS.SITES_ALL + "/copy", req);
  }

  async function handleRun(): Promise<void> {
    setWizard((w) => ({
      ...w,
      submitting: true,
    }));
    const req: PSSiteCopyRequest = {
      sourceSite,
      targetSite,
      targetFolder,
      workflows,
      templates,
    };
    try {
      const fn = submitOverride ?? defaultSubmit;
      await fn(req);
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
          <fieldset data-testid="site-copy-step-source">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_SOURCE)}</legend>
            <label
              htmlFor="site-copy-source"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_COPY_STEP_SOURCE)}
              <input
                id="site-copy-source"
                type="text"
                data-testid="site-copy-source"
                value={sourceSite}
                onChange={(e) => setSourceSite(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
          </fieldset>
        );
      case 1:
        return (
          <fieldset data-testid="site-copy-step-target">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_TARGET)}</legend>
            <label
              htmlFor="site-copy-target"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_COPY_STEP_TARGET)}
              <input
                id="site-copy-target"
                type="text"
                data-testid="site-copy-target"
                value={targetSite}
                onChange={(e) => setTargetSite(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
            <label
              htmlFor="site-copy-folder"
              style={{ display: "block", margin: "4px 0" }}
            >
              Target folder
              <input
                id="site-copy-folder"
                type="text"
                data-testid="site-copy-folder"
                value={targetFolder}
                onChange={(e) => setTargetFolder(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
          </fieldset>
        );
      case 2:
        return (
          <fieldset data-testid="site-copy-step-options">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_OPTIONS)}</legend>
            <label
              htmlFor="site-copy-workflows"
              style={{ display: "block", margin: "4px 0" }}
            >
              Workflows (or *)
              <input
                id="site-copy-workflows"
                type="text"
                data-testid="site-copy-workflows"
                value={workflows}
                onChange={(e) => setWorkflows(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
            <label
              htmlFor="site-copy-templates"
              style={{ display: "block", margin: "4px 0" }}
            >
              Templates (or *)
              <input
                id="site-copy-templates"
                type="text"
                data-testid="site-copy-templates"
                value={templates}
                onChange={(e) => setTemplates(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
          </fieldset>
        );
      case 3:
        return (
          <fieldset data-testid="site-copy-step-confirm">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_CONFIRM)}</legend>
            <ul
              style={{ listStyle: "none", padding: 0 }}
              data-testid="site-copy-confirm-summary"
            >
              <li>
                <strong>Source:</strong> <code>{sourceSite}</code>
              </li>
              <li>
                <strong>Target:</strong> <code>{targetSite}{targetFolder}</code>
              </li>
              <li>
                <strong>Workflows:</strong> <code>{workflows}</code>
              </li>
              <li>
                <strong>Templates:</strong> <code>{templates}</code>
              </li>
            </ul>
          </fieldset>
        );
      case 4:
        return (
          <fieldset data-testid="site-copy-step-progress">
            <legend>{message(EXPLORER_MSG.SITE_COPY_STEP_PROGRESS)}</legend>
            <p
              role="status"
              aria-live="polite"
              data-testid="site-copy-progress"
            >
              {wizard.submitting
                ? "Submitting\u2026"
                : wizard.result?.kind === "ok"
                  ? "Site copy completed"
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
      aria-label={ariaLabel ?? message(EXPLORER_MSG.SITE_COPY_TITLE)}
      data-testid="site-copy-wizard"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.SITE_COPY_TITLE)}
      </h2>
      <p
        role="status"
        aria-live="polite"
        data-testid="site-copy-step-count"
        style={{ color: "#888", margin: "0 0 8px 0" }}
      >
        {message(EXPLORER_MSG.WIZARD_STEP)} {wizard.current + 1}{" "}
        {message(EXPLORER_MSG.WIZARD_OF)} {STEPS.length}
      </p>
      {renderStep()}
      <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
        <button
          type="button"
          data-testid="site-copy-back"
          disabled={wizard.current === 0 || wizard.submitting || result !== undefined}
          onClick={() => setWizard((w) => back(w))}
        >
          {message(EXPLORER_MSG.WIZARD_BACK)}
        </button>
        {!finalStep ? (
          <button
            type="button"
            data-testid="site-copy-next"
            disabled={
              wizard.submitting ||
              (wizard.current === 0 && sourceSite.trim() === "") ||
              (wizard.current === 1 && targetSite.trim() === "")
            }
            onClick={() => setWizard((w) => advance(w))}
          >
            {message(EXPLORER_MSG.WIZARD_NEXT)}
          </button>
        ) : (
          <button
            type="button"
            data-testid="site-copy-run"
            disabled={wizard.submitting || result !== undefined}
            onClick={() => void handleRun()}
          >
            {message(EXPLORER_MSG.WIZARD_SUBMIT)}
          </button>
        )}
        <button
          type="button"
          data-testid="site-copy-cancel"
          onClick={() => setWizard((w) => resetWizard(w))}
        >
          {message(EXPLORER_MSG.WIZARD_CANCEL)}
        </button>
      </div>
    </section>
  );
}
