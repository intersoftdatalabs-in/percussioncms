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
 * IA (information architecture) Relationships view (US8 / T103).
 *
 * <p>Renders the same 6-dimension dependency model as
 * {@link DependencyViewer} but grouped by direction (outgoing / incoming) with an
 * explicit "IA focus" filter — the taxonomy dimension is elevated, and AA links are
 * demoted to a footer so the IA team can scan the relationship density without the link
 * count drowning out the node / taxonomy view they care about.</p>
 *
 * <p>As of US8 the consolidated server summary populates all 6 dimensions; the morning
 * client-side summary fallback is kept for test legibility but is no longer wired to the
 * production flow.</p>
 */

import React from "react";
import type { DependencyItem as DependencyItemShared } from "./DependencyViewer";
import type { NodeRelationshipSummary } from "./dependencyModel";
import type { RelationshipSummary } from "../../api/contentExplorer/types";
import type { PSNodeRelationshipSummary } from "../../api/contentExplorer/relationship";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import { composeFromServerSummary, labelFor } from "./dependencyModel";
import { parseExplorerContentId } from "../../api/contentExplorer/pathItemId";
import type { PSExplorerRelationshipEdge } from "../../api/contentExplorer/relationship";
import {
  changeSlotTemplateSlot,
  fetchSlotAllowedTemplates,
  fetchSlotCanvas,
  moveSlotRelationship,
  type SlotAllowedChoice,
  type SlotCanvasSlot,
  type SlotRelationship,
} from "../../api/contentExplorer/slotRelationshipApi";
import {
  addRelationshipEdge,
  fetchNodeSummary,
  fetchRelationshipEdges,
  isFolderRelationshipCategory,
  relatedItemOpenTarget,
  relationshipMoveEnds,
  removableOwnedEdges,
  removeAllOwnedRelationshipEdges,
  removeRelationshipEdge,
} from "../../api/contentExplorer/relationshipsApi";
import {
  applyRelationshipSnippetTemplate,
  canChangeRelationshipSnippetTemplate,
  gateRelationshipSnippetTemplate,
  relationshipTemplateLabel,
  type RelationshipTemplateBlock,
} from "../changeRelationshipTemplate";
import {
  applyRelationshipSlotMove,
  canMoveRelationshipToAnotherSlot,
  destinationSlotChoices,
  gateMoveRelationshipToSlot,
  groupActiveAssemblyBySlot,
  slotChoiceLabel,
  type RelationshipSlotChoice,
  type RelationshipSlotMoveBlock,
} from "../moveRelationshipSlot";
import { OpenRelatedItemDialog } from "../OpenRelatedItemDialog";
import {
  openRelatedItemInEditor,
  type OpenRelatedItemDeps,
  type OpenRelatedItemRequest,
  type OpenRelatedItemResult,
} from "../openRelatedItemInEditor";
import {
  closeReservedWindow,
  reserveEditorWindow,
} from "../../editor/openEditorHost";

export interface RelationshipsViewProps {
  item: DependencyItemShared;
  aaLinkCount?: number;
  /** Optional injection seam for tests: pre-loads the consolidated server summary. */
  loadServerSummary?: (itemId: string) => Promise<PSNodeRelationshipSummary>;
  /** Optional injection seam: removable relationships owned by the item. */
  loadEdges?: (itemId: string) => Promise<PSExplorerRelationshipEdge[]>;
  /** Optional injection seam: delete one owned relationship. */
  removeEdge?: (itemId: string, relationshipId: number) => Promise<void>;
  /** Optional injection seam: create one owned non-folder relationship. */
  addEdge?: (
    itemId: string,
    targetItemId: string,
    configName: string,
  ) => Promise<PSExplorerRelationshipEdge>;
  /**
   * Optional injection seam: move one owned Active Assembly relationship.
   * Defaults to the editor reorder API ({@code POST …/slot-relationships/{id}/move}).
   */
  moveEdge?: (
    relationshipId: number,
    direction: "UP" | "DOWN",
  ) => Promise<void>;
  /**
   * Optional injection seam: snippet templates allowed for one slot.
   * Defaults to {@code GET …/slot-relationships/allowed-templates}.
   */
  loadAllowedTemplates?: (slotId: number) => Promise<SlotAllowedChoice[]>;
  /**
   * Optional injection seam: change one relationship's snippet template.
   * Defaults to {@code POST …/slot-relationships/{id}/template-slot}.
   * Keeps the row's current slot and sort position. Index {@code 0} is
   * the first row and is sent.
   */
  changeTemplate?: (
    relationshipId: number,
    slotId: number,
    templateId: number,
    index: number,
  ) => Promise<SlotRelationship>;
  /**
   * Optional injection seam: slots on the selected page.
   * Defaults to {@code GET …/slot-relationships/canvas}.
   */
  loadPageSlots?: (ownerId: number) => Promise<SlotCanvasSlot[]>;
  /**
   * Optional injection seam: move one relationship to another slot.
   * Defaults to {@code POST …/slot-relationships/{id}/template-slot}
   * with the destination slot and the current snippet template.
   * Index is omitted so the server appends on the destination.
   */
  moveToSlot?: (
    relationshipId: number,
    slotId: number,
    templateId: number,
  ) => Promise<SlotRelationship>;
  /**
   * Optional injection seam: open one related content item in EditorHost.
   * Defaults to a fields probe, then the React editor. Does not change the
   * Explorer selection.
   */
  openRelated?: (
    request: OpenRelatedItemRequest,
    deps?: OpenRelatedItemDeps,
  ) => Promise<OpenRelatedItemResult>;
  /** Optional injection seam: popup reserved on the confirm gesture. */
  reserveRelatedWindow?: () => Window | null;
  /** Optional injection seam for tests: summarises server-shape with AA-link count. */
  composeSummary?: (
    item: DependencyItemShared,
    serverSummary: PSNodeRelationshipSummary,
    aaLinkCount: number,
  ) => NodeRelationshipSummary;
  ariaLabel?: string;
  className?: string;
}

