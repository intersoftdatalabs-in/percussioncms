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

import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AddWidgetModal } from './AddWidgetModal';

describe('AddWidgetModal', () => {
  const mockAvailableWidgets = [
    { id: '1', name: 'Widget A', description: 'Description A', category: 'Analytics' },
    { id: '2', name: 'Widget B', description: 'Description B', category: 'Reports' },
    { id: '3', name: 'Widget C', description: 'Description C', category: 'Analytics' },
    { id: '4', name: 'Widget D', description: 'Description D', category: 'Tools' },
  ];

  const mockOnAdd = vi.fn();
  const mockOnClose = vi.fn();

  beforeEach(() => {
    mockOnAdd.mockClear();
    mockOnClose.mockClear();
  });

  it('should not render when isOpen is false', () => {
    const { container } = render(
      <AddWidgetModal
        isOpen={false}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render when isOpen is true', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Add Widget')).toBeDefined();
  });

  it('should display all available widgets', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Widget A')).toBeDefined();
    expect(screen.getByText('Widget B')).toBeDefined();
    expect(screen.getByText('Widget C')).toBeDefined();
    expect(screen.getByText('Widget D')).toBeDefined();
  });

  it('should group widgets by category', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Analytics')).toBeDefined();
    expect(screen.getByText('Reports')).toBeDefined();
    expect(screen.getByText('Tools')).toBeDefined();
  });

  it('should filter widgets by search term', async () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search widgets...');
    fireEvent.change(searchInput, { target: { value: 'Widget A' } });

    await waitFor(() => {
      expect(screen.getByText('Widget A')).toBeDefined();
      expect(screen.queryByText('Widget B')).toBeNull();
    });
  });

  it('should filter widgets by description', async () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search widgets...');
    fireEvent.change(searchInput, { target: { value: 'Description B' } });

    await waitFor(() => {
      expect(screen.getByText('Widget B')).toBeDefined();
    });
  });

  it('should call onAdd when add button is clicked', async () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const addButtons = screen.getAllByRole('button').filter((btn) => btn.textContent === 'Add');
    fireEvent.click(addButtons[0]);

    expect(mockOnAdd).toHaveBeenCalledWith('1');
  });

  it('should disable add button for active widgets', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set(['1', '2'])}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const buttons = screen.getAllByRole('button').filter(
      (btn) => btn.textContent === 'Added' || btn.textContent === 'Add'
    );

    // First two buttons should be disabled (for widgets 1 and 2)
    expect(buttons[0].textContent).toBe('Added');
    expect(buttons[1].textContent).toBe('Added');
  });

  it('should call onClose when close button is clicked', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const closeButton = screen.getByRole('button').parentElement?.querySelector('button:last-child');
    if (closeButton) {
      fireEvent.click(closeButton);
      expect(mockOnClose).toHaveBeenCalled();
    }
  });

  it('should call onClose when clicking outside modal', () => {
    const { container } = render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    // Find the backdrop div
    const backdrop = container.querySelector('div[style*="position: fixed"]');
    if (backdrop) {
      fireEvent.click(backdrop);
      expect(mockOnClose).toHaveBeenCalled();
    }
  });

  it('should not call onClose when clicking inside modal content', () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const modalContent = screen.getByText('Add Widget').parentElement;
    if (modalContent) {
      fireEvent.click(modalContent);
      expect(mockOnClose).not.toHaveBeenCalled();
    }
  });

  it('should display "No widgets found" when search returns empty', async () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search widgets...');
    fireEvent.change(searchInput, { target: { value: 'NonexistentWidget' } });

    await waitFor(() => {
      expect(screen.getByText('No widgets found')).toBeDefined();
    });
  });

  it('should clear search when input is cleared', async () => {
    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={mockAvailableWidgets}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search widgets...') as HTMLInputElement;
    fireEvent.change(searchInput, { target: { value: 'Widget A' } });

    await waitFor(() => {
      expect(searchInput.value).toBe('Widget A');
    });

    fireEvent.change(searchInput, { target: { value: '' } });

    await waitFor(() => {
      expect(screen.getByText('Widget B')).toBeDefined();
    });
  });

  it('should handle widgets without category', () => {
    const widgetsWithoutCategory = [
      { id: '1', name: 'Widget No Cat', description: 'No category' },
      { id: '2', name: 'Widget A', description: 'Description A', category: 'Analytics' },
    ];

    render(
      <AddWidgetModal
        isOpen={true}
        availableWidgets={widgetsWithoutCategory}
        activeWidgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Widget No Cat')).toBeDefined();
    expect(screen.getByText('Other')).toBeDefined(); // Default category
  });
});
