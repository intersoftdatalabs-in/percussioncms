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

import React, { useState } from 'react';
import { message, MSG } from '../i18n/message';

interface AvailableGadget {
  id: string;
  name: string;
  /** TMX key for localized name; optional for tests that pass plain strings. */
  nameKey?: string;
  description?: string;
  /** TMX key for localized description; optional for tests. */
  descriptionKey?: string;
  category?: string;
}

export interface AddGadgetModalProps {
  isOpen: boolean;
  availableGadgets: AvailableGadget[];
  activeGadgetIds: Set<string>;
  onAdd: (gadgetId: string) => void;
  onClose: () => void;
}

/**
 * AddGadgetModal component for users to add new gadgets to their dashboard.
 * Displays available gadgets organized by category with descriptions.
 */
export const AddGadgetModal: React.FC<AddGadgetModalProps> = ({
  isOpen,
  availableGadgets,
  activeGadgetIds,
  onAdd,
  onClose,
}) => {
  const [searchTerm, setSearchTerm] = useState('');

  if (!isOpen) return null;

  const resolveName = (g: AvailableGadget): string =>
    g.nameKey ? message(g.nameKey) : g.name;
  const resolveDesc = (g: AvailableGadget): string => {
    const fromKey = g.descriptionKey ? message(g.descriptionKey) : '';
    return fromKey || g.description || '';
  };

  const lc = (s: string) => s.toLowerCase();
  const filteredGadgets = availableGadgets.filter((gadget) => {
    const term = lc(searchTerm);
    if (!term) return true;
    return (
      lc(resolveName(gadget)).includes(term) ||
      lc(resolveDesc(gadget)).includes(term)
    );
  });

  const groupedGadgets = filteredGadgets.reduce(
    (acc, gadget) => {
      const category = gadget.category || message(MSG.MODAL_DEFAULT_CATEGORY);
      if (!acc[category]) {
        acc[category] = [];
      }
      acc[category].push(gadget);
      return acc;
    },
    {} as Record<string, AvailableGadget[]>
  );

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      } as React.CSSProperties}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: 'white',
          borderRadius: '8px',
          width: '90%',
          maxWidth: '600px',
          maxHeight: '80vh',
          overflow: 'auto',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
        } as React.CSSProperties}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px',
            borderBottom: '1px solid #e0e0e0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          } as React.CSSProperties}
        >
          <h2 style={{ margin: 0, fontSize: '1.5em' }}>
            {message(MSG.MODAL_ADD_GADGET_TITLE)}
          </h2>
          <button
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5em',
              cursor: 'pointer',
              color: '#666',
            } as React.CSSProperties}
            onClick={onClose}
            aria-label={message(MSG.MODAL_ADD_GADGET_TITLE)}
          >
            ✕
          </button>
        </div>

        {/* Search */}
        <div style={{ padding: '16px 20px' } as React.CSSProperties}>
          <input
            type="text"
            placeholder={message(MSG.MODAL_SEARCH_PLACEHOLDER)}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 12px',
              fontSize: '0.95em',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              boxSizing: 'border-box',
            } as React.CSSProperties}
          />
        </div>

        {/* Gadgets List */}
        <div style={{ padding: '0 20px 20px' } as React.CSSProperties}>
          {Object.entries(groupedGadgets).length === 0 ? (
            <p
              style={{ color: '#999', textAlign: 'center', padding: '40px 0' }}
              data-testid="add-gadget-empty"
            >
              {message(MSG.MODAL_NO_RESULTS)}
            </p>
          ) : (
            Object.entries(groupedGadgets).map(([category, gadgets]) => (
              <div key={category} style={{ marginBottom: '20px' } as React.CSSProperties}>
                <h3 style={{ marginBottom: '12px', color: '#333', fontSize: '0.95em' }}>
                  {category}
                </h3>
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                  } as React.CSSProperties}
                >
                  {gadgets.map((gadget) => {
                    const isActive = activeGadgetIds.has(gadget.id);
                    const displayName = resolveName(gadget);
                    const displayDesc = resolveDesc(gadget);
                    return (
                      <div
                        key={gadget.id}
                        style={{
                          padding: '12px',
                          border: '1px solid #e0e0e0',
                          borderRadius: '4px',
                          backgroundColor: isActive ? '#f0f0f0' : 'white',
                          cursor: isActive ? 'default' : 'pointer',
                          transition: 'all 0.2s',
                          opacity: isActive ? 0.6 : 1,
                        } as React.CSSProperties}
                        onMouseEnter={(e) => {
                          if (!isActive) {
                            (e.currentTarget as HTMLDivElement).style.backgroundColor = '#f9f9f9';
                            (e.currentTarget as HTMLDivElement).style.borderColor = '#2196f3';
                          }
                        }}
                        onMouseLeave={(e) => {
                          (e.currentTarget as HTMLDivElement).style.backgroundColor = isActive ? '#f0f0f0' : 'white';
                          (e.currentTarget as HTMLDivElement).style.borderColor = '#e0e0e0';
                        }}
                      >
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'flex-start',
                          } as React.CSSProperties}
                        >
                          <div style={{ flex: 1 } as React.CSSProperties}>
                            <div style={{ fontWeight: '600', marginBottom: '4px', color: '#333' }}>
                              {displayName}
                            </div>
                            {displayDesc ? (
                              <div
                                style={{
                                  fontSize: '0.85em',
                                  color: '#666',
                                  lineHeight: '1.3',
                                }}
                              >
                                {displayDesc}
                              </div>
                            ) : null}
                          </div>
                          <button
                            onClick={() => onAdd(gadget.id)}
                            disabled={isActive}
                            style={{
                              marginLeft: '12px',
                              padding: '6px 12px',
                              backgroundColor: isActive ? '#ccc' : '#2196f3',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: isActive ? 'default' : 'pointer',
                              fontSize: '0.9em',
                              whiteSpace: 'nowrap',
                            } as React.CSSProperties}
                          >
                            {isActive
                              ? message(MSG.MODAL_ADDED_BUTTON)
                              : message(MSG.MODAL_ADD_BUTTON)}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default AddGadgetModal;
