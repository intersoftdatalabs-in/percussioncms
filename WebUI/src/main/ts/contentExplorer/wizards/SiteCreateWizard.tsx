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

/**
 * Create Site wizard for Explorer Content → Create Site and Navigation
 * New Site (#3520 / parent #3512).
 *
 * <p>First step is a site-type picker (Traditional / Page / Virtual).
 * Traditional: type → details (name, description, optional managed nav)
 * → confirm → progress. No page-template prompt.
 * Page: type → details (name, description, managed nav locked on)
 * → page/base template → confirm → progress. Create sends
 * {@code pageBased: true} on the existing {@code POST /sitemanage/site/}
 * contract. Virtual stays blocked until slice 3.</p>
 */

import React, { useEffect, useState } from "react";
import {
  createTraditionalSite,
  listBaseTemplates,
  pickDefaultBaseTemplate,
  PLAIN_BASE_TEMPLATE_NAME,
  siteFolderPath,
  type BaseTemplateSummary,
  type CreateSiteRequest,
  type CreatedSiteSummary,
} from "../../api/contentExplorer/siteCreateApi";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import {
  advance,
  back,
  createWizard,
  currentStepId,
  finishWizard,
  isFinalStep,
  resetWizard,
  type WizardState,
} from "./state";
import {
  canSubmitCreateSite,
  clampSiteDescription,
  defaultTemplateNameForSite,
  filterSiteNameInput,
  filterTemplateNameInput,
  isSiteCreateKindEnabled,
  managedNavigationForcedOn,
  requiresPageTemplate,
  validateSiteName,
  validateTemplateName,
  wizardStepsForKind,
  type SiteCreateKind,
} from "./siteCreateValidation";

export interface SiteCreateWizardProps {
  /** Optional submit override (default: POST /sitemanage/site/). */
  submit?: (request: CreateSiteRequest) => Promise<CreatedSiteSummary>;
  /** Optional base-template loader (default: GET readonly base templates). */
  loadBaseTemplates?: () => Promise<BaseTemplateSummary[]>;
  /** Optional seed site name. */
  initialSiteName?: string;
  /** Fired after successful create with site name + folder path. */
  onCreated?: (result: {
    siteName: string;
    folderPath: string;
    site: CreatedSiteSummary;
  }) => void;
  onSettled?: (ok: boolean) => void;
  ariaLabel?: string;
  className?: string;
}

const TYPE_TEST_IDS: Record<SiteCreateKind, string> = {
  traditional: "site-create-type-traditional",
  page: "site-create-type-page",
  virtual: "site-create-type-virtual",
};

function typeLabel(kind: SiteCreateKind): string {
  switch (kind) {
    case "page":
      return message(EXPLORER_MSG.SITE_CREATE_TYPE_PAGE);
    case "virtual":
      return message(EXPLORER_MSG.SITE_CREATE_TYPE_VIRTUAL);
    default:
      return message(EXPLORER_MSG.SITE_CREATE_TRADITIONAL);
  }
}

