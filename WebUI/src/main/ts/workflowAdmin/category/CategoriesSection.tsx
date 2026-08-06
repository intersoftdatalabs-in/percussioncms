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
import { get, post } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";

export interface CategoryNode {
  id: string;
  title: string;
  selectable: boolean;
  childNodes: CategoryNode[];
  deleted?: boolean;
  createdBy?: string;
}

export interface CategoryTree {
  title: string;
  allowedSites?: string;
  topLevelNodes: CategoryNode[];
}

export interface LockInfo {
  userName: string;
  sessionId: string;
  sitename: string;
}

export const CategoriesSection: React.FC = () => {
  const [tree, setTree] = useState<CategoryTree | null>(null);
  const [lockInfo, setLockInfo] = useState<LockInfo | null>(null);
  const [currentUser, setCurrentUser] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({});
  const [addChildParentId, setAddChildParentId] = useState<string | null>(null);
  const [newCategoryTitle, setNewCategoryTitle] = useState<string>("");
  const [newCategorySelectable, setNewCategorySelectable] = useState<boolean>(true);

  const isMountedRef = useRef<boolean>(true);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // 1. Fetch Categories tree
      const res = await get<CategoryTree>(`${PATHS.CATEGORY_ALL}/undefined`);
      // 2. Fetch Lock info
      const lock = await get<LockInfo>(PATHS.CATEGORY_LOCK_INFO);
      
      if (isMountedRef.current) {
        setTree(res || { title: "Categories", topLevelNodes: [] });
        setLockInfo(lock);
        // Try to identify current user if lock has it, or default to current user session check
        if (lock && lock.userName) {
          setCurrentUser(lock.userName); // Fallback assumption
        }
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || message(WF_ADMIN_MSG.ERROR_GENERIC));
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    isMountedRef.current = true;
    loadData();
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleAcquireLock = async () => {
    setError(null);
    try {
      const dateStr = new Date().getTime().toString();
      await post(`${PATHS.CATEGORY_LOCK_TAB}${dateStr}`, {});
      const lock = await get<LockInfo>(PATHS.CATEGORY_LOCK_INFO);
      if (isMountedRef.current) {
        setLockInfo(lock);
      }
    } catch (err: any) {
      setError(err?.message || "Failed to acquire lock.");
    }
  };

  const handleReleaseLock = async () => {
    setError(null);
    try {
      await post(PATHS.CATEGORY_REMOVE_LOCK_TAB, {});
      const lock = await get<LockInfo>(PATHS.CATEGORY_LOCK_INFO);
      if (isMountedRef.current) {
        setLockInfo(lock);
      }
    } catch (err: any) {
      setError(err?.message || "Failed to release lock.");
    }
  };

  const saveTree = async (updatedTree: CategoryTree) => {
    setError(null);
    try {
      // API expects form param or string payload
      const payloadString = JSON.stringify(updatedTree);
      await post(`${PATHS.CATEGORY_UPDATE}/undefined`, payloadString, {
        "Content-Type": "application/json"
      });
      setTree(updatedTree);
    } catch (err: any) {
      setError(err?.message || "Failed to save category tree.");
    }
  };

  const toggleExpand = (id: string) => {
    setExpandedNodes((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  // Helper to find and mutate a node in the tree recursively
  const mutateNode = (
    nodes: CategoryNode[],
    targetId: string,
    action: (node: CategoryNode, parentList: CategoryNode[], index: number) => void
  ): boolean => {
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === targetId) {
        action(nodes[i], nodes, i);
        return true;
      }
      if (nodes[i].childNodes && nodes[i].childNodes.length > 0) {
        const found = mutateNode(nodes[i].childNodes, targetId, action);
        if (found) return true;
      }
    }
    return false;
  };

  const handleAddCategory = () => {
    if (!newCategoryTitle.trim()) {
      setError(message(WF_ADMIN_MSG.CATEGORY_NAME_REQUIRED));
      return;
    }

    const newId = `cat-${Math.random().toString(36).substr(2, 9)}`;
    const newNode: CategoryNode = {
      id: newId,
      title: newCategoryTitle.trim(),
      selectable: newCategorySelectable,
      childNodes: [],
      createdBy: currentUser || "admin",
    };

    if (!tree) return;
    const cloned = JSON.parse(JSON.stringify(tree)) as CategoryTree;

    if (addChildParentId === "root") {
      cloned.topLevelNodes.push(newNode);
    } else if (addChildParentId) {
      mutateNode(cloned.topLevelNodes, addChildParentId, (parent) => {
        parent.childNodes = parent.childNodes || [];
        parent.childNodes.push(newNode);
      });
    }

    saveTree(cloned);
    setAddChildParentId(null);
    setNewCategoryTitle("");
    setNewCategorySelectable(true);
  };

  const handleDeleteCategory = (id: string, title: string) => {
    if (!window.confirm(message(WF_ADMIN_MSG.CONFIRM_DELETE_CATEGORY).replace("{0}", title))) {
      return;
    }

    if (!tree) return;
    const cloned = JSON.parse(JSON.stringify(tree)) as CategoryTree;

    mutateNode(cloned.topLevelNodes, id, (node, parentList, index) => {
      parentList.splice(index, 1);
    });

    saveTree(cloned);
  };

  const handleReorder = (id: string, direction: "up" | "down") => {
    if (!tree) return;
    const cloned = JSON.parse(JSON.stringify(tree)) as CategoryTree;

    mutateNode(cloned.topLevelNodes, id, (node, parentList, index) => {
      const targetIndex = direction === "up" ? index - 1 : index + 1;
      if (targetIndex >= 0 && targetIndex < parentList.length) {
        const temp = parentList[index];
        parentList[index] = parentList[targetIndex];
        parentList[targetIndex] = temp;
      }
    });

    saveTree(cloned);
  };

  const isLockedByOther = lockInfo && lockInfo.userName && lockInfo.userName !== currentUser;

  const renderNode = (node: CategoryNode, depth = 0) => {
    const isExpanded = !!expandedNodes[node.id];
    const hasChildren = node.childNodes && node.childNodes.length > 0;
    const isSystemNode = !node.createdBy; // System nodes don't have createdBy metadata

    return (
      <div key={node.id} style={{ marginLeft: `${depth * 20}px`, marginTop: "8px" }} data-testid={`category-node-${node.id}`}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            padding: "8px 12px",
            background: "#f8fafc",
            border: "1px solid #e2e8f0",
            borderRadius: "6px",
          }}
        >
          {/* Collapse/Expand Toggle */}
          <button
            type="button"
            onClick={() => toggleExpand(node.id)}
            style={{
              visibility: hasChildren ? "visible" : "hidden",
              background: "none",
              border: "none",
              cursor: "pointer",
              fontSize: "14px",
              padding: "0 4px",
            }}
            data-testid={`expand-toggle-${node.id}`}
          >
            {isExpanded ? "▼" : "▶"}
          </button>

          {/* Node Icon */}
          <span style={{ fontSize: "16px" }}>📁</span>

          {/* Title */}
          <span style={{ fontWeight: 500, color: "#1e293b", flexGrow: 1 }}>
            {node.title} {!node.selectable && <span style={{ fontSize: "12px", color: "#94a3b8", fontStyle: "italic" }}>({message(WF_ADMIN_MSG.SELECTABLE)}: No)</span>}
          </span>

          {/* System Lock Indicator */}
          {isSystemNode && (
            <span title="System Category (Read-Only)" style={{ fontSize: "14px", cursor: "help" }} data-testid={`lock-indicator-${node.id}`}>
              🔒
            </span>
          )}

          {/* Actions */}
          {!isLockedByOther && (
            <div style={{ display: "flex", gap: "4px" }}>
              <button
                type="button"
                onClick={() => setAddChildParentId(node.id)}
                className="perc-button-secondary"
                style={{ padding: "2px 8px", fontSize: "12px" }}
                data-testid={`add-child-${node.id}`}
              >
                + Child
              </button>
              <button
                type="button"
                onClick={() => handleReorder(node.id, "up")}
                className="perc-button-secondary"
                style={{ padding: "2px 8px", fontSize: "12px" }}
                data-testid={`move-up-${node.id}`}
              >
                ▲
              </button>
              <button
                type="button"
                onClick={() => handleReorder(node.id, "down")}
                className="perc-button-secondary"
                style={{ padding: "2px 8px", fontSize: "12px" }}
                data-testid={`move-down-${node.id}`}
              >
                ▼
              </button>
              {!isSystemNode && (
                <button
                  type="button"
                  onClick={() => handleDeleteCategory(node.id, node.title)}
                  className="perc-button-secondary"
                  style={{ padding: "2px 8px", fontSize: "12px", color: "#ef4444", borderColor: "#fecaca" }}
                  data-testid={`delete-node-${node.id}`}
                >
                  Delete
                </button>
              )}
            </div>
          )}
        </div>

        {/* Children rendering */}
        {isExpanded && hasChildren && (
          <div style={{ marginTop: "4px" }}>
            {node.childNodes.map((child) => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  if (isLoading) {
    return <div style={{ padding: "20px" }}>{message(WF_ADMIN_MSG.LOADING)}</div>;
  }

  return (
    <div className="perc-categories-section" data-testid="perc-categories-section">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h2>{message(WF_ADMIN_MSG.CATEGORIES_TITLE)}</h2>

        {/* Lock Controls */}
        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          {lockInfo && lockInfo.userName ? (
            <>
              <span style={{ fontSize: "14px", color: isLockedByOther ? "#ef4444" : "#15803d" }} data-testid="lock-banner">
                {message(WF_ADMIN_MSG.LOCK_ACQUIRED_BY).replace("{0}", lockInfo.userName)}
              </span>
              {!isLockedByOther && (
                <button type="button" onClick={handleReleaseLock} className="perc-button-secondary" data-testid="release-lock-btn">
                  {message(WF_ADMIN_MSG.REMOVE_LOCK)}
                </button>
              )}
            </>
          ) : (
            <button type="button" onClick={handleAcquireLock} className="perc-button-primary" data-testid="acquire-lock-btn">
              {message(WF_ADMIN_MSG.LOCK_TAB)}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div style={{ color: "#d9534f", marginBottom: "16px" }} data-testid="categories-error">
          {error}
        </div>
      )}

      {/* Main Categories Tree list */}
      <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "20px", marginBottom: "20px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
          <span style={{ fontWeight: 600, color: "#475569" }}>Hierarchy Tree</span>
          {!isLockedByOther && (
            <button
              type="button"
              onClick={() => setAddChildParentId("root")}
              className="perc-button-primary"
              style={{ padding: "6px 12px", fontSize: "13px" }}
              data-testid="add-root-category"
            >
              + Add Root Category
            </button>
          )}
        </div>

        {tree && tree.topLevelNodes.length === 0 ? (
          <div style={{ padding: "20px", textAlign: "center", color: "#94a3b8" }}>No categories available.</div>
        ) : (
          tree?.topLevelNodes.map((node) => renderNode(node))
        )}
      </div>

      {/* Add node Dialog / Modal overlay */}
      {addChildParentId && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0,0,0,0.4)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 1100,
          }}
          data-testid="add-category-dialog"
        >
          <div style={{ background: "#fff", padding: "24px", borderRadius: "8px", width: "100%", maxWidth: "400px" }}>
            <h3 style={{ margin: "0 0 16px 0" }}>{message(WF_ADMIN_MSG.ADD_CATEGORY)}</h3>
            <div style={{ marginBottom: "16px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
                {message(WF_ADMIN_MSG.TITLE)}
              </label>
              <input
                type="text"
                value={newCategoryTitle}
                onChange={(e) => setNewCategoryTitle(e.target.value)}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="new-category-title-input"
              />
            </div>
            <div style={{ marginBottom: "20px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={newCategorySelectable}
                  onChange={(e) => setNewCategorySelectable(e.target.checked)}
                  data-testid="new-category-selectable-input"
                />
                {message(WF_ADMIN_MSG.SELECTABLE)}
              </label>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button type="button" onClick={() => setAddChildParentId(null)} className="perc-button-secondary">
                {message(WF_ADMIN_MSG.CANCEL)}
              </button>
              <button type="button" onClick={handleAddCategory} className="perc-button-primary" data-testid="save-category-btn">
                {message(WF_ADMIN_MSG.SAVE)}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
