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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Pure state machine for the modern Content Explorer wizards (US7 / T072).
 *
 * <p>The wizard is a {@code Step -> Step} navigator with the three
 * standard operations: {@link advance} (Next), {@link back}, and
 * {@link reset}. The component layers (SiteCopyWizard /
 * SubfolderCopyWizard) own the per-step data; the state machine owns
 * the navigation so it can be unit-tested without a DOM.</p>
 *
 * <p>Steps are indexed by an arbitrary string the host provides
 * (e.g. {@code "source"}, {@code "target"}, {@code "options"},
 * {@code "confirm"}). The host renders the per-step body via a
 * switch on {@link WizardState.current}.</p>
 */

export interface WizardState {
  /** Step identifiers in order. */
  readonly steps: ReadonlyArray<string>;
  /** Index of the current step. */
  current: number;
  /** True if the wizard is currently submitting (run button disabled). */
  submitting: boolean;
  /** Submission outcome; reset by {@link resetWizard}. */
  result?: WizardResult;
}

export type WizardResult =
  | { kind: "ok" }
  | { kind: "error"; message: string };

/** Initial state with the supplied step list. */
export function createWizard(
  steps: ReadonlyArray<string>,
  initial?: string,
): WizardState {
  if (steps.length === 0) {
    throw new Error("wizard requires at least one step");
  }
  if (initial === undefined) {
    return { steps, current: 0, submitting: false };
  }
  const idx = steps.indexOf(initial);
  if (idx === -1) {
    throw new Error(`unknown initial step: ${initial}`);
  }
  return { steps, current: idx, submitting: false };
}

/**
 * Advance to the next step if there is one; otherwise mark the
 * wizard as submitting (so the host can wire a single submit
 * handler at the last step).
 */
export function advance(state: WizardState): WizardState {
  if (state.submitting) return state;
  const last = state.steps.length - 1;
  if (state.current >= last) {
    return { ...state, submitting: true };
  }
  return {
    ...state,
    current: Math.min(state.current + 1, last),
  };
}

/** Move back one step (no-op at the first step). */
export function back(state: WizardState): WizardState {
  if (state.submitting) return state;
  if (state.current === 0) return state;
  return { ...state, current: state.current - 1 };
}

/**
 * Mark the wizard as finished with the supplied result. Once the
 * host has called {@link finishWizard} the wizard has to be
 * {@link resetWizard} before further use.
 */
export function finishWizard(
  state: WizardState,
  result: WizardResult,
): WizardState {
  return { ...state, submitting: false, result };
}

/** Reset the wizard to its first step with no result. */
export function resetWizard(state: WizardState): WizardState {
  return { steps: state.steps, current: 0, submitting: false };
}

/** Identify the current step id; convenience for render. */
export function currentStepId(state: WizardState): string {
  const id = state.steps[state.current];
  if (id === undefined) {
    throw new Error("wizard state has no current step");
  }
  return id;
}

/** True when at the last step and not currently submitting. */
export function isFinalStep(state: WizardState): boolean {
  return state.current >= state.steps.length - 1 && !state.submitting;
}

/** True when the wizard has completed (ok or error). */
export function isFinished(state: WizardState): boolean {
  return state.result !== undefined;
}
