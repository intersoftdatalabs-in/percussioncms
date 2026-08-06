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

import React, { useEffect, useState, useRef } from "react";
import { get } from "../api/client";
import { PATHS } from "../api/paths";
import { TransitionDialog } from "./TransitionDialog";

interface WorkflowActionsPanelProps {
  itemId: string;
  onStateChange?: () => void;
}

interface ItemUserInfo {
  itemName: string;
  checkOutUser: string;
  currentUser: string;
  assignmentType: string;
}

interface ItemStateTransition {
  itemId: string;
  stateId: string;
  stateName: string;
  workflowId: string;
  transitionTriggers: string[];
}

export const WorkflowActionsPanel: React.FC<WorkflowActionsPanelProps> = ({
  itemId,
  onStateChange,
}) => {
  const [userInfo, setUserInfo] = useState<ItemUserInfo | null>(null);
  const [stateTransition, setStateTransition] = useState<ItemStateTransition | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTrigger, setActiveTrigger] = useState<string | null>(null);
  
  const isMountedRef = useRef<boolean>(true);
  const activeIdRef = useRef<string>(itemId);

  const loadWorkflowData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // 1. Fetch checkout/user info
      const info = await get<ItemUserInfo>(`${PATHS.ITEM_WORKFLOW_CHECKOUT}${encodeURIComponent(itemId)}`);
      // 2. Fetch state/transition details
      const trans = await get<ItemStateTransition>(`${PATHS.ITEM_WORKFLOW_TRANSITIONS}${encodeURIComponent(itemId)}`);
      
      if (isMountedRef.current && activeIdRef.current === itemId) {
        setUserInfo(info);
        setStateTransition(trans);
      }
    } catch (err: any) {
      if (isMountedRef.current && activeIdRef.current === itemId) {
        setError(err?.message || "Failed to load workflow status.");
      }
    } finally {
      if (isMountedRef.current && activeIdRef.current === itemId) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    isMountedRef.current = true;
    activeIdRef.current = itemId;
    loadWorkflowData();
    return () => {
      isMountedRef.current = false;
    };
  }, [itemId]);

  const handleCheckIn = async () => {
    setError(null);
    try {
      await get(`${PATHS.ITEM_WORKFLOW_CHECKIN}${encodeURIComponent(itemId)}`);
      await loadWorkflowData();
      if (onStateChange) onStateChange();
    } catch (err: any) {
      setError(err?.message || "Check in failed.");
    }
  };

  const handleCheckOut = async (force = false) => {
    setError(null);
    try {
      const endpoint = force ? PATHS.ITEM_WORKFLOW_FORCE_CHECKOUT : PATHS.ITEM_WORKFLOW_CHECKOUT;
      await get<ItemUserInfo>(`${endpoint}${encodeURIComponent(itemId)}`);
      await loadWorkflowData();
      if (onStateChange) onStateChange();
    } catch (err: any) {
      setError(err?.message || "Check out failed.");
    }
  };

  const handleTransitionSubmit = async (comment: string, assignees: string[]) => {
    if (!activeTrigger) return;
    setError(null);
    setActiveTrigger(null);
    try {
      let url = `${PATHS.ITEM_WORKFLOW_TRANSITION_WITH_COMMENTS}${encodeURIComponent(itemId)}/${encodeURIComponent(activeTrigger)}`;
      const params: string[] = [];
      if (comment) {
        params.push(`comment=${encodeURIComponent(comment)}`);
      }
      if (assignees && assignees.length > 0) {
        params.push(`adhocAssignees=${encodeURIComponent(assignees.join(","))}`);
      }
      if (params.length > 0) {
        url += `?${params.join("&")}`;
      }
      await get(url);
      await loadWorkflowData();
      if (onStateChange) onStateChange();
    } catch (err: any) {
      setError(err?.message || "Transition failed.");
    }
  };

  if (isLoading) {
    return <div style={{ padding: "16px", color: "#64748b" }}>Loading workflow status...</div>;
  }

  const isCheckedOutByMe = userInfo && userInfo.checkOutUser === userInfo.currentUser;
  const isCheckedOutByOther = userInfo && userInfo.checkOutUser && !isCheckedOutByMe;

  return (
    <div
      className="perc-workflow-actions-panel"
      data-testid="perc-workflow-actions-panel"
      style={{
        background: "#ffffff",
        border: "1px solid #e2e8f0",
        borderRadius: "8px",
        padding: "16px",
        boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <div>
          <span style={{ fontSize: "12px", color: "#64748b", display: "block" }}>CURRENT STATE</span>
          <strong style={{ fontSize: "16px", color: "#0f172a" }} data-testid="workflow-state-name">
            {stateTransition?.stateName || "Draft"}
          </strong>
        </div>

        {/* Lock controls */}
        <div style={{ display: "flex", gap: "8px" }}>
          {isCheckedOutByMe && (
            <button type="button" onClick={handleCheckIn} className="perc-button-secondary" data-testid="checkin-button">
              Check In
            </button>
          )}
          {!isCheckedOutByMe && !isCheckedOutByOther && (
            <button type="button" onClick={() => handleCheckOut(false)} className="perc-button-primary" data-testid="checkout-button">
              Check Out
            </button>
          )}
          {isCheckedOutByOther && (
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <span style={{ fontSize: "12px", color: "#ef4444" }}>
                Locked by: {userInfo.checkOutUser}
              </span>
              <button type="button" onClick={() => handleCheckOut(true)} className="perc-button-secondary" style={{ color: "#ef4444", borderColor: "#fecaca" }} data-testid="force-checkout-button">
                Force Check Out
              </button>
            </div>
          )}
        </div>
      </div>

      {error && (
        <div style={{ color: "#d9534f", fontSize: "14px", marginBottom: "12px" }}>{error}</div>
      )}

      {/* Available transitions triggers list */}
      <div style={{ borderTop: "1px solid #f1f5f9", paddingTop: "16px" }}>
        <span style={{ fontSize: "12px", color: "#64748b", display: "block", marginBottom: "8px" }}>AVAILABLE ACTIONS</span>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
          {stateTransition?.transitionTriggers.length === 0 ? (
            <span style={{ fontSize: "13px", color: "#94a3b8" }}>No transitions available.</span>
          ) : (
            stateTransition?.transitionTriggers.map((trigger) => (
              <button
                key={trigger}
                type="button"
                onClick={() => setActiveTrigger(trigger)}
                disabled={Boolean(isCheckedOutByOther)}
                className="perc-button-secondary"
                data-testid={`transition-button-${trigger}`}
              >
                {trigger}
              </button>
            ))
          )}
        </div>
      </div>

      {activeTrigger && (
        <TransitionDialog
          trigger={activeTrigger}
          requiresComment={false} // Matches backend optionals
          supportsAdhocAssignees={true}
          onSubmit={handleTransitionSubmit}
          onCancel={() => setActiveTrigger(null)}
        />
      )}
    </div>
  );
};