const IA_PRIMARY: ReadonlyArray<
  "outgoing" | "incoming" | "taxonomy" | "local"
> = ["outgoing", "incoming", "taxonomy", "local"];

/**
 * REST {@code /relationships/{id}} needs a numeric content id. Explorer
 * rows often carry a GUID ({@code 1-101-708}); taxonomy treats a
 * non-path string as a JCR path and the summary returns 403.
 */
export function relationshipSummaryItemId(
  raw: string | number | undefined,
): string {
  if (raw == null || raw === "") {
    return "";
  }
  const parsed = parseExplorerContentId(raw);
  if (parsed != null) {
    return String(parsed);
  }
  // Unparseable titles (timestamped percSimpleText names, slugs) must not
  // be sent as /relationships/{id} — that 403s and looks like a permission
  // error for Admin (#3811). Match DependencyViewer: empty id, no fetch.
  return "";
}

async function defaultLoadServerSummary(
  itemId: string,
): Promise<PSNodeRelationshipSummary> {
  return fetchNodeSummary(itemId);
}

function defaultComposeSummary(
  item: DependencyItemShared,
  server: PSNodeRelationshipSummary,
  aaLinkCount: number,
): NodeRelationshipSummary {
  return composeFromServerSummary(item, server, aaLinkCount);
}

