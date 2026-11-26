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
 * Contention predicates for the keyword editors in KeywordEditorPanel
 * (#6985 keyword-level, #6983 choice-level save reuse, #6984 choice-level
 * start reuse). Each editor handler used to inline a 15+ disjunct boolean
 * chain (cyclomatic 16-29); the busy/inflight/confirm tail of every chain
 * now lives here as a flat predicate over a snapshot the component assembles.
 *
 * The null/loading/editing head of each chain stays inline in the handler
 * so TypeScript keeps narrowing `id` and `serverKeyword` to non-null.
 * The predicates are pure and behavior-preserving by construction: every
 * flag maps one-to-one to a disjunct of the original guard.
 */
export type KeywordConfirmKind = "keyword" | "choice" | "clear-description";

/** Busy/inflight/confirm tail of a keyword-level save guard. */
export interface KeywordWriteContention {
  /** Any in-flight write the save must yield to (aggregated by the caller). */
  inflight: boolean;
  /** Any busy write the save must yield to (aggregated by the caller). */
  peerBusy: boolean;
  busy: boolean;
  confirmKind: KeywordConfirmKind | null;
}

/**
 * Shared contention check for the keyword save editors (#6985) and the
 * choice save editors (#6983). Each save passes its own inflight/peerBusy
 * aggregates so the predicate stays flat while preserving the exact
 * per-handler flag sets (each save yields to a different subset of the
 * choice busy flags, excluding its own).
 */
export function isKeywordWriteContended(contention: KeywordWriteContention): boolean {
  return (
    contention.inflight ||
    contention.peerBusy ||
    contention.busy ||
    contention.confirmKind != null
  );
}

/** Busy/inflight/confirm tail of a keyword-level edit-starter guard. */
export interface KeywordEditStartContention {
  /** choiceWriteBusy || busy, precomputed by the caller. */
  writeBusy: boolean;
  confirmOpen: boolean;
  /** Another inline editor is already open. */
  editOpen: boolean;
  /** Any in-flight write the starter must yield to (aggregated by the caller). */
  inflight: boolean;
}

/** Shared contention check for the keyword-level edit starters. */
export function isKeywordEditStartContended(contention: KeywordEditStartContention): boolean {
  return (
    contention.writeBusy || contention.confirmOpen || contention.editOpen || contention.inflight
  );
}

/** Start-guard tail of a choice-level edit starter (#6984). */
export interface ChoiceEditStartContention {
  /** choiceWriteBusy || busy, precomputed by the caller. */
  writeBusy: boolean;
  confirmOpen: boolean;
  /** Any in-flight write the starter must yield to (aggregated by the caller). */
  inflight: boolean;
  /**
   * Another inline editor is already open (per-handler peer aggregate:
   * the label starter excludes its own open flag; value, description, and
   * sequence block on all four choice edit-open flags).
   */
  peerEditOpen: boolean;
  /** The keyword sequence editor owns its own row. */
  keywordEditing: boolean;
  /** The requested row index. */
  index: number;
  /** listedChoices.length at call time. */
  choiceCount: number;
}

/**
 * Shared start-guard check for the choice edit starters (#6984). Each
 * startChoice*Edit handler used to inline a 20+ disjunct chain (cyclomatic
 * 22-23); the busy/inflight/confirm/edit-open/index tail now lives here as
 * a flat predicate over a snapshot the component assembles, while the
 * identity/loading head (`!id || isNew || !detailReady`) stays inline.
 * Every flag maps one-to-one to a disjunct of the original guard.
 */
export function isChoiceEditStartBlocked(start: ChoiceEditStartContention): boolean {
  return (
    start.writeBusy ||
    start.confirmOpen ||
    start.inflight ||
    start.peerEditOpen ||
    start.keywordEditing ||
    !Number.isInteger(start.index) ||
    start.index < 0 ||
    start.index >= start.choiceCount
  );
}

/** Aggregate a list of busy/inflight flags without adding branches. */
export function anyFlagSet(flags: readonly boolean[]): boolean {
  return flags.some(Boolean);
}