export function SiteCreateWizard(
  props: SiteCreateWizardProps,
): React.JSX.Element {
  const {
    submit: submitOverride,
    loadBaseTemplates = listBaseTemplates,
    initialSiteName = "",
    onCreated,
    onSettled,
    ariaLabel,
    className,
  } = props;

  const [wizard, setWizard] = useState<WizardState>(() =>
    createWizard(wizardStepsForKind("traditional")),
  );
  const [siteType, setSiteType] = useState<SiteCreateKind>("traditional");
  const [siteName, setSiteName] = useState(() =>
    filterSiteNameInput(initialSiteName),
  );
  const [description, setDescription] = useState("");
  const [templateName, setTemplateName] = useState(() =>
    defaultTemplateNameForSite(initialSiteName),
  );
  const [templateNameFollowsSite, setTemplateNameFollowsSite] = useState(true);
  const [baseTemplateName, setBaseTemplateName] = useState(
    PLAIN_BASE_TEMPLATE_NAME,
  );
  const [managedNavigation, setManagedNavigation] = useState(true);
  const [templates, setTemplates] = useState<BaseTemplateSummary[]>([]);
  const [templatesError, setTemplatesError] = useState<string | null>(null);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [createdName, setCreatedName] = useState<string | null>(null);

  useEffect(() => {
    if (!requiresPageTemplate(siteType)) {
      return;
    }
    let cancelled = false;
    setTemplatesLoading(true);
    loadBaseTemplates()
      .then((list) => {
        if (cancelled) return;
        const rows = list ?? [];
        setTemplates(rows);
        setBaseTemplateName(pickDefaultBaseTemplate(rows));
        setTemplatesError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setTemplates([]);
        setBaseTemplateName(PLAIN_BASE_TEMPLATE_NAME);
        setTemplatesError(
          err instanceof Error && err.message
            ? err.message
            : message(EXPLORER_MSG.SITE_CREATE_TEMPLATES_ERROR),
        );
      })
      .finally(() => {
        if (!cancelled) setTemplatesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [loadBaseTemplates, siteType]);

  function handleSiteTypeChange(kind: SiteCreateKind): void {
    setSiteType(kind);
    if (managedNavigationForcedOn(kind)) {
      setManagedNavigation(true);
    }
    setWizard((w) => ({
      ...w,
      steps: wizardStepsForKind(kind),
    }));
  }

  async function handleRun(): Promise<void> {
    const site = validateSiteName(siteName);
    const pageFlow = requiresPageTemplate(siteType);
    const tmpl = validateTemplateName(templateName);
    const base = baseTemplateName.trim();
    if (!site.ok || !isSiteCreateKindEnabled(siteType)) {
      setWizard((w) =>
        finishWizard(w, {
          kind: "error",
          message: message(EXPLORER_MSG.SITE_CREATE_VALIDATION),
        }),
      );
      onSettled?.(false);
      return;
    }
    let resolvedTemplate = defaultTemplateNameForSite(site.name);
    let resolvedBase = PLAIN_BASE_TEMPLATE_NAME;
    if (pageFlow) {
      if (!tmpl.ok || !base) {
        setWizard((w) =>
          finishWizard(w, {
            kind: "error",
            message: message(EXPLORER_MSG.SITE_CREATE_VALIDATION_PAGE),
          }),
        );
        onSettled?.(false);
        return;
      }
      resolvedTemplate = tmpl.name;
      resolvedBase = base;
    }
    setWizard((w) => ({ ...w, submitting: true }));
    const req: CreateSiteRequest = {
      name: site.name,
      label: site.name,
      description: clampSiteDescription(description),
      baseTemplateName: resolvedBase,
      templateName: resolvedTemplate,
      managedNavigation: pageFlow ? true : managedNavigation,
    };
    if (pageFlow) {
      req.pageBased = true;
    }
    try {
      const fn = submitOverride ?? createTraditionalSite;
      const created = await fn(req);
      const folderPath = siteFolderPath(created.name);
      setCreatedName(created.name);
      setWizard((w) => finishWizard(w, { kind: "ok" }));
      onCreated?.({ siteName: created.name, folderPath, site: created });
      onSettled?.(true);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err ?? "unknown");
      setWizard((w) => finishWizard(w, { kind: "error", message: msg }));
      onSettled?.(false);
    }
  }

  function handleSiteNameChange(value: string): void {
    const next = filterSiteNameInput(value);
    setSiteName(next);
    if (templateNameFollowsSite) {
      setTemplateName(defaultTemplateNameForSite(next));
    }
  }

  function handleTemplateNameChange(value: string): void {
    setTemplateNameFollowsSite(false);
    setTemplateName(filterTemplateNameInput(value));
  }

  function handleReset(): void {
    setWizard(() => createWizard(wizardStepsForKind("traditional")));
    setCreatedName(null);
    setSiteType("traditional");
    setManagedNavigation(true);
    setTemplateName(defaultTemplateNameForSite(siteName));
    setTemplateNameFollowsSite(true);
  }

  const typeReady = isSiteCreateKindEnabled(siteType);
  const detailsReady = validateSiteName(siteName).ok;
  const templateReady = baseTemplateName.trim().length > 0;
  const canSubmit = canSubmitCreateSite({
    siteName,
    siteType,
    templateName,
    baseTemplateName,
  });
  const stepId = currentStepId(wizard);
  const navLocked = managedNavigationForcedOn(siteType);

  function renderStep(): React.JSX.Element {
    switch (stepId) {
      case "type":
        return (
          <fieldset data-testid="site-create-step-type">
            <legend>{message(EXPLORER_MSG.SITE_CREATE_STEP_TYPE)}</legend>
            <p
              style={{ color: "#555", fontSize: "0.9em", margin: "0 0 8px 0" }}
            >
              {message(EXPLORER_MSG.SITE_CREATE_TYPE_LABEL)}
            </p>
            {(
              [
                "traditional",
                "page",
                "virtual",
              ] as const satisfies readonly SiteCreateKind[]
            ).map((kind) => (
              <label
                key={kind}
                htmlFor={TYPE_TEST_IDS[kind]}
                style={{ display: "block", margin: "6px 0" }}
              >
                <input
                  id={TYPE_TEST_IDS[kind]}
                  type="radio"
                  name="site-create-type"
                  data-testid={TYPE_TEST_IDS[kind]}
                  value={kind}
                  checked={siteType === kind}
                  onChange={() => handleSiteTypeChange(kind)}
                  style={{ marginRight: 8 }}
                />
                {typeLabel(kind)}
              </label>
            ))}
            {siteType === "traditional" ? (
              <p
                style={{
                  color: "#555",
                  fontSize: "0.85em",
                  margin: "8px 0 0 24px",
                }}
                data-testid="site-create-traditional-note"
              >
                {message(EXPLORER_MSG.SITE_CREATE_TRADITIONAL_NOTE)}
              </p>
            ) : null}
            {siteType === "page" ? (
              <p
                style={{
                  color: "#555",
                  fontSize: "0.85em",
                  margin: "8px 0 0 24px",
                }}
                data-testid="site-create-page-note"
              >
                {message(EXPLORER_MSG.SITE_CREATE_PAGE_NOTE)}
              </p>
            ) : null}
            {siteType === "virtual" ? (
              <p
                role="status"
                style={{
                  color: "#8a4b08",
                  fontSize: "0.9em",
                  margin: "8px 0 0 0",
                }}
                data-testid="site-create-type-unavailable"
              >
                {message(EXPLORER_MSG.SITE_CREATE_TYPE_UNAVAILABLE)}
              </p>
            ) : null}
          </fieldset>
        );
      case "details":
        return (
          <fieldset data-testid="site-create-step-details">
            <legend>{message(EXPLORER_MSG.SITE_CREATE_STEP_DETAILS)}</legend>
            <label
              htmlFor="site-create-name"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_CREATE_NAME_LABEL)}
              <input
                id="site-create-name"
                type="text"
                data-testid="site-create-name"
                autoComplete="off"
                value={siteName}
                maxLength={100}
                onChange={(e) => handleSiteNameChange(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
            <label
              htmlFor="site-create-description"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_CREATE_DESCRIPTION_LABEL)}
              <input
                id="site-create-description"
                type="text"
                data-testid="site-create-description"
                value={description}
                maxLength={255}
                onChange={(e) =>
                  setDescription(clampSiteDescription(e.target.value))
                }
                style={{ marginLeft: 8, width: 320 }}
              />
            </label>
            <label
              htmlFor="site-create-managed-nav"
              style={{ display: "block", margin: "8px 0 4px 0" }}
            >
              <input
                id="site-create-managed-nav"
                type="checkbox"
                data-testid="site-create-managed-nav"
                checked={navLocked ? true : managedNavigation}
                disabled={navLocked}
                onChange={(e) => {
                  if (!navLocked) {
                    setManagedNavigation(e.target.checked);
                  }
                }}
                style={{ marginRight: 8 }}
              />
              {message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_LABEL)}
            </label>
            <p
              style={{
                color: "#555",
                fontSize: "0.85em",
                margin: "0 0 8px 24px",
              }}
              data-testid="site-create-managed-nav-help"
            >
              {navLocked
                ? message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_REQUIRED)
                : message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_HELP)}
            </p>
          </fieldset>
        );
      case "template":
        return (
          <fieldset data-testid="site-create-step-template">
            <legend>{message(EXPLORER_MSG.SITE_CREATE_STEP_TEMPLATE)}</legend>
            {templatesLoading ? (
              <p role="status" data-testid="site-create-templates-loading">
                {message(EXPLORER_MSG.SITE_CREATE_TEMPLATES_LOADING)}
              </p>
            ) : null}
            {templatesError ? (
              <p
                role="alert"
                data-testid="site-create-templates-error"
                style={{ color: "#b00020" }}
              >
                {templatesError}
              </p>
            ) : null}
            <label
              htmlFor="site-create-template-name"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_CREATE_TEMPLATE_NAME_LABEL)}
              <input
                id="site-create-template-name"
                type="text"
                data-testid="site-create-template-name"
                value={templateName}
                maxLength={100}
                onChange={(e) => handleTemplateNameChange(e.target.value)}
                style={{ marginLeft: 8, width: 240 }}
              />
            </label>
            <label
              htmlFor="site-create-base-template"
              style={{ display: "block", margin: "4px 0" }}
            >
              {message(EXPLORER_MSG.SITE_CREATE_BASE_TEMPLATE_LABEL)}
              {templates.length > 0 ? (
                <select
                  id="site-create-base-template"
                  data-testid="site-create-base-template"
                  value={baseTemplateName}
                  onChange={(e) => setBaseTemplateName(e.target.value)}
                  style={{ marginLeft: 8, minWidth: 240 }}
                >
                  {templates.map((t) => (
                    <option key={t.id ?? t.name} value={t.name}>
                      {t.label || t.name}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id="site-create-base-template"
                  type="text"
                  data-testid="site-create-base-template"
                  value={baseTemplateName}
                  onChange={(e) => setBaseTemplateName(e.target.value)}
                  style={{ marginLeft: 8, width: 240 }}
                />
              )}
            </label>
          </fieldset>
        );
      case "confirm":
        return (
          <fieldset data-testid="site-create-step-confirm">
            <legend>{message(EXPLORER_MSG.SITE_CREATE_STEP_CONFIRM)}</legend>
            <ul
              style={{ listStyle: "none", padding: 0 }}
              data-testid="site-create-confirm-summary"
            >
              <li data-testid="site-create-confirm-type">
                <strong>{message(EXPLORER_MSG.SITE_CREATE_TYPE_LABEL)}:</strong>{" "}
                {typeLabel(siteType)}
              </li>
              <li>
                <strong>{message(EXPLORER_MSG.SITE_CREATE_NAME_LABEL)}:</strong>{" "}
                <code>{siteName}</code>
              </li>
              {requiresPageTemplate(siteType) ? (
                <>
                  <li data-testid="site-create-confirm-template-name">
                    <strong>
                      {message(EXPLORER_MSG.SITE_CREATE_TEMPLATE_NAME_LABEL)}:
                    </strong>{" "}
                    <code>{templateName}</code>
                  </li>
                  <li data-testid="site-create-confirm-base-template">
                    <strong>
                      {message(EXPLORER_MSG.SITE_CREATE_BASE_TEMPLATE_LABEL)}:
                    </strong>{" "}
                    <code>{baseTemplateName}</code>
                  </li>
                </>
              ) : null}
              <li data-testid="site-create-confirm-managed-nav">
                <strong>
                  {message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_LABEL)}:
                </strong>{" "}
                {navLocked || managedNavigation
                  ? message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_YES)
                  : message(EXPLORER_MSG.SITE_CREATE_MANAGED_NAV_NO)}
              </li>
            </ul>
          </fieldset>
        );
      case "progress":
        return (
          <fieldset data-testid="site-create-step-progress">
            <legend>{message(EXPLORER_MSG.SITE_CREATE_STEP_PROGRESS)}</legend>
            <p
              role="status"
              aria-live="polite"
              data-testid="site-create-progress"
            >
              {wizard.submitting
                ? message(EXPLORER_MSG.SITE_CREATE_SUBMITTING)
                : wizard.result?.kind === "ok"
                  ? message(EXPLORER_MSG.SITE_CREATE_SUCCESS).replace(
                      "{name}",
                      createdName ?? siteName,
                    )
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
  const nextDisabled =
    wizard.submitting ||
    (stepId === "type" && !typeReady) ||
    (stepId === "details" && !detailsReady) ||
    (stepId === "template" && !templateReady);

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.SITE_CREATE_TITLE)}
      data-testid="site-create-wizard"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.SITE_CREATE_TITLE)}
      </h2>
      <p
        role="status"
        aria-live="polite"
        data-testid="site-create-step-count"
        style={{ color: "#888", margin: "0 0 8px 0" }}
      >
        {message(EXPLORER_MSG.WIZARD_STEP)} {wizard.current + 1}{" "}
        {message(EXPLORER_MSG.WIZARD_OF)} {wizard.steps.length}
      </p>
      {renderStep()}
      <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
        <button
          type="button"
          data-testid="site-create-back"
          disabled={
            wizard.current === 0 || wizard.submitting || result !== undefined
          }
          onClick={() => setWizard((w) => back(w))}
        >
          {message(EXPLORER_MSG.WIZARD_BACK)}
        </button>
        {!finalStep ? (
          <button
            type="button"
            data-testid="site-create-next"
            disabled={nextDisabled}
            onClick={() => setWizard((w) => advance(w))}
          >
            {message(EXPLORER_MSG.WIZARD_NEXT)}
          </button>
        ) : (
          <button
            type="button"
            data-testid="site-create-run"
            disabled={
              wizard.submitting || result !== undefined || !canSubmit
            }
            onClick={() => void handleRun()}
          >
            {message(EXPLORER_MSG.SITE_CREATE_SUBMIT)}
          </button>
        )}
        <button
          type="button"
          data-testid="site-create-cancel"
          onClick={handleReset}
        >
          {message(EXPLORER_MSG.WIZARD_CANCEL)}
        </button>
      </div>
    </section>
  );
}
