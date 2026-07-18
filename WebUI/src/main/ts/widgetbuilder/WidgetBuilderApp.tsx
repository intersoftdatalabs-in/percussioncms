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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useState } from "react";
import {
  deleteDefinition,
  deployDefinition,
  fetchSummaries,
  isWidgetBuilderActive,
  loadDefinition,
  saveDefinition,
  validateDefinition,
} from "../api/widgetbuilder/widgetBuilderApi";
import { message } from "../i18n/message";
import { DefinitionList } from "./DefinitionList";
import { DefinitionEditor } from "./editor/DefinitionEditor";
import {
  emptyDefinition,
  extractValidationMessages,
  fromServer,
  toServerPayload,
  type WidgetDefinition,
  type WidgetSummary,
} from "./types";

const K = {
  TITLE: "perc.ui.widgetbuilder.modern@Title",
  DISABLED: "perc.ui.widgetbuilder.modern@Disabled",
  LOADING: "perc.ui.home.modern@Loading",
  ERROR: "perc.ui.home.modern@Error",
  SAVED: "perc.ui.widgetbuilder.modern@Saved",
  DEPLOYED: "perc.ui.widgetbuilder.modern@Deployed",
  VALID_OK: "perc.ui.widgetbuilder.modern@Valid",
  CONFIRM_DELETE: "perc.ui.widgetbuilder.modern@Confirm Delete",
};

type Mode = "list" | "edit";

function normalizeSummaries(raw: unknown[]): WidgetSummary[] {
  return raw.map((item) => {
    let o = item as Record<string, unknown>;
    if (o && "WidgetBuilderSummaryData" in o) {
      o = o.WidgetBuilderSummaryData as Record<string, unknown>;
    }
    return {
      widgetId: (o.widgetId as number) ?? o.id,
      label: String(o.label ?? ""),
      prefix: String(o.prefix ?? ""),
      version: String(o.version ?? ""),
      description: String(o.description ?? ""),
      author: String(o.author ?? ""),
    };
  });
}

export function WidgetBuilderApp(): React.ReactElement {
  const [active, setActive] = useState<boolean | null>(null);
  const [summaries, setSummaries] = useState<WidgetSummary[]>([]);
  const [mode, setMode] = useState<Mode>("list");
  const [draft, setDraft] = useState<WidgetDefinition>(emptyDefinition());
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState<string[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reloadList = useCallback(async () => {
    const list = await fetchSummaries();
    setSummaries(normalizeSummaries(list));
  }, []);

  useEffect(() => {
    isWidgetBuilderActive()
      .then(async (on) => {
        setActive(on);
        if (on) {
          await reloadList();
        }
      })
      .catch((err: unknown) => {
        // Surface status when possible (path/auth/JSON-parse issues)
        const status =
          err && typeof err === "object" && "status" in err
            ? String((err as { status: number }).status)
            : "";
        const detail = status ? ` (${status})` : "";
        console.error("[WidgetBuilder] enablement/list failed", err);
        setError(`${message(K.ERROR)}${detail}`);
      });
  }, [reloadList]);

  const openNew = () => {
    setDraft(emptyDefinition());
    setMessages([]);
    setStatus(null);
    setMode("edit");
  };

  const openEdit = async (id: number | string) => {
    setBusy(true);
    setError(null);
    try {
      const raw = await loadDefinition(id);
      setDraft(fromServer(raw));
      setMessages([]);
      setStatus(null);
      setMode("edit");
    } catch {
      setError(message(K.ERROR));
    } finally {
      setBusy(false);
    }
  };

  const onValidate = async () => {
    setBusy(true);
    setMessages([]);
    setStatus(null);
    try {
      const result = await validateDefinition(toServerPayload(draft));
      const msgs = extractValidationMessages(result);
      if (msgs.length === 0) {
        setStatus(message(K.VALID_OK));
      } else {
        setMessages(msgs);
      }
    } catch {
      setMessages([message(K.ERROR)]);
    } finally {
      setBusy(false);
    }
  };

  const onSave = async () => {
    setBusy(true);
    setMessages([]);
    setStatus(null);
    try {
      const result = await saveDefinition(toServerPayload(draft));
      const msgs = extractValidationMessages(result);
      if (msgs.length > 0) {
        setMessages(msgs);
        return;
      }
      // last-write-wins: confirm and reload server truth
      const r = result as Record<string, unknown>;
      const nested =
        (r.WidgetBuilderValidationResults as Record<string, unknown>) || r;
      const defId = nested.definitionId ?? nested.DefinitionId;
      if (defId != null) {
        const reloaded = await loadDefinition(String(defId));
        setDraft(fromServer(reloaded));
      }
      setStatus(message(K.SAVED));
      await reloadList();
    } catch {
      setMessages([message(K.ERROR)]);
    } finally {
      setBusy(false);
    }
  };

  const onDelete = async (id: number | string) => {
    if (!window.confirm(message(K.CONFIRM_DELETE))) {
      return;
    }
    setBusy(true);
    try {
      await deleteDefinition(id);
      await reloadList();
    } catch {
      setError(message(K.ERROR));
    } finally {
      setBusy(false);
    }
  };

  const onDeploy = async (id: number | string) => {
    setBusy(true);
    setStatus(null);
    try {
      await deployDefinition(id);
      setStatus(message(K.DEPLOYED));
    } catch {
      setError(message(K.ERROR));
    } finally {
      setBusy(false);
    }
  };

  if (error) {
    return (
      <p role="alert" style={{ padding: 16 }}>
        {error}
      </p>
    );
  }
  if (active === null) {
    return (
      <p role="status" style={{ padding: 16 }}>
        {message(K.LOADING)}
      </p>
    );
  }
  if (!active) {
    return (
      <p style={{ padding: 16 }} data-testid="wb-disabled">
        {message(K.DISABLED)}
      </p>
    );
  }

  return (
    <div data-testid="widget-builder-app" style={{ padding: 16 }}>
      <h1 style={{ fontSize: "1.25rem" }}>{message(K.TITLE)}</h1>
      {mode === "list" ? (
        <DefinitionList
          summaries={summaries}
          onNew={openNew}
          onEdit={openEdit}
          onDelete={onDelete}
          onDeploy={onDeploy}
        />
      ) : (
        <DefinitionEditor
          value={draft}
          busy={busy}
          messages={messages}
          status={status}
          onChange={setDraft}
          onSave={onSave}
          onValidate={onValidate}
          onCancel={() => {
            setMode("list");
            setMessages([]);
            setStatus(null);
          }}
        />
      )}
      {mode === "list" && status && <p role="status">{status}</p>}
    </div>
  );
}
