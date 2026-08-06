/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useMemo, useState } from "react";
import {
  GADGET_CATALOG,
  loadPreferredGadgetIds,
  savePreferredGadgetIds,
} from "./gadgetsCatalog";
import { message, MSG } from "../i18n/message";
import { styles } from "./dashboard.styles";

export interface WidgetConfigurationWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Widget Configuration** / dashboard configuration gadget.
 * Session-local preferred gadget set; Dashboard listens and rebuilds layout.
 */
export const WidgetConfigurationWidget: React.FC<
  WidgetConfigurationWidgetProps
> = ({ title }) => {
  const initial = useMemo(() => {
    const saved = loadPreferredGadgetIds();
    if (saved && saved.length > 0) return new Set(saved);
    // Sensible default if nothing saved yet
    return new Set(
      GADGET_CATALOG.filter((g) =>
        [
          "welcome",
          "blogs",
          "workflow",
          "activity",
          "assets-status",
          "process-monitor",
          "google-setup",
          "traffic",
          "effectiveness",
        ].includes(g.id),
      ).map((g) => g.id),
    );
  }, []);

  const [selected, setSelected] = useState<Set<string>>(initial);
  const [message_, setMessage] = useState<string | null>(null);

  const toggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const apply = () => {
    const ids = GADGET_CATALOG.map((g) => g.id).filter((id) => selected.has(id));
    if (ids.length === 0) {
      setMessage(message(MSG.WIDGET_CONFIG_EMPTY));
      return;
    }
    savePreferredGadgetIds(ids);
    setMessage(message(MSG.WIDGET_CONFIG_APPLIED));
  };

  const byCategory = useMemo(() => {
    const map = new Map<string, typeof GADGET_CATALOG>();
    for (const g of GADGET_CATALOG) {
      const list = map.get(g.category) || [];
      list.push(g);
      map.set(g.category, list);
    }
    return map;
  }, []);

  return (
    <div style={styles.widget} data-testid="widget-configuration-widget">
      <div style={styles.widgetTitle}>{title ?? message(MSG.GADGET_DASHBOARD_CONFIG)}</div>
      <div style={styles.widgetContent}>
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          {message(MSG.WIDGET_CONFIG_HINT)}
        </p>
        {message_ ? (
          <p
            data-testid="widget-configuration-message"
            style={{ fontSize: "0.85em", color: "#2e7d32" }}
          >
            {message_}
          </p>
        ) : null}
        <div
          style={{ maxHeight: 320, overflowY: "auto" }}
          data-testid="widget-configuration-list"
        >
          {[...byCategory.entries()].map(([cat, items]) => (
            <div key={cat} style={{ marginBottom: 12 }}>
              <div style={{ fontWeight: 600, fontSize: "0.85em", marginBottom: 6 }}>
                {cat}
              </div>
              {items.map((g) => (
                <label
                  key={g.id}
                  style={{
                    display: "flex",
                    gap: 8,
                    alignItems: "flex-start",
                    fontSize: "0.85em",
                    marginBottom: 6,
                  }}
                >
                  <input
                    type="checkbox"
                    checked={selected.has(g.id)}
                    onChange={() => toggle(g.id)}
                    data-testid={`widget-config-cb-${g.id}`}
                  />
                  <span>
                    <strong>{message(g.nameKey)}</strong>
                    <div style={{ color: "#666", fontSize: "0.9em" }}>
                      {message(g.descriptionKey)}
                    </div>
                  </span>
                </label>
              ))}
            </div>
          ))}
        </div>
        <button
          type="button"
          onClick={apply}
          data-testid="widget-configuration-apply"
          style={{ padding: "8px 14px", marginTop: 8 }}
        >
          {message(MSG.WIDGET_CONFIG_APPLY)}
        </button>
      </div>
    </div>
  );
};

export default WidgetConfigurationWidget;