export function RelationshipsView(
  props: RelationshipsViewProps,
): React.JSX.Element {
  const {
    item,
    aaLinkCount = 0,
    loadServerSummary = defaultLoadServerSummary,
    loadEdges = fetchRelationshipEdges,
    removeEdge = removeRelationshipEdge,
    addEdge = addRelationshipEdge,
    moveEdge = (relationshipId, direction) =>
      moveSlotRelationship(relationshipId, direction),
    loadAllowedTemplates = fetchSlotAllowedTemplates,
    changeTemplate = changeSlotTemplateSlot,
    loadPageSlots = async (ownerId) => (await fetchSlotCanvas(ownerId)).slots,
    moveToSlot = (relationshipId, slotId, templateId) =>
      changeSlotTemplateSlot(relationshipId, slotId, templateId),
    openRelated = openRelatedItemInEditor,
    reserveRelatedWindow = reserveEditorWindow,
    composeSummary,
    ariaLabel,
    className,
  } = props;
  const summarise = composeSummary ?? defaultComposeSummary;

  const itemId = relationshipSummaryItemId(item.id);
  const [reloadToken, setReloadToken] = React.useState(0);
  const [confirmId, setConfirmId] = React.useState<number | null>(null);
  const [confirmAll, setConfirmAll] = React.useState(false);
  const [removedNotice, setRemovedNotice] = React.useState<"one" | "all" | null>(
    null,
  );
  const [addedNotice, setAddedNotice] = React.useState(false);
  const [removeError, setRemoveError] = React.useState<string | null>(null);
  const [addError, setAddError] = React.useState<string | null>(null);
  const [removing, setRemoving] = React.useState(false);
  const [adding, setAdding] = React.useState(false);
  const [moving, setMoving] = React.useState(false);
  const [moveRequest, setMoveRequest] = React.useState<{
    id: number;
    direction: "UP" | "DOWN";
  } | null>(null);
  const [movedNotice, setMovedNotice] = React.useState(false);
  const [moveError, setMoveError] = React.useState<string | null>(null);
  const [openPrompt, setOpenPrompt] = React.useState<{
    relationshipId: number;
    contentId: number;
    label: string;
  } | null>(null);
  const [opening, setOpening] = React.useState(false);
  const [openedNotice, setOpenedNotice] = React.useState(false);
  const [openError, setOpenError] = React.useState<string | null>(null);
  const [templateRequest, setTemplateRequest] = React.useState<number | null>(
    null,
  );
  const [templateChoices, setTemplateChoices] = React.useState<
    SlotAllowedChoice[]
  >([]);
  const [pickedTemplate, setPickedTemplate] = React.useState("");
  const [templateBusy, setTemplateBusy] = React.useState(false);
  const [templateNotice, setTemplateNotice] = React.useState(false);
  const [templateError, setTemplateError] = React.useState<string | null>(null);
  const [slotMoveRequest, setSlotMoveRequest] = React.useState<number | null>(
    null,
  );
  const [pageSlots, setPageSlots] = React.useState<SlotCanvasSlot[]>([]);
  const [slotChoices, setSlotChoices] = React.useState<RelationshipSlotChoice[]>(
    [],
  );
  const [pickedSlot, setPickedSlot] = React.useState("");
  const [slotMoveBusy, setSlotMoveBusy] = React.useState(false);
  const [slotMoveNotice, setSlotMoveNotice] = React.useState(false);
  const [slotMoveError, setSlotMoveError] = React.useState<string | null>(null);
  const [addOpen, setAddOpen] = React.useState(false);
  const [addTarget, setAddTarget] = React.useState("");
  const [addType, setAddType] = React.useState("Translation");
  const [edges, setEdges] = React.useState<PSExplorerRelationshipEdge[]>([]);
  const [state, setState] = React.useState<
    | { kind: "loading" }
    | { kind: "ok"; summary: PSNodeRelationshipSummary }
    | { kind: "auth" }
    | { kind: "error"; message: string }
  >({ kind: "loading" });

  React.useEffect(() => {
    let alive = true;
    if (!itemId) {
      // No item id → don't fire a network round-trip; the rest endpoint
      // would 404 on the empty path segment. Render the auth placeholder
      // instead (per the bot review on PR #1410).
      setState({ kind: "auth" });
      setEdges([]);
      setRemovedNotice(null);
      setAddedNotice(false);
      setMovedNotice(false);
      setMoveError(null);
      setMoveRequest(null);
      setAddOpen(false);
      setOpenPrompt(null);
      setOpenedNotice(false);
      setOpenError(null);
      setTemplateRequest(null);
      setTemplateNotice(false);
      setTemplateError(null);
      setPickedTemplate("");
      setSlotMoveRequest(null);
      setSlotMoveNotice(false);
      setSlotMoveError(null);
      setPickedSlot("");
      setSlotChoices([]);
      setPageSlots([]);
      return;
    }
    setState({ kind: "loading" });
    setConfirmId(null);
    setConfirmAll(false);
    setMoveRequest(null);
    setAddOpen(false);
    setOpenPrompt(null);
    setTemplateRequest(null);
    setSlotMoveRequest(null);
    loadServerSummary(itemId)
      .then(async (summary) => {
        if (!alive) return;
        let nextEdges: PSExplorerRelationshipEdge[] = [];
        try {
          nextEdges = await loadEdges(itemId);
        } catch (edgeErr: unknown) {
          if (!alive) return;
          setEdges([]);
          setRemoveError(removeFailureMessage(edgeErr));
          setState({ kind: "ok", summary });
          return;
        }
        if (!alive) return;
        setEdges(nextEdges);
        setState({ kind: "ok", summary });
      })
      .catch((err: unknown) => {
        if (!alive) return;
        if (
          err &&
          typeof err === "object" &&
          "status" in err &&
          (err as { status: number }).status === 403
        ) {
          setState({ kind: "auth" });
          return;
        }
        setState({
          kind: "error",
          message: err instanceof Error ? err.message : String(err),
        });
      });
    return () => {
      alive = false;
    };
  }, [itemId, loadServerSummary, loadEdges, reloadToken]);

  React.useEffect(() => {
    setRemoveError(null);
    setAddError(null);
    setMoveError(null);
    setRemovedNotice(null);
    setAddedNotice(false);
    setMovedNotice(false);
    setOpenError(null);
    setOpenedNotice(false);
    setOpenPrompt(null);
    setTemplateRequest(null);
    setTemplateNotice(false);
    setTemplateError(null);
    setPickedTemplate("");
    setSlotMoveRequest(null);
    setSlotMoveNotice(false);
    setSlotMoveError(null);
    setPickedSlot("");
    setSlotChoices([]);
    setPageSlots([]);
  }, [itemId]);

  function statusOf(err: unknown): number | undefined {
    if (err && typeof err === "object" && "status" in err) {
      const status = (err as { status: unknown }).status;
      return typeof status === "number" ? status : undefined;
    }
    return undefined;
  }

  function removeFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_409);
    return err instanceof Error ? err.message : String(err);
  }

  function addFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_ADD_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_ADD_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_ADD_FAILED_409);
    return err instanceof Error ? err.message : String(err);
  }

  function moveFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_FAILED_409);
    return err instanceof Error ? err.message : String(err);
  }

  function templateFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_FAILED_409);
    return err instanceof Error
      ? err.message
      : message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_FAILED);
  }

  function templateGateMessage(reason: RelationshipTemplateBlock): string {
    if (reason === "folder") return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_FOLDER);
    if (reason === "not_assembly") {
      return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_NOT_ASSEMBLY);
    }
    if (reason === "no_slot") return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_NO_SLOT);
    if (reason === "same") return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_SAME);
    if (reason === "not_allowed") {
      return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_NOT_ALLOWED);
    }
    return message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_NEEDS);
  }

  function slotMoveFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FAILED_409);
    return err instanceof Error
      ? err.message
      : message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FAILED);
  }

  function slotMoveGateMessage(reason: RelationshipSlotMoveBlock): string {
    if (reason === "folder") return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FOLDER);
    if (reason === "not_assembly") {
      return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NOT_ASSEMBLY);
    }
    if (reason === "no_slot") return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NO_SLOT);
    if (reason === "no_template") {
      return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NO_TEMPLATE);
    }
    if (reason === "same") return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_SAME);
    if (reason === "not_allowed") {
      return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NOT_ALLOWED);
    }
    return message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NEEDS);
  }

  function slotLabelFor(slotId: number): string {
    const found = pageSlots.find((slot) => slot.slotId === slotId);
    return found ? slotChoiceLabel(found) : String(slotId);
  }

  function openFailureMessage(result: OpenRelatedItemResult): string {
    if (result.reason === "forbidden") {
      return message(EXPLORER_MSG.RELATIONSHIPS_OPEN_FORBIDDEN);
    }
    if (result.reason === "not_found") {
      return message(EXPLORER_MSG.RELATIONSHIPS_OPEN_NOT_FOUND);
    }
    if (result.reason === "folder") {
      return message(EXPLORER_MSG.RELATIONSHIPS_OPEN_FOLDER);
    }
    if (result.reason === "no_content") {
      return message(EXPLORER_MSG.RELATIONSHIPS_OPEN_NO_CONTENT);
    }
    const text = message(EXPLORER_MSG.RELATIONSHIPS_OPEN_FAILED);
    return result.detail ? `${text} ${result.detail}` : text;
  }

  function openMove(relationshipId: number, direction: "UP" | "DOWN"): void {
    setRemovedNotice(null);
    setRemoveError(null);
    setMovedNotice(false);
    setMoveError(null);
    setOpenedNotice(false);
    setOpenError(null);
    setOpenPrompt(null);
    setConfirmId(null);
    setConfirmAll(false);
    setMoveRequest({ id: relationshipId, direction });
  }

  function beginOpen(edge: PSExplorerRelationshipEdge): void {
    const target = relatedItemOpenTarget(edge);
    setRemovedNotice(null);
    setRemoveError(null);
    setMovedNotice(false);
    setMoveError(null);
    setAddedNotice(false);
    setAddError(null);
    setMoveRequest(null);
    setConfirmId(null);
    setConfirmAll(false);
    setAddOpen(false);
    setOpenedNotice(false);
    if (target.kind !== "content") {
      setOpenPrompt(null);
      setOpenError(
        message(
          target.kind === "folder"
            ? EXPLORER_MSG.RELATIONSHIPS_OPEN_FOLDER
            : EXPLORER_MSG.RELATIONSHIPS_OPEN_NO_CONTENT,
        ),
      );
      return;
    }
    setOpenError(null);
    setOpenPrompt({
      relationshipId: edge.relationshipId,
      contentId: target.contentId,
      label: edge.label,
    });
  }

  async function confirmOpen(): Promise<void> {
    if (!itemId || opening || openPrompt == null) return;
    const edge = edges.find(
      (candidate) => candidate.relationshipId === openPrompt.relationshipId,
    );
    const target = edge
      ? relatedItemOpenTarget(edge)
      : { kind: "no_content" as const };
    if (target.kind !== "content") {
      setOpenPrompt(null);
      setOpenedNotice(false);
      setOpenError(
        message(
          target.kind === "folder"
            ? EXPLORER_MSG.RELATIONSHIPS_OPEN_FOLDER
            : EXPLORER_MSG.RELATIONSHIPS_OPEN_NO_CONTENT,
        ),
      );
      return;
    }
    if (target.contentId !== openPrompt.contentId) {
      setOpenPrompt(null);
      setOpenedNotice(false);
      setOpenError(message(EXPLORER_MSG.RELATIONSHIPS_OPEN_NO_CONTENT));
      return;
    }
    const contentId = target.contentId;
    setOpening(true);
    setOpenError(null);
    setOpenedNotice(false);
    const reserved = reserveRelatedWindow();
    try {
      const result = await openRelated(
        { contentId, folder: false },
        { reservedWindow: reserved },
      );
      setOpenPrompt(null);
      if (result.ok) {
        setOpenError(null);
        setOpenedNotice(true);
        return;
      }
      closeReservedWindow(reserved);
      setOpenedNotice(false);
      setOpenError(openFailureMessage(result));
    } catch (err: unknown) {
      closeReservedWindow(reserved);
      setOpenPrompt(null);
      setOpenedNotice(false);
      setOpenError(
        openFailureMessage({
          ok: false,
          reason: "failed",
          detail: err instanceof Error ? err.message : String(err),
        }),
      );
    } finally {
      setOpening(false);
    }
  }

  async function confirmMove(): Promise<void> {
    if (!itemId || moving || moveRequest == null) return;
    const edge = edges.find(
      (candidate) => candidate.relationshipId === moveRequest.id,
    );
    const ends = edge ? relationshipMoveEnds(edges, edge) : { up: false, down: false };
    if (
      !edge ||
      (moveRequest.direction === "UP" && !ends.up) ||
      (moveRequest.direction === "DOWN" && !ends.down)
    ) {
      setMoveRequest(null);
      return;
    }
    const direction = moveRequest.direction;
    const relationshipId = moveRequest.id;
    setMoving(true);
    setMoveError(null);
    setMovedNotice(false);
    try {
      await moveEdge(relationshipId, direction);
      setMoveRequest(null);
      setMovedNotice(true);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      setMovedNotice(false);
      setMoveError(moveFailureMessage(err));
      setMoveRequest(null);
    } finally {
      setMoving(false);
    }
  }

  function clearTemplateChrome(): void {
    setTemplateNotice(false);
    setTemplateError(null);
  }

  async function openTemplate(edge: PSExplorerRelationshipEdge): Promise<void> {
    if (!canChangeRelationshipSnippetTemplate(edge)) {
      setTemplateRequest(null);
      setTemplateNotice(false);
      setTemplateError(message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_NOT_ASSEMBLY));
      return;
    }
    setRemovedNotice(null);
    setRemoveError(null);
    setMovedNotice(false);
    setMoveError(null);
    setOpenedNotice(false);
    setOpenError(null);
    setOpenPrompt(null);
    setAddedNotice(false);
    setAddError(null);
    setConfirmId(null);
    setConfirmAll(false);
    setMoveRequest(null);
    setAddOpen(false);
    setSlotMoveRequest(null);
    setSlotMoveNotice(false);
    setSlotMoveError(null);
    clearTemplateChrome();
    setPickedTemplate("");
    setTemplateChoices([]);
    setTemplateRequest(edge.relationshipId);
    try {
      const choices = await loadAllowedTemplates(Number(edge.slotId));
      setTemplateChoices(choices);
    } catch (err: unknown) {
      setTemplateNotice(false);
      setTemplateError(templateFailureMessage(err));
    }
  }

  async function confirmTemplate(): Promise<void> {
    if (!itemId || templateBusy || templateRequest == null) return;
    const edge = edges.find(
      (candidate) => candidate.relationshipId === templateRequest,
    );
    const gate = gateRelationshipSnippetTemplate({
      edge,
      templateId: Number(pickedTemplate),
      allowedIds: templateChoices.map((choice) => choice.id),
    });
    if (!gate.ok) {
      setTemplateNotice(false);
      setTemplateError(templateGateMessage(gate.reason));
      return;
    }
    const choice = templateChoices.find((row) => row.id === gate.templateId);
    const templateName = (choice?.label || choice?.name || "").trim();
    setTemplateBusy(true);
    clearTemplateChrome();
    try {
      const updated = await changeTemplate(
        gate.relationshipId,
        gate.slotId,
        gate.templateId,
        gate.index,
      );
      setEdges((current) =>
        applyRelationshipSnippetTemplate(
          current,
          gate.relationshipId,
          {
            relationshipId: updated.relationshipId,
            slotId: gate.slotId,
            templateId:
              updated.templateId > 0 ? updated.templateId : gate.templateId,
          },
          templateName,
        ),
      );
      setTemplateRequest(null);
      setTemplateError(null);
      setTemplateNotice(true);
    } catch (err: unknown) {
      setTemplateNotice(false);
      setTemplateError(templateFailureMessage(err));
    } finally {
      setTemplateBusy(false);
    }
  }

  async function openSlotMove(edge: PSExplorerRelationshipEdge): Promise<void> {
    if (!canMoveRelationshipToAnotherSlot(edge)) {
      setSlotMoveRequest(null);
      setSlotMoveNotice(false);
      setSlotMoveError(message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NOT_ASSEMBLY));
      return;
    }
    setRemovedNotice(null);
    setRemoveError(null);
    setMovedNotice(false);
    setMoveError(null);
    setOpenedNotice(false);
    setOpenError(null);
    setOpenPrompt(null);
    setAddedNotice(false);
    setAddError(null);
    setConfirmId(null);
    setConfirmAll(false);
    setMoveRequest(null);
    setAddOpen(false);
    setTemplateRequest(null);
    setTemplateNotice(false);
    setTemplateError(null);
    setSlotMoveNotice(false);
    setSlotMoveError(null);
    setPickedSlot("");
    setSlotChoices([]);
    setSlotMoveRequest(edge.relationshipId);
    const ownerId = Number(itemId);
    if (!(ownerId > 0)) {
      setSlotMoveError(message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_NO_SLOT));
      return;
    }
    try {
      const slots = await loadPageSlots(ownerId);
      setPageSlots(slots);
      setSlotChoices(destinationSlotChoices(Number(edge.slotId), slots));
    } catch (err: unknown) {
      setSlotMoveNotice(false);
      setSlotMoveError(slotMoveFailureMessage(err));
    }
  }

  async function confirmSlotMove(): Promise<void> {
    if (!itemId || slotMoveBusy || slotMoveRequest == null) return;
    const edge = edges.find(
      (candidate) => candidate.relationshipId === slotMoveRequest,
    );
    const gate = gateMoveRelationshipToSlot({
      edge,
      destinationSlotId: Number(pickedSlot),
      pageSlotIds: pageSlots.map((slot) => slot.slotId),
    });
    if (!gate.ok) {
      setSlotMoveNotice(false);
      setSlotMoveError(slotMoveGateMessage(gate.reason));
      return;
    }
    setSlotMoveBusy(true);
    setSlotMoveNotice(false);
    setSlotMoveError(null);
    try {
      const updated = await moveToSlot(
        gate.relationshipId,
        gate.destinationSlotId,
        gate.templateId,
      );
      if (updated.slotId !== gate.destinationSlotId) {
        setSlotMoveNotice(false);
        setSlotMoveError(message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_FAILED));
        return;
      }
      setEdges((current) =>
        applyRelationshipSlotMove(current, gate.relationshipId, {
          relationshipId: updated.relationshipId,
          slotId: updated.slotId,
          templateId:
            updated.templateId > 0 ? updated.templateId : gate.templateId,
          sortRank: updated.sortRank,
        }),
      );
      setSlotMoveRequest(null);
      setSlotMoveError(null);
      setSlotMoveNotice(true);
    } catch (err: unknown) {
      setSlotMoveNotice(false);
      setSlotMoveError(slotMoveFailureMessage(err));
    } finally {
      setSlotMoveBusy(false);
    }
  }

  async function confirmAdd(): Promise<void> {
    if (!itemId || adding) return;
    const target = addTarget.trim();
    const typeName = addType.trim();
    if (!target || !typeName || isFolderRelationshipCategory(typeName)) {
      setAddedNotice(false);
      setAddError(
        isFolderRelationshipCategory(typeName)
          ? message(EXPLORER_MSG.RELATIONSHIPS_ADD_FAILED_409)
          : message(EXPLORER_MSG.RELATIONSHIPS_ADD_FAILED_400),
      );
      return;
    }
    setAdding(true);
    setAddError(null);
    setAddedNotice(false);
    try {
      await addEdge(itemId, target, typeName);
      setAddOpen(false);
      setAddTarget("");
      setAddedNotice(true);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      setAddedNotice(false);
      setAddError(addFailureMessage(err));
    } finally {
      setAdding(false);
    }
  }

  async function confirmRemove(relationshipId: number): Promise<void> {
    if (!itemId || removing) return;
    setRemoving(true);
    setRemoveError(null);
    setRemovedNotice(null);
    try {
      await removeEdge(itemId, relationshipId);
      setConfirmId(null);
      setRemovedNotice("one");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      setRemovedNotice(null);
      setRemoveError(removeFailureMessage(err));
      setConfirmId(null);
    } finally {
      setRemoving(false);
    }
  }

  async function confirmRemoveAll(): Promise<void> {
    if (!itemId || removing) return;
    const targets = removableOwnedEdges(edges);
    if (targets.length === 0) {
      setConfirmAll(false);
      return;
    }
    setRemoving(true);
    setRemoveError(null);
    setRemovedNotice(null);
    try {
      await removeAllOwnedRelationshipEdges(itemId, edges, removeEdge);
      setConfirmAll(false);
      setRemovedNotice("all");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      setRemovedNotice(null);
      setRemoveError(removeFailureMessage(err));
      setConfirmAll(false);
      setReloadToken((n) => n + 1);
    } finally {
      setRemoving(false);
    }
  }

  if (state.kind === "loading") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
        data-testid-state="loading"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p aria-live="polite">{message(EXPLORER_MSG.RELATIONSHIPS_LOADING)}</p>
      </section>
    );
  }
  if (state.kind === "auth") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
        data-testid-state="auth"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="status" aria-live="polite">
          {message(EXPLORER_MSG.PERMISSION_DENIED)}
        </p>
      </section>
    );
  }
  if (state.kind === "error") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
        data-testid-state="error"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="alert">
          {message(EXPLORER_MSG.RELATIONSHIPS_ERROR)}: {state.message}
        </p>
      </section>
    );
  }

  const summary: NodeRelationshipSummary = summarise(
    item,
    state.summary,
    aaLinkCount,
  );
  const primary = summary.dimensions.filter((d: RelationshipSummary) =>
    IA_PRIMARY.some((k) => k === d.dimension),
  );
  const aaRow = summary.dimensions.find(
    (d: RelationshipSummary) => d.dimension === "aa",
  );
  const reverseRow = summary.dimensions.find(
    (d: RelationshipSummary) => d.dimension === "reverse",
  );

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
      data-testid="relationships-view"
      data-testid-state="ok"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}:{" "}
        <code style={{ fontSize: "0.85rem" }}>
          {summary.nodePath ?? summary.nodeId}
        </code>
      </h2>
      <ul
        data-testid="relationships-primary"
        style={{ listStyle: "none", padding: 0, margin: 0 }}
      >
        {primary.map((d: RelationshipSummary) => (
          <li
            key={d.dimension}
            data-testid={`relationships-row-${d.dimension}`}
            style={{
              display: "flex",
              justifyContent: "space-between",
              padding: "4px 0",
              borderBottom: "1px solid #eee",
              color: d.unknown ? "#888" : "#222",
            }}
          >
            <span>{labelFor(d.dimension)}</span>
            <span>{d.unknown ? "\u2014" : (d.label ?? `${d.count}`)}</span>
          </li>
        ))}
      </ul>
      <div data-testid="relationships-add-section" style={{ marginTop: 12 }}>
        <h3 style={{ fontSize: "0.95rem", margin: "0 0 8px 0" }}>
          {message(EXPLORER_MSG.RELATIONSHIPS_ADD)}
        </h3>
        {addedNotice ? (
          <p role="status" data-testid="relationships-added">
            {message(EXPLORER_MSG.RELATIONSHIPS_ADDED)}
          </p>
        ) : null}
        {addError ? (
          <p role="alert" data-testid="relationships-add-error">
            {addError}
          </p>
        ) : null}
        <button
          type="button"
          data-testid="relationships-add"
          onClick={() => {
            setAddedNotice(false);
            setAddError(null);
            setOpenedNotice(false);
            setOpenError(null);
            setOpenPrompt(null);
            setAddOpen(true);
          }}
        >
          {message(EXPLORER_MSG.RELATIONSHIPS_ADD)}
        </button>
        {addOpen ? (
          <div
            role="dialog"
            aria-modal="true"
            data-testid="relationships-add-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <label>
              {message(EXPLORER_MSG.RELATIONSHIPS_ADD_TARGET)}
              <input
                data-testid="relationships-add-target"
                value={addTarget}
                onChange={(event) => setAddTarget(event.target.value)}
              />
            </label>
            <label>
              {message(EXPLORER_MSG.RELATIONSHIPS_ADD_TYPE)}
              <input
                data-testid="relationships-add-type"
                value={addType}
                onChange={(event) => setAddType(event.target.value)}
              />
            </label>
            <button
              type="button"
              data-testid="relationships-add-cancel"
              onClick={() => setAddOpen(false)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-add-confirm"
              disabled={adding}
              onClick={() => {
                void confirmAdd();
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_ADD_DO)}
            </button>
          </div>
        ) : null}
      </div>
      <div data-testid="relationships-remove-section" style={{ marginTop: 12 }}>
        <h3 style={{ fontSize: "0.95rem", margin: "0 0 8px 0" }}>
          {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_DO)}
        </h3>
        {removedNotice ? (
          <p
            role="status"
            data-testid={
              removedNotice === "all"
                ? "relationships-removed-all"
                : "relationships-removed"
            }
          >
            {message(
              removedNotice === "all"
                ? EXPLORER_MSG.RELATIONSHIPS_REMOVED_ALL
                : EXPLORER_MSG.RELATIONSHIPS_REMOVED,
            )}
          </p>
        ) : null}
        {removeError ? (
          <p role="alert" data-testid="relationships-remove-error">
            {removeError}
          </p>
        ) : null}
        {movedNotice ? (
          <p role="status" data-testid="relationships-moved">
            {message(EXPLORER_MSG.RELATIONSHIPS_MOVED)}
          </p>
        ) : null}
        {moveError ? (
          <p role="alert" data-testid="relationships-move-error">
            {moveError}
          </p>
        ) : null}
        {openedNotice ? (
          <p role="status" data-testid="relationships-opened">
            {message(EXPLORER_MSG.RELATIONSHIPS_OPENED)}
          </p>
        ) : null}
        {openError ? (
          <p role="alert" data-testid="relationships-open-error">
            {openError}
          </p>
        ) : null}
        {templateNotice ? (
          <p role="status" data-testid="relationships-template-changed">
            {message(EXPLORER_MSG.RELATIONSHIPS_TEMPLATE_CHANGED)}
          </p>
        ) : null}
        {templateError ? (
          <p role="alert" data-testid="relationships-template-error">
            {templateError}
          </p>
        ) : null}
        {slotMoveNotice ? (
          <p role="status" data-testid="relationships-slot-moved">
            {message(EXPLORER_MSG.RELATIONSHIPS_SLOT_MOVED)}
          </p>
        ) : null}
        {slotMoveError ? (
          <p role="alert" data-testid="relationships-slot-move-error">
            {slotMoveError}
          </p>
        ) : null}
        {removableOwnedEdges(edges).length > 0 ? (
          <p>
            <button
              type="button"
              data-testid="relationships-remove-all"
              onClick={() => {
                setRemovedNotice(null);
                setRemoveError(null);
                setOpenedNotice(false);
                setOpenError(null);
                setOpenPrompt(null);
                setConfirmId(null);
                setConfirmAll(true);
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_ALL)}
            </button>
          </p>
        ) : null}
        {edges.length === 0 ? (
          <p data-testid="relationships-remove-empty">
            {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_EMPTY)}
          </p>
        ) : (
          <ul
            data-testid="relationships-edge-list"
            style={{ listStyle: "none", padding: 0, margin: 0 }}
          >
            {(() => {
              const grouped = groupActiveAssemblyBySlot(edges);
              const renderEdge = (edge: PSExplorerRelationshipEdge) => {
              const ends = relationshipMoveEnds(edges, edge);
              const openTarget = relatedItemOpenTarget(edge);
              const templateText = relationshipTemplateLabel(edge);
              return (
              <li
                key={edge.relationshipId}
                data-testid={`relationships-edge-${edge.relationshipId}`}
                data-slot-id={String(Number(edge.slotId ?? 0))}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 8,
                  padding: "4px 0",
                }}
              >
                <span>
                  {edge.label}
                  {Number(edge.slotId) > 0 &&
                  canMoveRelationshipToAnotherSlot(edge) ? (
                    <span
                      data-testid={`relationships-slot-${edge.relationshipId}`}
                      data-slot-id={String(edge.slotId)}
                    >
                      {" "}
                      {message(EXPLORER_MSG.RELATIONSHIPS_SLOT_LABEL)}{" "}
                      {slotLabelFor(Number(edge.slotId))}
                    </span>
                  ) : null}
                  {templateText ? (
                    <span
                      data-testid={`relationships-template-${edge.relationshipId}`}
                      data-template-id={String(edge.templateId ?? 0)}
                      data-template-name={edge.templateName ?? ""}
                    >
                      {" "}
                      {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE_LABEL)}{" "}
                      {templateText}
                    </span>
                  ) : null}
                </span>
                <span style={{ display: "flex", gap: 8 }}>
                  {canChangeRelationshipSnippetTemplate(edge) ? (
                    <button
                      type="button"
                      data-testid={`relationships-change-template-${edge.relationshipId}`}
                      onClick={() => {
                        void openTemplate(edge);
                      }}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE)}
                    </button>
                  ) : null}
                  {canMoveRelationshipToAnotherSlot(edge) ? (
                    <button
                      type="button"
                      data-testid={`relationships-move-slot-${edge.relationshipId}`}
                      onClick={() => {
                        void openSlotMove(edge);
                      }}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT)}
                    </button>
                  ) : null}
                  {openTarget.kind === "content" ? (
                    <button
                      type="button"
                      data-testid={`relationships-open-${edge.relationshipId}`}
                      data-content-id={String(openTarget.contentId)}
                      onClick={() => beginOpen(edge)}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_OPEN)}
                    </button>
                  ) : null}
                  {ends.up ? (
                    <button
                      type="button"
                      data-testid={`relationships-move-up-${edge.relationshipId}`}
                      onClick={() => openMove(edge.relationshipId, "UP")}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_UP)}
                    </button>
                  ) : null}
                  {ends.down ? (
                    <button
                      type="button"
                      data-testid={`relationships-move-down-${edge.relationshipId}`}
                      onClick={() => openMove(edge.relationshipId, "DOWN")}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_DOWN)}
                    </button>
                  ) : null}
                  {removableOwnedEdges([edge]).length === 0 ? (
                    <span data-testid={`relationships-folder-${edge.relationshipId}`}>
                      {edge.category}
                    </span>
                  ) : (
                    <button
                      type="button"
                      data-testid={`relationships-remove-${edge.relationshipId}`}
                      onClick={() => {
                        setRemovedNotice(null);
                        setRemoveError(null);
                        setMovedNotice(false);
                        setMoveError(null);
                        setOpenedNotice(false);
                        setOpenError(null);
                        setOpenPrompt(null);
                        setMoveRequest(null);
                        setConfirmAll(false);
                        setConfirmId(edge.relationshipId);
                      }}
                    >
                      {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE)}
                    </button>
                  )}
                </span>
              </li>
              );
              };
              return (
                <>
                  {grouped.groups.map((group) => (
                    <li
                      key={`slot-${group.slotId}`}
                      data-testid={`relationships-slot-group-${group.slotId}`}
                      data-slot-id={String(group.slotId)}
                    >
                      <span>
                        {message(EXPLORER_MSG.RELATIONSHIPS_SLOT_LABEL)}{" "}
                        {slotLabelFor(group.slotId)}
                      </span>
                      <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                        {group.edges.map((edge) => renderEdge(edge))}
                      </ul>
                    </li>
                  ))}
                  {grouped.rest.map((edge) => renderEdge(edge))}
                </>
              );
            })()}
          </ul>
        )}
        {confirmAll ? (
          <div
            role="dialog"
            aria-modal="true"
            data-testid="relationships-remove-all-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <p>{message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_ALL_CONFIRM)}</p>
            <button
              type="button"
              data-testid="relationships-remove-all-cancel"
              onClick={() => setConfirmAll(false)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-remove-all-confirm"
              disabled={removing}
              onClick={() => {
                void confirmRemoveAll();
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_ALL)}
            </button>
          </div>
        ) : null}
        {templateRequest != null ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="relationships-template-title"
            data-testid="relationships-template-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <h3 id="relationships-template-title" style={{ fontSize: "1rem" }}>
              {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE_TITLE)}
            </h3>
            <label htmlFor="relationships-template-select">
              {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE_LABEL)}
            </label>
            <select
              id="relationships-template-select"
              data-testid="relationships-template-select"
              value={pickedTemplate}
              onChange={(event) => setPickedTemplate(event.target.value)}
            >
              <option value="">
                {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE_PLACEHOLDER)}
              </option>
              {templateChoices.map((choice) => (
                <option key={choice.id} value={String(choice.id)}>
                  {choice.label || choice.name || String(choice.id)}
                </option>
              ))}
            </select>
            <button
              type="button"
              data-testid="relationships-template-cancel"
              onClick={() => setTemplateRequest(null)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-template-confirm"
              disabled={templateBusy}
              onClick={() => {
                void confirmTemplate();
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_CHANGE_TEMPLATE_DO)}
            </button>
          </div>
        ) : null}
        {slotMoveRequest != null ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="relationships-slot-move-title"
            data-testid="relationships-slot-move-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <h3 id="relationships-slot-move-title" style={{ fontSize: "1rem" }}>
              {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_TITLE)}
            </h3>
            <label htmlFor="relationships-slot-move-select">
              {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_LABEL)}
            </label>
            <select
              id="relationships-slot-move-select"
              data-testid="relationships-slot-move-select"
              value={pickedSlot}
              onChange={(event) => setPickedSlot(event.target.value)}
            >
              <option value="">
                {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_PLACEHOLDER)}
              </option>
              {slotChoices.map((choice) => (
                <option key={choice.slotId} value={String(choice.slotId)}>
                  {choice.label}
                </option>
              ))}
            </select>
            <button
              type="button"
              data-testid="relationships-slot-move-cancel"
              onClick={() => setSlotMoveRequest(null)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-slot-move-confirm"
              disabled={slotMoveBusy}
              onClick={() => {
                void confirmSlotMove();
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_SLOT_DO)}
            </button>
          </div>
        ) : null}
        {moveRequest != null ? (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="relationships-move-prompt"
            data-testid="relationships-move-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <p id="relationships-move-prompt">
              {message(
                moveRequest.direction === "UP"
                  ? EXPLORER_MSG.RELATIONSHIPS_MOVE_UP_CONFIRM
                  : EXPLORER_MSG.RELATIONSHIPS_MOVE_DOWN_CONFIRM,
              )}
            </p>
            <button
              type="button"
              data-testid="relationships-move-cancel"
              onClick={() => setMoveRequest(null)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-move-confirm"
              disabled={moving}
              onClick={() => {
                void confirmMove();
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_MOVE_DO)}
            </button>
          </div>
        ) : null}
        {openPrompt != null ? (
          <OpenRelatedItemDialog
            itemLabel={openPrompt.label || String(openPrompt.contentId)}
            busy={opening}
            onCancel={() => {
              if (opening) {
                return;
              }
              setOpenPrompt(null);
              setOpenedNotice(false);
              setOpenError(null);
            }}
            onConfirm={() => {
              void confirmOpen();
            }}
          />
        ) : null}
        {confirmId != null ? (
          <div
            role="dialog"
            aria-modal="true"
            data-testid="relationships-remove-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <p>{message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CONFIRM)}</p>
            <button
              type="button"
              data-testid="relationships-remove-cancel"
              onClick={() => setConfirmId(null)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-remove-confirm"
              disabled={removing}
              onClick={() => {
                void confirmRemove(confirmId);
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_DO)}
            </button>
          </div>
        ) : null}
      </div>
      <details style={{ marginTop: 12 }}>
        <summary>Supplementary links</summary>
        <ul
          style={{ listStyle: "none", padding: 0 }}
          data-testid="relationships-extra"
        >
          {aaRow ? (
            <li data-testid="relationships-row-aa">
              {labelFor("aa")}:{" "}
              {aaRow.unknown ? "\u2014" : (aaRow.label ?? `${aaRow.count}`)}
            </li>
          ) : null}
          {reverseRow ? (
            <li data-testid="relationships-row-reverse">
              {labelFor("reverse")}:{" "}
              {reverseRow.unknown
                ? "\u2014"
                : (reverseRow.label ?? `${reverseRow.count}`)}
            </li>
          ) : null}
        </ul>
      </details>
    </section>
  );
}
